package com.example.spatialstickynotes.lod

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.platform.LengthUnit
import com.pico.spatial.ui.platform.LocalPhysicalLengthConverter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Viewer-to-window distance estimate for Shared Space.
 *
 * HMD tracking is only available in Full Space, so in Shared Space the app cannot read the head
 * pose directly. PICO OS keeps a WorldScale.Dynamic window at a constant *apparent* size by scaling
 * its physical size with the viewing distance; the PhysicalLengthConverter therefore reports a
 * changing dp→metre factor. A new WindowContainer opens ~1.75 m in front of the user, so the first
 * sample is taken as the 1.75 m baseline and later samples are scaled proportionally.
 *
 * Returns null until a baseline is available. If the platform does not vary the factor, the
 * estimate stays at the baseline (treated as "near" by [LodController]).
 */
private const val DEFAULT_OPEN_DISTANCE_M = 1.75f
private const val SAMPLE_INTERVAL_MS = 200L
private const val TAG = "ViewerDistance"

@Composable
fun rememberViewerDistanceEstimate(): State<Float?> {
    val converter = LocalPhysicalLengthConverter.current
    val estimate = remember { mutableStateOf<Float?>(null) }
    var baseline by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(converter) {
        var lastLogged = -1f
        while (isActive) {
            val sample = runCatching { converter.dpToLength(100.dp, LengthUnit.Meters) }
            val metresPer100dp = sample.getOrDefault(0f)
            if (sample.isFailure) {
                Log.w(TAG, "dpToLength failed: ${sample.exceptionOrNull()}")
            } else if (metresPer100dp <= 0f) {
                Log.w(TAG, "dpToLength returned $metresPer100dp (density=${converter.density}); no estimate")
            }
            if (metresPer100dp > 0f) {
                if (baseline == null) baseline = metresPer100dp
                val b = baseline!!
                val d = DEFAULT_OPEN_DISTANCE_M * (metresPer100dp / b)
                estimate.value = d
                if (kotlin.math.abs(d - lastLogged) > 0.05f) {
                    Log.i(TAG, "metresPer100dp=$metresPer100dp baseline=$b estimate=${"%.2f".format(d)} m")
                    lastLogged = d
                }
            }
            delay(SAMPLE_INTERVAL_MS)
        }
    }
    return estimate
}

/**
 * Two-level LOD with hysteresis (PRD 4.2): enter "near" at <= 1.5 m, leave it at >= 1.8 m, keep the
 * current level in between to avoid flicker at the boundary.
 */
class LodController(
    private val nearEnterM: Float = 1.5f,
    private val nearExitM: Float = 1.8f,
) {
    var isNear by mutableStateOf(true)
        private set
    private var initialized = false

    fun update(distanceM: Float?) {
        if (distanceM == null) return
        if (!initialized) {
            isNear = distanceM <= nearEnterM
            initialized = true
            return
        }
        if (isNear && distanceM >= nearExitM) isNear = false
        else if (!isNear && distanceM <= nearEnterM) isNear = true
    }
}
