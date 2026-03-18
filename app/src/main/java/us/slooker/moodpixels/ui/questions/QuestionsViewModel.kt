package us.slooker.moodpixels.ui.questions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import us.slooker.moodpixels.MoodPixelsApp
import us.slooker.moodpixels.data.db.Question
import us.slooker.moodpixels.notifications.AlarmScheduler

class QuestionsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as MoodPixelsApp).questionRepository
    val questions = repo.getAllQuestions().asLiveData()

    fun save(question: Question) = viewModelScope.launch {
        val id = if (question.id == 0L) repo.save(question) else { repo.update(question); question.id }
        val saved = repo.getById(id) ?: return@launch
        AlarmScheduler.scheduleNext(getApplication(), saved)
    }

    fun toggleActive(question: Question) = viewModelScope.launch {
        val updated = question.copy(isActive = !question.isActive)
        repo.update(updated)
        if (updated.isActive) {
            AlarmScheduler.scheduleNext(getApplication(), updated)
        } else {
            AlarmScheduler.cancel(getApplication(), updated.id)
        }
    }

    fun delete(question: Question) = viewModelScope.launch {
        AlarmScheduler.cancel(getApplication(), question.id)
        repo.delete(question)
    }
}
