package us.slooker.moodpixels.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import us.slooker.moodpixels.data.db.MoodEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Handles Day (1 column), 3-Day (3 columns), and Week (7 columns) views.
 * Each column is one day; each row is one hour (0-23).
 */
class TimeGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : BaseCalendarView(context, attrs, defStyle) {

    var columnCount: Int = 1
        set(value) {
            field = value
            invalidate()
        }

    private val moodLabelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dpToPx(9f)
        style = android.graphics.Paint.Style.FILL
    }

    private val timeLabelWidth get() = dpToPx(52f)
    private val headerHeight get() = dpToPx(44f)
    private val rowHeight get() = dpToPx(52f)
    private val today = LocalDate.now()
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Total scrollable height for the time grid (24 rows) */
    val totalGridHeight get() = headerHeight + rowHeight * 24

    private var touchStartY = 0f
    private var isDragging = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val colWidth = (width - timeLabelWidth) / columnCount.toFloat()

        // Column header backgrounds
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, headerPaint)

        for (col in 0 until columnCount) {
            val date = anchorDate.plusDays(col.toLong())
            val dateStr = date.format(isoFormatter)
            val x = timeLabelWidth + col * colWidth

            // Highlight today's column
            if (date == today) {
                canvas.drawRect(x, 0f, x + colWidth, height.toFloat(), todayPaint)
            }

            // Column header text
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val dayNum = date.dayOfMonth.toString()
            val headerLabel = "$dayName $dayNum"
            val textX = x + colWidth / 2f - headerTextPaint.measureText(headerLabel) / 2f
            canvas.drawText(headerLabel, textX, headerHeight - dpToPx(10f), headerTextPaint)

            // Vertical divider
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)

            // Hour cells for this column
            val dayEntries = entriesMap[dateStr] ?: emptyMap()
            for (hour in 0..23) {
                val y = headerHeight + hour * rowHeight
                val entry = dayEntries[hour]
                if (entry != null) {
                    cellPaint.color = entry.colorValue
                    canvas.drawRect(x + 1f, y + 1f, x + colWidth - 1f, y + rowHeight - 1f, cellPaint)
                    // Mood label in cell
                    moodLabelPaint.color = contrastColor(entry.colorValue)
                    val label = entry.moodName.take(8)
                    val lx = x + colWidth / 2f - moodLabelPaint.measureText(label) / 2f
                    val ly = y + rowHeight / 2f + moodLabelPaint.textSize / 3f
                    canvas.drawText(label, lx, ly, moodLabelPaint)
                }
                // Horizontal grid line
                canvas.drawLine(timeLabelWidth, y, width.toFloat(), y, gridPaint)
            }
        }

        // Bottom grid line
        canvas.drawLine(timeLabelWidth, headerHeight + 24 * rowHeight, width.toFloat(), headerHeight + 24 * rowHeight, gridPaint)

        // Time labels column background
        cellPaint.color = Color.parseColor("#FAFAFA")
        canvas.drawRect(0f, headerHeight, timeLabelWidth, height.toFloat(), cellPaint)
        canvas.drawLine(timeLabelWidth, 0f, timeLabelWidth, height.toFloat(), gridPaint)

        // Time labels
        for (hour in 0..23) {
            val y = headerHeight + hour * rowHeight
            val label = if (hour == 0) "12am" else if (hour < 12) "${hour}am" else if (hour == 12) "12pm" else "${hour - 12}pm"
            canvas.drawText(label, dpToPx(4f), y + rowHeight / 2f + timeTextPaint.textSize / 3f, timeTextPaint)
            canvas.drawLine(0f, y, timeLabelWidth, y, gridPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (totalGridHeight).toInt()
        setMeasuredDimension(w, h)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartY = event.y
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (Math.abs(event.y - touchStartY) > dpToPx(8f)) {
                    isDragging = true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    handleTap(event.x, event.y)
                }
            }
        }
        return true
    }

    private fun handleTap(x: Float, y: Float) {
        if (x < timeLabelWidth || y < headerHeight) return
        val colWidth = (width - timeLabelWidth) / columnCount.toFloat()
        val col = ((x - timeLabelWidth) / colWidth).toInt().coerceIn(0, columnCount - 1)
        val row = ((y - headerHeight) / rowHeight).toInt().coerceIn(0, 23)
        val date = anchorDate.plusDays(col.toLong())
        val dateStr = date.format(isoFormatter)
        val existing = entriesMap[dateStr]?.get(row)
        onSlotClick?.invoke(dateStr, row, existing)
    }
}
