package com.freegymtrainer.app.presentation.components.body3d

import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freegymtrainer.app.presentation.components.motion.MotionDef
import com.freegymtrainer.app.presentation.components.motion.drawGuides
import com.google.android.filament.android.UiHelper
import kotlinx.coroutines.launch

/** False when assets/models/athlete.glb is not in the APK (it is gitignored); the player then falls back to the 2D view. */
fun isAthleteModelAvailable(context: android.content.Context): Boolean =
    try { context.assets.open("models/athlete.glb").close(); true } catch (e: java.io.IOException) { false }

/** Debug-build shortcut state (set from intent extras in MainActivity) to freeze / aim the 3D view for screenshots. */
object DebugView {
    var yaw: Float? = null
    var pitch: Float? = null
    var t: Float? = null
    /** Hides UI chrome (labels) so screenshots can be cut into clean pictures. */
    var clean: Boolean = false
    /** In clean mode the coaching guides are hidden (so pictures come out plain) unless this is set (`debug_guides`). */
    var guides: Boolean = false
}

/**
 * The 3D athlete performing [motion]: a realistic skinned bodybuilder model rendered with Filament. Reads
 * [progress] (0..1) every frame; drag sideways to orbit (see [OrbitState]), double-tap to reset the view.
 */
@Composable
fun Athlete3DCanvas(
    motion: MotionDef,
    progress: () -> Float,
    orbit: OrbitState,
    modifier: Modifier = Modifier,
    exerciseName: String? = null,
    /** Draws the coaching guides (dashed lines, rings, angle readouts) over the athlete. */
    showGuides: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val backdrop = remember(cs) {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(androidx.compose.ui.graphics.lerp(cs.surface, Color.Black, 0.35f), androidx.compose.ui.graphics.lerp(cs.surface, cs.primary, 0.16f))
        )
    }
    val stage = remember { AthleteStage(context) }
    val uiHelper = remember { UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK) }
    val choreographer = remember { Choreographer.getInstance() }

    LaunchedEffect(motion, exerciseName) {
        stage.exerciseName = exerciseName
        stage.setMotion(motion)
    }
    val frame = remember {
        object : Choreographer.FrameCallback {
            var running = true
            override fun doFrame(frameTimeNanos: Long) {
                if (!running) return
                choreographer.postFrameCallback(this)
                stage.render(frameTimeNanos, DebugView.t ?: progressRef(), DebugView.yaw ?: orbit.yaw.value, DebugView.pitch ?: orbit.pitch.value, 0f)
            }
            var progressRef: () -> Float = { 0f }
        }
    }
    frame.progressRef = progress

    DisposableEffect(Unit) {
        choreographer.postFrameCallback(frame)
        onDispose {
            frame.running = false
            choreographer.removeFrameCallback(frame)
            uiHelper.detach()
            stage.destroy()
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(backdrop)
            .pointerInput(Unit) { detectHorizontalDragGestures { _, dx -> scope.launch { orbit.dragBy(dx * 0.45f) } } }
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { scope.launch { orbit.goTo(OrbitState.DEFAULT_YAW) } }) }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                TextureView(ctx).also { tv ->
                    uiHelper.isOpaque = false
                    uiHelper.renderCallback = object : UiHelper.RendererCallback {
                        override fun onNativeWindowChanged(surface: Surface) = stage.onSurface(surface)
                        override fun onDetachedFromSurface() = stage.onSurfaceDestroyed()
                        override fun onResized(width: Int, height: Int) = stage.onResized(width, height)
                    }
                    uiHelper.attachTo(tv)
                }
            }
        )
        if (showGuides && (!DebugView.clean || DebugView.guides) && motion.guides.isNotEmpty()) {
            val measurer = androidx.compose.ui.text.rememberTextMeasurer()
            val style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = Color.White
            )
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                stage.frameTick.intValue
                val space = stage.guideSpace(size.width, size.height) ?: return@Canvas
                val p = DebugView.t ?: progress()
                val pulse = 0.5f + 0.5f * kotlin.math.sin(p * 4f * Math.PI.toFloat())
                val layout = com.freegymtrainer.app.presentation.components.motion.PillLayout()
                drawGuides(motion.guides, space, pulse, measurer, style, layout)
            }
        }
        if (!stage.ready) {
            Text(
                "3D model unavailable",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
