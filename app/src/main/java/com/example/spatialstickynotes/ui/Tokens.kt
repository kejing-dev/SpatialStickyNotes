package com.example.spatialstickynotes.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.TextStyle

/** Colour tokens taken from the Figma "02 本地组件" page (PICO Spatial UI · PRD v0.2). */
object Tokens {
    val Canvas = Color(0xFFF4F5F1)      // color/canvas – dialog / panel background
    val Ink = Color(0xFF1F2A25)         // color/ink – primary text
    val Muted = Color(0xFF6A756E)       // color/muted – secondary text
    val Border = Color(0xFFCAD2C6)      // color/border
    val Accent = Color(0xFF2E5F4B)      // Button/Primary
    val OnAccent = Color(0xFFFFFFFF)
    val Secondary = Color(0xFFDCE4D9)   // Button/Secondary, Toolbar chips
    val DangerBg = Color(0xFFF0D9D3)    // Button/Danger
    val DangerText = Color(0xFF8C3B2D)
    val NoteFill = Color(0xFFF5E9A9)    // color/note – sticky card & input
    val NoteInk = Color(0xFF2B2F27)
    val Pill = Color(0xFFF6F7F3)        // Feedback pill
    val White = Color(0xFFFFFFFF)

    val NoteCornerRadius = 20.dp
    val PanelCornerRadius = 24.dp
    val NoteCardWidth = 328.dp
    /** Card height follows the text: 1 line = 30 dp line height + 2 × 22 dp padding; at most 7 lines, then it scrolls. */
    val NoteCardMinHeight = 74.dp
    val NoteCardMaxHeight = 254.dp
    /** The inline editor keeps room for at least 4 lines. */
    val NoteEditorMinHeight = 164.dp

    /**
     * Note body: 20 sp on an exact 30 dp line pitch. Compose (PICO fork) trims the first/last line's
     * half-leading by default, which would make n lines measure 30n − 6 dp; Trim.None keeps every line
     * at 30 dp so the 1-line / 7-line card heights above are exact.
     */
    fun noteTextStyle(base: TextStyle): TextStyle = base.copy(
        color = NoteInk,
        fontSize = 20.sp,
        lineHeight = 30.sp,
        lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None),
    )
}
