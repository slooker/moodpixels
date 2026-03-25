package us.slooker.moodpixels.ui.reports

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import us.slooker.moodpixels.R
import us.slooker.moodpixels.data.db.MoodEntry
import us.slooker.moodpixels.data.db.QuestionAnswer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class ReportItem {
    data class DayHeader(
        val date: LocalDate,
    ) : ReportItem()

    data class MoodRow(
        val entry: MoodEntry,
    ) : ReportItem()

    data class AnswerRow(
        val answer: QuestionAnswer,
    ) : ReportItem()
}

class ReportAdapter(
    private val items: List<ReportItem>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_MOOD = 1
        private const val TYPE_ANSWER = 2
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is ReportItem.DayHeader -> TYPE_HEADER
            is ReportItem.MoodRow -> TYPE_MOOD
            is ReportItem.AnswerRow -> TYPE_ANSWER
        }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER ->
                HeaderHolder(
                    inflater.inflate(R.layout.item_report_header, parent, false),
                )
            TYPE_MOOD ->
                MoodHolder(
                    inflater.inflate(R.layout.item_report_mood, parent, false),
                )
            else ->
                AnswerHolder(
                    inflater.inflate(R.layout.item_report_answer, parent, false),
                )
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        when (val item = items[position]) {
            is ReportItem.DayHeader -> (holder as HeaderHolder).bind(item.date)
            is ReportItem.MoodRow -> (holder as MoodHolder).bind(item.entry)
            is ReportItem.AnswerRow -> (holder as AnswerHolder).bind(item.answer)
        }
    }

    class HeaderHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        private val text = view.findViewById<TextView>(R.id.dateHeaderText)
        private val fmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())

        fun bind(date: LocalDate) {
            text.text = date.format(fmt)
        }
    }

    class MoodHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        private val swatch = view.findViewById<View>(R.id.moodSwatch)
        private val name = view.findViewById<TextView>(R.id.moodName)
        private val time = view.findViewById<TextView>(R.id.moodTime)
        private val note = view.findViewById<TextView>(R.id.moodNote)

        fun bind(entry: MoodEntry) {
            (swatch.background as? GradientDrawable)?.setColor(entry.colorValue)
            name.text = entry.moodName
            time.text =
                if (entry.hourSlot < 0) {
                    ""
                } else {
                    val h = entry.hourSlot
                    when {
                        h == 0 -> "12am"
                        h < 12 -> "${h}am"
                        h == 12 -> "12pm"
                        else -> "${h - 12}pm"
                    }
                }
            val noteText = entry.note?.takeIf { it.isNotBlank() }
            if (noteText != null) {
                note.text = noteText
                note.visibility = View.VISIBLE
            } else {
                note.visibility = View.GONE
            }
        }
    }

    class AnswerHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        private val question = view.findViewById<TextView>(R.id.questionText)
        private val answer = view.findViewById<TextView>(R.id.answerText)
        private val timeFmt =
            DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

        fun bind(a: QuestionAnswer) {
            question.text = a.questionText
            val answerStr =
                when {
                    a.answerText != null -> a.answerText
                    a.answerBool != null -> if (a.answerBool) "Yes" else "No"
                    a.answerNumber != null ->
                        if (a.answerNumber == a.answerNumber.toLong().toDouble()) {
                            a.answerNumber.toLong().toString()
                        } else {
                            a.answerNumber.toString()
                        }
                    else -> "—"
                }
            val answeredTime =
                Instant
                    .ofEpochMilli(a.answeredAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                    .format(timeFmt)
            answer.text = "$answerStr  ·  $answeredTime"
        }
    }
}
