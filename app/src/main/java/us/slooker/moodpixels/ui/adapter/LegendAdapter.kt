package us.slooker.moodpixels.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import us.slooker.moodpixels.R
import us.slooker.moodpixels.data.model.LegendEntry

class LegendAdapter(
    private val entries: MutableList<LegendEntry>,
    private val onColorClick: (index: Int, current: Int) -> Unit,
    private val onRemove: (index: Int) -> Unit,
    private val onNameChanged: (index: Int, name: String) -> Unit
) : RecyclerView.Adapter<LegendAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val colorSwatch: View = view.findViewById(R.id.colorSwatch)
        val nameInput: EditText = view.findViewById(R.id.moodNameInput)
        val removeBtn: ImageButton = view.findViewById(R.id.removeButton)
        var textWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_legend_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]

        (holder.colorSwatch.background as? GradientDrawable)?.setColor(entry.colorValue)
        holder.colorSwatch.setOnClickListener {
            onColorClick(holder.adapterPosition, entries[holder.adapterPosition].colorValue)
        }

        holder.textWatcher?.let { holder.nameInput.removeTextChangedListener(it) }
        holder.nameInput.setText(entry.moodName)
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onNameChanged(holder.adapterPosition, s?.toString() ?: "")
            }
        }
        holder.nameInput.addTextChangedListener(watcher)
        holder.textWatcher = watcher

        holder.removeBtn.setOnClickListener { onRemove(holder.adapterPosition) }
    }

    override fun getItemCount() = entries.size

    fun updateColor(index: Int, color: Int) {
        if (index in entries.indices) {
            entries[index] = entries[index].copy(colorValue = color)
            notifyItemChanged(index)
        }
    }

    fun updateName(index: Int, name: String) {
        if (index in entries.indices) {
            entries[index] = entries[index].copy(moodName = name)
        }
    }

    fun removeAt(index: Int) {
        if (index in entries.indices) {
            entries.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun addEntry(entry: LegendEntry) {
        entries.add(entry)
        notifyItemInserted(entries.size - 1)
    }
}
