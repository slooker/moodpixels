package us.slooker.moodpixels.export

import android.content.Context
import android.graphics.Color
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import us.slooker.moodpixels.MoodPixelsApp
import us.slooker.moodpixels.data.db.MoodEntry
import us.slooker.moodpixels.data.db.Question
import us.slooker.moodpixels.data.db.QuestionAnswer
import us.slooker.moodpixels.notifications.AlarmScheduler
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object JsonImporter {

    private val dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    private val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    data class ImportResult(
        val moodEntriesImported: Int,
        val questionsImported: Int,
        val answersImported: Int
    )

    sealed class ImportOutcome {
        data class Success(val result: ImportResult) : ImportOutcome()
        data class Failure(val message: String) : ImportOutcome()
    }

    suspend fun importFromUri(context: Context, uri: Uri): ImportOutcome {
        val json = try {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.readText()
                ?: return ImportOutcome.Failure("Could not open file")
        } catch (e: Exception) {
            return ImportOutcome.Failure("Could not read file: ${e.message}")
        }

        val root: JsonExporter.ExportRoot = try {
            Gson().fromJson(json, JsonExporter.ExportRoot::class.java)
        } catch (e: JsonSyntaxException) {
            return ImportOutcome.Failure("File is not a valid MoodPixels export")
        } ?: return ImportOutcome.Failure("File is empty or unreadable")

        val app = context.applicationContext as MoodPixelsApp
        val moodRepo = app.repository
        val questionRepo = app.questionRepository

        var moodCount = 0
        var questionCount = 0
        var answerCount = 0

        // ── Mood entries ──────────────────────────────────────────────────────
        root.moodEntries.forEach { record ->
            try {
                val entry = MoodEntry(
                    entryDate = record.date,
                    hourSlot = record.hour ?: -1,
                    colorValue = Color.parseColor(record.color),
                    moodName = record.mood,
                    note = record.note,
                    createdAt = System.currentTimeMillis()
                )
                moodRepo.upsertEntry(entry)
                moodCount++
            } catch (_: Exception) { /* skip malformed entries */ }
        }

        // ── Questions + answers ───────────────────────────────────────────────
        root.questions.forEach { qRecord ->
            try {
                val daysMask = if (qRecord.scheduleType == "DAILY") 127
                else qRecord.scheduleDays.fold(0) { acc, name ->
                    val bit = dayNames.indexOf(name)
                    if (bit >= 0) acc or (1 shl bit) else acc
                }

                val timeParts = qRecord.notifyTime.split(":")
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()

                val question = Question(
                    text = qRecord.text,
                    answerType = qRecord.answerType,
                    scheduleType = qRecord.scheduleType,
                    scheduleDays = daysMask,
                    notifyHour = hour,
                    notifyMinute = minute,
                    isActive = qRecord.isActive,
                    createdAt = System.currentTimeMillis()
                )
                val newId = questionRepo.save(question)
                questionCount++

                val savedQuestion = questionRepo.getById(newId)
                if (savedQuestion != null && savedQuestion.isActive) {
                    AlarmScheduler.scheduleNext(context, savedQuestion)
                }

                // Import answers, remapping to new question ID
                qRecord.answers.forEach { aRecord ->
                    try {
                        val answer = QuestionAnswer(
                            questionId = newId,
                            questionText = qRecord.text,
                            answerText = if (qRecord.answerType == "TEXT") aRecord.value?.toString() else null,
                            answerBool = if (qRecord.answerType == "YES_NO") aRecord.value as? Boolean else null,
                            answerNumber = if (qRecord.answerType == "NUMBER") (aRecord.value as? Double) else null,
                            answeredAt = parseDateTime(aRecord.answeredAt.toString())
                        )
                        questionRepo.saveAnswer(answer)
                        answerCount++
                    } catch (_: Exception) { }
                }
            } catch (_: Exception) { }
        }

        return ImportOutcome.Success(ImportResult(moodCount, questionCount, answerCount))
    }

    private fun parseDateTime(dt: String): Long {
        return try {
            LocalDateTime.parse(dt, dtFormatter)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: DateTimeParseException) {
            System.currentTimeMillis()
        }
    }
}
