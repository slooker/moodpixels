package us.slooker.moodpixels.ui.questions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import us.slooker.moodpixels.R
import us.slooker.moodpixels.data.db.Question

class QuestionAdapter(
    private val onToggle: (Question) -> Unit,
    private val onEdit: (Question) -> Unit,
    private val onDelete: (Question) -> Unit
) : ListAdapter<Question, QuestionAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.questionText)
        val schedule: TextView = view.findViewById(R.id.questionSchedule)
        val activeSwitch: Switch = view.findViewById(R.id.activeSwitch)
        val editBtn: ImageButton = view.findViewById(R.id.editButton)
        val deleteBtn: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_question, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val q = getItem(position)
        holder.text.text = q.text
        holder.schedule.text = buildScheduleLabel(q)

        holder.activeSwitch.setOnCheckedChangeListener(null)
        holder.activeSwitch.isChecked = q.isActive
        holder.activeSwitch.setOnCheckedChangeListener { _, _ -> onToggle(q) }

        holder.editBtn.setOnClickListener { onEdit(q) }
        holder.deleteBtn.setOnClickListener { onDelete(q) }
        holder.itemView.setOnClickListener { onEdit(q) }
    }

    private fun buildScheduleLabel(q: Question): String {
        val amPm = if (q.notifyHour < 12) "AM" else "PM"
        val h = if (q.notifyHour == 0) 12 else if (q.notifyHour > 12) q.notifyHour - 12 else q.notifyHour
        val time = "%d:%02d %s".format(h, q.notifyMinute, amPm)
        val days = if (q.scheduleType == "DAILY") "Daily" else {
            val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            names.filterIndexed { i, _ -> q.scheduleDays and (1 shl i) != 0 }.joinToString(", ")
        }
        return "$days at $time"
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Question>() {
            override fun areItemsTheSame(a: Question, b: Question) = a.id == b.id
            override fun areContentsTheSame(a: Question, b: Question) = a == b
        }
    }
}
