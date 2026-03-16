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
    private lateinit var doneButton: Button

    val isEditMode get() = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legend_setup)

        val titleView = findViewById<TextView>(R.id.setupTitle)
        val subtitleView = findViewById<TextView>(R.id.setupSubtitle)
        val recyclerView = findViewById<RecyclerView>(R.id.legendRecyclerView)
        val addFab = findViewById<FloatingActionButton>(R.id.addMoodFab)
        doneButton = findViewById(R.id.doneButton)

        if (isEditMode) {
            titleView.text = "Edit Your Legend"
            subtitleView.text = "Update your mood colors and names"
            doneButton.text = "Save Changes"
            viewModel.loadExisting()
        }

        val entries = viewModel.entries.value ?: mutableListOf()
        adapter = LegendAdapter(
            entries = entries,
            onColorClick = { index, current -> showColorPicker(index, current) },
            onRemove = { index ->
                viewModel.removeEntry(index)
                adapter.removeAt(index)
                updateDoneButton()
            },
            onNameChanged = { index, name ->
                viewModel.updateEntry(index, entries[index].copy(moodName = name))
                updateDoneButton()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        viewModel.entries.observe(this) { list ->
            updateDoneButton()
        }

        addFab.setOnClickListener {
            val newEntry = LegendEntry(colorValue = Color.GRAY, moodName = "")
            viewModel.addEntry(newEntry)
            adapter.addEntry(newEntry)
            recyclerView.scrollToPosition(adapter.itemCount - 1)
            updateDoneButton()
        }

        doneButton.setOnClickListener {
            if (viewModel.isValid()) {
                viewModel.saveAndComplete()
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
            viewModel.updateEntry(index, (viewModel.entries.value ?: mutableListOf())[index].copy(colorValue = color))
            adapter.updateColor(index, color)
        }
        dialog.show(supportFragmentManager, "color_picker")
    }

    private fun updateDoneButton() {
        doneButton.isEnabled = viewModel.isValid()
    }

    companion object {
        const val EXTRA_EDIT_MODE = "edit_mode"
    }
}
