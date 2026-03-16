package us.slooker.moodpixels.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import us.slooker.moodpixels.MoodPixelsApp
import us.slooker.moodpixels.R
import us.slooker.moodpixels.export.JsonExporter
import us.slooker.moodpixels.ui.setup.LegendSetupActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val editLegendBtn = findViewById<Button>(R.id.editLegendButton)
        val exportBtn = findViewById<Button>(R.id.exportButton)
        val deleteAllBtn = findViewById<Button>(R.id.deleteAllButton)
        val legendPreview = findViewById<LinearLayout>(R.id.legendPreview)

        refreshLegendPreview(legendPreview)

        editLegendBtn.setOnClickListener {
            startActivity(Intent(this, LegendSetupActivity::class.java).apply {
                putExtra(LegendSetupActivity.EXTRA_EDIT_MODE, true)
            })
        }

        exportBtn.setOnClickListener {
            val app = application as MoodPixelsApp
            lifecycleScope.launch {
                val entries = app.repository.getAllEntries()
                if (entries.isEmpty()) {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("No Data")
                        .setMessage("You haven't logged any moods yet.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@launch
                }
                val shareIntent = JsonExporter.buildShareIntent(this@SettingsActivity, entries)
                startActivity(Intent.createChooser(shareIntent, "Export Mood Data"))
            }
        }

        deleteAllBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete All Data")
                .setMessage("This will permanently delete all your mood entries. This cannot be undone.")
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
