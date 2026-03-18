package us.slooker.moodpixels.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
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

    private val dayHeaders = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    )

    private val headerHeight get() = dpToPx(36f)
    private val cellWidth get() = (width / 7).toFloat()
    private val cellHeight get() = dpToPx(84f)

    private val rowCount: Int
        get() {
            val firstDay = anchorDate.withDayOfMonth(1)
            val startOffset = firstDay.dayOfWeek.value - 1
            return Math.ceil((startOffset + anchorDate.lengthOfMonth()) / 7.0).toInt()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (headerHeight + rowCount * dpToPx(84f)).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cw = cellWidth
        val ch = cellHeight

        // Day-of-week header row
        headerPaint.color = Color.parseColor("#F5F5F5")
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, headerPaint)
        dayHeaders.forEachIndexed { i, dow ->
            val label = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val x = i * cw + cw / 2f - headerTextPaint.measureText(label) / 2f
            canvas.drawText(label, x, headerHeight - dpToPx(6f), headerTextPaint)
        }

        val firstDay = anchorDate.withDayOfMonth(1)
        val startOffset = firstDay.dayOfWeek.value - 1

        for (row in 0 until rowCount) {
            for (col in 0 until 7) {
                val dayIndex = row * 7 + col - startOffset
                val dayNum = dayIndex + 1
                if (dayNum < 1 || dayNum > anchorDate.lengthOfMonth()) continue

                val date = anchorDate.withDayOfMonth(dayNum)
                val dateStr = date.format(isoFormatter)
                val x = col * cw
                val y = headerHeight + row * ch

                // Cell background
                cellPaint.color = if (date == today) Color.parseColor("#FFF9C4") else Color.WHITE
                canvas.drawRect(x, y, x + cw, y + ch, cellPaint)

                // Day number
                dayNumPaint.color = if (date == today) Color.parseColor("#E53935") else Color.parseColor("#333333")
                dayNumPaint.textSize = dpToPx(12f)
                canvas.drawText(dayNum.toString(), x + dpToPx(4f), y + dpToPx(14f), dayNumPaint)

                // Mood pixel rows
                val dayEntries = entriesMap[dateStr]
                if (!dayEntries.isNullOrEmpty()) {
                    // Group by mood, count per mood, sort by count descending
                    val moodGroups = dayEntries.values
                        .groupBy { it.colorValue to it.moodName }
                        .map { (key, entries) -> Triple(key.first, key.second, entries.size) }
                        .sortedByDescending { it.third }

                    val pixelSize = dpToPx(9f)
                    val pixelGap = dpToPx(2f)
                    val rowGap = dpToPx(4f)
                    val cellPadding = dpToPx(4f)
                    val maxPixelsWide = ((cw - cellPadding * 2) / (pixelSize + pixelGap)).toInt()
                        .coerceAtLeast(1)
                    var rowY = y + dpToPx(19f)

                    for ((color, _, count) in moodGroups) {
                        // Stop if no room left in the cell
                        if (rowY + pixelSize > y + ch - cellPadding) break

                        val pixelsToShow = count.coerceAtMost(maxPixelsWide)
                        var pixelX = x + cellPadding
                        repeat(pixelsToShow) {
                            cellPaint.color = color
                            canvas.drawRect(pixelX, rowY, pixelX + pixelSize, rowY + pixelSize, cellPaint)
                            pixelX += pixelSize + pixelGap
                        }
                        rowY += pixelSize + rowGap
                    }
                }

                // Cell border
                canvas.drawRect(x, y, x + cw, y + ch, gridPaint)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val col = (event.x / cellWidth).toInt().coerceIn(0, 6)
            val row = ((event.y - headerHeight) / cellHeight).toInt()
            if (row < 0) return true

            val firstDay = anchorDate.withDayOfMonth(1)
            val startOffset = firstDay.dayOfWeek.value - 1
            val dayNum = row * 7 + col - startOffset + 1
            if (dayNum < 1 || dayNum > anchorDate.lengthOfMonth()) return true

            val date = anchorDate.withDayOfMonth(dayNum)
            onSlotClick?.invoke(date.format(isoFormatter), -1, null)
        }
        return true
    }
}
