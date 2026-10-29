import math
from lib import *
from engine import vec, add

FLAT = foot(0.0)
KNEE = lambda: meas("Knee", "hip", "kneeN", "ankleN")
BACK = lambda: meas("Back lean", "lean")
DBS = [db("wristN", .045), db("wristF", .045)]

# ---------------------------------------------------------------- SQUATS
reg("Barbell Back Squat", [
    # The bar used to be anchored to a fixed shoulder offset, independent of where the hands actually
    # grip - it drifted out of sync with the arm and ended up floating by the head. Anchoring it to the
    # wrists (like every other barbell exercise) keeps it glued to the hands; the arm angles below were
    # then retuned so the grip sits behind the head at trap height instead of overlapping the face.
    key(0.0, (-.02, .5295), 8, FLAT, fkarm(195, -25, rel=True), label="Stand tall", cue="Bar on upper back, brace your core"),
    key(0.5, (-.16, .33), 40, FLAT, fkarm(195, -25, rel=True), label="Hips back & down", cue="Knees track over toes, chest up, thighs to parallel"),
], [plate("wristN", .085)], [KNEE(), BACK()], ["thigh", "glutes"], dur=3600)

_fr = {"p": "shoulder", "off": [.07, .04]}
reg("Front Squat", [
    key(0.0, (-.02, .5295), 4, FLAT, fkarm(95, -50, rel=True), label="Stand tall", cue="Elbows high, bar resting on front shoulders"),
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

_rdl_top = key(0.0, (0, .5295), 3, FLAT, handrel(.03, -.35), label="Stand tall", cue="Soft knees, shoulders back")
reg("Romanian Deadlift", [
    _rdl_top,
    key(0.5, (-.24, .46), 75, FLAT, handrel(.02, -.35), label="Hinge back", cue="Push hips back, bar slides down your legs, flat back"),
], [plate("wristN", .085)], [BACK(), KNEE()], ["hamstrings", "glutes", "back"], dur=3600)

# ---------------------------------------------------------------- MACHINES
#  At the old hip height (.27) the "knees bent" foot (toes pointed down at ft=165) computed below
#  floor level, so the renderer's own "keep every limb above the floor" safety lifted the WHOLE
#  seated body to compensate - and let go of that lift as the leg extended and the foot rose back
#  above the floor on its own, which read as the person sliding upward out of the seat mid-rep.
#  Raising the seat height and easing the toe-down angle keeps the foot clear for the whole rep,
#  so nothing ever needs correcting and the hip stays exactly where it's authored to be.
reg("Leg Extension", [
    key(0.0, (-.06, .36), -4, fkleg(90, 178, ft=100), hiprel(.10, .03), label="Knees bent", cue="Back flat against the pad"),
    key(0.5, (-.06, .36), -4, fkleg(90, 98, ft=20), hiprel(.10, .03), label="Extend & squeeze", cue="Pause with quads tight, lower slowly"),
], [prop_line((-.2, .29), (.08, .29), .05), prop_line((-.17, .31), (-.17, .89), .05), prop_line((-.02, .26), (-.02, 0), .03),
    prop_circle(at("ankleN", (.03, .05)), .028, "metal", True, 1)],
    [KNEE()], ["thigh"], dur=3000, view=[-0.5, 0.6, -0.08, 1.1])

# ---------------------------------------------------------------- LUNGES
_wl = lambda t, hip, T, fa, bth, bsh, bft, label="", cue="": key(
    t, hip, T, foot(*fa), handrel(0.0, -.347), legF=fkleg(bth, bsh, ft=bft), armF=handrel(0.0, -.347), label=label, cue=cue)
reg("Walking Lunges", [
    _wl(0.0, (0, .5295), 0, (0, .04), 180, 180, 90, "Stand tall", "Core braced, chest up"),
    _wl(0.5, (0.0, .285), 0, (.245, .04), 200, 270, 120, "Lower: both knees ~90°", "Front knee over ankle, back knee hovers above floor"),
], DBS, [meas("Front knee", "hip", "kneeN", "ankleN")], ["thigh", "glutes"], dur=3400, view=[-0.65, 0.7, -0.08, 1.2])

reg("Bulgarian Split Squat", [
    key(0.0, (-.02, .47), 12, foot(.22, .04), handrel(.02, -.33), legF=foot(-.42, .46, ft=265), armF=handrel(.02, -.33), label="Top", cue="Rear foot on bench, front shin near vertical"),
    key(0.5, (-.02, .30), 12, foot(.22, .04), handrel(.02, -.33), legF=foot(-.42, .46, ft=265), armF=handrel(.02, -.33), label="Lower straight down", cue="Front knee tracks over toes, torso tall"),
], DBS + [prop_line((-.62, .38), (-.30, .38), .04), prop_line((-.50, .36), (-.50, 0), .03)],
    [meas("Front knee", "hip", "kneeN", "ankleN")], ["thigh", "glutes"], dur=3400, view=[-0.75, 0.55, -0.08, 1.15])

reg("Calf Raise", [
    key(0.0, (0, .5295), 0, foot(0.0, .04), handrel(0, -.347), armF=handrel(0, -.347), label="Heels down", cue="Full stretch at the bottom"),
    key(0.5, (0.05, .60), 0, foot(.05, .127, ft=150), handrel(0, -.347), armF=handrel(0, -.347), label="Rise onto toes", cue="Pause and squeeze the calves at the top"),
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
# Textbook glute bridge: lie on your back, knees ~90 deg, feet flat and hip-width. Drive through the heels until
# shoulder-hip-knee form one straight line (hip angle ~180, knee ~100), head and neck neutral on the floor, arms
# flat on the floor beside the body. The head key keeps the head level (no neck extension); arms are FK so the
# 3D athlete lays them flat instead of reaching.
_gk = lambda T: dict(head=-90 - T)
_ga = fkarm(96, 96)
reg("Glute Bridge", [
    key(0.0, _gb(-90), -90, foot(.34, .04), _ga, label="Hips down", cue="Back flat, knees bent about 90 degrees, feet flat and hip-width apart, arms by your sides", **_gk(-90)),
    key(0.5, _gb(-108), -108, foot(.34, .04), _ga, label="Lift hips", cue="Push through your heels and squeeze your glutes until knees, hips and shoulders form a straight line", **_gk(-108)),
], [prop_line((-.65, .02), (.55, .02), .03)],
    [meas("Knee", "hip", "kneeN", "ankleN"), meas("Hip", "shoulder", "hip", "kneeN")], ["glutes"], dur=3200, view=[-0.7, 0.55, -0.08, 0.7])
