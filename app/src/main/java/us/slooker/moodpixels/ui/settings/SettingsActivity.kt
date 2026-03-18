package us.slooker.moodpixels.ui.settings

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

    private val importFilePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val outcome = JsonImporter.importFromUri(this@SettingsActivity, uri)
            when (outcome) {
                is JsonImporter.ImportOutcome.Success -> {
                    val r = outcome.result
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("Import complete")
                        .setMessage(
                            "Imported:\n" +
                            "• ${r.moodEntriesImported} mood entries\n" +
                            "• ${r.questionsImported} questions\n" +
                            "• ${r.answersImported} answers"
                        )
                        .setPositiveButton("OK", null)
                        .show()
                }
                is JsonImporter.ImportOutcome.Failure -> {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("Import failed")
                        .setMessage(outcome.message)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val editLegendBtn = findViewById<Button>(R.id.editLegendButton)
        val manageQuestionsBtn = findViewById<Button>(R.id.manageQuestionsButton)
        val importBtn = findViewById<Button>(R.id.importButton)
        val exportBtn = findViewById<Button>(R.id.exportButton)
        val deleteAllBtn = findViewById<Button>(R.id.deleteAllButton)
        val legendPreview = findViewById<LinearLayout>(R.id.legendPreview)

        refreshLegendPreview(legendPreview)

        editLegendBtn.setOnClickListener {
            startActivity(Intent(this, LegendSetupActivity::class.java).apply {
                putExtra(LegendSetupActivity.EXTRA_EDIT_MODE, true)
            })
        }

        manageQuestionsBtn.setOnClickListener {
            startActivity(Intent(this, QuestionsActivity::class.java))
        }

        importBtn.setOnClickListener {
            importFilePicker.launch(arrayOf("application/json", "*/*"))
        }

        exportBtn.setOnClickListener {
            val app = application as MoodPixelsApp
            lifecycleScope.launch {
                val entries = app.repository.getAllEntries()
                val questions = app.questionRepository.getAllQuestionsSnapshot()
                val answers = app.questionRepository.getAllAnswers()
                if (entries.isEmpty() && questions.isEmpty()) {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("No Data")
                        .setMessage("You haven't logged any moods or questions yet.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@launch
                }
                val shareIntent = JsonExporter.buildShareIntent(
                    this@SettingsActivity, entries, questions, answers
                )
                startActivity(Intent.createChooser(shareIntent, "Export Mood Data"))
            }
        }

        deleteAllBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete All Data")
                .setMessage("This will permanently delete all mood entries and question answers. This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        (application as MoodPixelsApp).repository.deleteAll()
                    }
                }
                .setNegativeButton("Cancel", null)
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
