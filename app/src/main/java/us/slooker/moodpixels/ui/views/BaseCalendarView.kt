package us.slooker.moodpixels.ui.views

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import us.slooker.moodpixels.data.db.MoodEntry
import java.time.LocalDate

abstract class BaseCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var anchorDate: LocalDate = LocalDate.now()
        set(value) {
            field = value
            invalidate()
        }

    /** Keyed by ISO date string -> (hourSlot -> MoodEntry) */
    var entriesMap: Map<String, Map<Int, MoodEntry>> = emptyMap()
        set(value) {
            field = value
            invalidate()
        }

    /** Called when a time slot is tapped. hour = -1 means "whole day" */
    var onSlotClick: ((date: String, hour: Int, existing: MoodEntry?) -> Unit)? = null

    protected val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    protected val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        textSize = 32f
        style = Paint.Style.FILL
    }

    protected val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    protected val todayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFF9C4")
        style = Paint.Style.FILL
    }

    protected val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5F5F5")
        style = Paint.Style.FILL
    }

    protected val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#555555")
        textSize = 30f
        isFakeBoldText = true
        style = Paint.Style.FILL
    }

    protected val timeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
        textSize = 26f
        style = Paint.Style.FILL
    }

    /** Returns contrast color (black or white) for a given background color */
    protected fun contrastColor(bgColor: Int): Int {
        val r = Color.red(bgColor) / 255.0
        val g = Color.green(bgColor) / 255.0
        val b = Color.blue(bgColor) / 255.0
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return if (luminance > 0.45) Color.BLACK else Color.WHITE
    }

    protected fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
}
