package us.slooker.moodpixels.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class YearView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
    ) : BaseCalendarView(context, attrs, defStyle) {
        private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val today = LocalDate.now()

        private val monthLabelPaint =
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#555555")
                isFakeBoldText = true
                style = android.graphics.Paint.Style.FILL
            }

        private val yearDayHeaderPaint =
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#888888")
                style = android.graphics.Paint.Style.FILL
            }

        var weekStartsSunday: Boolean = false
            set(value) {
                field = value
                invalidate()
            }

        // 4 columns x 3 rows of month blocks
        private val monthCols = 3
        private val monthRows = 4

        private val blockWidth get() = width / monthCols.toFloat()
        private val blockHeight get() = height / monthRows.toFloat()

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val w = MeasureSpec.getSize(widthMeasureSpec)
            setMeasuredDimension(w, (w * 1.4f).toInt())
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val year = anchorDate.year
            val dark = isDarkMode

            monthLabelPaint.color = if (dark) Color.parseColor("#BBBBBB") else Color.parseColor("#555555")
            yearDayHeaderPaint.color = if (dark) Color.parseColor("#AAAAAA") else Color.parseColor("#888888")

            for (monthIdx in 0..11) {
                val month = monthIdx + 1
                val monthDate = LocalDate.of(year, month, 1)
                val col = monthIdx % monthCols
                val row = monthIdx / monthCols

                val bx = col * blockWidth
                val by = row * blockHeight

                // Month label
                val monthLabel = monthDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                monthLabelPaint.textSize = dpToPx(11f)
                canvas.drawText(monthLabel, bx + dpToPx(4f), by + dpToPx(14f), monthLabelPaint)

                // Mini day-of-week headers
                val headerH = dpToPx(16f)
                val labelH = dpToPx(16f)
                val gridTop = by + labelH + headerH
                val gridWidth = blockWidth - dpToPx(4f)
                val gridHeight = blockHeight - labelH - headerH - dpToPx(4f)
                val dayCellW = gridWidth / 7f
                val numWeekRows = 6
                val dayCellH = gridHeight / numWeekRows

                // Day header row (S M T W T F S  or  M T W T F S S)
                yearDayHeaderPaint.textSize = dpToPx(7f)
                val dayLetters =
                    if (weekStartsSunday) {
                        listOf("S", "M", "T", "W", "T", "F", "S")
                    } else {
                        listOf("M", "T", "W", "T", "F", "S", "S")
                    }
                dayLetters.forEachIndexed { i, d ->
                    canvas.drawText(
                        d,
                        bx + dpToPx(2f) + i * dayCellW + dayCellW / 2f - yearDayHeaderPaint.measureText(d) / 2f,
                        by + labelH + dpToPx(11f),
                        yearDayHeaderPaint,
                    )
                }

                // Sun=7→0, Mon=1, …, Sat=6 for Sunday-start; Mon=0, …, Sun=6 for Monday-start
                val startOffset =
                    if (weekStartsSunday) {
                        monthDate.dayOfWeek.value % 7
                    } else {
                        monthDate.dayOfWeek.value - 1
                    }
                val daysInMonth = monthDate.lengthOfMonth()

                for (d in 1..daysInMonth) {
                    val dayDate = LocalDate.of(year, month, d)
                    val dateStr = dayDate.format(isoFormatter)
                    val cellIdx = startOffset + d - 1
                    val cellCol = cellIdx % 7
                    val cellRow = cellIdx / 7

                    val cx = bx + dpToPx(2f) + cellCol * dayCellW
                    val cy = gridTop + cellRow * dayCellH

                    // Dominant color for this day
                    val dayEntries = entriesMap[dateStr]
                    if (!dayEntries.isNullOrEmpty()) {
                        val dominantColor =
                            dayEntries.values
                                .groupBy { it.colorValue }
                                .maxByOrNull { it.value.size }!!
                                .key
                        cellPaint.color = dominantColor
                    } else {
                        cellPaint.color =
                            if (dayDate == today) {
                                if (dark) Color.parseColor("#3D3000") else Color.parseColor("#FFF9C4")
                            } else {
                                if (dark) Color.parseColor("#1A1A1A") else Color.parseColor("#EEEEEE")
                            }
                    }
                    canvas.drawRect(cx, cy, cx + dayCellW - 1f, cy + dayCellH - 1f, cellPaint)
                }

                // Month block border
                gridPaint.color = if (dark) Color.parseColor("#444444") else Color.parseColor("#CCCCCC")
                canvas.drawRect(bx, by, bx + blockWidth, by + blockHeight, gridPaint)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                val col = (event.x / blockWidth).toInt().coerceIn(0, monthCols - 1)
                val row = (event.y / blockHeight).toInt().coerceIn(0, monthRows - 1)
                val monthIdx = row * monthCols + col
                val month = monthIdx + 1
                val date = LocalDate.of(anchorDate.year, month, 1)
                // -2 signals "navigate to month view"
                onSlotClick?.invoke(date.format(isoFormatter), -2, null)
            }
            return true
        }
    }
