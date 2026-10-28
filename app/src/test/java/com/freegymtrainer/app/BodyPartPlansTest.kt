package com.freegymtrainer.app

import com.freegymtrainer.app.data.seed.ExerciseSeedData
import com.freegymtrainer.app.domain.training.BodyPartPlans
import com.freegymtrainer.app.domain.training.TrainingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyPartPlansTest {

    private val exerciseNames = ExerciseSeedData.all.map { it.name }.toSet()

    @Test fun everyRoutineExerciseExistsInTheLibrary() {
        val missing = BodyPartPlans.all.flatMap { part ->
            part.allExerciseNames.filter { it !in exerciseNames }.map { "${part.title}: $it" }
        }
        assertTrue("routine exercises missing from the library: $missing", missing.isEmpty())
    }

    @Test fun everyBodyPartHasARoutineForEveryLevel() {
        for (part in BodyPartPlans.all) for (level in TrainingLevel.values()) {
            val plan = part.plan(level)
            assertTrue("${part.title} $level needs at least 3 exercises", plan.size >= 3)
            assertEquals("${part.title} $level repeats an exercise", plan.size, plan.map { it.exercise }.toSet().size)
            plan.forEach {
                assertTrue("${it.exercise}: sets", it.sets in 1..6)
                assertTrue("${it.exercise}: rest", it.restSeconds in 30..240)
                assertTrue("${it.exercise}: tip", it.tip.isNotBlank())
            }
        }
    }

    @Test fun harderLevelsDoNotHaveLessWork() {
        for (part in BodyPartPlans.all) {
            val beg = part.plan(TrainingLevel.BEGINNER).sumOf { it.sets }
            val adv = part.plan(TrainingLevel.ADVANCED).sumOf { it.sets }
            assertTrue("${part.title}: advanced ($adv sets) should be at least beginner ($beg sets)", adv >= beg)
        }
    }

    @Test fun idsAreUniqueAndLookupWorks() {
        val ids = BodyPartPlans.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertTrue(BodyPartPlans.byId(it) != null) }
    }
}
