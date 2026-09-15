package com.example.spatialstickynotes

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.spatialstickynotes.content.HomeWindow
import com.example.spatialstickynotes.content.NoteWindow
import com.example.spatialstickynotes.content.Windows
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
        // the caption bar stays visible so the user can always drag the note; WorldScale.Dynamic lets
        // the distance estimator work (see lod/ViewerDistance.kt).
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
            PicoTheme {
                NoteWindow(
                    bundle = bundle,
                    modifier = Modifier.windowConstraints(minWidth = 400.dp, minHeight = 400.dp, maxWidth = 400.dp, maxHeight = 400.dp),
                )
            }
        }
    }
