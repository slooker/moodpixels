package us.slooker.moodpixels.ui.settings

import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import us.slooker.moodpixels.MoodPixelsApp
import us.slooker.moodpixels.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val app = application as MoodPixelsApp

        val themeGroup = findViewById<RadioGroup>(R.id.themeGroup)
        when (app.legendPrefs.getThemeMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> themeGroup.check(R.id.radioThemeLight)
            AppCompatDelegate.MODE_NIGHT_YES -> themeGroup.check(R.id.radioThemeDark)
            else -> themeGroup.check(R.id.radioThemeSystem)
        }
        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode =
                when (checkedId) {
                    R.id.radioThemeLight -> AppCompatDelegate.MODE_NIGHT_NO
                    R.id.radioThemeDark -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            app.legendPrefs.setThemeMode(mode)
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        val textSizeGroup = findViewById<RadioGroup>(R.id.textSizeGroup)
        when (app.legendPrefs.getTextSizeIndex()) {
            1 -> textSizeGroup.check(R.id.radioTextLarge)
            2 -> textSizeGroup.check(R.id.radioTextXLarge)
            else -> textSizeGroup.check(R.id.radioTextNormal)
        }
        textSizeGroup.setOnCheckedChangeListener { _, checkedId ->
            val index =
                when (checkedId) {
                    R.id.radioTextLarge -> 1
                    R.id.radioTextXLarge -> 2
                    else -> 0
                }
            app.legendPrefs.setTextSizeIndex(index)
        }

        val weekStartGroup = findViewById<RadioGroup>(R.id.weekStartGroup)
        if (app.legendPrefs.getWeekStartsSunday()) {
            weekStartGroup.check(R.id.radioStartSunday)
        } else {
            weekStartGroup.check(R.id.radioStartMonday)
        }
        weekStartGroup.setOnCheckedChangeListener { _, checkedId ->
            app.legendPrefs.setWeekStartsSunday(checkedId == R.id.radioStartSunday)
        }
    }
}
