package us.slooker.moodpixels.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY created_at ASC")
    fun getAllQuestions(): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE is_active = 1 ORDER BY created_at ASC")
    suspend fun getActiveQuestionsSnapshot(): List<Question>

    @Query("SELECT * FROM questions ORDER BY created_at ASC")
    suspend fun getAllQuestionsSnapshot(): List<Question>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: Long): Question?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: Question): Long

    @Update
    suspend fun update(question: Question)

    @Delete
    suspend fun delete(question: Question)

    @Insert
    suspend fun insertAnswer(answer: QuestionAnswer): Long

    @Query("SELECT * FROM question_answers WHERE question_id = :questionId ORDER BY answered_at DESC")
    suspend fun getAnswersForQuestion(questionId: Long): List<QuestionAnswer>

    @Query("SELECT * FROM question_answers ORDER BY answered_at DESC")
    suspend fun getAllAnswersSnapshot(): List<QuestionAnswer>
}
