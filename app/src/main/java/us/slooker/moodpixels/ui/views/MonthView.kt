package us.slooker.moodpixels.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import us.slooker.moodpixels.data.db.MoodEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MonthView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : BaseCalendarView(context, attrs, defStyle) {

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val today = LocalDate.now()

    private val dayNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        style = Paint.Style.FILL
    }

    private val dayHeaders = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    private val headerHeight get() = dpToPx(36f)
    private val cellSize get() = (width / 7).toFloat()

    /** rows needed for this month */
    private val rowCount: Int
        get() {
            val firstDay = anchorDate.withDayOfMonth(1)
            val startOffset = (firstDay.dayOfWeek.value - 1) // Monday=0
            val daysInMonth = anchorDate.lengthOfMonth()
            return Math.ceil((startOffset + daysInMonth) / 7.0).toInt()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, w)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cs = cellSize
        val numRows = rowCount

        // Day-of-week headers
        headerPaint.color = Color.parseColor("#F5F5F5")
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, headerPaint)
        dayHeaders.forEachIndexed { i, dow ->
            val label = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val x = i * cs + cs / 2f - headerTextPaint.measureText(label) / 2f
            canvas.drawText(label, x, headerHeight - dpToPx(6f), headerTextPaint)
        }

        val firstDay = anchorDate.withDayOfMonth(1)
        val startOffset = firstDay.dayOfWeek.value - 1 // Monday=0

        for (row in 0 until numRows) {
            for (col in 0 until 7) {
                val dayIndex = row * 7 + col - startOffset
                val dayNum = dayIndex + 1
                if (dayNum < 1 || dayNum > anchorDate.lengthOfMonth()) continue

                val date = anchorDate.withDayOfMonth(dayNum)
                val dateStr = date.format(isoFormatter)
                val x = col * cs
                val y = headerHeight + row * cs

                // Background
                if (date == today) {
                    todayPaint.color = Color.parseColor("#FFF9C4")
                    canvas.drawRect(x, y, x + cs, y + cs, todayPaint)
                } else {
                    cellPaint.color = Color.WHITE
                    canvas.drawRect(x, y, x + cs, y + cs, cellPaint)
                }

                // Draw mood pixels (micro grid: up to 24 pixels in 6x4)
                val dayEntries = entriesMap[dateStr]
                if (!dayEntries.isNullOrEmpty()) {
                    val pixelCols = 6
                    val pixelRows = 4
                    val padding = dpToPx(16f)
                    val pixW = (cs - padding * 2) / pixelCols
                    val pixH = (cs - padding * 2 - dpToPx(18f)) / pixelRows
                    var pIdx = 0
                    for (h in 0..23) {
                        val entry = dayEntries[h] ?: continue
                        if (pIdx >= pixelCols * pixelRows) break
                        val pc = pIdx % pixelCols
                        val pr = pIdx / pixelCols
                        val px = x + padding + pc * pixW
                        val py = y + dpToPx(18f) + pr * pixH
                        cellPaint.color = entry.colorValue
                        canvas.drawRect(px, py, px + pixW - 1f, py + pixH - 1f, cellPaint)
                        pIdx++
                    }
                }

                // Day number
                val dayLabel = dayNum.toString()
                dayNumPaint.color = if (date == today) Color.parseColor("#E53935") else Color.parseColor("#333333")
                canvas.drawText(dayLabel, x + dpToPx(4f), y + dpToPx(14f), dayNumPaint)

                // Grid lines
                canvas.drawRect(x, y, x + cs, y + cs, gridPaint)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val cs = cellSize
            val col = (event.x / cs).toInt().coerceIn(0, 6)
            val row = ((event.y - headerHeight) / cs).toInt()
            if (row < 0) return true

            val firstDay = anchorDate.withDayOfMonth(1)
            val startOffset = firstDay.dayOfWeek.value - 1
            val dayIndex = row * 7 + col - startOffset
            val dayNum = dayIndex + 1
            if (dayNum < 1 || dayNum > anchorDate.lengthOfMonth()) return true

            val date = anchorDate.withDayOfMonth(dayNum)
            val dateStr = date.format(isoFormatter)
            // -1 signals "navigate to day view" for this date
            onSlotClick?.invoke(dateStr, -1, null)
        }
        return true
    }
}
