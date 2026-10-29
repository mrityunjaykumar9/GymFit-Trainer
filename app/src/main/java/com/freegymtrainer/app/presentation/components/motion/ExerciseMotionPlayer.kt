package com.freegymtrainer.app.presentation.components.motion

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freegymtrainer.app.data.local.entity.Exercise
import com.freegymtrainer.app.presentation.components.body3d.Athlete3DCanvas
import com.freegymtrainer.app.presentation.components.body3d.isAthleteModelAvailable
import com.freegymtrainer.app.presentation.components.body3d.OrbitState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** Solid (opaque) colors derived once per theme so overlapping strokes never double-darken like alpha strokes would. */
private class MotionPalette(
    val bg: Color, val backdrop: Brush, val near: Color, val torso: Color, val far: Color,
    val furniture: Color, val metal: Color, val accent: Color, val label: Color
)

@Composable
private fun rememberMotionPalette(): MotionPalette {
    val cs = MaterialTheme.colorScheme
    return remember(cs) {
        MotionPalette(
            bg = cs.surface,
            backdrop = Brush.verticalGradient(listOf(cs.surface, lerp(cs.surface, cs.primary, 0.10f))),
            near = cs.onSurface,
            torso = lerp(cs.surface, cs.onSurface, 0.80f),
            far = lerp(cs.surface, cs.onSurface, 0.48f),
            furniture = lerp(cs.surface, cs.onSurface, 0.17f),
            metal = lerp(cs.surface, cs.onSurface, 0.50f),
            accent = cs.primary,
            label = cs.onSurface
        )
    }
}

/**
 * Draws [motion] at [progress] (0..1, read inside the draw phase so animating it never
 * recomposes). [showAngles] adds a live joint-angle readout and a guide arc on the main joint,
 * so people can see how far to bend - the part a still photo can't show.
 */
@Composable
fun ExerciseMotionCanvas(
    motion: MotionDef,
    progress: () -> Float,
    showAngles: Boolean,
    modifier: Modifier = Modifier,
    description: String? = null,
    /** Draw the athlete facing left instead of right (a mirror image; labels stay readable). */
    mirror: Boolean = false
) {
    val palette = rememberMotionPalette()
    val context = LocalContext.current
    val sprites = remember { BodySpriteLibrary.get(context) }
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = palette.label)
    val semanticsModifier = if (description != null) Modifier.semantics { contentDescription = description } else Modifier

    Canvas(modifier.then(semanticsModifier).fillMaxSize().background(palette.backdrop)) {
        val p = progress()
        drawMotion(motion, motion.skeletonAt(p), p, palette, showAngles, measurer, labelStyle, sprites, mirror)
    }
}

private fun DrawScope.drawMotion(
    motion: MotionDef,
    sk: Skeleton,
    progress: Float,
    pal: MotionPalette,
    showAngles: Boolean,
    measurer: TextMeasurer,
    labelStyle: androidx.compose.ui.text.TextStyle,
    sprites: BodySprites?,
    mirror: Boolean
) {
    val view = motion.frame
    val sc = minOf(size.width / (view[1] - view[0]), size.height / (view[3] - view[2]))
    val ox = size.width / 2f - (view[0] + view[1]) / 2f * sc
    val oy = size.height / 2f + (view[2] + view[3]) / 2f * sc
    // Mirroring flips only where things are drawn (about the middle of the canvas); text is drawn normally.
    fun s(p: Pt) = Offset(if (mirror) size.width - (ox + p.x * sc) else ox + p.x * sc, oy - p.y * sc)

    fun propColor(role: String) = when (role) {
        "metal" -> pal.metal
        "acc" -> pal.accent
        else -> pal.furniture
    }

    val machine = MachineColors(frame = pal.furniture, steel = pal.metal, accent = pal.accent, bg = pal.bg)

    /** pass 0 = behind the athlete, pass 1 = in front (a cable's tower is behind, the grip is in front of the hand). */
    fun drawProp(p: Prop, pass: Int) {
        if (p !is Prop.Cable && (p.z > 0) != (pass == 1)) return
        when (p) {
            is Prop.Line -> drawLine(propColor(p.role), s(sk.resolve(p.a)), s(sk.resolve(p.b)), p.w * sc, StrokeCap.Round)
            is Prop.Circle -> {
                val c = s(sk.resolve(p.at))
                if (p.fill) drawCircle(propColor(p.role), p.r * sc, c)
                else drawCircle(propColor(p.role), p.r * sc, c, style = Stroke(width = 0.012f * sc))
            }
            is Prop.Plate -> {
                val c = s(sk.resolve(p.at))
                drawCircle(pal.metal, p.r * sc, c)
                drawCircle(pal.accent, p.r * 0.72f * sc, c)
                drawCircle(pal.metal, 0.02f * sc, c)
            }
            is Prop.Dumbbell -> drawCircle(pal.metal, p.r * sc, s(sk.resolve(p.at)))
            is Prop.Cable -> {
                val pulley = sk.resolve(p.from)
                val hand = sk.resolve(p.at)
                if (pass == 0) {
                    drawCableTower(pulley, p.tower, ::s, sc, machine)
                    drawCableLine(pulley, hand, ::s, sc, machine)
                } else {
                    drawCableGrip(pulley, hand, p.grip, ::s, sc, machine)
                }
            }
            is Prop.RopeLoop -> {
                val c = sk.resolve(p.at)
                val topLeft = s(Pt(c.x - p.rx, c.y))
                val bottomRight = s(Pt(c.x + p.rx, 0f))
                drawOval(pal.metal, topLeft, Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y), style = Stroke(width = 0.008f * sc))
            }
        }
    }

    // Stage: a soft spotlight behind the athlete and a rubber-mat floor that fades toward the bottom.
    val floorY = s(Pt(0f, 0f)).y
    drawRect(
        Brush.radialGradient(
            listOf(pal.accent.copy(alpha = 0.20f), Color.Transparent),
            center = Offset(size.width / 2f, floorY - (view[3] - view[2]) * 0.40f * sc),
            radius = size.width * 0.62f
        )
    )
    if (floorY < size.height) {
        drawRect(
            Brush.verticalGradient(
                listOf(pal.furniture.copy(alpha = 0.85f), pal.furniture.copy(alpha = 0.15f)),
                startY = floorY, endY = size.height
            ),
            topLeft = Offset(0f, floorY), size = Size(size.width, size.height - floorY)
        )
    }
    drawLine(pal.furniture, Offset(0f, floorY), Offset(size.width, floorY), 0.010f * sc)

    motion.props.forEach { drawProp(it, 0) }

    // Soft ground shadow so the athlete sits on the floor instead of floating.
    val shadowX = (sk.ankleN.x + sk.ankleF.x + sk.hip.x) / 3f
    drawOval(
        Color.Black.copy(alpha = 0.16f),
        Offset(s(Pt(shadowX - 0.20f, 0f)).x, s(Pt(0f, 0f)).y - 0.012f * sc),
        Size(0.40f * sc, 0.030f * sc)
    )

    // Dotted path of the working joint (bar / foot path) behind the figure.
    drawTrail(motion.trail, progress, ::s, sc, pal.accent)

    val pulse = 0.5f + 0.5f * sin(progress * 2f * Math.PI.toFloat() * 2f)
    drawBodybuilder(sk, motion.focus, pulse, ::s, sc, sprites)

    motion.props.forEach { drawProp(it, 1) }

    if (showAngles && motion.guides.isNotEmpty()) {
        drawGuides(motion.guides, FlatGuideSpace(sk, sc, ::s), pulse, measurer, labelStyle.copy(fontSize = 11.sp, color = Color.White))
    }
    if (showAngles && motion.hasMeasures) {
        motion.measures.firstOrNull { !it.isLean }?.let { drawAngleGuide(sk, it, ::s, sc, pal, measurer, labelStyle) }
    }
}

private fun DrawScope.drawLabelPill(
    text: String, center: Offset, pal: MotionPalette, measurer: TextMeasurer, style: androidx.compose.ui.text.TextStyle, anchorTopLeft: Boolean = false
) {
    val layout = measurer.measure(AnnotatedString(text), style)
    val padX = 7.dp.toPx()
    val padY = 3.dp.toPx()
    val w = layout.size.width + padX * 2
    val h = layout.size.height + padY * 2
    val topLeft = if (anchorTopLeft) center else Offset(center.x - w / 2f, center.y - h / 2f)
    drawRoundRect(pal.bg.copy(alpha = 0.94f), topLeft, Size(w, h), CornerRadius(h / 2f))
    drawRoundRect(pal.accent, topLeft, Size(w, h), CornerRadius(h / 2f), style = Stroke(1.dp.toPx()))
    drawText(layout, pal.label, Offset(topLeft.x + padX, topLeft.y + padY))
}

private fun DrawScope.drawAngleGuide(
    sk: Skeleton, m: Measure, s: (Pt) -> Offset, sc: Float, pal: MotionPalette,
    measurer: TextMeasurer, style: androidx.compose.ui.text.TextStyle
) {
    val a = s(sk.point(m.joints[0]))
    val b = s(sk.point(m.joints[1]))
    val c = s(sk.point(m.joints[2]))
    val r = 0.075f * sc
    val a1 = Math.toDegrees(atan2((a.y - b.y).toDouble(), (a.x - b.x).toDouble())).toFloat()
    val a2 = Math.toDegrees(atan2((c.y - b.y).toDouble(), (c.x - b.x).toDouble())).toFloat()
    var sweep = a2 - a1
    while (sweep > 180f) sweep -= 360f
    while (sweep < -180f) sweep += 360f
    val topLeft = Offset(b.x - r, b.y - r)
    val arcSize = Size(r * 2, r * 2)
    drawArc(pal.accent.copy(alpha = 0.25f), a1, sweep, true, topLeft, arcSize)
    drawArc(pal.accent, a1, sweep, false, topLeft, arcSize, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

    // Label sits just outside the wedge, along its bisector.
    val u1 = Offset(a.x - b.x, a.y - b.y).let { val l = hypot(it.x, it.y).coerceAtLeast(1e-3f); Offset(it.x / l, it.y / l) }
    val u2 = Offset(c.x - b.x, c.y - b.y).let { val l = hypot(it.x, it.y).coerceAtLeast(1e-3f); Offset(it.x / l, it.y / l) }
    var bis = Offset(u1.x + u2.x, u1.y + u2.y)
    val bl = hypot(bis.x, bis.y)
    bis = if (bl < 0.2f) Offset(-u1.y, u1.x) else Offset(bis.x / bl, bis.y / bl)
    val labelCenter = Offset(b.x + bis.x * (r + 22.dp.toPx()), b.y + bis.y * (r + 22.dp.toPx()))
}

private fun DrawScope.drawAngleHud(
    motion: MotionDef, sk: Skeleton, pal: MotionPalette, measurer: TextMeasurer, style: androidx.compose.ui.text.TextStyle
) {
    var y = 10.dp.toPx()
    val x = 10.dp.toPx()
    for (m in motion.measures) {
        drawLabelPill("${m.name} ${sk.measure(m).roundToInt()}°", Offset(x, y), pal, measurer, style, anchorTopLeft = true)
        y += 28.dp.toPx()
    }
}

/** Thin progress/scrub bar with a tick at every key pose. */
@Composable
private fun MotionScrubber(
    motion: MotionDef,
    progress: () -> Float,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val fill = MaterialTheme.colorScheme.primary
    val keyTimes = remember(motion) { motion.keyTimes }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(motion) {
                detectTapGestures { offset -> onScrubStart(); onScrub((offset.x / size.width).coerceIn(0f, 0.999f)) }
            }
            .pointerInput(motion) {
                detectHorizontalDragGestures(
                    onDragStart = { onScrubStart() },
                    onHorizontalDrag = { change, _ -> onScrub((change.position.x / size.width).coerceIn(0f, 0.999f)) }
                )
            }
    ) {
        val cy = size.height / 2f
        val barH = 4.dp.toPx()
        val p = progress().coerceIn(0f, 1f)
        drawRoundRect(track, Offset(0f, cy - barH / 2), Size(size.width, barH), CornerRadius(barH / 2))
        drawRoundRect(fill, Offset(0f, cy - barH / 2), Size(size.width * p, barH), CornerRadius(barH / 2))
        keyTimes.drop(1).forEach { t -> drawCircle(track, 3.dp.toPx(), Offset(size.width * t, cy)) }
        drawCircle(fill, 8.dp.toPx(), Offset(size.width * p, cy))
    }
}

/** The viewer's last choice of facing direction, kept for the session so it carries from one exercise to the next. */
private var rememberedMirror = false

private fun animationsDisabledBySystem(context: android.content.Context): Boolean =
    try {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    } catch (e: Exception) {
        false
    }

/**
 * The full exercise "movement guide": looping animation with live joint angles, a scrubber,
 * play/pause, slow motion, and a plain-language cue for each phase of the rep.
 */
@Composable
fun ExerciseMotionPlayer(
    exercise: Exercise,
    motion: MotionDef,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val context = LocalContext.current
    val progress = remember(motion) { mutableFloatStateOf(0f) }
    var playing by rememberSaveable { mutableStateOf(!animationsDisabledBySystem(context)) }
    var slow by rememberSaveable { mutableStateOf(false) }
    var mirrored by rememberSaveable { mutableStateOf(rememberedMirror) }
    var showAngles by rememberSaveable { mutableStateOf(true) }
    var view3d by rememberSaveable { mutableStateOf(isAthleteModelAvailable(context)) }
    val orbit = remember { OrbitState() }
    val scope = rememberCoroutineScope()
    val speed = if (slow) 0.5f else 1f

    LaunchedEffect(playing, speed, motion) {
        if (!playing) return@LaunchedEffect
        var last = awaitFrame()
        while (true) {
            val now = awaitFrame()
            // Clamp: after the app was backgrounded the first frame can be minutes later.
            val dtMs = ((now - last) / 1_000_000f).coerceIn(0f, 50f)
            last = now
            progress.floatValue = (progress.floatValue + dtMs * speed / motion.durationMs) % 1f
        }
    }

    val phase by remember(motion) { derivedStateOf { motion.phaseIndex(progress.floatValue) } }

    // Wide, low movements (bench press) get a wide, short frame; tall ones (pull-up) a taller one.
    val frameRatio = remember(motion) { ((motion.frame[1] - motion.frame[0]) / (motion.frame[3] - motion.frame[2])).coerceIn(if (compact) 1.3f else 0.9f, if (compact) 1.8f else 1.5f) }

    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(if (view3d) (if (compact) 1.3f else 1.0f) else frameRatio)
                .clip(RoundedCornerShape(16.dp))
        ) {
            if (view3d) Athlete3DCanvas(motion = motion, progress = { progress.floatValue }, orbit = orbit, exerciseName = exercise.name, showGuides = showAngles)
            else ExerciseMotionCanvas(
                motion = motion,
                progress = { progress.floatValue },
                showAngles = showAngles,
                mirror = mirrored,
                description = "Animated side view showing how to perform ${exercise.name}"
            )
            if (!com.freegymtrainer.app.presentation.components.body3d.DebugView.clean) Text(
                if (view3d) "DRAG TO ROTATE" else "SIDE VIEW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
            )
        }

        // The words for the numbered badges on the animation (kept out of the picture so the athlete stays visible).
        if (showAngles && motion.hasGuides) GuideLegend(motion, { progress.floatValue })

        if (view3d) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Front" to OrbitState.FRONT, "3/4 view" to OrbitState.DEFAULT_YAW, "Side" to OrbitState.SIDE, "Back" to OrbitState.BACK, "Other side" to OrbitState.OTHER_SIDE)
                    .forEach { (label, yaw) ->
                        AssistChip(onClick = { scope.launch { orbit.goTo(yaw) } }, label = { Text(label) })
                    }
            }
        }
        MuscleChips(exercise, compact)
        Spacer(Modifier.height(6.dp))

        if (!compact) {
            MotionScrubber(
                motion = motion,
                progress = { progress.floatValue },
                onScrubStart = { playing = false },
                onScrub = { progress.floatValue = it }
            )
        } else {
            Spacer(Modifier.height(6.dp))
        }

        // A wrapping row: with the Guides chip the controls no longer fit on one line on narrow phones in the 2D view.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            FilledTonalIconButton(onClick = { playing = !playing }, modifier = Modifier.align(Alignment.CenterVertically)) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause animation" else "Play animation"
                )
            }
            FilledTonalIconButton(onClick = { progress.floatValue = 0f; playing = true }, modifier = Modifier.align(Alignment.CenterVertically)) {
                Icon(Icons.Filled.Replay, contentDescription = "Restart animation")
            }
            FilterChip(selected = view3d, onClick = { view3d = !view3d }, label = { Text("3D") }, modifier = Modifier.align(Alignment.CenterVertically))
            if (!view3d) FilterChip(
                modifier = Modifier.align(Alignment.CenterVertically),
                selected = mirrored,
                onClick = { mirrored = !mirrored; rememberedMirror = mirrored },
                label = { Text("Flip") },
                leadingIcon = { Icon(Icons.Filled.SwapHoriz, contentDescription = "Flip direction", modifier = Modifier.size(18.dp)) }
            )
            FilterChip(selected = slow, onClick = { slow = !slow }, label = { Text("0.5×") }, modifier = Modifier.align(Alignment.CenterVertically))
            if (motion.hasGuides) {
                FilterChip(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    selected = showAngles,
                    onClick = { showAngles = !showAngles },
                    label = { Text("Guides") },
                    leadingIcon = { Icon(Icons.Filled.Straighten, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        // Plain swap (no cross-fade): two different-length captions overlapping mid-fade looked garbled.
        run {
            val index = phase
            Column(Modifier.fillMaxWidth().heightIn(min = if (compact) 36.dp else 64.dp)) {
                val label = motion.phaseLabel(index)
                if (label.isNotBlank()) {
                    Text(
                        "STEP ${index + 1} OF ${motion.keyTimes.size}  ·  ${label.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val cue = motion.phaseCue(index)
                if (cue.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(cue, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (motion.tip.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(motion.tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

/** Small self-running loop for thumbnails/list rows where there is no photo (no controls, no angle readout). */
@Composable
fun ExerciseMotionLoop(motion: MotionDef, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "motionLoop")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(motion.durationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "progress"
    )
    ExerciseMotionCanvas(motion = motion, progress = { progress.value }, showAngles = false, modifier = modifier)
}

/** "Working muscles" legend: the glowing red areas on the athlete, named from the exercise's own data. */
@Composable
private fun MuscleChips(exercise: Exercise, compact: Boolean) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(BodyPalette.hot))
            Spacer(Modifier.width(6.dp))
            // Yoga poses list "Flexibility" / "Full Body" as the primary area, which is a focus rather than a muscle.
            val isMuscle = exercise.primaryMuscle.lowercase() !in setOf("flexibility", "full body", "cardio", "mobility")
            Text(
                if (isMuscle) "WORKING MUSCLES" else "FOCUS AREAS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MuscleChip(exercise.primaryMuscle, primary = true)
            if (!compact) exercise.secondaryMuscles.filter { it.isNotBlank() }.forEach { MuscleChip(it, primary = false) }
        }
    }
}

@Composable
private fun MuscleChip(name: String, primary: Boolean) {
    val bg = if (primary) BodyPalette.hot else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (primary) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        name,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = fg,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
