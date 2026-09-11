package com.example.spatialstickynotes.content

import android.os.Bundle

/** WindowContainer ids and bundle keys shared by the app's windows. */
object Windows {
    const val HOME = "StickyNotesHome"
    const val NOTE = "StickyNoteWindow"
    const val EDITOR = "StickyNoteEditor"

    const val KEY_NOTE_ID = "noteId"
    const val KEY_MODE = "mode"
    const val MODE_CREATE = "create"
    const val MODE_EDIT = "edit"

    fun noteBundle(noteId: String): Bundle = Bundle().apply { putString(KEY_NOTE_ID, noteId) }

    fun editorBundle(mode: String, noteId: String? = null): Bundle = Bundle().apply {
        putString(KEY_MODE, mode)
        if (noteId != null) putString(KEY_NOTE_ID, noteId)
    }

    /** Tag used for the editor window: one editor per note, one for creation. */
    fun editorTag(noteId: String?): String = if (noteId == null) "create" else "edit-$noteId"
}
