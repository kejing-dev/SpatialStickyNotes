package com.example.spatialstickynotes.content

import android.app.Activity
import android.app.ActivityManager
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spatialstickynotes.R
import com.example.spatialstickynotes.data.Note
import com.example.spatialstickynotes.data.NoteSession
import com.example.spatialstickynotes.data.NoteStore
import com.example.spatialstickynotes.lod.LodController
import com.example.spatialstickynotes.lod.rememberViewerDistanceEstimate
import com.example.spatialstickynotes.ui.ActionStrip
import com.example.spatialstickynotes.ui.CounterRing
import com.example.spatialstickynotes.ui.DeleteConfirmDialog
import com.example.spatialstickynotes.ui.IconBtn
import com.example.spatialstickynotes.ui.Role
import com.example.spatialstickynotes.ui.StickyCard
import com.example.spatialstickynotes.ui.StickyMarker
import com.example.spatialstickynotes.ui.StripDivider
import com.example.spatialstickynotes.ui.Tokens
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.TextArea
import com.pico.spatial.ui.design.TextFieldDefaults
import com.pico.spatial.ui.platform.LocalSpatialContainerStateManager
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * One sticky note in the Shared Space (v1.2, icon-only).
 *
 * - Selection == the OS window focus: focused → the caption bar (drag = move) and the ✎ / 🗑 strip below the card.
 * - ✎, or a tap on the selected card, enters inline editing (single ✓ = done, autosave; no cancel).
 * - Far (LOD): the folded marker; one tap expands it; ⤡ collapses again.
 * - Delete asks once with an icon-only dialog that previews the note.
 * - A failed write is surfaced by the home window's title bar ("!" = retry); the note itself stays clean.
 */
@OptIn(FlowPreview::class)
@Composable
fun NoteWindow(bundle: Bundle?, modifier: Modifier = Modifier) {
    val noteId = bundle?.getString(Windows.KEY_NOTE_ID) ?: return
    val draftFlag = bundle.getBoolean(Windows.KEY_DRAFT, false)
    val notes by NoteStore.notes.collectAsState()
    val note = notes.firstOrNull { it.id == noteId }
    val isDraft = draftFlag && note == null
    val navigator = LocalSpatialNavigator.current
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as? Activity

    val stateManager = LocalSpatialContainerStateManager.current
    val isFocused by stateManager.isFocused

    var editing by remember { mutableStateOf(isDraft) }
    var text by remember { mutableStateOf(note?.text ?: "") }
    var focusedAt by remember { mutableStateOf(0L) }
    var pendingDelete by remember { mutableStateOf(false) }
    // Once the note has been observed in the store, a later `note == null` means it was deleted
    // (here or from the home list); nothing may write it back after that.
    var wasPersisted by remember { mutableStateOf(note != null) }
    var deleted by remember { mutableStateOf(false) }

    // ---- LOD (PRD 4.2) — frozen while the window is focused or being edited ------------------
    val distance by rememberViewerDistanceEstimate()
    val lod = remember { LodController() }
    LaunchedEffect(distance, isFocused, editing) { if (!isFocused && !editing) lod.update(distance) }
    var manuallyExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(lod.isNear) { if (lod.isNear) manuallyExpanded = false }
    val showFull = editing || lod.isNear || manuallyExpanded
    val selected = isFocused && !editing

    // ---- open / draft bookkeeping --------------------------------------------------------------
    DisposableEffect(noteId) {
        NoteSession.markOpen(noteId)
        if (draftFlag) NoteSession.markDraft(noteId)
        onDispose { NoteSession.markClosed(noteId) }
    }
    LaunchedEffect(note?.id) { if (note != null) { wasPersisted = true; NoteSession.clearDraft(noteId) } }
    // Deleted elsewhere (home list) → close this window and never write it back.
    LaunchedEffect(note == null) {
        if (note == null && wasPersisted && !deleted) { deleted = true; navigator.closeWindowContainer() }
    }
    // Keep local text in sync with external edits while not editing.
    LaunchedEffect(note?.text, editing) { if (!editing && note != null) text = note.text }

    // ---- persistence -----------------------------------------------------------------------------
    suspend fun persist(current: String): Boolean {
        val trimmed = current.trim()
        if (trimmed.isEmpty() || deleted) return true
        val r = if (NoteStore.get(noteId) == null) NoteStore.create(noteId, trimmed).map { } else NoteStore.updateText(noteId, trimmed)
        return r.isSuccess
    }
    // Autosave while editing (debounced), as in Apple Notes: there is nothing to cancel.
    LaunchedEffect(editing) {
        if (!editing) return@LaunchedEffect
        snapshotFlow { text }.debounce(600).collect { persist(it) }
    }
    fun finishEditing() {
        editing = false
        val current = text
        if (current.isBlank() && NoteStore.get(noteId) == null) { navigator.closeWindowContainer(); return } // empty draft → discard
        // The store's own scope: this save must survive the window being closed right after ✓ / unfocus.
        NoteStore.scope.launch { persist(current) }
    }
    // Leaving focus while editing ends the edit (autosaved); closing the window saves on the store's own scope.
    // Only a real focus *loss* counts: a new window starts unfocused, and a fresh draft must not be
    // discarded before the OS has handed it focus.
    var hadFocus by remember { mutableStateOf(false) }
    LaunchedEffect(isFocused) {
        if (isFocused) { focusedAt = System.currentTimeMillis(); hadFocus = true }
        else if (hadFocus && editing) finishEditing()
    }
    // Fallback flush for a window closed by the OS while still in edit mode. Never after a delete,
    // and never for a window that is not editing (its text is already the store's text).
    DisposableEffect(Unit) {
        onDispose {
            val current = text
            if (!deleted && editing && current.isNotBlank()) NoteStore.scope.launch { NoteStore.create(noteId, current) }
        }
    }
    // Best-effort per-window system title: first characters of the note, else a numeric timestamp.
    LaunchedEffect(note?.text) {
        val title = note?.summary(12)?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        activity?.let { it.title = title; @Suppress("DEPRECATION") it.setTaskDescription(ActivityManager.TaskDescription(title)) }
    }

    fun tapCard() {
        // The tap that focuses the window is ignored; a tap on an already-selected card edits.
        if (!isFocused || System.currentTimeMillis() - focusedAt < 600) return
        editing = true
    }

    // Card on top, action strip BELOW the card (never covering the note text); the OS caption bar
    // sits outside the window, so the bottom of the 400 dp window is free for the strip.
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.TopCenter) {
            when {
                editing -> EditingCard(
                    text = text,
                    onTextChange = { text = Note.clamp(it) },
                )
                showFull -> StickyCard(text = note?.text ?: text, selected = selected, onClick = { tapCard() })
                else -> Box(modifier = Modifier.width(Tokens.NoteCardWidth).height(Tokens.NoteCardHeight), contentAlignment = Alignment.BottomCenter) {
                    StickyMarker(onClick = { manuallyExpanded = true }, selected = selected)
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(60.dp), contentAlignment = Alignment.TopCenter) {
            when {
                editing -> ActionStrip {
                    IconBtn(R.drawable.ic_check, "Done", onClick = { finishEditing() }, role = Role.Accent, enabled = text.isNotBlank())
                }
                selected && showFull -> ActionStrip {
                    IconBtn(R.drawable.ic_edit, "Edit", onClick = { editing = true })
                    StripDivider()
                    IconBtn(R.drawable.ic_delete, "Delete", onClick = { pendingDelete = true }, role = Role.Danger)
                    if (!lod.isNear && manuallyExpanded) {
                        StripDivider()
                        IconBtn(R.drawable.ic_collapse, "Collapse", onClick = { manuallyExpanded = false })
                    }
                }
            }
        }
    }

    if (pendingDelete) {
        DeleteConfirmDialog(
            note = note,
            onDismiss = { pendingDelete = false },
            onConfirm = {
                pendingDelete = false
                deleted = true
                scope.launch {
                    NoteStore.delete(noteId)
                    navigator.closeWindowContainer()
                }
            },
        )
    }
}

/** Inline editor: the yellow card becomes a TextArea, with the character ring in its corner. */
@Composable
private fun EditingCard(text: String, onTextChange: (String) -> Unit) {
    // No automatic focus request: the system keyboard button (PICO OS IME affordance) only appears
    // once the user taps into the text, not as soon as ✎ is pressed.
    Box(modifier = Modifier.width(Tokens.NoteCardWidth).height(Tokens.NoteCardHeight)) {
        TextArea(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = PicoTheme.typography.bodyLargeMultiline.copy(color = Tokens.NoteInk, fontSize = 20.sp, lineHeight = 30.sp),
            minLines = 4,
            cornerRadius = Tokens.NoteCornerRadius,
            colors = TextFieldDefaults.textFieldColors(
                textColor = Tokens.NoteInk,
                backgroundColor = Tokens.NoteFill,
                focusedColor = Tokens.NoteFill,
                placeholderColor = Tokens.NoteInk.copy(alpha = 0.45f),
            ),
        )
        CounterRing(
            count = Note.countCodePoints(text),
            max = Note.MAX_CODE_POINTS,
            modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
        )
    }
}
