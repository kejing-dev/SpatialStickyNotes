package com.example.spatialstickynotes.data

import android.content.Context
import android.util.Log
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local persistence for notes (PRD F07/F08). Notes are stored as JSON in the app's private files
 * directory and written atomically (temp file + rename). Every mutation returns a [Result] so the
 * UI can show "已保存" only after the write actually succeeded, and offer a retry on failure.
 */
object NoteStore {
    private const val TAG = "NoteStore"
    private const val FILE_NAME = "notes.json"

    private lateinit var file: File
    private val mutex = Mutex()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> get() = _notes

    /** True while the in-memory state is ahead of what is on disk (last write failed). */
    private val _dirty = MutableStateFlow(false)
    val dirty: StateFlow<Boolean> get() = _dirty

    fun init(context: Context) {
        file = File(context.filesDir, FILE_NAME)
        val loaded = runCatching { readFromDisk() }.getOrElse {
            Log.w(TAG, "Failed to read notes", it); emptyList()
        }
        // Shared Space: window positions are owned by PICO OS and are not restored across a cold
        // start, so anything that was placed last session must be re-placed by the user (F08).
        val restored = loaded.map {
            if (it.placement == PlacementState.PLACED) it.copy(placement = PlacementState.NEEDS_REPLACE) else it
        }
        _notes.value = restored.sortedByDescending { it.updatedAt }
        if (restored != loaded) {
            runCatching { writeToDisk(restored) }.onFailure { Log.w(TAG, "Failed to persist restore state", it) }
        }
    }

    fun get(id: String): Note? = _notes.value.firstOrNull { it.id == id }

    suspend fun create(text: String): Result<Note> {
        val clamped = Note.clamp(text.trim())
        if (clamped.isEmpty()) return Result.failure(IllegalArgumentException("empty"))
        if (_notes.value.size >= Note.MAX_NOTES) return Result.failure(CapacityExceeded())
        val now = System.currentTimeMillis()
        val note = Note(UUID.randomUUID().toString(), clamped, now, now, PlacementState.PENDING)
        return mutate { list -> listOf(note) + list }.map { note }
    }

    suspend fun updateText(id: String, text: String): Result<Unit> {
        val clamped = Note.clamp(text.trim())
        if (clamped.isEmpty()) return Result.failure(IllegalArgumentException("empty"))
        return mutate { list ->
            list.map { if (it.id == id) it.copy(text = clamped, updatedAt = System.currentTimeMillis()) else it }
        }
    }

    suspend fun setPlacement(id: String, placement: PlacementState): Result<Unit> = mutate { list ->
        list.map { if (it.id == id) it.copy(placement = placement) else it }
    }

    suspend fun delete(id: String): Result<Unit> = mutate { list -> list.filterNot { it.id == id } }

    /** Re-attempt writing the current in-memory state to disk. */
    suspend fun retrySave(): Result<Unit> = mutate { it }

    private suspend fun mutate(transform: (List<Note>) -> List<Note>): Result<Unit> = mutex.withLock {
        val next = transform(_notes.value)
        _notes.value = next
        val result = withContext(Dispatchers.IO) { runCatching { writeToDisk(next) } }
        _dirty.update { result.isFailure }
        result.onFailure { Log.e(TAG, "Failed to save notes", it) }
        result
    }

    private fun readFromDisk(): List<Note> {
        if (!file.exists()) return emptyList()
        val array = JSONArray(file.readText())
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Note(
                id = o.getString("id"),
                text = o.getString("text"),
                createdAt = o.optLong("createdAt"),
                updatedAt = o.optLong("updatedAt"),
                placement = runCatching { PlacementState.valueOf(o.optString("placement")) }
                    .getOrDefault(PlacementState.NEEDS_REPLACE),
            )
        }
    }

    private fun writeToDisk(notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { n ->
            array.put(
                JSONObject()
                    .put("id", n.id)
                    .put("text", n.text)
                    .put("createdAt", n.createdAt)
                    .put("updatedAt", n.updatedAt)
                    .put("placement", n.placement.name)
            )
        }
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(array.toString())
        if (!tmp.renameTo(file)) {
            file.writeText(array.toString())
            tmp.delete()
        }
    }

    class CapacityExceeded : Exception("capacity exceeded")
}
