package us.slooker.moodpixels.ui.answer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import us.slooker.moodpixels.MoodPixelsApp
import us.slooker.moodpixels.data.db.Question
import us.slooker.moodpixels.data.db.QuestionAnswer

class AnswerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as MoodPixelsApp).questionRepository
    val question = MutableLiveData<Question?>()
    private var questionId: Long = -1L

    fun load(id: Long) {
        questionId = id
        viewModelScope.launch {
            question.postValue(repo.getById(id))
        }
    }

    fun saveTextAnswer(text: String) = save(answerText = text)
    fun saveBoolAnswer(value: Boolean) = save(answerBool = value)
    fun saveNumberAnswer(value: Double) = save(answerNumber = value)

    private fun save(
        answerText: String? = null,
        answerBool: Boolean? = null,
        answerNumber: Double? = null
    ) {
        val q = question.value ?: return
        viewModelScope.launch {
            repo.saveAnswer(
                QuestionAnswer(
                    questionId = questionId,
                    questionText = q.text,
                    answerText = answerText,
                    answerBool = answerBool,
                    answerNumber = answerNumber
                )
            )
        }
    }
}
