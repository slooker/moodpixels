package us.slooker.moodpixels.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

class ManageDataActivity : AppCompatActivity() {
    private var pendingExportJson: String? = null

    private val importFilePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch {
                val outcome = JsonImporter.importFromUri(this@ManageDataActivity, uri)
                when (outcome) {
                    is JsonImporter.ImportOutcome.Success -> {
                        val r = outcome.result
                        AlertDialog.Builder(this@ManageDataActivity)
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
                        AlertDialog.Builder(this@ManageDataActivity)
                            .setTitle("Import failed")
                            .setMessage(outcome.message)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
        }

    private val saveToFilePicker =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            val json = pendingExportJson ?: return@registerForActivityResult
            pendingExportJson = null
            if (uri == null) return@registerForActivityResult
            try {
                contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            } catch (e: Exception) {
                AlertDialog.Builder(this)
                    .setTitle("Save failed")
                    .setMessage(e.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_data)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val app = application as MoodPixelsApp

        findViewById<Button>(R.id.importButton).setOnClickListener {
            importFilePicker.launch(arrayOf("application/json", "*/*"))
        }

        findViewById<Button>(R.id.exportButton).setOnClickListener {
            lifecycleScope.launch {
                val legend = app.legendPrefs.getLegend()
                val entries = app.repository.getAllEntries()
                val questions = app.questionRepository.getAllQuestionsSnapshot()
                val answers = app.questionRepository.getAllAnswers()
                if (entries.isEmpty() && questions.isEmpty()) {
                    AlertDialog.Builder(this@ManageDataActivity)
                        .setTitle("No Data")
                        .setMessage("You haven't logged any moods or questions yet.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@launch
                }
                AlertDialog.Builder(this@ManageDataActivity)
                    .setTitle("Export data")
                    .setItems(arrayOf("Share…", "Save to Files…")) { _, which ->
                        when (which) {
                            0 -> {
                                val shareIntent = JsonExporter.buildShareIntent(
                                    this@ManageDataActivity, legend, entries, questions, answers,
                                )
                                startActivity(Intent.createChooser(shareIntent, "Export Mood Data"))
                            }
                            1 -> {
                                pendingExportJson = JsonExporter.buildJson(legend, entries, questions, answers)
                                val timestamp = java.time.LocalDate.now().toString()
                                saveToFilePicker.launch("mood_pixels_$timestamp.json")
                            }
                        }
                    }.show()
            }
        }

        findViewById<Button>(R.id.deleteAllButton).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete All Data")
                .setMessage("This will permanently delete all mood entries and question answers. This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        app.repository.deleteAll()
                    }
                }.setNegativeButton("Cancel", null)
                .show()
        }
    }
}
