package com.example.spatialstickynotes.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Runtime-only state: which note windows are currently open in the space. */
object NoteSession {
    private val _openIds = MutableStateFlow<Set<String>>(emptySet())
    val openIds: StateFlow<Set<String>> get() = _openIds

    fun markOpen(id: String) = _openIds.update { it + id }
    fun markClosed(id: String) = _openIds.update { it - id }
}
