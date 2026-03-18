package us.slooker.moodpixels.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import us.slooker.moodpixels.R
import us.slooker.moodpixels.data.db.Question
import us.slooker.moodpixels.ui.answer.AnswerActivity

object NotificationHelper {

    const val CHANNEL_ID = "questions_channel"
    const val EXTRA_QUESTION_ID = "question_id"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mood Check-ins",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to answer your scheduled mood questions"
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun postNotification(context: Context, question: Question) {
        val answerIntent = Intent(context, AnswerActivity::class.java).apply {
            putExtra(EXTRA_QUESTION_ID, question.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            question.id.toInt(),
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Mood check-in")
            .setContentText(question.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(question.text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            NotificationManagerCompat.from(context).notify(question.id.toInt(), notification)
        }
    }

    fun cancelNotification(context: Context, questionId: Long) {
        NotificationManagerCompat.from(context).cancel(questionId.toInt())
    }
}
