package com.example.spatialstickynotes.content

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.spatialstickynotes.data.Note
import com.example.spatialstickynotes.data.NoteSession
import com.example.spatialstickynotes.data.NoteStore
import com.example.spatialstickynotes.data.PlacementState
import com.example.spatialstickynotes.lod.LodController
import com.example.spatialstickynotes.lod.rememberViewerDistanceEstimate
import com.example.spatialstickynotes.ui.DangerButton
import com.example.spatialstickynotes.ui.MutedText
import com.example.spatialstickynotes.ui.PrimaryButton
import com.example.spatialstickynotes.ui.SecondaryButton
import com.example.spatialstickynotes.ui.StatusPill
import com.example.spatialstickynotes.ui.StickyCard
import com.example.spatialstickynotes.ui.StickyMarker
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.platform.LocalSpatialContainerStateManager
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One sticky note in the Shared Space (Figma 02 放置 / 03 查看 / 05 移动).
 *
 * The window itself is moved with the system caption bar; PICO OS handles placement and any
 * surface snapping. The app tracks the placement state (PRD F02/F03), the far/near LOD (F04) and the
 * edit / move / delete actions (F05/F06), and persists every change (F07).
 */
@Composable
fun NoteWindow(bundle: Bundle?, modifier: Modifier = Modifier) {
    val noteId = bundle?.getString(Windows.KEY_NOTE_ID)
    val notes by NoteStore.notes.collectAsState()
    val note = notes.firstOrNull { it.id == noteId }
    val navigator = LocalSpatialNavigator.current
    val scope = rememberCoroutineScope()

    DisposableEffect(noteId) {
        if (noteId != null) NoteSession.markOpen(noteId)
        onDispose { if (noteId != null) NoteSession.markClosed(noteId) }
    }

    if (note == null) {
        // Deleted elsewhere (home list) while this window was open.
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MutedText("这条便签已被删除")
                Spacer(Modifier.height(12.dp))
                SecondaryButton(text = "关闭", onClick = { navigator.closeWindowContainer() })
            }
        }
        return
    }

    val stateManager = LocalSpatialContainerStateManager.current
    val isFocused by stateManager.isFocused

    // ---- LOD (PRD 4.2) --------------------------------------------------------------------
    val distance by rememberViewerDistanceEstimate()
    val lod = remember { LodController() }
    LaunchedEffect(distance) { lod.update(distance) }
    var manuallyExpanded by remember { mutableStateOf(false) }
    // Collapse the manual expansion again once the user has walked away and come back near.
    LaunchedEffect(lod.isNear) { if (lod.isNear) manuallyExpanded = false }
    val showFullText = note.placement != PlacementState.PLACED || lod.isNear || manuallyExpanded

    // ---- transient status (e.g. "已固定在空间 · 已保存") ----------------------------------
    var transientStatus by remember { mutableStateOf<String?>(null) }
    fun flash(message: String) {
        scope.launch {
            transientStatus = message
            delay(2500)
            if (transientStatus == message) transientStatus = null
        }
    }

    var pendingDelete by remember { mutableStateOf(false) }
    var savingPlacement by remember { mutableStateOf(false) }

    fun confirmPlacement() {
        savingPlacement = true
        scope.launch {
            val r = NoteStore.setPlacement(note.id, PlacementState.PLACED)
            savingPlacement = false
            flash(if (r.isSuccess) "已固定在空间 · 已保存" else "已固定 · 保存失败，请在主窗口点「重试保存」")
        }
    }

    fun startMove() {
        scope.launch {
            NoteStore.setPlacement(note.id, PlacementState.PENDING)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (showFullText) {
                StickyCard(text = note.text)
            } else {
                StickyMarker(onClick = { manuallyExpanded = true })
            }
        }

        Spacer(Modifier.height(12.dp))

        // Toolbar (Figma 03 查看 selected state): 编辑 / 移动 / 删除, shown while the window has focus.
        if (isFocused && note.placement == PlacementState.PLACED) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(
                    text = "编辑", size = ButtonDefaults.Small,
                    onClick = {
                        navigator.openWindowContainer(
                            Windows.EDITOR, Windows.editorTag(note.id), Windows.editorBundle(Windows.MODE_EDIT, note.id)
                        )
                    },
                )
                SecondaryButton(text = "移动", size = ButtonDefaults.Small, onClick = { startMove() })
                DangerButton(text = "删除", size = ButtonDefaults.Small, onClick = { pendingDelete = true })
                if (!lod.isNear) {
                    SecondaryButton(
                        text = if (manuallyExpanded) "收起" else "展开",
                        size = ButtonDefaults.Small,
                        onClick = { manuallyExpanded = !manuallyExpanded },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        when (note.placement) {
            PlacementState.PENDING, PlacementState.NEEDS_REPLACE -> {
                // Figma 02 放置: "松手固定在这里".
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StatusPill(
                        text = if (note.placement == PlacementState.NEEDS_REPLACE)
                            "上次位置未恢复 · 拖动标题栏放回原处"
                        else "拖动标题栏放到墙面或桌面"
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(
                            text = if (savingPlacement) "正在保存…" else "固定在这里",
                            size = ButtonDefaults.Small,
                            enabled = !savingPlacement,
                            onClick = { confirmPlacement() },
                        )
                        DangerButton(text = "删除", size = ButtonDefaults.Small, onClick = { pendingDelete = true })
                    }
                }
            }
            PlacementState.PLACED -> {
                val status = transientStatus ?: distanceStatus(distance, lod.isNear, manuallyExpanded)
                StatusPill(text = status, emphasized = transientStatus != null)
            }
        }
    }

    if (pendingDelete) {
        DeleteConfirmDialog(
            note = note,
            onDismiss = { pendingDelete = false },
            onConfirm = {
                pendingDelete = false
                scope.launch {
                    val r = NoteStore.delete(note.id)
                    if (r.isSuccess) {
                        // Also close this note's editor window if it is open, then close ourselves.
                        navigator.closeWindowContainer(Windows.EDITOR, Windows.editorTag(note.id))
                        navigator.closeWindowContainer()
                    } else {
                        flash("删除后保存失败 · 请在主窗口点「重试保存」")
                    }
                }
            },
        )
    }
}

/** Figma 03 查看 pills: "2.4 m · 远处隐藏正文", "1.2 m · 近处全文", "主动打开 · 全文保持展开". */
private fun distanceStatus(distance: Float?, isNear: Boolean, manuallyExpanded: Boolean): String {
    val d = distance?.let { "%.1f m".format(it) }
    return when {
        manuallyExpanded && !isNear -> "主动打开 · 全文保持展开"
        d == null -> "已固定在空间 · 已保存"
        isNear -> "$d · 近处全文"
        else -> "$d · 远处隐藏正文"
    }
}

@Suppress("unused")
private fun Note.debugLabel() = "${id.take(6)}:${summary(12)}"
