package us.slooker.moodpixels.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.slooker.moodpixels.MoodPixelsApp
import us.slooker.moodpixels.data.db.MoodEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class MainViewModel(
    app: Application,
) : AndroidViewModel(app) {
    private val repo = (app as MoodPixelsApp).repository
    val legendPrefs = (app as MoodPixelsApp).legendPrefs

    val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _viewMode = MutableLiveData(CalendarViewMode.MONTH)
    val viewMode: LiveData<CalendarViewMode> = _viewMode

    private val _anchorDate = MutableLiveData(LocalDate.now())
    val anchorDate: LiveData<LocalDate> = _anchorDate

    /** Emits the current date range as a Pair<start, end> ISO strings */
    private val _dateRange = MutableLiveData(currentRange())
    val dateRange: LiveData<Pair<String, String>> = _dateRange

    /** Map of date-string -> (hourSlot -> MoodEntry) for fast View lookups */
    val entriesMap: LiveData<Map<String, Map<Int, MoodEntry>>> =
        _dateRange.switchMap { (start, end) ->
            repo
                .getEntriesForRange(start, end)
                .map { list ->
                    list
                        .groupBy { it.entryDate }
                        .mapValues { (_, entries) -> entries.associateBy { it.hourSlot } }
                }.asLiveData()
        }

    fun setViewMode(mode: CalendarViewMode) {
        _viewMode.value = mode
        if (mode == CalendarViewMode.WEEK) {
            _anchorDate.value = weekStart(_anchorDate.value ?: LocalDate.now())
        }
        refreshRange()
    }

    fun shiftPeriod(delta: Int) {
        val current = _anchorDate.value ?: LocalDate.now()
        _anchorDate.value =
            when (_viewMode.value) {
                CalendarViewMode.DAY -> current.plusDays(delta.toLong())
                CalendarViewMode.THREE_DAY -> current.plusDays(delta * 3L)
                CalendarViewMode.WEEK -> current.plusWeeks(delta.toLong())
                CalendarViewMode.MONTH -> current.plusMonths(delta.toLong())
                CalendarViewMode.YEAR -> current.plusYears(delta.toLong())
                null -> current
            }
        refreshRange()
    }

    fun goToToday() {
        val today = LocalDate.now()
        _anchorDate.value = if (_viewMode.value == CalendarViewMode.WEEK) weekStart(today) else today
        refreshRange()
    }

    fun setAnchorDate(date: LocalDate) {
        _anchorDate.value = if (_viewMode.value == CalendarViewMode.WEEK) weekStart(date) else date
        refreshRange()
    }

    fun upsertEntry(entry: MoodEntry) =
        viewModelScope.launch {
            repo.upsertEntry(entry)
        }

    fun deleteEntry(entry: MoodEntry) =
        viewModelScope.launch {
            repo.deleteEntry(entry)
        }

    fun deleteAll() =
        viewModelScope.launch {
            repo.deleteAll()
        }

    suspend fun getAllEntries() = repo.getAllEntries()

    fun getDateLabel(): String {
        val anchor = _anchorDate.value ?: LocalDate.now()
        return when (_viewMode.value) {
            CalendarViewMode.DAY -> anchor.format(DateTimeFormatter.ofPattern("EEEE, MMM d yyyy"))
            CalendarViewMode.THREE_DAY -> {
                val end = anchor.plusDays(2)
                "${anchor.format(DateTimeFormatter.ofPattern("MMM d"))} – ${end.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
            }
            CalendarViewMode.WEEK -> {
                val start = weekStart(anchor)
                val end = start.plusDays(6)
                "${start.format(DateTimeFormatter.ofPattern("MMM d"))} – ${end.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
            }
            CalendarViewMode.MONTH -> anchor.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            CalendarViewMode.YEAR -> anchor.format(DateTimeFormatter.ofPattern("yyyy"))
            null -> ""
        }
    }

    private fun refreshRange() {
        _dateRange.value = currentRange()
    }

    private fun currentRange(): Pair<String, String> {
        val anchor = _anchorDate.value ?: LocalDate.now()
        return when (_viewMode.value ?: CalendarViewMode.MONTH) {
            CalendarViewMode.DAY -> anchor.fmt() to anchor.fmt()
            CalendarViewMode.THREE_DAY -> anchor.fmt() to anchor.plusDays(2).fmt()
            CalendarViewMode.WEEK -> {
                val start = weekStart(anchor)
                start.fmt() to start.plusDays(6).fmt()
            }
            CalendarViewMode.MONTH -> {
                anchor.withDayOfMonth(1).fmt() to
                    anchor.withDayOfMonth(anchor.lengthOfMonth()).fmt()
            }
            CalendarViewMode.YEAR -> {
                LocalDate.of(anchor.year, 1, 1).fmt() to
                    LocalDate.of(anchor.year, 12, 31).fmt()
            }
        }
    }

    private fun weekStart(date: LocalDate): LocalDate =
        if (legendPrefs.getWeekStartsSunday()) {
            date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        } else {
            date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }

    private fun LocalDate.fmt() = format(formatter)
}

enum class CalendarViewMode { DAY, THREE_DAY, WEEK, MONTH, YEAR }
