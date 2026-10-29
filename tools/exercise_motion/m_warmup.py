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

_SB_HANG = fkarm(172, 172)
reg("Standing Side Bend", [
    key(0.00, (0, .5295), 0, FLAT, _SB_HANG, legF=FLAT, armF=_SB_HANG, label="Stand tall", cue="Feet hip-width, arms relaxed"),
    key(0.50, (0, .50), 18, FLAT, fkarm(10, 5), legF=FLAT, armF=_SB_HANG, head=10,
        label="Reach and lean", cue="Reach one arm overhead and lean away from it"),
], [MAT], [meas("Side bend", "lean")], ["back"], dur=3400, view=[-0.55, 0.65, -0.08, 1.4], frontal=True,
    tip="Opens the side body - switch sides halfway through your set.")

reg("Standing Toe Touch", [
    key(0.00, (0, .5295), 0, FLAT, hand(0.045, 0.475), legF=FLAT, armF=hand(0.045, 0.475), label="Stand tall", cue="Feet hip-width apart"),
    key(0.50, (-.10, .50), 130, foot(.0, .04, b=1), hand(.12, .07), legF=foot(-.03, .04, b=1), armF=hand(.10, .07), head=30,
        label="Hinge and reach", cue="Soft knees, hinge from the hips, reach toward your toes"),
], [MAT], [meas("Hip", "shoulder", "hip", "kneeN")], ["hamstrings", "back"], dur=3600, view=[-0.55, 0.6, -0.08, 1.35],
    tip="A dynamic hamstring stretch - bend your knees as much as you need to.")

# walked-out plank: the feet stay where the fold left them, the body is one straight line at ~21 degrees
_iw_a = 21.6
_iw_ankle = (-.05, .0866)
_iw_hip = (_iw_ankle[0] + .49 * math.cos(math.radians(_iw_a)), _iw_ankle[1] + .49 * math.sin(math.radians(_iw_a)))
_iw_T = 90 - _iw_a
_iw_sh = shoulder_of(_iw_hip, _iw_T)
_iw_leg = fkleg(270 - _iw_a, 270 - _iw_a, ft=150)

reg("Inchworm", [
    key(0.00, (-.05, .42), 75, fkleg(170, 190, ft=90), hand(.30, .04), legF=fkleg(170, 190, ft=90), armF=hand(.26, .04), head=18,
        label="Fold, hands to floor", cue="Bend your knees as much as needed to reach the floor"),
    key(0.50, _iw_hip, _iw_T, _iw_leg, hand(_iw_sh[0], .03), legF=_iw_leg, armF=hand(_iw_sh[0] - .04, .03), head=5,
        label="Walk out to plank", cue="Walk your hands forward, keep your core braced, then walk back in"),
], [MAT], [], ["abs", "shoulder"], dur=4200, view=[-0.45, 0.85, -0.08, 1.0],
    tip="A full-body warm-up: stretches the hamstrings and activates the shoulders and core.")
