package us.slooker.moodpixels.data.model

import java.util.UUID

data class LegendEntry(
    val id: String = UUID.randomUUID().toString(),
    val colorValue: Int,
    val moodName: String,
)
