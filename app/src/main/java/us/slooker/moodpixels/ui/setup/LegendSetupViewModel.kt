package us.slooker.moodpixels.ui.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import us.slooker.moodpixels.MoodPixelsApp
import us.slooker.moodpixels.data.model.LegendEntry

class LegendSetupViewModel(app: Application) : AndroidViewModel(app) {

    private val legendPrefs = (app as MoodPixelsApp).legendPrefs

    /** Initial entries loaded from prefs; adapter owns the live list after this. */
    val entries = MutableLiveData<MutableList<LegendEntry>>(mutableListOf())

    fun loadExisting() {
        entries.value = legendPrefs.getLegend().toMutableList()
    }

    fun saveEntries(list: List<LegendEntry>) {
        legendPrefs.saveLegend(list)
        legendPrefs.setSetupComplete(true)
    }
}
