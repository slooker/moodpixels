package us.slooker.moodpixels.data.repository

import kotlinx.coroutines.flow.Flow
import us.slooker.moodpixels.data.db.AppDatabase
import us.slooker.moodpixels.data.db.MoodEntry

class MoodRepository(
    db: AppDatabase,
) {
    private val dao = db.moodEntryDao()

    fun getEntriesForDate(date: String): Flow<List<MoodEntry>> = dao.getEntriesForDate(date)

    fun getEntriesForRange(
        start: String,
        end: String,
    ): Flow<List<MoodEntry>> = dao.getEntriesForRange(start, end)

    suspend fun getAllEntries(): List<MoodEntry> = dao.getAllEntriesSnapshot()

    suspend fun upsertEntry(entry: MoodEntry) = dao.upsert(entry)

    suspend fun deleteEntry(entry: MoodEntry) = dao.delete(entry)

    suspend fun deleteAll() = dao.deleteAll()

    companion object {
        @Volatile private var instance: MoodRepository? = null

        fun getInstance(db: AppDatabase): MoodRepository =
            instance ?: synchronized(this) {
                instance ?: MoodRepository(db).also { instance = it }
            }
    }
}
