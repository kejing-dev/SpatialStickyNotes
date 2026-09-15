package com.example.spatialstickynotes.data

import android.content.Context
import android.util.Log
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local persistence (PRD F07/F08): a JSON array in the app's private files directory, written
 * atomically (temp file + rename). Mutations update memory first and then write; a failed write
 * leaves the store [dirty] and records the touched ids in [unsavedIds] until a later write succeeds.
 */
object NoteStore {
    private const val TAG = "NoteStore"
    private const val FILE_NAME = "notes.json"

    /** Application-scoped IO scope for writes that must outlive a window's composition (e.g. autosave on close). */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var file: File
    private val mutex = Mutex()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> get() = _notes

    private val _dirty = MutableStateFlow(false)
    /** True while the in-memory list is ahead of what is on disk. */
    val dirty: StateFlow<Boolean> get() = _dirty

    private val _unsavedIds = MutableStateFlow<Set<String>>(emptySet())
    /** Ids touched since the last successful write (cleared on success). */
    val unsavedIds: StateFlow<Set<String>> get() = _unsavedIds

    fun init(context: Context) {
        file = File(context.filesDir, FILE_NAME)
        val loaded = runCatching { readFromDisk() }.getOrElse {
            Log.w(TAG, "Failed to read notes", it); emptyList()
        }
        _notes.value = loaded.sortedByDescending { it.updatedAt }
    }

    fun get(id: String): Note? = _notes.value.firstOrNull { it.id == id }

    /**
     * Create a note with a caller-supplied id (the note window's tag), so a draft window keeps its identity.
     * Idempotent: if the id already exists this is an update, so concurrent saves (debounced autosave,
     * ✓, dispose fallback) can never produce two notes with the same id.
     */
    suspend fun create(id: String, text: String): Result<Note> {
        val clamped = Note.clamp(text.trim())
        if (clamped.isEmpty()) return Result.failure(IllegalArgumentException("empty"))
        return mutex.withLock {
            val current = _notes.value
            val existing = current.firstOrNull { it.id == id }
            when {
                existing != null && existing.text == clamped -> Result.success(existing)
                existing != null -> {
                    val updated = existing.copy(text = clamped, updatedAt = System.currentTimeMillis())
                    commitLocked(setOf(id), current.map { if (it.id == id) updated else it }).map { updated }
                }
                current.size >= Note.MAX_NOTES -> Result.failure(CapacityExceeded())
                else -> {
                    val now = System.currentTimeMillis()
                    val note = Note(id, clamped, now, now)
                    commitLocked(setOf(id), listOf(note) + current).map { note }
                }
            }
        }
    }

    suspend fun updateText(id: String, text: String): Result<Unit> {
        val clamped = Note.clamp(text.trim())
        if (clamped.isEmpty()) return Result.failure(IllegalArgumentException("empty"))
        return mutex.withLock {
            val current = _notes.value
            if (current.firstOrNull { it.id == id }?.text == clamped) return@withLock Result.success(Unit)
            commitLocked(setOf(id), current.map { if (it.id == id) it.copy(text = clamped, updatedAt = System.currentTimeMillis()) else it })
        }
    }

    suspend fun delete(id: String): Result<Unit> = mutate(setOf(id)) { list -> list.filterNot { it.id == id } }

    /** Re-attempt writing the current in-memory state to disk. */
    suspend fun retrySave(): Result<Unit> = mutate(emptySet()) { it }

    /** Fire-and-forget retry (used from UI callbacks that have no composition scope left). */
    fun retrySaveAsync() { scope.launch { retrySave() } }

    private suspend fun mutate(touched: Set<String>, transform: (List<Note>) -> List<Note>): Result<Unit> =
        mutex.withLock { commitLocked(touched, transform(_notes.value)) }

    /**
     * Publish [next] and write it to disk. Must be called with [mutex] held. Runs non-cancellable so the
     * dirty/unsaved flags always reflect the outcome even if the caller (e.g. a window's composition
     * scope) is cancelled mid-write; otherwise a failed write would be silent and unretryable.
     */
    private suspend fun commitLocked(touched: Set<String>, next: List<Note>): Result<Unit> = withContext(NonCancellable) {
        _notes.value = next
        val result = withContext(Dispatchers.IO) { runCatching { writeToDisk(next) } }
        if (result.isSuccess) {
            _dirty.value = false
            _unsavedIds.value = emptySet()
        } else {
            _dirty.value = true
            _unsavedIds.value = _unsavedIds.value + touched
            Log.e(TAG, "Failed to save notes", result.exceptionOrNull())
        }
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
            )
        }
    }

    private fun writeToDisk(notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { n ->
            array.put(JSONObject().put("id", n.id).put("text", n.text).put("createdAt", n.createdAt).put("updatedAt", n.updatedAt))
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
