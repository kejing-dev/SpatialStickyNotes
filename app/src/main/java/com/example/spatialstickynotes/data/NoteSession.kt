package com.example.spatialstickynotes.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Runtime-only state: which note windows are open, and which of them are still unsaved drafts. */
object NoteSession {
    private val _openIds = MutableStateFlow<Set<String>>(emptySet())
    val openIds: StateFlow<Set<String>> get() = _openIds

    private val _draftIds = MutableStateFlow<Set<String>>(emptySet())
    /** Draft note windows opened from 「+」 that have not been persisted yet. */
    val draftIds: StateFlow<Set<String>> get() = _draftIds

    fun markOpen(id: String) = _openIds.update { it + id }
    fun markClosed(id: String) { _openIds.update { it - id }; _draftIds.update { it - id } }
    fun markDraft(id: String) = _draftIds.update { it + id }
    fun clearDraft(id: String) = _draftIds.update { it - id }
}
