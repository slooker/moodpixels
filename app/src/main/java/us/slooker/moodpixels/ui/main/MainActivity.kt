package us.slooker.moodpixels.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import us.slooker.moodpixels.MoodPixelsApp
import us.slooker.moodpixels.R
import us.slooker.moodpixels.data.db.MoodEntry
import us.slooker.moodpixels.ui.dialogs.MoodEntryDialog
import us.slooker.moodpixels.ui.questions.QuestionsActivity
import us.slooker.moodpixels.ui.reports.ReportsActivity
import us.slooker.moodpixels.ui.settings.AboutActivity
import us.slooker.moodpixels.ui.settings.HelpActivity
import us.slooker.moodpixels.ui.settings.ManageDataActivity
import us.slooker.moodpixels.ui.settings.SettingsActivity
import us.slooker.moodpixels.ui.setup.LegendSetupActivity
import us.slooker.moodpixels.ui.views.MonthView
import us.slooker.moodpixels.ui.views.TimeGridView
import us.slooker.moodpixels.ui.views.YearView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var calendarContainer: FrameLayout
    private lateinit var dateLabel: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var drawerLayout: DrawerLayout

    private var currentTimeGridView: TimeGridView? = null
    private var currentMonthView: MonthView? = null
    private var currentYearView: YearView? = null
    private var suppressTabCallback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as MoodPixelsApp
        if (!app.legendPrefs.isSetupComplete()) {
            startActivity(Intent(this, LegendSetupActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navigationView)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.nav_drawer_open,
            R.string.nav_drawer_close,
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_reports -> startActivity(Intent(this, ReportsActivity::class.java))
                R.id.nav_help -> startActivity(Intent(this, HelpActivity::class.java))
                R.id.nav_edit_legend -> startActivity(
                    Intent(this, LegendSetupActivity::class.java).putExtra(LegendSetupActivity.EXTRA_EDIT_MODE, true)
                )
                R.id.nav_questions -> startActivity(Intent(this, QuestionsActivity::class.java))
                R.id.nav_data -> startActivity(Intent(this, ManageDataActivity::class.java))
                R.id.nav_about -> startActivity(Intent(this, AboutActivity::class.java))
                R.id.nav_coffee -> startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/featurecreeplabs"))
                )
                R.id.nav_display -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        calendarContainer = findViewById(R.id.calendarContainer)
        dateLabel = findViewById(R.id.dateLabel)
        tabLayout = findViewById(R.id.tabLayout)

        val prevBtn = findViewById<ImageButton>(R.id.prevButton)
        val nextBtn = findViewById<ImageButton>(R.id.nextButton)
        val todayBtn = findViewById<TextView>(R.id.todayButton)

        prevBtn.setOnClickListener { viewModel.shiftPeriod(-1) }
        nextBtn.setOnClickListener { viewModel.shiftPeriod(1) }
        todayBtn.setOnClickListener { viewModel.goToToday() }

        setupTabs()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Re-create the calendar view in case week-start setting changed in Settings
        switchCalendarView(viewModel.viewMode.value ?: CalendarViewMode.MONTH)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupTabs() {
        listOf("Day", "3-Day", "Week", "Month", "Year")
            .forEach { tabLayout.addTab(tabLayout.newTab().setText(it)) }

        tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (suppressTabCallback) return
                    val mode =
                        when (tab.position) {
                            0 -> CalendarViewMode.DAY
                            1 -> CalendarViewMode.THREE_DAY
                            2 -> CalendarViewMode.WEEK
                            3 -> CalendarViewMode.MONTH
                            4 -> CalendarViewMode.YEAR
                            else -> CalendarViewMode.MONTH
                        }
                    viewModel.setViewMode(mode)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}

                override fun onTabReselected(tab: TabLayout.Tab) {}
            },
        )

        // Default to Month view (index 3)
        tabLayout.getTabAt(3)?.select()
        viewModel.setViewMode(CalendarViewMode.MONTH)
    }

    private fun observeViewModel() {
        viewModel.viewMode.observe(this) { mode ->
            switchCalendarView(mode)
            dateLabel.text = viewModel.getDateLabel()
        }

        viewModel.anchorDate.observe(this) { date ->
            currentTimeGridView?.anchorDate = date
            currentMonthView?.anchorDate = date
            currentYearView?.anchorDate = date
            dateLabel.text = viewModel.getDateLabel()
        }

        viewModel.entriesMap.observe(this) { map ->
            currentTimeGridView?.entriesMap = map
            currentMonthView?.entriesMap = map
            currentYearView?.entriesMap = map
        }
    }

    private fun switchCalendarView(mode: CalendarViewMode) {
        // Remove old views from container except the persistent ScrollView
        calendarContainer.removeAllViews()
        currentTimeGridView = null
        currentMonthView = null
        currentYearView = null

        val entriesMap = viewModel.entriesMap.value ?: emptyMap()
        val anchor = viewModel.anchorDate.value ?: LocalDate.now()

        when (mode) {
            CalendarViewMode.DAY, CalendarViewMode.THREE_DAY, CalendarViewMode.WEEK -> {
                val timeGrid =
                    TimeGridView(this).apply {
                        columnCount =
                            when (mode) {
                                CalendarViewMode.DAY -> 1
                                CalendarViewMode.THREE_DAY -> 3
                                else -> 7
                            }
                        textScale = (application as MoodPixelsApp).legendPrefs.getTextScale()
                        anchorDate = anchor
                        this.entriesMap = entriesMap
                        onSlotClick = { date, hour, existing ->
                            showMoodEntryDialog(date, hour, existing)
                        }
                    }
                currentTimeGridView = timeGrid

                val scroll =
                    ScrollView(this).apply {
                        layoutParams =
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT,
                            )
                        addView(timeGrid)
                    }
                calendarContainer.addView(scroll)
            }

            CalendarViewMode.MONTH -> {
                val monthView =
                    MonthView(this).apply {
                        layoutParams =
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT,
                            )
                        weekStartsSunday = (application as MoodPixelsApp).legendPrefs.getWeekStartsSunday()
                        anchorDate = anchor
                        this.entriesMap = entriesMap
                        onSlotClick = { date, hour, _ ->
                            if (hour == -1) navigateToDayView(date)
                        }
                    }
                currentMonthView = monthView
                calendarContainer.addView(monthView)
            }

            CalendarViewMode.YEAR -> {
                val yearView =
                    YearView(this).apply {
                        layoutParams =
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT,
                            )
                        weekStartsSunday = (application as MoodPixelsApp).legendPrefs.getWeekStartsSunday()
                        anchorDate = anchor
                        this.entriesMap = entriesMap
                        onSlotClick = { date, hour, _ ->
                            if (hour == -2) navigateToMonthView(date)
                        }
                    }
                currentYearView = yearView
                val scroll =
                    ScrollView(this).apply {
                        layoutParams =
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT,
                            )
                        addView(yearView)
                    }
                calendarContainer.addView(scroll)
            }
        }
    }

    private fun navigateToDayView(dateStr: String) {
        val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        viewModel.setAnchorDate(date)
        suppressTabCallback = true
        tabLayout.getTabAt(0)?.select()
        suppressTabCallback = false
        viewModel.setViewMode(CalendarViewMode.DAY)
    }

    private fun navigateToMonthView(dateStr: String) {
        val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        viewModel.setAnchorDate(date)
        suppressTabCallback = true
        tabLayout.getTabAt(3)?.select()
        suppressTabCallback = false
        viewModel.setViewMode(CalendarViewMode.MONTH)
    }

    private fun showMoodEntryDialog(
        date: String,
        hour: Int,
        existing: MoodEntry?,
    ) {
        val legend = (application as MoodPixelsApp).legendPrefs.getLegend()
        if (legend.isEmpty()) return

        MoodEntryDialog()
            .apply {
                this.date = date
                this.hour = hour
                this.existingEntry = existing
                this.legendEntries = legend
                onSave = { entry -> viewModel.upsertEntry(entry) }
                onDelete = { entry -> viewModel.deleteEntry(entry) }
            }.show(supportFragmentManager, "mood_entry")
    }

}
