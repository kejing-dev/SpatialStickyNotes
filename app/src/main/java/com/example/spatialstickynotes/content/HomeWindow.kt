package com.example.spatialstickynotes.content

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spatialstickynotes.data.Note
import com.example.spatialstickynotes.data.NoteSession
import com.example.spatialstickynotes.data.NoteStore
import com.example.spatialstickynotes.data.PlacementState
import com.example.spatialstickynotes.ui.DangerButton
import com.example.spatialstickynotes.ui.MutedText
import com.example.spatialstickynotes.ui.Panel
import com.example.spatialstickynotes.ui.PrimaryButton
import com.example.spatialstickynotes.ui.SecondaryButton
import com.example.spatialstickynotes.ui.StatusPill
import com.example.spatialstickynotes.ui.Tokens
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.ListItem
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.TitleBar
import com.pico.spatial.ui.design.windows.AlertDialog
import com.pico.spatial.ui.design.windows.LocalSnackbarHostState
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import kotlinx.coroutines.launch

/**
 * Default (main) WindowContainer: entry point, note list (PRD F09) and capacity guard (F10).
 * Creation and editing happen in [NoteEditorWindow]; each placed note lives in its own
 * [NoteWindow] so PICO OS lets the user drag it anywhere in the Shared Space.
 */
@Composable
fun HomeWindow(modifier: Modifier = Modifier) {
    val notes by NoteStore.notes.collectAsState()
    val openIds by NoteSession.openIds.collectAsState()
    val dirty by NoteStore.dirty.collectAsState()
    val navigator = LocalSpatialNavigator.current
    val snack = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Note?>(null) }

    fun openEditorForCreate() {
        if (notes.size >= Note.MAX_NOTES) {
            scope.launch { snack.show(message = "已达到 ${Note.MAX_NOTES} 条上限，请先删除旧便签") }
            return
        }
        navigator.openWindowContainer(Windows.EDITOR, Windows.editorTag(null), Windows.editorBundle(Windows.MODE_CREATE))
    }

    Column(modifier = modifier.fillMaxSize()) {
        TitleBar(
            title = {
                Text(
                    text = "空间便利贴",
                    color = Tokens.Ink,
                    style = PicoTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            trailingActions = {
                if (dirty) {
                    SecondaryButton(
                        text = "重试保存",
                        size = ButtonDefaults.Small,
                        onClick = {
                            scope.launch {
                                val r = NoteStore.retrySave()
                                snack.show(message = if (r.isSuccess) "已保存到本机" else "保存失败，请重试")
                            }
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                }
                PrimaryButton(text = "新建便签", size = ButtonDefaults.Small, onClick = { openEditorForCreate() })
            },
        )

        if (notes.isEmpty()) {
            FirstUsePanel(onCreate = { openEditorForCreate() })
        } else {
            NoteList(
                notes = notes,
                openIds = openIds,
                onOpen = { note ->
                    navigator.openWindowContainer(Windows.NOTE, note.id, Windows.noteBundle(note.id))
                },
                onEdit = { note ->
                    navigator.openWindowContainer(
                        Windows.EDITOR, Windows.editorTag(note.id), Windows.editorBundle(Windows.MODE_EDIT, note.id)
                    )
                },
                onDelete = { pendingDelete = it },
            )
        }
    }

    pendingDelete?.let { note ->
        DeleteConfirmDialog(
            note = note,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                pendingDelete = null
                scope.launch {
                    val r = NoteStore.delete(note.id)
                    if (r.isSuccess) {
                        navigator.closeWindowContainer(Windows.NOTE, note.id)
                        navigator.closeWindowContainer(Windows.EDITOR, Windows.editorTag(note.id))
                        snack.show(message = "便签已删除")
                    } else {
                        snack.show(message = "删除后保存失败，请点「重试保存」")
                    }
                }
            },
        )
    }
}

/** Figma 01 创建 · 01 首次使用. */
@Composable
private fun FirstUsePanel(onCreate: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Panel(modifier = Modifier.width(420.dp)) {
            Column {
                Text(
                    text = "空间便利贴",
                    color = Tokens.Ink,
                    style = PicoTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "把想法留在空间里\n写下来，放在想放的位置。",
                    color = Tokens.Ink,
                    style = PicoTheme.typography.bodyLargeMultiline,
                    lineHeight = 28.sp,
                )
                Spacer(Modifier.height(20.dp))
                PrimaryButton(text = "新建便签", onClick = onCreate)
            }
        }
    }
}

@Composable
private fun NoteList(
    notes: List<Note>,
    openIds: Set<String>,
    onOpen: (Note) -> Unit,
    onEdit: (Note) -> Unit,
    onDelete: (Note) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MutedText("共 ${notes.size} / ${Note.MAX_NOTES} 条 · 找不到的便签可在这里放回空间")
            val pending = notes.count { it.placement != PlacementState.PLACED }
            if (pending > 0) StatusPill(text = "$pending 条待放置")
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(notes, key = { it.id }) { note ->
                val isOpen = note.id in openIds
                ListItem(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Tokens.Canvas),
                    headlineContent = {
                        Text(
                            text = note.summary(),
                            color = Tokens.Ink,
                            style = PicoTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        MutedText(statusLine(note, isOpen))
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Tokens.NoteFill)
                        )
                    },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SecondaryButton(
                                text = if (isOpen) "查看" else if (note.placement == PlacementState.NEEDS_REPLACE) "放回空间" else "放置",
                                size = ButtonDefaults.Small,
                                onClick = { onOpen(note) },
                            )
                            SecondaryButton(text = "编辑", size = ButtonDefaults.Small, onClick = { onEdit(note) })
                            DangerButton(text = "删除", size = ButtonDefaults.Small, onClick = { onDelete(note) })
                        }
                    },
                )
            }
        }
    }
}

private fun statusLine(note: Note, isOpen: Boolean): String = when {
    note.placement == PlacementState.PLACED && isOpen -> "已固定在空间 · 已保存"
    note.placement == PlacementState.PLACED -> "已保存 · 当前未显示"
    note.placement == PlacementState.PENDING && isOpen -> "待放置 · 拖到目标位置后固定"
    note.placement == PlacementState.PENDING -> "待放置"
    note.placement == PlacementState.NEEDS_REPLACE -> "上次位置未恢复 · 待重新放置"
    else -> ""
}

/** PRD F06: explicit delete with one confirmation. */
@Composable
fun DeleteConfirmDialog(note: Note, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "删除这条便签？", color = Tokens.Ink, style = PicoTheme.typography.titleMedium) },
        content = {
            Text(
                text = "“${note.summary(24)}”将从空间和本机中移除，重启后不再出现。",
                color = Tokens.Muted,
                style = PicoTheme.typography.bodyMediumMultiline,
            )
        },
        buttons = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "取消", onClick = onDismiss)
                DangerButton(text = "删除", onClick = onConfirm)
            }
        },
    )
}
