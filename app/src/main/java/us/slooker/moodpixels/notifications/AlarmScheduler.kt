package us.slooker.moodpixels.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import us.slooker.moodpixels.data.db.Question
import java.util.Calendar

object AlarmScheduler {

    private const val ACTION_QUESTION_ALARM = "us.slooker.moodpixels.QUESTION_ALARM"

    fun scheduleNext(context: Context, question: Question) {
        if (!question.isActive) return
        val triggerAt = nextTriggerMillis(question) ?: return
        val intent = buildIntent(context, question.id)
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        }
    }

    fun cancel(context: Context, questionId: Long) {
        val intent = buildIntent(context, questionId)
        context.getSystemService(AlarmManager::class.java).cancel(intent)
        intent.cancel()
    }

    /** Returns epoch millis of the next time this question should fire, or null if inactive. */
    fun nextTriggerMillis(question: Question): Long? {
        if (!question.isActive) return null

        val now = Calendar.getInstance()
        val candidate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, question.notifyHour)
            set(Calendar.MINUTE, question.notifyMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (question.scheduleType == "DAILY") {
            if (candidate.timeInMillis <= now.timeInMillis) {
                candidate.add(Calendar.DAY_OF_MONTH, 1)
            }
            return candidate.timeInMillis
        }

        // SPECIFIC_DAYS: find next day whose bit is set
        for (daysAhead in 0..7) {
            val checkCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, daysAhead)
                set(Calendar.HOUR_OF_DAY, question.notifyHour)
                set(Calendar.MINUTE, question.notifyMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (checkCal.timeInMillis <= now.timeInMillis) continue
            // Calendar.DAY_OF_WEEK: 1=Sun,2=Mon,...,7=Sat → bit index: Mon=0...Sun=6
            val calDow = checkCal.get(Calendar.DAY_OF_WEEK)
            val bit = when (calDow) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> continue
            }
            if (question.scheduleDays and (1 shl bit) != 0) {
                return checkCal.timeInMillis
            }
        }
        return null
    }

    private fun buildIntent(context: Context, questionId: Long): PendingIntent {
        val intent = Intent(context, QuestionAlarmReceiver::class.java).apply {
            action = ACTION_QUESTION_ALARM
            putExtra(NotificationHelper.EXTRA_QUESTION_ID, questionId)
        }
        return PendingIntent.getBroadcast(
            context,
            questionId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
