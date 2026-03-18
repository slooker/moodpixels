package us.slooker.moodpixels.ui.answer

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import us.slooker.moodpixels.R
import us.slooker.moodpixels.notifications.NotificationHelper

class AnswerActivity : AppCompatActivity() {

    private val viewModel: AnswerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_answer)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val questionId = intent.getLongExtra(NotificationHelper.EXTRA_QUESTION_ID, -1L)
        if (questionId == -1L) { finish(); return }

        val questionText = findViewById<TextView>(R.id.questionText)
        val textLayout = findViewById<LinearLayout>(R.id.textAnswerLayout)
        val yesNoLayout = findViewById<LinearLayout>(R.id.yesNoLayout)
        val numberLayout = findViewById<LinearLayout>(R.id.numberLayout)
        val textInput = findViewById<EditText>(R.id.textAnswerInput)
        val numberInput = findViewById<EditText>(R.id.numberAnswerInput)
        val submitTextBtn = findViewById<Button>(R.id.submitTextButton)
        val submitNumberBtn = findViewById<Button>(R.id.submitNumberButton)
        val yesBtn = findViewById<Button>(R.id.yesButton)
        val noBtn = findViewById<Button>(R.id.noButton)

        viewModel.load(questionId)

        viewModel.question.observe(this) { question ->
            if (question == null) { finish(); return@observe }
            questionText.text = question.text

            textLayout.visibility = View.GONE
            yesNoLayout.visibility = View.GONE
            numberLayout.visibility = View.GONE

            when (question.answerType) {
                "YES_NO" -> yesNoLayout.visibility = View.VISIBLE
                "NUMBER" -> numberLayout.visibility = View.VISIBLE
                else -> textLayout.visibility = View.VISIBLE
            }
        }

        submitTextBtn.setOnClickListener {
            val text = textInput.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.saveTextAnswer(text)
                NotificationHelper.cancelNotification(this, questionId)
                finish()
            }
        }

        submitNumberBtn.setOnClickListener {
            val num = numberInput.text.toString().toDoubleOrNull()
            if (num != null) {
                viewModel.saveNumberAnswer(num)
                NotificationHelper.cancelNotification(this, questionId)
                finish()
            }
        }

        yesBtn.setOnClickListener {
            viewModel.saveBoolAnswer(true)
            NotificationHelper.cancelNotification(this, questionId)
            finish()
        }

        noBtn.setOnClickListener {
            viewModel.saveBoolAnswer(false)
            NotificationHelper.cancelNotification(this, questionId)
            finish()
        }
    }
}
