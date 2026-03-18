package us.slooker.moodpixels.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.slooker.moodpixels.MoodPixelsApp

class QuestionAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val questionId = intent.getLongExtra(NotificationHelper.EXTRA_QUESTION_ID, -1L)
        if (questionId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as MoodPixelsApp
                val question = app.questionRepository.getById(questionId) ?: return@launch
                if (!question.isActive) return@launch

                NotificationHelper.postNotification(context, question)
                AlarmScheduler.scheduleNext(context, question)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
