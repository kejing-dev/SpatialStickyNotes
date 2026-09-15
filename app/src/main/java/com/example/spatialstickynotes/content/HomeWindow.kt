package com.example.spatialstickynotes.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.spatialstickynotes.R
import com.example.spatialstickynotes.data.Note
import com.example.spatialstickynotes.data.NoteSession
import com.example.spatialstickynotes.data.NoteStore
import com.example.spatialstickynotes.ui.DeleteConfirmDialog
import com.example.spatialstickynotes.ui.IconBtn
import com.example.spatialstickynotes.ui.Role
import com.example.spatialstickynotes.ui.Tokens
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.TitleBar
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * Main WindowContainer (v1.2, icon-only): app glyph, 「+」 with a capacity ring, a danger 「!」 while a
 * write failed, and the note list (tap a row = open/bring the note window forward, 🗑 = delete).
 */
@Composable
fun HomeWindow(modifier: Modifier = Modifier) {
    val notes by NoteStore.notes.collectAsState()
    val openIds by NoteSession.openIds.collectAsState()
    val draftIds by NoteSession.draftIds.collectAsState()
    val unsavedIds by NoteStore.unsavedIds.collectAsState()
    val dirty by NoteStore.dirty.collectAsState()
    val navigator = LocalSpatialNavigator.current
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Note?>(null) }

    val count = notes.size + draftIds.size
    val full = count >= Note.MAX_NOTES

    fun createNote() {
        // One draft at a time: 「+」 again just brings the open draft forward.
        draftIds.firstOrNull()?.let { navigator.openWindowContainer(Windows.NOTE, it, Windows.noteBundle(it, draft = true)); return }
        if (full) return
        val id = UUID.randomUUID().toString()
        NoteSession.markDraft(id)
        navigator.openWindowContainer(Windows.NOTE, id, Windows.noteBundle(id, draft = true))
    }

    // Near-white scrim over the OS material glass so the panel reads light regardless of what is
    // behind it (the bare glass turned grey in front of dark furniture).
    Column(modifier = modifier.fillMaxSize().background(Tokens.Canvas.copy(alpha = 0.85f))) {
        TitleBar(
            title = {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Tokens.NoteFill), contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(R.drawable.ic_note), contentDescription = "Spatial Sticky Notes", tint = Tokens.Ink, modifier = Modifier.size(26.dp))
                }
            },
            trailingActions = {
                if (dirty) {
                    IconBtn(R.drawable.ic_error, "Save failed, tap to retry", onClick = { NoteStore.retrySaveAsync() }, role = Role.Danger, width = 48.dp, height = 48.dp, round = true)
                    Spacer(Modifier.width(12.dp))
                }
                PlusWithCapacityRing(count = count, max = Note.MAX_NOTES, onClick = { createNote() })
            },
        )
        if (notes.isEmpty()) {
            EmptyState(onCreate = { createNote() })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteRow(
                        note = note,
                        shown = note.id in openIds,
                        unsaved = note.id in unsavedIds,
                        onOpen = { navigator.openWindowContainer(Windows.NOTE, note.id, Windows.noteBundle(note.id)) },
                        onDelete = { pendingDelete = note },
                    )
                }
            }
        }
    }

    pendingDelete?.let { note ->
        DeleteConfirmDialog(
            note = note,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                pendingDelete = null
                scope.launch {
                    NoteStore.delete(note.id)
                    navigator.closeWindowContainer(Windows.NOTE, note.id)
                }
            },
        )
    }
}

/** 「+」 wrapped by a thin ring showing notes / 30; the ring turns danger and the button greys out when full. */
@Composable
private fun PlusWithCapacityRing(count: Int, max: Int, onClick: () -> Unit) {
    val full = count >= max
    Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(64.dp)) {
            val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val sz = Size(size.width - stroke.width, size.height - stroke.width)
            drawArc(Tokens.Border, 0f, 360f, false, Offset(inset, inset), sz, style = stroke)
            if (count > 0) drawArc(if (full) Tokens.DangerText else Tokens.Accent, -90f, 360f * (count.toFloat() / max).coerceIn(0f, 1f), false, Offset(inset, inset), sz, style = stroke)
        }
        IconBtn(R.drawable.ic_plus, "New note", onClick = onClick, role = if (full) Role.Secondary else Role.Accent, width = 52.dp, height = 52.dp, iconSize = 32.dp, round = true, enabled = !full)
    }
}

@Composable
private fun EmptyState(onCreate: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(modifier = Modifier.width(160.dp).height(120.dp).clip(RoundedCornerShape(20.dp)).background(Tokens.NoteFill).padding(start = 28.dp, top = 36.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(64, 48, 32).forEach { w -> Box(Modifier.width(w.dp).height(6.dp).clip(CircleShape).background(Tokens.NoteInk.copy(alpha = 0.35f))) }
                }
            }
            IconBtn(R.drawable.ic_plus, "New note", onClick = onCreate, role = Role.Accent, width = 96.dp, height = 64.dp, iconSize = 36.dp)
        }
    }
}

@Composable
private fun NoteRow(note: Note, shown: Boolean, unsaved: Boolean, onOpen: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Tokens.Canvas)
            .clickable(onClick = onOpen)
            .padding(start = 20.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = Modifier.size(40.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Tokens.NoteFill).padding(start = 9.dp, top = 12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(18, 13, 9).forEach { w -> Box(Modifier.width(w.dp).height(2.5.dp).clip(CircleShape).background(Tokens.NoteInk.copy(alpha = 0.7f))) }
                }
            }
            if (shown) Box(Modifier.align(Alignment.TopEnd).size(12.dp).clip(CircleShape).background(Tokens.Canvas).padding(2.dp).clip(CircleShape).background(Tokens.Accent))
            if (unsaved) Box(Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape).background(Tokens.Canvas).padding(2.dp).clip(CircleShape).background(Tokens.DangerText))
        }
        Text(
            text = note.summary(60),
            color = Tokens.Ink,
            style = PicoTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconBtn(R.drawable.ic_delete, "Delete", onClick = onDelete, role = Role.Danger, width = 46.dp, height = 40.dp, iconSize = 24.dp)
    }
}
