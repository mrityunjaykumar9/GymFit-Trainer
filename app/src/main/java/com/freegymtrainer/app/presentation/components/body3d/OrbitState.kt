package com.freegymtrainer.app.presentation.components.body3d

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** The viewer's camera: drag sideways to orbit, or jump to a preset. Yaw 0 is the side view from the near side. */
@Stable
class OrbitState(initialYaw: Float = DEFAULT_YAW, initialPitch: Float = DEFAULT_PITCH) {
    val yaw = Animatable(initialYaw)
    val pitch = Animatable(initialPitch)

    suspend fun dragBy(deltaYaw: Float) { yaw.snapTo(yaw.value + deltaYaw) }

    suspend fun goTo(yawDeg: Float, pitchDeg: Float = DEFAULT_PITCH) {
        // take the short way round the circle
        var target = yawDeg
        while (target - yaw.value > 180f) target -= 360f
        while (target - yaw.value < -180f) target += 360f
        coroutineScope {
            launch { yaw.animateTo(target, tween(420)) }
            launch { pitch.animateTo(pitchDeg, tween(420)) }
        }
    }

    companion object {
        const val DEFAULT_YAW = -35f   // front three-quarter view
        const val DEFAULT_PITCH = 10f
        const val FRONT = -90f
        const val SIDE = 0f
        const val BACK = 90f
        const val OTHER_SIDE = 180f
    }
}
