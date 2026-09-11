package com.example.spatialstickynotes

import androidx.compose.ui.unit.dp
import com.example.spatialstickynotes.content.HomeWindow
import com.example.spatialstickynotes.content.NoteEditorWindow
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
 * Shared Space app: a default Planar window (home / list), an editor window and one Planar window
 * per sticky note. No Stage is opened, so the app never takes over the space and needs no MR
 * permissions (plane detection / anchors are intentionally not used).
 */
fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultWindowContainer {
            PicoTheme {
                HomeWindow(modifier = androidx.compose.ui.Modifier.windowConstraints(minWidth = 900.dp, minHeight = 560.dp))
            }
        }

        // Figma "新建便签 / 编辑便签" panel: 560 × 640 dp, opened in front of the viewer.
        WindowContainer(
            id = Windows.EDITOR,
            form = Form.Planar,
            defaultSize = WindowContainerSize(width = 560.dp, height = 640.dp),
            resizeType = ContainerResizeType.ContentSize,
            defaultResizeRestriction = ContainerResizeRestriction.UniformResizable,
            enableMaterialBackground = true,
        ) {
            PicoTheme {
                NoteEditorWindow(
                    bundle = bundle,
                    modifier = androidx.compose.ui.Modifier.windowConstraints(
                        minWidth = 560.dp, minHeight = 640.dp, maxWidth = 560.dp, maxHeight = 640.dp,
                    ),
                )
            }
        }

        // One window per sticky note. No glass background so the yellow card reads as a real note;
        // WorldScale.Dynamic lets the distance estimator work (see lod/ViewerDistance.kt).
        WindowContainer(
            id = Windows.NOTE,
            form = Form.Planar,
            defaultSize = WindowContainerSize(width = 400.dp, height = 400.dp),
            resizeType = ContainerResizeType.ContentSize,
            defaultResizeRestriction = ContainerResizeRestriction.UniformResizable,
            enableMaterialBackground = false,
            defaultCaptionBarType = CaptionBarType.AutomaticHide,
            worldScale = WorldScale.Dynamic,
        ) {
            PicoTheme {
                NoteWindow(
                    bundle = bundle,
                    modifier = androidx.compose.ui.Modifier.windowConstraints(
                        minWidth = 400.dp, minHeight = 400.dp, maxWidth = 400.dp, maxHeight = 400.dp,
                    ),
                )
            }
        }
    }
