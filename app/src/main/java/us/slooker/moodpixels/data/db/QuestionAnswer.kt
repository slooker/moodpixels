package us.slooker.moodpixels.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "question_answers",
    foreignKeys = [ForeignKey(
        entity = Question::class,
        parentColumns = ["id"],
        childColumns = ["question_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("question_id"),
        Index("answered_at")
    ]
)
data class QuestionAnswer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "question_id") val questionId: Long,
    /** Snapshot of question text at answer time for export stability */
    @ColumnInfo(name = "question_text") val questionText: String,
    @ColumnInfo(name = "answer_text") val answerText: String? = null,
    @ColumnInfo(name = "answer_bool") val answerBool: Boolean? = null,
    @ColumnInfo(name = "answer_number") val answerNumber: Double? = null,
    @ColumnInfo(name = "answered_at") val answeredAt: Long = System.currentTimeMillis()
)
