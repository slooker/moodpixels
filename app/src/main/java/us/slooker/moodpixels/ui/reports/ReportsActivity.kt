package us.slooker.moodpixels.ui.reports

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import us.slooker.moodpixels.MoodPixelsApp
import us.slooker.moodpixels.R
import us.slooker.moodpixels.data.db.MoodEntry
import us.slooker.moodpixels.data.db.QuestionAnswer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReportsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.reportsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val app = application as MoodPixelsApp
        lifecycleScope.launch {
            val moodEntries = app.repository.getAllEntries()
            val answers = app.questionRepository.getAllAnswers()
            val items = buildReportItems(moodEntries, answers)
            recyclerView.adapter = ReportAdapter(items)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.reports_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_share) {
            shareReport()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun shareReport() {
        val app = application as MoodPixelsApp
        lifecycleScope.launch {
            val entries = app.repository.getAllEntries()
            val answers = app.questionRepository.getAllAnswers()
            val text = buildReportText(entries, answers)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Mood Pixels Report")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Report"))
        }
    }

    private fun buildReportText(
        moodEntries: List<MoodEntry>,
        answers: List<QuestionAnswer>,
    ): String {
        val zone = ZoneId.systemDefault()
        val headerFmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
        val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        val exportedFmt = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())
        val divider = "─".repeat(36)

        val moodsByDate = moodEntries.groupBy { it.entryDate }
        val answersByDate = answers.groupBy { answer ->
            Instant.ofEpochMilli(answer.answeredAt).atZone(zone).toLocalDate().toString()
        }
        val allDates = (moodsByDate.keys + answersByDate.keys)
            .toSortedSet(compareByDescending { it })

        return buildString {
            appendLine("Mood Pixels Report")
            appendLine(LocalDateTime.now().format(exportedFmt))
            appendLine()
            for (dateStr in allDates) {
                appendLine(divider)
                appendLine(LocalDate.parse(dateStr).format(headerFmt))
                appendLine()
                moodsByDate[dateStr]
                    ?.sortedBy { it.hourSlot }
                    ?.forEach { entry ->
                        val timeLabel = when {
                            entry.hourSlot < 0 -> "All day"
                            entry.hourSlot == 0 -> "12am"
                            entry.hourSlot < 12 -> "${entry.hourSlot}am"
                            entry.hourSlot == 12 -> "12pm"
                            else -> "${entry.hourSlot - 12}pm"
                        }
                        appendLine("  $timeLabel  •  ${entry.moodName}")
                        entry.note?.takeIf { it.isNotBlank() }?.let { note ->
                            appendLine("       \"$note\"")
                        }
                    }
                answersByDate[dateStr]
                    ?.sortedBy { it.answeredAt }
                    ?.forEach { answer ->
                        val answerStr = when {
                            answer.answerText != null -> answer.answerText
                            answer.answerBool != null -> if (answer.answerBool) "Yes" else "No"
                            answer.answerNumber != null -> {
                                val n = answer.answerNumber
                                if (n == n.toLong().toDouble()) n.toLong().toString() else n.toString()
                            }
                            else -> "—"
                        }
                        val time = Instant.ofEpochMilli(answer.answeredAt)
                            .atZone(zone).toLocalTime().format(timeFmt)
                        appendLine()
                        appendLine("  ${answer.questionText}")
                        appendLine("  $answerStr  ·  $time")
                    }
                appendLine()
            }
        }.trimEnd()
    }

    private fun buildReportItems(
        moodEntries: List<MoodEntry>,
        answers: List<QuestionAnswer>,
    ): List<ReportItem> {
        val zone = ZoneId.systemDefault()

        val moodsByDate = moodEntries.groupBy { it.entryDate }
        val answersByDate =
            answers.groupBy { answer ->
                Instant
                    .ofEpochMilli(answer.answeredAt)
                    .atZone(zone)
                    .toLocalDate()
                    .toString()
            }

        val allDates =
            (moodsByDate.keys + answersByDate.keys)
                .toSortedSet(compareByDescending { it })

        return buildList {
            for (dateStr in allDates) {
                val date = LocalDate.parse(dateStr)
                add(ReportItem.DayHeader(date))
                moodsByDate[dateStr]
                    ?.sortedBy { it.hourSlot }
                    ?.forEach { add(ReportItem.MoodRow(it)) }
                answersByDate[dateStr]
                    ?.sortedBy { it.answeredAt }
                    ?.forEach { add(ReportItem.AnswerRow(it)) }
            }
        }
    }
}
