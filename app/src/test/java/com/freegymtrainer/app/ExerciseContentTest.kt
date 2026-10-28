package com.freegymtrainer.app

import com.freegymtrainer.app.data.seed.ExerciseSeedData
import com.freegymtrainer.app.domain.training.ExerciseCoach
import com.freegymtrainer.app.presentation.components.exerciseSlug
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Every exercise in the library must come complete: motion data, coaching text and its three pictures. */
class ExerciseContentTest {
    private val assets = File("src/main/assets")
    private val motionSlugs: Set<String> by lazy {
        // org.json is not available in plain JVM tests, so read the top-level names with a regex
        Regex("\"([^\"]+)\":[{]\"keys\"").findAll(File(assets, "exercise_motions.json").readText()).map { exerciseSlug(it.groupValues[1]) }.toSet()
    }
    private val slugs = ExerciseSeedData.all.map { exerciseSlug(it.name) }

    @Test fun everyExerciseHasAMotion() {
        val missing = slugs.filter { it !in motionSlugs }
        assertTrue("no animation for: $missing", missing.isEmpty())
    }

    @Test fun everyExerciseHasCoaching() {
        val missing = slugs.filter { ExerciseCoach.of(it) == null }
        assertTrue("no coaching text for: $missing", missing.isEmpty())
        for (s in slugs) {
            val c = ExerciseCoach.of(s)!!
            assertEquals("$s needs exactly 3 form cues", 3, c.cues.size)
            assertTrue("$s: builds line too long", c.builds.length <= 80)
            c.cues.forEach { assertTrue("$s: cue too long: $it", it.length <= 52) }
        }
    }

    @Test fun everyExerciseHasItsPictures() {
        val missing = slugs.flatMap { s -> listOf("a", "b", "t").map { "exercise_pics/${s}_$it.webp" } }
            .filter { !File(assets, it).exists() }
        assertTrue("missing pictures: $missing", missing.isEmpty())
    }

    @Test fun noPhotosShipInTheApp() {
        val banned = listOf("exercise_photos", "exercise_photos_full", "exercise_videos", "gym_pics", "nutrients_pics")
        assertTrue("photo folders are back in assets: ${banned.filter { File(assets, it).exists() }}", banned.none { File(assets, it).exists() })
    }
}
