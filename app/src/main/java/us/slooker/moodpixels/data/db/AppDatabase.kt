package us.slooker.moodpixels.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MoodEntry::class, Question::class, QuestionAnswer::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun moodEntryDao(): MoodEntryDao

    abstract fun questionDao(): QuestionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `questions` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `text` TEXT NOT NULL,
                            `answer_type` TEXT NOT NULL,
                            `schedule_type` TEXT NOT NULL,
                            `schedule_days` INTEGER NOT NULL DEFAULT 127,
                            `notify_hour` INTEGER NOT NULL,
                            `notify_minute` INTEGER NOT NULL,
                            `is_active` INTEGER NOT NULL DEFAULT 1,
                            `created_at` INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `question_answers` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `question_id` INTEGER NOT NULL,
                            `question_text` TEXT NOT NULL,
                            `answer_text` TEXT,
                            `answer_bool` INTEGER,
                            `answer_number` REAL,
                            `answered_at` INTEGER NOT NULL,
                            FOREIGN KEY(`question_id`) REFERENCES `questions`(`id`) ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_question_answers_question_id` ON `question_answers` (`question_id`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_question_answers_answered_at` ON `question_answers` (`answered_at`)")
                }
            }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "mood_pixels.db",
                    ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
