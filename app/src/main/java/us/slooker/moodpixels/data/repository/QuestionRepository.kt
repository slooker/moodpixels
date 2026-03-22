package us.slooker.moodpixels.data.repository

import kotlinx.coroutines.flow.Flow
import us.slooker.moodpixels.data.db.AppDatabase
import us.slooker.moodpixels.data.db.Question
import us.slooker.moodpixels.data.db.QuestionAnswer

class QuestionRepository(
    db: AppDatabase,
) {
    private val dao = db.questionDao()

    fun getAllQuestions(): Flow<List<Question>> = dao.getAllQuestions()

    suspend fun getActiveQuestions(): List<Question> = dao.getActiveQuestionsSnapshot()

    suspend fun getAllQuestionsSnapshot(): List<Question> = dao.getAllQuestionsSnapshot()

    suspend fun getById(id: Long): Question? = dao.getById(id)

    suspend fun save(question: Question): Long = dao.insert(question)

    suspend fun update(question: Question) = dao.update(question)

    suspend fun delete(question: Question) = dao.delete(question)

    suspend fun saveAnswer(answer: QuestionAnswer) = dao.insertAnswer(answer)

    suspend fun getAnswersForQuestion(questionId: Long): List<QuestionAnswer> = dao.getAnswersForQuestion(questionId)

    suspend fun getAllAnswers(): List<QuestionAnswer> = dao.getAllAnswersSnapshot()

    companion object {
        @Volatile private var instance: QuestionRepository? = null

        fun getInstance(db: AppDatabase): QuestionRepository =
            instance ?: synchronized(this) {
                instance ?: QuestionRepository(db).also { instance = it }
            }
    }
}
