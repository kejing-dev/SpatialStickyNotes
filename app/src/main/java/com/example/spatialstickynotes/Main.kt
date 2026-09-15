package com.example.spatialstickynotes

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.spatialstickynotes.content.HomeWindow
import com.example.spatialstickynotes.content.NoteWindow
import com.example.spatialstickynotes.content.Windows
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.Form
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.foundation.dsl.WindowContainer
import com.pico.spatial.ui.foundation.dsl.WindowContainerSize
import com.pico.spatial.ui.foundation.dsl.WorldScale
import com.pico.spatial.ui.platform.resize.CaptionBarType
import com.pico.spatial.ui.platform.resize.ContainerResizeRestriction
import com.pico.spatial.ui.platform.resize.ContainerResizeType

/**
 * Shared Space app (v1.2, icon-only): a default Planar window (home / list) and one Planar window
 * per sticky note. Creation and editing happen inline in the note window. No Stage is opened and no
 * MR permissions are used.
 */
fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultWindowContainer {
            PicoTheme {
                HomeWindow(modifier = Modifier.windowConstraints(minWidth = 900.dp, minHeight = 560.dp))
            }
        }

        // One window per sticky note. No glass background so the yellow card reads as a real note;
        // the caption bar stays visible so the user can always drag the note. The window height follows
        // the content (one-line card, folded marker, or up to the 400 dp maximum).
        WindowContainer(
            id = Windows.NOTE,
            form = Form.Planar,
            defaultSize = WindowContainerSize(width = 400.dp, height = 400.dp),
            resizeType = ContainerResizeType.ContentSize,
            defaultResizeRestriction = ContainerResizeRestriction.UniformResizable,
            enableMaterialBackground = false,
            defaultCaptionBarType = CaptionBarType.Default,
            worldScale = WorldScale.Dynamic,
        ) {
            // The SDK resizes a ContentSize window whenever its windowConstraints change, so the note
            // reports its content height and the constraints pin the window to exactly that height.
            var noteHeight by remember { mutableStateOf(400.dp) }
            PicoTheme {
                NoteWindow(
                    bundle = bundle,
                    onContentHeight = { noteHeight = it.coerceIn(120.dp, 400.dp) },
                    modifier = Modifier.windowConstraints(minWidth = 400.dp, minHeight = noteHeight, maxWidth = 400.dp, maxHeight = noteHeight),
                )
            }
        }
    }
