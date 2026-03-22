package us.slooker.moodpixels.ui.settings

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import us.slooker.moodpixels.MoodPixelsApp
import us.slooker.moodpixels.R
import us.slooker.moodpixels.export.JsonExporter
import us.slooker.moodpixels.export.JsonImporter
import us.slooker.moodpixels.ui.questions.QuestionsActivity
import us.slooker.moodpixels.ui.setup.LegendSetupActivity

class SettingsActivity : AppCompatActivity() {
    // Holds the serialized JSON while waiting for the user to pick a save location
    private var pendingExportJson: String? = null

    private val importFilePicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch {
                val outcome = JsonImporter.importFromUri(this@SettingsActivity, uri)
                when (outcome) {
                    is JsonImporter.ImportOutcome.Success -> {
                        val r = outcome.result
                        AlertDialog
                            .Builder(this@SettingsActivity)
                            .setTitle("Import complete")
                            .setMessage(
                                "Imported:\n" +
                                    "• ${r.legendEntriesAdded} new legend entries\n" +
                                    "• ${r.moodEntriesImported} mood entries\n" +
                                    "• ${r.questionsImported} questions\n" +
                                    "• ${r.answersImported} answers",
                            ).setPositiveButton("OK", null)
                            .show()
                    }
                    is JsonImporter.ImportOutcome.Failure -> {
                        AlertDialog
                            .Builder(this@SettingsActivity)
                            .setTitle("Import failed")
                            .setMessage(outcome.message)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
        }

    private val saveToFilePicker =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            val json = pendingExportJson ?: return@registerForActivityResult
            pendingExportJson = null
            if (uri == null) return@registerForActivityResult
            try {
                contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            } catch (e: Exception) {
                AlertDialog
                    .Builder(this)
                    .setTitle("Save failed")
                    .setMessage(e.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val app = application as MoodPixelsApp

        val themeGroup = findViewById<RadioGroup>(R.id.themeGroup)
        when (app.legendPrefs.getThemeMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> themeGroup.check(R.id.radioThemeLight)
            AppCompatDelegate.MODE_NIGHT_YES -> themeGroup.check(R.id.radioThemeDark)
            else -> themeGroup.check(R.id.radioThemeSystem)
        }
        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode =
                when (checkedId) {
                    R.id.radioThemeLight -> AppCompatDelegate.MODE_NIGHT_NO
                    R.id.radioThemeDark -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            app.legendPrefs.setThemeMode(mode)
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        val textSizeGroup = findViewById<RadioGroup>(R.id.textSizeGroup)
        when (app.legendPrefs.getTextSizeIndex()) {
            1 -> textSizeGroup.check(R.id.radioTextLarge)
            2 -> textSizeGroup.check(R.id.radioTextXLarge)
            else -> textSizeGroup.check(R.id.radioTextNormal)
        }
        textSizeGroup.setOnCheckedChangeListener { _, checkedId ->
            val index =
                when (checkedId) {
                    R.id.radioTextLarge -> 1
                    R.id.radioTextXLarge -> 2
                    else -> 0
                }
            app.legendPrefs.setTextSizeIndex(index)
        }

        val weekStartGroup = findViewById<RadioGroup>(R.id.weekStartGroup)
        if (app.legendPrefs.getWeekStartsSunday()) {
            weekStartGroup.check(R.id.radioStartSunday)
        } else {
            weekStartGroup.check(R.id.radioStartMonday)
        }
        weekStartGroup.setOnCheckedChangeListener { _, checkedId ->
            app.legendPrefs.setWeekStartsSunday(checkedId == R.id.radioStartSunday)
        }

        val editLegendBtn = findViewById<Button>(R.id.editLegendButton)
        val manageQuestionsBtn = findViewById<Button>(R.id.manageQuestionsButton)
        val importBtn = findViewById<Button>(R.id.importButton)
        val exportBtn = findViewById<Button>(R.id.exportButton)
        val deleteAllBtn = findViewById<Button>(R.id.deleteAllButton)
        val buyMeCoffeeBtn = findViewById<Button>(R.id.buyMeCoffeeButton)
        val legendPreview = findViewById<LinearLayout>(R.id.legendPreview)

        refreshLegendPreview(legendPreview)

        buyMeCoffeeBtn.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://buymeacoffee.com/featurecreeplabs")))
        }

        editLegendBtn.setOnClickListener {
            startActivity(
                Intent(this, LegendSetupActivity::class.java).apply {
                    putExtra(LegendSetupActivity.EXTRA_EDIT_MODE, true)
                },
            )
        }

        manageQuestionsBtn.setOnClickListener {
            startActivity(Intent(this, QuestionsActivity::class.java))
        }

        importBtn.setOnClickListener {
            importFilePicker.launch(arrayOf("application/json", "*/*"))
        }

        exportBtn.setOnClickListener {
            lifecycleScope.launch {
                val legend = app.legendPrefs.getLegend()
                val entries = app.repository.getAllEntries()
                val questions = app.questionRepository.getAllQuestionsSnapshot()
                val answers = app.questionRepository.getAllAnswers()
                if (entries.isEmpty() && questions.isEmpty()) {
                    AlertDialog
                        .Builder(this@SettingsActivity)
                        .setTitle("No Data")
                        .setMessage("You haven't logged any moods or questions yet.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@launch
                }

                AlertDialog
                    .Builder(this@SettingsActivity)
                    .setTitle("Export data")
                    .setItems(arrayOf("Share…", "Save to Files…")) { _, which ->
                        when (which) {
                            0 -> {
                                val shareIntent =
                                    JsonExporter.buildShareIntent(
                                        this@SettingsActivity,
                                        legend,
                                        entries,
                                        questions,
                                        answers,
                                    )
                                startActivity(Intent.createChooser(shareIntent, "Export Mood Data"))
                            }
                            1 -> {
                                pendingExportJson = JsonExporter.buildJson(legend, entries, questions, answers)
                                val timestamp =
                                    java.time.LocalDate
                                        .now()
                                        .toString()
                                saveToFilePicker.launch("mood_pixels_$timestamp.json")
                            }
                        }
                    }.show()
            }
        }

        deleteAllBtn.setOnClickListener {
            AlertDialog
                .Builder(this)
                .setTitle("Delete All Data")
                .setMessage("This will permanently delete all mood entries and question answers. This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        (application as MoodPixelsApp).repository.deleteAll()
                    }
                }.setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLegendPreview(findViewById(R.id.legendPreview))
    }

    private fun refreshLegendPreview(container: LinearLayout) {
        container.removeAllViews()
        val legend = (application as MoodPixelsApp).legendPrefs.getLegend()
        legend.forEach { entry ->
            val row = layoutInflater.inflate(R.layout.item_legend_preview, container, false)
            val swatch = row.findViewById<View>(R.id.previewSwatch)
            val label = row.findViewById<TextView>(R.id.previewLabel)
            (swatch.background as? GradientDrawable)?.setColor(entry.colorValue)
            label.text = entry.moodName
            container.addView(row)
        }
    }
}
