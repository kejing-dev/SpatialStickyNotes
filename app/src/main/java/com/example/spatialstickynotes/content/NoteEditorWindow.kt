package com.example.spatialstickynotes.content

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spatialstickynotes.data.Note
import com.example.spatialstickynotes.data.NoteStore
import com.example.spatialstickynotes.ui.MutedText
import com.example.spatialstickynotes.ui.PrimaryButton
import com.example.spatialstickynotes.ui.SecondaryButton
import com.example.spatialstickynotes.ui.Tokens
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.TextArea
import com.pico.spatial.ui.design.TextFieldDefaults
import com.pico.spatial.ui.design.windows.LocalSnackbarHostState
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import kotlinx.coroutines.launch

/**
 * Figma 01 创建 · 02 写下文字 / 04 编辑: a 560×640 dp panel with the yellow input, a live
 * code-point counter (PRD F01: 200 chars, Chinese and emoji count as one) and 取消 / 下一步·放置 (or 保存).
 */
@Composable
fun NoteEditorWindow(bundle: Bundle?, modifier: Modifier = Modifier) {
    val mode = bundle?.getString(Windows.KEY_MODE) ?: Windows.MODE_CREATE
    val noteId = bundle?.getString(Windows.KEY_NOTE_ID)
    val isEdit = mode == Windows.MODE_EDIT && noteId != null
    val original = remember(noteId) { noteId?.let { NoteStore.get(it) } }

    val navigator = LocalSpatialNavigator.current
    val snack = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf(original?.text ?: "") }
    var saving by remember { mutableStateOf(false) }
    val count = Note.countCodePoints(text)
    val canSubmit = text.isNotBlank() && !saving

    fun close() = navigator.closeWindowContainer()

    fun submit() {
        if (!canSubmit) return
        saving = true
        scope.launch {
            if (isEdit) {
                val r = NoteStore.updateText(noteId!!, text)
                saving = false
                if (r.isSuccess) close()
                else snack.show(message = "保存失败，请重试")
            } else {
                val r = NoteStore.create(text)
                saving = false
                r.onSuccess { note ->
                    // F02: after submitting, the note enters the pending-placement state in its own window.
                    navigator.openWindowContainer(Windows.NOTE, note.id, Windows.noteBundle(note.id))
                    close()
                }.onFailure { e ->
                    snack.show(
                        message = if (e is NoteStore.CapacityExceeded) "已达到 ${Note.MAX_NOTES} 条上限，请先删除旧便签"
                        else "保存失败，请重试"
                    )
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 28.dp)) {
        Text(
            text = if (isEdit) "编辑便签" else "新建便签",
            color = Tokens.Ink,
            style = PicoTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(20.dp))
        TextArea(
            value = text,
            onValueChange = { text = Note.clamp(it) },
            modifier = Modifier.fillMaxWidth().weight(1f),
            placeholder = {
                Text(
                    text = "写下要贴在空间里的内容…",
                    color = Tokens.NoteInk.copy(alpha = 0.45f),
                    style = PicoTheme.typography.bodyLargeMultiline,
                    fontSize = 20.sp,
                )
            },
            textStyle = PicoTheme.typography.bodyLargeMultiline.copy(
                color = Tokens.NoteInk,
                fontSize = 20.sp,
                lineHeight = 30.sp,
            ),
            minLines = 4,
            cornerRadius = Tokens.NoteCornerRadius,
            colors = TextFieldDefaults.textFieldColors(
                textColor = Tokens.NoteInk,
                backgroundColor = Tokens.NoteFill,
                focusedColor = Tokens.NoteFill,
                placeholderColor = Tokens.NoteInk.copy(alpha = 0.45f),
            ),
        )
        Spacer(Modifier.height(12.dp))
        MutedText(
            text = "$count / ${Note.MAX_CODE_POINTS}",
            color = if (count >= Note.MAX_CODE_POINTS) Tokens.DangerText else Tokens.Muted,
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryButton(text = "取消", onClick = { close() }, modifier = Modifier.weight(1f), enabled = !saving)
            PrimaryButton(
                text = when {
                    saving -> "正在保存…"
                    isEdit -> "保存"
                    else -> "下一步 · 放置"
                },
                onClick = { submit() },
                modifier = Modifier.weight(1f),
                enabled = canSubmit,
            )
        }
    }
}
