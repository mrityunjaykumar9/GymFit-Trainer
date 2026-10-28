import math
from lib import *
from engine import vec, add

FLAT = foot(0.0)
KNEE = lambda: meas("Knee", "hip", "kneeN", "ankleN")
BACK = lambda: meas("Back lean", "lean")
DBS = [db("wristN", .045), db("wristF", .045)]

# ---------------------------------------------------------------- SQUATS
reg("Barbell Back Squat", [
    key(0.0, (-.02, .525), 8, FLAT, fkarm(205, 22, rel=True), label="Stand tall", cue="Bar on upper back, brace your core"),
    key(0.5, (-.16, .33), 40, FLAT, fkarm(205, 22, rel=True), label="Hips back & down", cue="Knees track over toes, chest up, thighs to parallel"),
], [plate({"p": "shoulder", "off": [-.055, .035]}, .085)], [KNEE(), BACK()], ["thigh", "glutes"], dur=3600)

_fr = {"p": "shoulder", "off": [.07, .04]}
reg("Front Squat", [
    key(0.0, (-.02, .525), 4, FLAT, fkarm(95, -50, rel=True), label="Stand tall", cue="Elbows high, bar resting on front shoulders"),
    key(0.5, (-.15, .33), 28, FLAT, fkarm(95, -50, rel=True), label="Sit straight down", cue="Keep elbows up and torso upright"),
], [plate(_fr, .08)], [KNEE(), BACK()], ["thigh", "glutes"], dur=3600)

# leg press: sled along bearing 55
_lp_hip = (-.25, .25)
_lp_dir = vec(55)
_lp_bot = add(_lp_hip, _lp_dir, .35)
_lp_top = add(_lp_hip, _lp_dir, .475)
_lp_perp = (-_lp_dir[1], _lp_dir[0])
_lp_a = add(add((0, 0), _lp_dir, .05), _lp_perp, .15)
_lp_b = add(add((0, 0), _lp_dir, .05), _lp_perp, -.15)
reg("Leg Press", [
    key(0.0, _lp_hip, -50, foot(*_lp_bot, ft=-35), hiprel(.16, -.07), label="Lower slowly", cue="Knees ~90°, lower back stays on the pad"),
    key(0.5, _lp_hip, -50, foot(*_lp_top, ft=-35), hiprel(.16, -.07), label="Press", cue="Don't lock your knees out at the top"),
], [prop_line((-.32, .17), (.50, .62), .03, "furn"), prop_line((-.34, .15), (-.20, .15), .05),
    prop_line((_lp_hip[0] - .08, _lp_hip[1] - .06), add(_lp_hip, vec(-50), .55), .05),
    prop_line(at("ankleN", _lp_a), at("ankleN", _lp_b), .05, "metal", 0)],
    [KNEE()], ["thigh", "glutes"], dur=3400, view=[-0.85, 0.65, -0.08, 0.95])

_rdl_top = key(0.0, (0, .525), 3, FLAT, handrel(.03, -.35), label="Stand tall", cue="Soft knees, shoulders back")
reg("Romanian Deadlift", [
    _rdl_top,
    key(0.5, (-.24, .46), 75, FLAT, handrel(.02, -.35), label="Hinge back", cue="Push hips back, bar slides down your legs, flat back"),
], [plate("wristN", .085)], [BACK(), KNEE()], ["hamstrings", "glutes", "back"], dur=3600)

# ---------------------------------------------------------------- MACHINES
reg("Leg Curl", [
    key(0.0, (0, .27), -90, fkleg(91, 83, ft=100), handrel(.06, -.08), label="Legs extended", cue="Hips pressed into the bench"),
    key(0.5, (0, .27), -90, fkleg(91, -12, ft=100), handrel(.06, -.08), label="Curl heels to glutes", cue="Squeeze hamstrings, control the way down"),
], [prop_line((-0.55, 0.195), (0.20, 0.195), .05), prop_line((-0.48, 0.17), (-0.48, 0.02), .03), prop_line((0.10, 0.17), (0.10, 0.02), .03),
    prop_circle(at("ankleN", (0.0, .05)), .035, "metal", True, 1)],
    [meas("Knee", "hip", "kneeN", "ankleN")], ["hamstrings"], dur=3000, view=[-0.7, 0.6, -0.08, 0.8])

reg("Leg Extension", [
    key(0.0, (-.06, .27), -4, fkleg(90, 178, ft=165), hiprel(.10, .03), label="Knees bent", cue="Back flat against the pad"),
    key(0.5, (-.06, .27), -4, fkleg(90, 98, ft=80), hiprel(.10, .03), label="Extend & squeeze", cue="Pause with quads tight, lower slowly"),
], [prop_line((-.2, .20), (.08, .20), .05), prop_line((-.17, .22), (-.17, .80), .05), prop_line((-.02, .17), (-.02, 0), .03),
    prop_circle(at("ankleN", (-.03, .015)), .04, "metal", True, 1)],
    [KNEE()], ["thigh"], dur=3000, view=[-0.5, 0.6, -0.08, 1.0])

# ---------------------------------------------------------------- LUNGES
_wl = lambda t, hip, T, fa, bth, bsh, bft, label="", cue="": key(
    t, hip, T, foot(*fa), handrel(0.0, -.34), legF=fkleg(bth, bsh, ft=bft), armF=handrel(0.0, -.34), label=label, cue=cue)
reg("Walking Lunges", [
    _wl(0.0, (0, .525), 0, (0, .04), 180, 180, 90, "Stand tall", "Core braced, chest up"),
    _wl(0.5, (0.0, .285), 0, (.245, .04), 200, 270, 120, "Lower: both knees ~90°", "Front knee over ankle, back knee hovers above floor"),
], DBS, [meas("Front knee", "hip", "kneeN", "ankleN")], ["thigh", "glutes"], dur=3400, view=[-0.65, 0.7, -0.08, 1.2])

reg("Bulgarian Split Squat", [
    key(0.0, (-.02, .47), 12, foot(.22, .04), handrel(.02, -.33), legF=foot(-.42, .46, ft=265), armF=handrel(.02, -.33), label="Top", cue="Rear foot on bench, front shin near vertical"),
    key(0.5, (-.02, .30), 12, foot(.22, .04), handrel(.02, -.33), legF=foot(-.42, .46, ft=265), armF=handrel(.02, -.33), label="Lower straight down", cue="Front knee tracks over toes, torso tall"),
], DBS + [prop_line((-.62, .38), (-.30, .38), .04), prop_line((-.50, .36), (-.50, 0), .03)],
    [meas("Front knee", "hip", "kneeN", "ankleN")], ["thigh", "glutes"], dur=3400, view=[-0.75, 0.55, -0.08, 1.15])

reg("Calf Raise", [
    key(0.0, (0, .525), 0, foot(0.0, .04), handrel(0, -.34), armF=handrel(0, -.34), label="Heels down", cue="Full stretch at the bottom"),
    key(0.5, (0.05, .60), 0, foot(.05, .127, ft=150), handrel(0, -.34), armF=handrel(0, -.34), label="Rise onto toes", cue="Pause and squeeze the calves at the top"),
], DBS, [], ["shin"], dur=2600)

# ---------------------------------------------------------------- GLUTES
_hs = (-.30, .29)
_bridge_hip = lambda T: add(_hs, vec(T), -.30)
reg("Hip Thrust", [
    key(0.0, _bridge_hip(-55), -55, foot(.245, .04), hiprel(.07, .06), label="Hips low", cue="Upper back on bench, chin tucked"),
    key(0.5, _bridge_hip(-90), -90, foot(.245, .04), hiprel(.07, .06), label="Drive hips up", cue="Shins vertical, squeeze glutes hard at the top"),
], [prop_line((-.62, .255), (-.26, .255), .05), prop_line((-.50, .23), (-.50, 0), .03), plate(at("hip", (.0, .075)), .08)],
    [meas("Knee", "hip", "kneeN", "ankleN"), meas("Hip", "shoulder", "hip", "kneeN")], ["glutes"], dur=3200, view=[-0.7, 0.55, -0.08, 0.7])

_gs = (-.30, .075)
_gb = lambda T: add(_gs, vec(T), -.30)
reg("Glute Bridge", [
    key(0.0, _gb(-90), -90, foot(.27, .04), handrel(.30, -.03), label="Hips down", cue="Feet flat, knees bent, arms by your sides"),
    key(0.5, _gb(-118), -118, foot(.27, .04), handrel(.30, -.03), label="Lift hips", cue="Squeeze glutes, straight line from knees to shoulders"),
], [prop_line((-.65, .02), (.55, .02), .03)],
    [meas("Knee", "hip", "kneeN", "ankleN"), meas("Hip", "shoulder", "hip", "kneeN")], ["glutes"], dur=3000, view=[-0.7, 0.55, -0.08, 0.7])
