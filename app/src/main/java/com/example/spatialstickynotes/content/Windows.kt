package com.example.spatialstickynotes.content

import android.os.Bundle

/** WindowContainer ids and bundle keys shared by the app's windows. */
object Windows {
    /** Mirrors `pico.spatial.windowcontainer.id` in AndroidManifest.xml (the default window container). */
    const val HOME = "StickyNotesHome"
    const val NOTE = "StickyNoteWindow"

    const val KEY_NOTE_ID = "noteId"
    const val KEY_DRAFT = "draft"

    fun noteBundle(noteId: String, draft: Boolean = false): Bundle = Bundle().apply {
        putString(KEY_NOTE_ID, noteId)
        putBoolean(KEY_DRAFT, draft)
    }
}
