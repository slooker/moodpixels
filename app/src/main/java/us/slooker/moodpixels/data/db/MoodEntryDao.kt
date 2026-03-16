package us.slooker.moodpixels.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodEntryDao {

    @Query("SELECT * FROM mood_entries WHERE entry_date = :date ORDER BY hour_slot ASC")
    fun getEntriesForDate(date: String): Flow<List<MoodEntry>>

    @Query("SELECT * FROM mood_entries WHERE entry_date BETWEEN :start AND :end ORDER BY entry_date ASC, hour_slot ASC")
    fun getEntriesForRange(start: String, end: String): Flow<List<MoodEntry>>

    @Query("SELECT * FROM mood_entries ORDER BY entry_date ASC, hour_slot ASC")
    suspend fun getAllEntriesSnapshot(): List<MoodEntry>

    @Query("DELETE FROM mood_entries WHERE entry_date = :date AND hour_slot = :hour")
    suspend fun deleteSlot(date: String, hour: Int)

    @Insert
    suspend fun insert(entry: MoodEntry): Long

    @Transaction
    suspend fun upsert(entry: MoodEntry) {
        deleteSlot(entry.entryDate, entry.hourSlot)
        insert(entry)
    }

    @Delete
    suspend fun delete(entry: MoodEntry)

    @Query("DELETE FROM mood_entries")
    suspend fun deleteAll()
}
