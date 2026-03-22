package us.slooker.moodpixels.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "text") val text: String,
    /** "TEXT", "YES_NO", or "NUMBER" */
    @ColumnInfo(name = "answer_type") val answerType: String = "TEXT",
    /** "DAILY" or "SPECIFIC_DAYS" */
    @ColumnInfo(name = "schedule_type") val scheduleType: String = "DAILY",
    /** Bitmask: bit 0=Mon … bit 6=Sun. 127 = every day. */
    @ColumnInfo(name = "schedule_days") val scheduleDays: Int = 127,
    @ColumnInfo(name = "notify_hour") val notifyHour: Int = 9,
    @ColumnInfo(name = "notify_minute") val notifyMinute: Int = 0,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
