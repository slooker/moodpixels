package us.slooker.moodpixels.data.prefs

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import us.slooker.moodpixels.data.model.LegendEntry

class LegendPrefs(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("mood_pixels_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun isSetupComplete(): Boolean = prefs.getBoolean(KEY_SETUP_COMPLETE, false)

    fun setSetupComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, complete).apply()
    }

    fun getLegend(): List<LegendEntry> {
        val json = prefs.getString(KEY_LEGEND, null) ?: return defaultLegend()
        val type = object : TypeToken<List<LegendEntry>>() {}.type
        return gson.fromJson(json, type) ?: defaultLegend()
    }

    private fun defaultLegend(): List<LegendEntry> =
        listOf(
            LegendEntry(colorValue = android.graphics.Color.parseColor("#4CAF50"), moodName = "Happy"),
            LegendEntry(colorValue = android.graphics.Color.parseColor("#2196F3"), moodName = "Sad"),
            LegendEntry(colorValue = android.graphics.Color.parseColor("#F44336"), moodName = "Angry"),
            LegendEntry(colorValue = android.graphics.Color.parseColor("#FFEB3B"), moodName = "Meh"),
            LegendEntry(colorValue = android.graphics.Color.parseColor("#9C27B0"), moodName = "Excited"),
        )

    fun saveLegend(entries: List<LegendEntry>) {
        prefs.edit().putString(KEY_LEGEND, gson.toJson(entries)).apply()
    }

    fun getWeekStartsSunday(): Boolean = prefs.getBoolean(KEY_WEEK_STARTS_SUNDAY, false)

    fun setWeekStartsSunday(sunday: Boolean) {
        prefs.edit().putBoolean(KEY_WEEK_STARTS_SUNDAY, sunday).apply()
    }

    /** Returns one of AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM / NO / YES */
    fun getThemeMode(): Int = prefs.getInt(KEY_THEME_MODE, androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
    }

    /** 0 = Normal (1.0×), 1 = Large (1.25×), 2 = X-Large (1.5×) */
    fun getTextSizeIndex(): Int = prefs.getInt(KEY_TEXT_SIZE, 0)

    fun getTextScale(): Float =
        when (getTextSizeIndex()) {
            1 -> 1.25f
            2 -> 1.5f
            else -> 1.0f
        }

    fun setTextSizeIndex(index: Int) {
        prefs.edit().putInt(KEY_TEXT_SIZE, index).apply()
    }

    companion object {
        private const val KEY_SETUP_COMPLETE = "legend_setup_complete"
        private const val KEY_LEGEND = "legend_entries"
        private const val KEY_WEEK_STARTS_SUNDAY = "week_starts_sunday"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_TEXT_SIZE = "text_size_index"

        @Volatile private var instance: LegendPrefs? = null

        fun getInstance(context: Context): LegendPrefs =
            instance ?: synchronized(this) {
                instance ?: LegendPrefs(context.applicationContext).also { instance = it }
            }
    }
}
