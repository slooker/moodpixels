package us.slooker.moodpixels.data.prefs

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import us.slooker.moodpixels.data.model.LegendEntry

class LegendPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("mood_pixels_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun isSetupComplete(): Boolean = prefs.getBoolean(KEY_SETUP_COMPLETE, false)

    fun setSetupComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, complete).apply()
    }

    fun getLegend(): List<LegendEntry> {
        val json = prefs.getString(KEY_LEGEND, null) ?: return emptyList()
        val type = object : TypeToken<List<LegendEntry>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun saveLegend(entries: List<LegendEntry>) {
        prefs.edit().putString(KEY_LEGEND, gson.toJson(entries)).apply()
    }

    companion object {
        private const val KEY_SETUP_COMPLETE = "legend_setup_complete"
        private const val KEY_LEGEND = "legend_entries"

        @Volatile private var INSTANCE: LegendPrefs? = null

        fun getInstance(context: Context): LegendPrefs =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LegendPrefs(context.applicationContext).also { INSTANCE = it }
            }
    }
}
