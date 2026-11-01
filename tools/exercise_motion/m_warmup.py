"""Dynamic warm-up / mobility drills - the pre-lifting counterpart to m_yoga.py's held stretches.
These are reps, not holds: short 2-4 key loops meant to play continuously, same engine as
everything else. Registered separately so the "Warm-Up & Mobility" section can draw on both
pools (see ExerciseSeedData.kt's isHeldPose flag for which is which)."""
from lib import *
from engine import vec, add

FLAT = foot(0.0)
STAND_ARMS = fkarm(180, 180)
MAT = prop_line((-.75, .02), (.75, .02), .03)
STAND = lambda t, arms=STAND_ARMS, T=0, **kw: key(t, (0, .5295), T, FLAT, arms, **kw)

reg("Leg Swings", [
    key(0.00, (0, .5295), 0, FLAT, STAND_ARMS, legF=fkleg(130, 135, ft=115), armF=STAND_ARMS,
        label="Swing forward", cue="Hold a wall or rack for balance"),
    key(0.50, (0, .5295), 0, FLAT, STAND_ARMS, legF=fkleg(210, 205, ft=220), armF=STAND_ARMS,
        label="Swing back", cue="Keep the swinging leg mostly straight, controlled not kicking"),
], [MAT], [], ["thigh", "glutes"], dur=2600, view=[-0.6, 0.6, -0.08, 1.35],
    tip="Opens the hips before squats or deadlifts - 10-12 swings each leg, then switch sides.")

reg("Jumping Jacks", [
    key(0.00, (0, .5295), 0, FLAT, STAND_ARMS, legF=FLAT, armF=STAND_ARMS,
        label="Feet together, arms down", cue="Soft knees, ready to jump"),
    key(0.50, (0, .60), 0, foot(.18, .10), fkarm(5, 3), legF=foot(-.18, .10), armF=fkarm(5, 3),
        label="Jump out, arms overhead", cue="Land softly on the balls of your feet"),
], [MAT], [], ["shoulder", "thigh"], dur=2200, view=[-0.6, 0.6, -0.08, 1.5],
    tip="A quick, simple way to raise your heart rate before a session.")

_hk_up = fkleg(100, 210, ft=200)
_hk_down = fkleg(180, 180, ft=90)
reg("High Knees", [
    key(0.00, (0, .56), 0, _hk_up, fkarm(70, 120), legF=_hk_down, armF=fkarm(220, 170),
        label="Right knee up", cue="Drive your knee up toward hip height"),
    key(0.50, (0, .56), 0, _hk_down, fkarm(220, 170), legF=_hk_up, armF=fkarm(70, 120),
        label="Switch legs", cue="Quick, light steps, stay on the balls of your feet"),
], [MAT], [], ["thigh", "abs"], dur=1800, view=[-0.5, 0.5, -0.08, 1.4],
    tip="Raises your heart rate and primes the hip flexors - 20-30 seconds is plenty.")

reg("Standing Toe Touch", [
    key(0.00, (0, .5295), 0, FLAT, hand(0.045, 0.475), legF=FLAT, armF=hand(0.045, 0.475), label="Stand tall", cue="Feet hip-width apart"),
    # The old hand target (.12,.07) was only 68% of max arm reach from the shoulder - nowhere near
    # straight, and well short of the feet (at x~0) too. Targeting the feet directly (same point as
    # the foot target) and trimming T from 130 to 120 (which, with this hip position, actually
    # brings the shoulder closer to the floor target, not further - the shallower hinge angle
    # matters more here than the extra lean) gets the reach to 98% of max: a straight arm that
    # actually reaches the shoes instead of stopping well short of them.
    key(0.50, (-.10, .50), 120, foot(.0, .04, b=1), hand(0, .045), legF=foot(-.03, .04, b=1), armF=hand(0, .045), head=30,
        label="Hinge and reach", cue="Soft knees, hinge from the hips, reach toward your toes"),
], [MAT], [meas("Hip", "shoulder", "hip", "kneeN")], ["hamstrings", "back"], dur=3600, view=[-0.55, 0.6, -0.08, 1.35],
    tip="A dynamic hamstring stretch - bend your knees as much as you need to.")
