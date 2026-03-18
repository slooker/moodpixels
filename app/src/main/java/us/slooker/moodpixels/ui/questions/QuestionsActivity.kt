package us.slooker.moodpixels.ui.questions

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import us.slooker.moodpixels.R
import us.slooker.moodpixels.data.db.Question

class QuestionsActivity : AppCompatActivity() {

    private val viewModel: QuestionsViewModel by viewModels()
    private lateinit var adapter: QuestionAdapter

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* proceed regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_questions)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        requestNotificationPermissionIfNeeded()
        requestExactAlarmPermissionIfNeeded()

        adapter = QuestionAdapter(
            onToggle = { question -> viewModel.toggleActive(question) },
            onEdit = { question -> showEditDialog(question) },
            onDelete = { question -> confirmDelete(question) }
        )

        val recycler = findViewById<RecyclerView>(R.id.questionsRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        viewModel.questions.observe(this) { adapter.submitList(it) }

        findViewById<FloatingActionButton>(R.id.addQuestionFab).setOnClickListener {
            showEditDialog(null)
        }
    }

    private fun showEditDialog(existing: Question?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_question, null)
        val textInput = view.findViewById<EditText>(R.id.questionTextInput)
        val answerTypeGroup = view.findViewById<RadioGroup>(R.id.answerTypeGroup)
        val scheduleGroup = view.findViewById<RadioGroup>(R.id.scheduleTypeGroup)
        val daysContainer = view.findViewById<android.widget.LinearLayout>(R.id.daysContainer)
        val timeDisplay = view.findViewById<TextView>(R.id.timeDisplay)
        val dayCheckboxIds = listOf(
            R.id.cbMon, R.id.cbTue, R.id.cbWed, R.id.cbThu,
            R.id.cbFri, R.id.cbSat, R.id.cbSun
        )

        var selectedHour = existing?.notifyHour ?: 9
        var selectedMinute = existing?.notifyMinute ?: 0

        fun updateTimeDisplay() {
            val amPm = if (selectedHour < 12) "AM" else "PM"
            val h = if (selectedHour == 0) 12 else if (selectedHour > 12) selectedHour - 12 else selectedHour
            timeDisplay.text = "%d:%02d %s".format(h, selectedMinute, amPm)
        }

        existing?.let {
            textInput.setText(it.text)
            when (it.answerType) {
                "YES_NO" -> answerTypeGroup.check(R.id.radioYesNo)
                "NUMBER" -> answerTypeGroup.check(R.id.radioNumber)
                else -> answerTypeGroup.check(R.id.radioText)
            }
            if (it.scheduleType == "SPECIFIC_DAYS") {
                scheduleGroup.check(R.id.radioSpecificDays)
                daysContainer.visibility = android.view.View.VISIBLE
                dayCheckboxIds.forEachIndexed { i, id ->
                    view.findViewById<CheckBox>(id).isChecked = (it.scheduleDays and (1 shl i)) != 0
                }
            }
        }

        updateTimeDisplay()

        scheduleGroup.setOnCheckedChangeListener { _, id ->
            daysContainer.visibility =
                if (id == R.id.radioSpecificDays) android.view.View.VISIBLE else android.view.View.GONE
        }

        timeDisplay.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                selectedHour = h; selectedMinute = m; updateTimeDisplay()
            }, selectedHour, selectedMinute, false).show()
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add Question" else "Edit Question")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val text = textInput.text.toString().trim()
                if (text.isEmpty()) return@setPositiveButton

                val answerType = when (answerTypeGroup.checkedRadioButtonId) {
                    R.id.radioYesNo -> "YES_NO"
                    R.id.radioNumber -> "NUMBER"
                    else -> "TEXT"
                }
                val scheduleType = if (scheduleGroup.checkedRadioButtonId == R.id.radioSpecificDays)
                    "SPECIFIC_DAYS" else "DAILY"

                var daysMask = 127
                if (scheduleType == "SPECIFIC_DAYS") {
                    daysMask = dayCheckboxIds.foldIndexed(0) { i, acc, id ->
                        if (view.findViewById<CheckBox>(id).isChecked) acc or (1 shl i) else acc
                    }
                    if (daysMask == 0) daysMask = 127 // fallback to daily if nothing selected
                }

                val question = (existing ?: Question(text = "")).copy(
                    text = text,
                    answerType = answerType,
                    scheduleType = scheduleType,
                    scheduleDays = daysMask,
                    notifyHour = selectedHour,
                    notifyMinute = selectedMinute
                )
                viewModel.save(question)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(question: Question) {
        AlertDialog.Builder(this)
            .setTitle("Delete Question")
            .setMessage("Delete \"${question.text}\"? All answers will also be deleted.")
            .setPositiveButton("Delete") { _, _ -> viewModel.delete(question) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("Enable precise notifications")
                    .setMessage(
                        "For questions to notify you at the exact time you set, " +
                        "please allow MoodPixels to schedule exact alarms in the next screen."
                    )
                    .setPositiveButton("Open Settings") { _, _ ->
                        startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:$packageName")
                            )
                        )
                    }
                    .setNegativeButton("Not now", null)
                    .show()
            }
        }
    }
}
