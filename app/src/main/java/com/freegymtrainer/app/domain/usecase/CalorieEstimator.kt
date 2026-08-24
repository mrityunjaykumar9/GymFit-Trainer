package com.freegymtrainer.app.domain.usecase

/** Rough MET-based calorie estimate for a resistance-training session. Explicitly an estimate. */
object CalorieEstimator {
    // Average MET for moderate-to-vigorous resistance training is ~5-6.
    private const val MET = 5.5

    fun estimateForSession(durationMinutes: Int, bodyWeightKg: Float): Int {
        val hours = durationMinutes / 60.0
        return (MET * bodyWeightKg * hours).toInt()
    }
}
