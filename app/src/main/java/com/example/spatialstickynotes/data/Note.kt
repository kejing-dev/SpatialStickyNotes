package com.example.spatialstickynotes.data

/** A sticky note. Window position is owned by PICO OS (Shared Space), so only text and the folded state are stored. */
data class Note(
    val id: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** Folded into the 96 dp marker (user's ⤡); persisted so it survives a restart. */
    val collapsed: Boolean = false,
) {
    /** First non-empty line, clipped to [maxCodePoints] code points (used for list rows and window titles). */
    fun summary(maxCodePoints: Int = 36): String {
        val firstLine = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: ""
        return if (firstLine.codePointCount(0, firstLine.length) <= maxCodePoints) firstLine
        else firstLine.substring(0, firstLine.offsetByCodePoints(0, maxCodePoints)) + "…"
    }

    companion object {
        const val MAX_CODE_POINTS = 200
        const val MAX_NOTES = 30

        fun countCodePoints(text: String): Int = text.codePointCount(0, text.length)

        /** Clamp to [MAX_CODE_POINTS] code points (a CJK character or a single-code-point emoji counts as one). */
        fun clamp(text: String): String {
            val count = countCodePoints(text)
            return if (count <= MAX_CODE_POINTS) text
            else text.substring(0, text.offsetByCodePoints(0, MAX_CODE_POINTS))
        }
    }
}
