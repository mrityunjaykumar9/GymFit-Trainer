package com.freegymtrainer.app.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.StrokeCap

private enum class ExerciseMotion { SQUAT, PRESS, PULL, CORE, CARDIO, GENERIC }

private fun motionForCategory(category: String): ExerciseMotion = when (category) {
    "Legs" -> ExerciseMotion.SQUAT
    "Push" -> ExerciseMotion.PRESS
    "Pull" -> ExerciseMotion.PULL
    "Core" -> ExerciseMotion.CORE
    "Cardio" -> ExerciseMotion.CARDIO
    else -> ExerciseMotion.GENERIC
}

/** Joint positions as fractions (0..1) of the canvas width/height, so the pose is resolution-independent. */
private data class FigurePose(
    val head: Offset,
    val hip: Offset,
    val handL: Offset,
    val handR: Offset,
    val footL: Offset,
    val footR: Offset
)

private fun lerpPose(a: FigurePose, b: FigurePose, t: Float) = FigurePose(
    head = lerp(a.head, b.head, t),
    hip = lerp(a.hip, b.hip, t),
    handL = lerp(a.handL, b.handL, t),
    handR = lerp(a.handR, b.handR, t),
    footL = lerp(a.footL, b.footL, t),
    footR = lerp(a.footR, b.footR, t)
)

private fun posesFor(motion: ExerciseMotion): Pair<FigurePose, FigurePose> = when (motion) {
    ExerciseMotion.SQUAT -> FigurePose(
        head = Offset(0.5f, 0.15f), hip = Offset(0.5f, 0.45f),
        handL = Offset(0.34f, 0.42f), handR = Offset(0.66f, 0.42f),
        footL = Offset(0.42f, 0.85f), footR = Offset(0.58f, 0.85f)
    ) to FigurePose(
        head = Offset(0.5f, 0.32f), hip = Offset(0.5f, 0.62f),
        handL = Offset(0.28f, 0.50f), handR = Offset(0.72f, 0.50f),
        footL = Offset(0.36f, 0.85f), footR = Offset(0.64f, 0.85f)
    )
    ExerciseMotion.PRESS -> FigurePose(
        head = Offset(0.5f, 0.15f), hip = Offset(0.5f, 0.45f),
        handL = Offset(0.42f, 0.35f), handR = Offset(0.58f, 0.35f),
        footL = Offset(0.45f, 0.85f), footR = Offset(0.55f, 0.85f)
    ) to FigurePose(
        head = Offset(0.5f, 0.15f), hip = Offset(0.5f, 0.45f),
        handL = Offset(0.18f, 0.28f), handR = Offset(0.82f, 0.28f),
        footL = Offset(0.45f, 0.85f), footR = Offset(0.55f, 0.85f)
    )
    ExerciseMotion.PULL -> FigurePose(
        head = Offset(0.5f, 0.15f), hip = Offset(0.5f, 0.45f),
        handL = Offset(0.18f, 0.40f), handR = Offset(0.82f, 0.40f),
        footL = Offset(0.45f, 0.85f), footR = Offset(0.55f, 0.85f)
    ) to FigurePose(
        head = Offset(0.5f, 0.15f), hip = Offset(0.5f, 0.45f),
        handL = Offset(0.40f, 0.42f), handR = Offset(0.60f, 0.42f),
        footL = Offset(0.45f, 0.85f), footR = Offset(0.55f, 0.85f)
    )
    ExerciseMotion.CORE -> FigurePose(
        head = Offset(0.5f, 0.24f), hip = Offset(0.5f, 0.55f),
        handL = Offset(0.34f, 0.40f), handR = Offset(0.66f, 0.40f),
        footL = Offset(0.40f, 0.85f), footR = Offset(0.60f, 0.85f)
    ) to FigurePose(
        head = Offset(0.5f, 0.38f), hip = Offset(0.5f, 0.55f),
        handL = Offset(0.40f, 0.46f), handR = Offset(0.60f, 0.46f),
        footL = Offset(0.40f, 0.85f), footR = Offset(0.60f, 0.85f)
    )
    ExerciseMotion.CARDIO -> FigurePose(
        head = Offset(0.5f, 0.12f), hip = Offset(0.5f, 0.40f),
        handL = Offset(0.30f, 0.30f), handR = Offset(0.70f, 0.50f),
        footL = Offset(0.36f, 0.85f), footR = Offset(0.64f, 0.78f)
    ) to FigurePose(
        head = Offset(0.5f, 0.14f), hip = Offset(0.5f, 0.42f),
        handL = Offset(0.70f, 0.30f), handR = Offset(0.30f, 0.50f),
        footL = Offset(0.64f, 0.85f), footR = Offset(0.36f, 0.78f)
    )
    ExerciseMotion.GENERIC -> FigurePose(
        head = Offset(0.5f, 0.15f), hip = Offset(0.5f, 0.45f),
        handL = Offset(0.34f, 0.40f), handR = Offset(0.66f, 0.40f),
        footL = Offset(0.42f, 0.85f), footR = Offset(0.58f, 0.85f)
    ) to FigurePose(
        head = Offset(0.5f, 0.22f), hip = Offset(0.5f, 0.52f),
        handL = Offset(0.28f, 0.56f), handR = Offset(0.72f, 0.30f),
        footL = Offset(0.40f, 0.85f), footR = Offset(0.60f, 0.85f)
    )
}

/**
 * A small, original, procedurally-animated stick-figure that loops through a simplified
 * version of the exercise's movement pattern (squat, press, pull, core curl, or a running
 * bounce for cardio). Every shape here is drawn with Canvas primitives at runtime - nothing
 * is a bundled or generated photo/video of a real or fake person, so there's no likeness or
 * stock-footage licensing risk to worry about for a Play Store release.
 */
@Composable
fun AnimatedExerciseFigure(category: String, modifier: Modifier = Modifier) {
    val motion = motionForCategory(category)
    val (poseA, poseB) = posesFor(motion)

    val infiniteTransition = rememberInfiniteTransition(label = "exerciseFigure")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    val figureColor = MaterialTheme.colorScheme.primary
    val groundColor = MaterialTheme.colorScheme.onSurfaceVariant
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier.fillMaxSize().background(backgroundColor)) {
        val w = size.width
        val h = size.height
        val pose = lerpPose(poseA, poseB, phase)
        fun at(o: Offset) = Offset(o.x * w, o.y * h)

        val groundY = 0.85f * h
        drawLine(
            color = groundColor.copy(alpha = 0.25f),
            start = Offset(0.12f * w, groundY),
            end = Offset(0.88f * w, groundY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        val head = at(pose.head)
        val hip = at(pose.hip)
        val shoulder = lerp(head, hip, 0.28f)
        val headRadius = h * 0.075f
        val strokeWidth = h * 0.035f

        // Torso
        drawLine(figureColor, shoulder, hip, strokeWidth, cap = StrokeCap.Round)
        // Arms
        drawLine(figureColor, shoulder, at(pose.handL), strokeWidth * 0.85f, cap = StrokeCap.Round)
        drawLine(figureColor, shoulder, at(pose.handR), strokeWidth * 0.85f, cap = StrokeCap.Round)
        // Legs
        drawLine(figureColor, hip, at(pose.footL), strokeWidth * 0.9f, cap = StrokeCap.Round)
        drawLine(figureColor, hip, at(pose.footR), strokeWidth * 0.9f, cap = StrokeCap.Round)
        // Head
        drawCircle(figureColor, headRadius, head)
    }
}
