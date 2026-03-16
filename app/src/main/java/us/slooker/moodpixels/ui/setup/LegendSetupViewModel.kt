package us.slooker.moodpixels.ui.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import us.slooker.moodpixels.MoodPixelsApp
import us.slooker.moodpixels.data.model.LegendEntry
import us.slooker.moodpixels.data.prefs.LegendPrefs

class LegendSetupViewModel(app: Application) : AndroidViewModel(app) {

    private val legendPrefs = (app as MoodPixelsApp).legendPrefs
    val entries = MutableLiveData<MutableList<LegendEntry>>(mutableListOf())

    fun loadExisting() {
        entries.value = legendPrefs.getLegend().toMutableList()
    }

    fun addEntry(entry: LegendEntry) {
        entries.value = entries.value?.apply { add(entry) }
    }

    fun updateEntry(index: Int, entry: LegendEntry) {
        entries.value = entries.value?.apply { set(index, entry) }
    }

    fun removeEntry(index: Int) {
        entries.value = entries.value?.apply { removeAt(index) }
    }

    fun saveAndComplete() {
        val list = entries.value ?: return
        legendPrefs.saveLegend(list)
        legendPrefs.setSetupComplete(true)
    }

    fun isValid(): Boolean {
        val list = entries.value ?: return false
        return list.isNotEmpty() && list.all { it.moodName.isNotBlank() }
    }
}
