package com.example.spatialstickynotes.data

/** Placement state of a note in space (Shared Space: window position is managed by PICO OS). */
enum class PlacementState {
    /** Text submitted, window opened, waiting for the user to drag it to a surface and confirm. */
    PENDING,
    /** User confirmed the position ("固定在这里"). */
    PLACED,
    /** Was placed in a previous session; position could not be restored, needs re-placement. */
    NEEDS_REPLACE,
}

data class Note(
    val id: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
    val placement: PlacementState,
) {
    /** One-line summary for lists (first line, at most [maxCodePoints] code points). */
    fun summary(maxCodePoints: Int = 36): String {
        val firstLine = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: ""
        return if (firstLine.codePointCount(0, firstLine.length) <= maxCodePoints) firstLine
        else firstLine.substring(0, firstLine.offsetByCodePoints(0, maxCodePoints)) + "…"
    }

    companion object {
        const val MAX_CODE_POINTS = 200
        const val MAX_NOTES = 30

        fun countCodePoints(text: String): Int = text.codePointCount(0, text.length)

        /** Clamp [text] to [MAX_CODE_POINTS] code points (Chinese characters and emoji count as one). */
        fun clamp(text: String): String {
            val count = countCodePoints(text)
            return if (count <= MAX_CODE_POINTS) text
            else text.substring(0, text.offsetByCodePoints(0, MAX_CODE_POINTS))
        }
    }
}
