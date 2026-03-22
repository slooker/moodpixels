package us.slooker.moodpixels.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** ISO date string "YYYY-MM-DD" */
    @ColumnInfo(name = "entry_date", index = true)
    val entryDate: String,
    /** Hour of day 0-23. Use -1 for a "whole day" entry from non-hour views. */
    @ColumnInfo(name = "hour_slot")
    val hourSlot: Int,
    /** Android @ColorInt value */
    @ColumnInfo(name = "color_value")
    val colorValue: Int,
    /** Mood name copied from legend at time of entry */
    @ColumnInfo(name = "mood_name")
    val moodName: String,
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
