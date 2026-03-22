package us.slooker.moodpixels.ui.reports

import android.os.Bundle
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
import java.time.ZoneId

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
