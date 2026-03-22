package us.slooker.moodpixels.ui.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import us.slooker.moodpixels.R
import us.slooker.moodpixels.data.db.MoodEntry
import us.slooker.moodpixels.data.model.LegendEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MoodEntryDialog : DialogFragment() {
    var date: String = ""
    var hour: Int = 0
    var existingEntry: MoodEntry? = null
    var legendEntries: List<LegendEntry> = emptyList()
    var onSave: ((MoodEntry) -> Unit)? = null
    var onDelete: ((MoodEntry) -> Unit)? = null

    private var selectedLegendEntry: LegendEntry? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_mood_entry, null)
        val titleView = view.findViewById<TextView>(R.id.entryTitle)
        val legendContainer = view.findViewById<LinearLayout>(R.id.legendContainer)
        val noteInput = view.findViewById<EditText>(R.id.noteInput)
        val deleteBtn = view.findViewById<View>(R.id.deleteButton)

        // Title
        val dateObj = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
        val dayName = dateObj.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val monthDay = dateObj.format(DateTimeFormatter.ofPattern("MMM d"))
        titleView.text =
            if (hour >= 0) {
                val amPm = if (hour < 12) "${if (hour == 0) 12 else hour}:00 AM" else "${if (hour == 12) 12 else hour - 12}:00 PM"
                "$dayName, $monthDay — $amPm"
            } else {
                "$dayName, $monthDay"
            }

        // Pre-select existing entry's legend item
        val existing = existingEntry
        if (existing != null) {
            selectedLegendEntry = legendEntries.find { it.colorValue == existing.colorValue }
                ?: legendEntries.firstOrNull()
            noteInput.setText(existing.note)
        } else {
            selectedLegendEntry = legendEntries.firstOrNull()
        }

        // Legend swatches
        legendEntries.forEach { legend ->
            val item =
                LayoutInflater
                    .from(requireContext())
                    .inflate(R.layout.item_legend_picker, legendContainer, false)
            val swatch = item.findViewById<View>(R.id.swatchView)
            val label = item.findViewById<TextView>(R.id.moodLabel)

            (swatch.background as? GradientDrawable)?.setColor(legend.colorValue)
            label.text = legend.moodName

            fun updateSelection(selected: LegendEntry) {
                selectedLegendEntry = selected
                // Reset all borders
                for (i in 0 until legendContainer.childCount) {
                    val child = legendContainer.getChildAt(i)
                    child.findViewById<View>(R.id.swatchView)?.background?.let { bg ->
                        (bg as? GradientDrawable)?.setStroke(0, Color.TRANSPARENT)
                    }
                }
                (swatch.background as? GradientDrawable)?.setStroke(
                    4,
                    if (isDarkColor(legend.colorValue)) Color.WHITE else Color.parseColor("#333333"),
                )
            }

            item.setOnClickListener { updateSelection(legend) }

            // Highlight the currently selected one
            if (legend == selectedLegendEntry) {
                (swatch.background as? GradientDrawable)?.setStroke(
                    4,
                    if (isDarkColor(legend.colorValue)) Color.WHITE else Color.parseColor("#333333"),
                )
            }

            legendContainer.addView(item)
        }

        // Delete button visibility
        deleteBtn.visibility = if (existing != null) View.VISIBLE else View.GONE
        deleteBtn.setOnClickListener {
            existing?.let { onDelete?.invoke(it) }
            dismiss()
        }

        return AlertDialog
            .Builder(requireContext())
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val legend = selectedLegendEntry ?: return@setPositiveButton
                val entry =
                    MoodEntry(
                        id = existingEntry?.id ?: 0,
                        entryDate = date,
                        hourSlot = hour,
                        colorValue = legend.colorValue,
                        moodName = legend.moodName,
                        note = noteInput.text.toString().ifBlank { null },
                    )
                onSave?.invoke(entry)
            }.setNegativeButton("Cancel", null)
            .create()
    }

    private fun isDarkColor(color: Int): Boolean {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) < 0.45
    }
}
