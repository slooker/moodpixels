package us.slooker.moodpixels.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import us.slooker.moodpixels.data.db.MoodEntry
import java.io.File

object JsonExporter {

    data class ExportRecord(
        val date: String,
        val hour: Int?,
        val color: String,
        val mood: String,
        val note: String?
    )

    fun buildShareIntent(context: Context, entries: List<MoodEntry>): Intent {
        val records = entries.map { entry ->
            ExportRecord(
                date = entry.entryDate,
                hour = if (entry.hourSlot >= 0) entry.hourSlot else null,
                color = colorIntToHex(entry.colorValue),
                mood = entry.moodName,
                note = entry.note?.ifBlank { null }
            )
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(records)

        val exportDir = File(context.cacheDir, "exports")
        exportDir.mkdirs()
        val file = File(exportDir, "mood_pixels_export.json")
        file.writeText(json)

        val uri = FileProvider.getUriForFile(
            context,
            "us.slooker.moodpixels.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Mood Pixels Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun colorIntToHex(color: Int): String =
        String.format("#%02X%02X%02X", Color.red(color), Color.green(color), Color.blue(color))
}
