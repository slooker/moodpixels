package us.slooker.moodpixels.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.slooker.moodpixels.MoodPixelsApp

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as MoodPixelsApp
                app.questionRepository.getActiveQuestions().forEach { question ->
                    AlarmScheduler.scheduleNext(context, question)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
