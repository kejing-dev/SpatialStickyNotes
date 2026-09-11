package com.example.spatialstickynotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.ButtonSize
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text

/** Figma Button/Primary. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonDefaults.Regular,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        colors = ButtonDefaults.buttonColors(containerColor = Tokens.Accent, contentColor = Tokens.OnAccent),
    ) { Text(text) }
}

/** Figma Button/Secondary (also used for the 编辑 / 移动 toolbar chips). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonDefaults.Regular,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        colors = ButtonDefaults.buttonColors(containerColor = Tokens.Secondary, contentColor = Tokens.Ink),
    ) { Text(text) }
}

/** Figma Button/Danger. */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonDefaults.Regular,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        colors = ButtonDefaults.buttonColors(containerColor = Tokens.DangerBg, contentColor = Tokens.DangerText),
    ) { Text(text) }
}

/** Figma "Sticky": the yellow note card. Scrolls when the text overflows (PRD F04). */
@Composable
fun StickyCard(
    text: String,
    modifier: Modifier = Modifier,
    width: Dp = Tokens.NoteCardWidth,
    minHeight: Dp = Tokens.NoteCardMinHeight,
    scrollable: Boolean = true,
) {
    val scroll = rememberScrollState()
    Box(
        modifier = modifier
            .width(width)
            .defaultMinSize(minHeight = minHeight)
            .clip(RoundedCornerShape(Tokens.NoteCornerRadius))
            .background(Tokens.NoteFill)
            .border(1.dp, Tokens.White.copy(alpha = 0.9f), RoundedCornerShape(Tokens.NoteCornerRadius))
            .padding(horizontal = 24.dp, vertical = 22.dp)
    ) {
        Text(
            text = text,
            color = Tokens.NoteInk,
            style = PicoTheme.typography.bodyLargeMultiline,
            fontSize = 20.sp,
            lineHeight = 30.sp,
            modifier = if (scrollable) Modifier.verticalScroll(scroll) else Modifier,
        )
    }
}

/** Compact far-distance marker (PRD F04 / Figma 03 查看 first frame): no body text. */
@Composable
fun StickyMarker(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Tokens.NoteFill)
            .border(1.dp, Tokens.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .width(22.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Tokens.NoteInk.copy(alpha = 0.75f))
                )
            }
        }
    }
}

/** Figma "Feedback" pill, e.g. "已固定在空间 · 已保存". */
@Composable
fun StatusPill(text: String, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (emphasized) Tokens.Accent else Tokens.Pill)
            .border(1.dp, if (emphasized) Tokens.Accent else Tokens.Border, CircleShape)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (emphasized) Tokens.OnAccent else Tokens.Ink,
            style = PicoTheme.typography.labelMedium,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** Rounded panel on the canvas colour (dialog / list container). */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(24.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Tokens.PanelCornerRadius))
            .background(Tokens.Canvas)
            .padding(padding)
    ) { content() }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Tokens.Ink,
        style = PicoTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier, color: Color = Tokens.Muted) {
    Text(text = text, color = color, style = PicoTheme.typography.bodySmall, modifier = modifier)
}

@Composable
fun FullWidthSpacer(height: Dp) {
    Box(Modifier.fillMaxWidth().height(height))
}
