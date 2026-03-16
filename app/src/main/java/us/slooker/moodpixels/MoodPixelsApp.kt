package us.slooker.moodpixels

import android.app.Application
import us.slooker.moodpixels.data.db.AppDatabase
import us.slooker.moodpixels.data.prefs.LegendPrefs
import us.slooker.moodpixels.data.repository.MoodRepository

class MoodPixelsApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { MoodRepository.getInstance(database) }
    val legendPrefs by lazy { LegendPrefs.getInstance(this) }
}
