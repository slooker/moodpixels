package us.slooker.moodpixels.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import us.slooker.moodpixels.data.db.MoodEntry
import us.slooker.moodpixels.data.db.Question
import us.slooker.moodpixels.data.db.QuestionAnswer
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object JsonExporter {

    private val dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    private val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    // ── Output data classes ──────────────────────────────────────────────────

    data class ExportRoot(
        val exportedAt: String,
        val moodEntries: List<MoodEntryRecord>,
        val questions: List<QuestionRecord>
    )

    data class MoodEntryRecord(
        val date: String,
        val hour: Int?,
        val color: String,
        val mood: String,
        val note: String?
    )

    data class QuestionRecord(
        val id: Long,
        val text: String,
        val answerType: String,
        val scheduleType: String,
        val scheduleDays: List<String>,
        val notifyTime: String,
        val isActive: Boolean,
        val answers: List<AnswerRecord>
    )

    data class AnswerRecord(
        val answeredAt: String,
        val value: Any?
    )

    // ── Public API ───────────────────────────────────────────────────────────

    fun buildShareIntent(
        context: Context,
        moodEntries: List<MoodEntry>,
        questions: List<Question>,
        answers: List<QuestionAnswer>
    ): Intent {
        val answersByQuestion = answers.groupBy { it.questionId }

        val root = ExportRoot(
            exportedAt = LocalDateTime.now().format(dtFormatter),
            moodEntries = moodEntries.map { entry ->
                MoodEntryRecord(
                    date = entry.entryDate,
                    hour = if (entry.hourSlot >= 0) entry.hourSlot else null,
                    color = colorIntToHex(entry.colorValue),
                    mood = entry.moodName,
                    note = entry.note?.ifBlank { null }
                )
            },
            questions = questions.map { q ->
                val qAnswers = answersByQuestion[q.id] ?: emptyList()
                QuestionRecord(
                    id = q.id,
                    text = q.text,
                    answerType = q.answerType,
                    scheduleType = q.scheduleType,
                    scheduleDays = if (q.scheduleType == "DAILY") dayNames
                    else dayNames.filterIndexed { i, _ -> q.scheduleDays and (1 shl i) != 0 },
                    notifyTime = "%d:%02d".format(q.notifyHour, q.notifyMinute),
                    isActive = q.isActive,
                    answers = qAnswers.map { a ->
                        AnswerRecord(
                            answeredAt = epochToDateTime(a.answeredAt),
                            value = when (q.answerType) {
                                "YES_NO" -> a.answerBool
                                "NUMBER" -> a.answerNumber
                                else -> a.answerText
                            }
                        )
                    }
                )
            }
        )

        val json = GsonBuilder().setPrettyPrinting().create().toJson(root)

        val exportDir = File(context.cacheDir, "exports")
        exportDir.mkdirs()
        val file = File(exportDir, "mood_pixels_export.json")
        file.writeText(json)

        val uri = FileProvider.getUriForFile(
            context, "us.slooker.moodpixels.fileprovider", file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Mood Pixels Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun colorIntToHex(color: Int) =
        String.format("#%02X%02X%02X", Color.red(color), Color.green(color), Color.blue(color))

    private fun epochToDateTime(millis: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
            .format(dtFormatter)
}
