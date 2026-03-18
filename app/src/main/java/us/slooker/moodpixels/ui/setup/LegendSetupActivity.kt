package us.slooker.moodpixels.ui.setup

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import us.slooker.moodpixels.R
import us.slooker.moodpixels.data.model.LegendEntry
import us.slooker.moodpixels.ui.adapter.LegendAdapter
import us.slooker.moodpixels.ui.dialogs.ColorPickerDialog
import us.slooker.moodpixels.ui.main.MainActivity

class LegendSetupActivity : AppCompatActivity() {

    private val viewModel: LegendSetupViewModel by viewModels()
    private lateinit var adapter: LegendAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var doneButton: Button

    val isEditMode get() = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legend_setup)

        val titleView = findViewById<TextView>(R.id.setupTitle)
        val subtitleView = findViewById<TextView>(R.id.setupSubtitle)
        recyclerView = findViewById(R.id.legendRecyclerView)
        val addFab = findViewById<FloatingActionButton>(R.id.addMoodFab)
        doneButton = findViewById(R.id.doneButton)

        // Load existing entries for edit mode before creating the adapter
        if (isEditMode) {
            titleView.text = "Edit Your Legend"
            subtitleView.text = "Update your mood colors and names."
            doneButton.text = "Save Changes"
            viewModel.loadExisting()
        } else {
            subtitleView.text = "On this screen, choose a color and choose a mood to match that color. " +
                "We'll be using those colors as \"pixels\" to track your mood each hour, day, week or month. " +
                "You can add or remove as many colors/moods as you like."
        }

        // Give the adapter its own copy so it is the sole owner of this list
        val initialEntries = viewModel.entries.value?.toMutableList() ?: mutableListOf()
        adapter = LegendAdapter(
            entries = initialEntries,
            onColorClick = { index, current -> showColorPicker(index, current) },
            onRemove = { index ->
                // Adapter removes from its own list; ViewModel is not touched here
                adapter.removeAt(index)
                updateDoneButton()
            },
            onNameChanged = { _, _ ->
                updateDoneButton()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        addFab.setOnClickListener {
            val newEntry = LegendEntry(colorValue = Color.GRAY, moodName = "")
            adapter.addEntry(newEntry)
            recyclerView.scrollToPosition(adapter.itemCount - 1)
            updateDoneButton()
        }

        doneButton.setOnClickListener {
            val current = adapter.getCurrentEntries()
            if (isAdapterValid(current)) {
                viewModel.saveEntries(current)
                if (isEditMode) {
                    finish()
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }

        updateDoneButton()
    }

    private fun showColorPicker(index: Int, currentColor: Int) {
        val dialog = ColorPickerDialog.newInstance(currentColor)
        dialog.onColorSelected = { color ->
            adapter.updateColor(index, color)
        }
        dialog.show(supportFragmentManager, "color_picker")
    }

    private fun updateDoneButton() {
        doneButton.isEnabled = isAdapterValid(adapter.getCurrentEntries())
    }

    private fun isAdapterValid(entries: List<LegendEntry>): Boolean =
        entries.isNotEmpty() && entries.all { it.moodName.isNotBlank() }

    companion object {
        const val EXTRA_EDIT_MODE = "edit_mode"
    }
}
