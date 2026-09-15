package com.example.spatialstickynotes.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spatialstickynotes.R
import com.example.spatialstickynotes.data.Note
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.IconButton
import com.pico.spatial.ui.design.IconButtonDefaults
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.windows.AlertDialog
import androidx.compose.foundation.gestures.Orientation
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect

/** Colour role of an icon button (Figma Button/Secondary · Button/Danger · Button/Primary). */
enum class Role { Secondary, Danger, Accent }

private fun Role.container() = when (this) { Role.Secondary -> Tokens.Secondary; Role.Danger -> Tokens.DangerBg; Role.Accent -> Tokens.Accent }
private fun Role.content() = when (this) { Role.Secondary -> Tokens.Ink; Role.Danger -> Tokens.DangerText; Role.Accent -> Tokens.OnAccent }

/**
 * Icon-only PICO IconButton. [contentDescription] is never rendered (accessibility only) — the UI
 * itself carries no text, so no localisation is needed.
 */
@Composable
fun IconBtn(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Secondary,
    width: Dp = 56.dp,
    height: Dp = 48.dp,
    iconSize: Dp = 30.dp,
    enabled: Boolean = true,
    round: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = IconButtonDefaults.iconButtonSize(width, height),
        colors = IconButtonDefaults.iconButtonColors(containerColor = role.container(), contentColor = role.content()),
        shape = if (round) CircleShape else RoundedCornerShape(35.dp),
    ) {
        Icon(painter = painterResource(icon), contentDescription = contentDescription, tint = role.content(), modifier = Modifier.size(iconSize))
    }
}

/** Pill container for a row of icon buttons (Figma ActionStrip / Toolbar). */
@Composable
fun ActionStrip(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Tokens.Pill)
            .border(1.dp, Tokens.Border, CircleShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
fun StripDivider() {
    Box(Modifier.width(1.dp).height(28.dp).background(Tokens.Border))
}

/** Figma "Sticky": the yellow card. Scrolls when the text overflows (PRD F04). */
@Composable
fun StickyCard(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    width: Dp = Tokens.NoteCardWidth,
    height: Dp = Tokens.NoteCardHeight,
) {
    val scroll = rememberScrollState()
    val shape = RoundedCornerShape(Tokens.NoteCornerRadius)
    var m = modifier
        .width(width)
        .height(height)
        .clip(shape)
        .background(Tokens.NoteFill)
        .border(if (selected) 2.dp else 1.dp, if (selected) Tokens.Accent else Tokens.White.copy(alpha = 0.9f), shape)
    if (onClick != null) m = m.spatialHoverEffect().clickable(onClick = onClick)
    Box(modifier = m.padding(horizontal = 24.dp, vertical = 22.dp)) {
        Text(
            text = text,
            color = Tokens.NoteInk,
            style = PicoTheme.typography.bodyLargeMultiline,
            fontSize = 20.sp,
            lineHeight = 30.sp,
            modifier = Modifier.verticalScroll(scroll),
        )
    }
}

/** Far-distance marker (folded mini note) with an accent unfold badge at the top-right; one tap expands. */
@Composable
fun StickyMarker(onClick: () -> Unit, modifier: Modifier = Modifier, selected: Boolean = false) {
    val shape = RoundedCornerShape(20.dp)
    Box(modifier = modifier.size(96.dp)) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(shape)
                .background(Tokens.NoteFill)
                .border(if (selected) 2.dp else 1.dp, if (selected) Tokens.Accent else Tokens.White.copy(alpha = 0.9f), shape)
                .spatialHoverEffect()
                .clickable(onClick = onClick)
                .padding(start = 18.dp, top = 30.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(28, 22, 16).forEach { w ->
                    Box(Modifier.width(w.dp).height(4.dp).clip(CircleShape).background(Tokens.NoteInk.copy(alpha = 0.7f)))
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 0.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Tokens.Accent)
                .border(2.dp, Tokens.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter = painterResource(R.drawable.ic_expand), contentDescription = "Expand", tint = Tokens.OnAccent, modifier = Modifier.size(20.dp))
        }
    }
}

/** Character-count ring (Figma: 20 dp ring in the card corner; accent → danger at the limit). */
@Composable
fun CounterRing(count: Int, max: Int, modifier: Modifier = Modifier) {
    val fraction = (count.toFloat() / max).coerceIn(0f, 1f)
    val color = when {
        count >= max -> Tokens.DangerText
        fraction >= 0.8f -> Tokens.Accent
        else -> Tokens.Muted
    }
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        val inset = stroke.width / 2
        val sz = Size(size.width - stroke.width, size.height - stroke.width)
        drawArc(Tokens.Ink.copy(alpha = 0.18f), 0f, 360f, false, Offset(inset, inset), sz, style = stroke)
        if (fraction > 0f) drawArc(color, -90f, 360f * fraction, false, Offset(inset, inset), sz, style = stroke)
    }
}

/** PRD F06: icon-only delete confirmation. The note's own text is the "what" — no chrome text. */
@Composable
fun DeleteConfirmDialog(note: Note?, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // Vertical: 360 dp surface, which fits inside the 400 dp note window (horizontal would be 480 dp).
        orientation = Orientation.Vertical,
        title = { Text(text = "Delete", color = Tokens.Ink, style = PicoTheme.typography.titleLarge) },
        content = {
            if (note != null) {
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Tokens.NoteFill)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Text(text = note.text, color = Tokens.NoteInk, style = PicoTheme.typography.bodyLargeMultiline, fontSize = 20.sp, lineHeight = 30.sp, maxLines = 3)
                }
            }
        },
        buttons = {
            // Buttons stay icon-only (✕ / 🗑); only the title is text.
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconBtn(R.drawable.ic_close, "Cancel", onClick = onDismiss, width = 120.dp, height = 52.dp, iconSize = 28.dp)
                IconBtn(R.drawable.ic_delete, "Delete", onClick = onConfirm, role = Role.Danger, width = 120.dp, height = 52.dp, iconSize = 28.dp)
            }
        },
    )
}

