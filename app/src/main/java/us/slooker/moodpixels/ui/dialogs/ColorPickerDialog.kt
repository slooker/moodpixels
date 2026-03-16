package us.slooker.moodpixels.ui.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import us.slooker.moodpixels.R

class ColorPickerDialog : DialogFragment() {

    private var selectedColor: Int = Color.RED
    var onColorSelected: ((Int) -> Unit)? = null

    companion object {
        private val PRESET_COLORS = listOf(
            0xFFE53935.toInt(), // Red
            0xFFFF7043.toInt(), // Deep Orange
            0xFFFB8C00.toInt(), // Orange
            0xFFFFB300.toInt(), // Amber
            0xFFFFEE58.toInt(), // Yellow
            0xFF9CCC65.toInt(), // Light Green
            0xFF43A047.toInt(), // Green
            0xFF26A69A.toInt(), // Teal
            0xFF29B6F6.toInt(), // Light Blue
            0xFF1E88E5.toInt(), // Blue
            0xFF3949AB.toInt(), // Indigo
            0xFF7E57C2.toInt(), // Deep Purple
            0xFFAB47BC.toInt(), // Purple
            0xFFEC407A.toInt(), // Pink
            0xFF8D6E63.toInt(), // Brown
            0xFF78909C.toInt(), // Blue Grey
            0xFF546E7A.toInt(), // Dark Blue Grey
            0xFF212121.toInt(), // Near Black
        )

        fun newInstance(currentColor: Int): ColorPickerDialog {
            return ColorPickerDialog().apply {
                selectedColor = currentColor
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_color_picker, null)
        val grid = view.findViewById<GridLayout>(R.id.colorGrid)
        val hexInput = view.findViewById<EditText>(R.id.hexInput)
        val preview = view.findViewById<ImageView>(R.id.colorPreview)

        updatePreview(preview, selectedColor)
        hexInput.setText(colorToHex(selectedColor))

        // Populate preset grid
        PRESET_COLORS.forEach { color ->
            val swatch = View(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 80; height = 80
                    setMargins(6, 6, 6, 6)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
                setOnClickListener {
                    selectedColor = color
                    updatePreview(preview, selectedColor)
                    hexInput.setText(colorToHex(selectedColor))
                }
            }
            grid.addView(swatch)
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Pick a Color")
            .setView(view)
            .setPositiveButton("OK") { _, _ ->
                val hexText = hexInput.text.toString().trim()
                if (hexText.isNotEmpty()) {
                    try {
                        val parsed = Color.parseColor(if (hexText.startsWith("#")) hexText else "#$hexText")
                        onColorSelected?.invoke(parsed)
                    } catch (e: IllegalArgumentException) {
                        onColorSelected?.invoke(selectedColor)
                    }
                } else {
                    onColorSelected?.invoke(selectedColor)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun updatePreview(view: ImageView, color: Int) {
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(2, Color.parseColor("#CCCCCC"))
        }
    }

    private fun colorToHex(color: Int) =
        String.format("#%02X%02X%02X", Color.red(color), Color.green(color), Color.blue(color))
}
