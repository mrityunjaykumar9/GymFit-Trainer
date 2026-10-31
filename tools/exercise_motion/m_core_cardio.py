import math
from lib import *
from engine import vec, add, TORSO, HEAD_D

FLAT = foot(0.0)
DBS = [db("wristN", .05), db("wristF", .05)]
FLOOR_MAT = prop_line((-.7, .02), (.7, .02), .03)


def sh_of(hip, T):
    return shoulder_of(hip, T)


def pushup_geom(alpha_deg, toe_x):
    ang = math.radians(alpha_deg)
    ankle = (toe_x - 0.05, 0.0866)
    d = (math.cos(ang), math.sin(ang))
    hip = (ankle[0] + d[0] * 0.49, ankle[1] + d[1] * 0.49)
    return hip, 90 - alpha_deg, ankle


# ---------------------------------------------------------------- PLANK
def plank_key(t, alpha, label, cue):
    hip, T, ankle = pushup_geom(alpha, -0.62)
    sh = sh_of(hip, T)
    legs = foot(ankle[0], ankle[1], ft=150)
    # head=6: neutral neck, gaze on the floor just ahead of the hands; upper arm a touch behind vertical so the 3D elbow lands under the shoulder
    return key(t, hip, T, legs, fkarm(190, 90), head=6, label=label, cue=cue)


reg("Plank", [
    plank_key(0.0, 8.0, "Hold", "Elbows under shoulders, forearms parallel, body in one straight line from head to heels, glutes and abs tight"),
    plank_key(0.5, 6.4, "Keep breathing", "Keep breathing. Don't let your hips sag or pike up, and look at the floor"),
], [FLOOR_MAT], [meas("Body line", "shoulder", "hip", "ankleN"), meas("Elbow", "shoulder", "elbowN", "wristN")], ["abs"], dur=4200, view=[-0.85, 0.6, -0.08, 0.7])

# ---------------------------------------------------------------- ABS
def head_hands(hip, T, head, frac=0.7):
    # A fixed world-space (dx, dy) offset from the head broke at the "lie back" key: the head itself
    # sits barely above the floor when T=-90 (lying flat), so subtracting a fixed dy to pull the hand
    # down toward ear height instead pulled it down to floor level. Going a fraction of the way along
    # the shoulder->head axis instead keeps the hand at ear height (short of the crown) relative to
    # the head's own orientation, so it still works once the torso angle changes as the rep curls up.
    sh = sh_of(hip, T)
    return hand(*add(sh, vec(T + head), HEAD_D * frac), b=-1)


_cr_hip = (0.0, 0.075)
reg("Crunch", [
    key(0.0, _cr_hip, -90, foot(.30, .04), head_hands(_cr_hip, -90, 0), label="Lie back", cue="Lower back stays pressed into the floor"),
    key(0.5, _cr_hip, -58, foot(.30, .04), head_hands(_cr_hip, -58, 18), spine=-0.04, head=18, label="Curl up", cue="Lift shoulder blades only, don't pull on your neck"),
], [FLOOR_MAT], [], ["abs"], dur=2800, view=[-0.7, 0.55, -0.08, 0.7])

_hb = fkleg(184, 186)
_hang = hand(0.05, 1.40)
reg("Hanging Leg Raise", [
    key(0.0, (0.0, 0.75), 3, fkleg(184, 188), _hang, label="Dead hang", cue="Shoulders active, no swinging"),
    key(0.5, (-0.02, 0.75), -9, fkleg(96, 90), _hang, spine=-0.02, label="Raise legs to bar height", cue="Curl your pelvis up, control the descent"),
], [prop_line((-0.55, 1.40), (0.55, 1.40), .022, "metal"), prop_line((-0.55, 1.40), (-0.55, 0), .03), prop_line((0.55, 1.40), (0.55, 0), .03)],
    [meas("Hip", "shoulder", "hip", "kneeN")], ["abs"], dur=3400, view=[-0.7, 0.7, -0.08, 1.58])

_cc_hip = (0.0, 0.29)
_kn = fkleg(180, 270, ft=270)


def cc_key(t, T, spine, head, label, cue):
    # Same fix as Crunch's head_hands: a fixed offset from the head CENTER put the hand jutting out
    # below the chin instead of beside the head, and broke across the torso-angle range this exercise
    # covers (T=2 kneeling tall to T=62 crunched down). A fraction along the shoulder->head axis keeps
    # it at ear height, relative to the head's own orientation, at every torso angle.
    return key(t, _cc_hip, T, _kn, head_hands(_cc_hip, T, head), spine=spine, head=head, label=label, cue=cue)


reg("Cable Crunch", [
    cc_key(0.0, 2, 0.0, 0, "Kneel tall", "Rope beside your head, hips stay still"),
    cc_key(0.5, 62, -0.05, 22, "Crunch down", "Round your spine, bring elbows toward thighs"),
], [cable([.12, 1.55], "wristN", g="rope"), FLOOR_MAT],
    [meas("Torso curl", "lean")], ["abs"], dur=3000, view=[-0.55, 0.6, -0.08, 1.65])


# ---------------------------------------------------------------- WALK / RUN cycle
def gait(name, stride, lift, lean, swing, bob, stance, hold=None, arms=True, props=(), measure=(), focus=(), dur=1400, y0=0.5295, view=None, tip=""):
    keys = []
    N = 8
    for i in range(N):
        ph = i / N

        def foot_at(p):
            p %= 1.0
            if p < stance:
                u = p / stance
                return (stride * (1 - 2 * u), 0.04)
            u = (p - stance) / (1 - stance)
            return (stride * (-1 + 2 * u), 0.04 + lift * math.sin(math.pi * u))
        fx, fy = foot_at(ph)
        gx, gy = foot_at(ph + .5)
        hip = (0.0, y0 + bob * math.cos(4 * math.pi * ph))
        if hold is None:
            sw = swing * math.sin(2 * math.pi * ph)
            an = fkarm(180 + sw, 180 + sw - 85)
            af = fkarm(180 - sw, 180 - sw - 85)
        else:
            an = af = hold
        keys.append(key(ph, hip, lean, foot(fx, fy, b=1), an, legF=foot(gx, gy, b=1), armF=af,
                        label="Stride"))
    reg(name, keys, list(props), list(measure), list(focus), dur=dur, ease="linear", view=view, tip=tip)


gait("Running", stride=.20, lift=.20, lean=10, swing=40, bob=.018, stance=.38, dur=1000,
     measure=[meas("Knee", "hip", "kneeN", "ankleN")], focus=["thigh", "shin"], view=[-0.6, 0.6, -0.08, 1.3])
gait("Farmer's Carry", stride=.12, lift=.05, lean=0, swing=0, bob=.008, stance=.5, hold=handrel(0.0, -.347), dur=2200,
     props=DBS, focus=["farm", "traps"], view=[-0.6, 0.6, -0.08, 1.3])

# ---------------------------------------------------------------- ROWING MACHINE
def row_key(t, hipx, T, hand_off, label, cue, ankle=(.36, .15)):
    hip = (hipx, 0.27)
    sh = sh_of(hip, T)
    return key(t, hip, T, foot(*ankle, ft=70, b=1), hand(sh[0] + hand_off[0], sh[1] + hand_off[1]), label=label, cue=cue)


reg("Rowing Machine", [
    row_key(0.00, 0.10, 24, (.34, -.02), "Catch", "Shins vertical, arms straight, chest up"),
    row_key(0.20, -0.06, 22, (.34, -.02), "Drive with legs", "Push with legs first, arms stay straight"),
    row_key(0.42, -0.12, -22, (.14, -.12), "Finish", "Lean back slightly, handle to lower ribs"),
    row_key(0.60, -0.12, -8, (.30, -.06), "Arms away", "Hands go first on the recovery"),
    row_key(0.78, -0.03, 20, (.34, -.02), "Recover", "Slide forward, then bend your knees"),
], [prop_line((-.45, .17), (.45, .17), .03), prop_line(at("hip", (-.07, -.055)), at("hip", (.07, -.055)), .05, "metal"),
    prop_line((.34, .08), (.44, .38), .035), prop_circle([.56, .27], .13, "furn", True, 0), cable([.56, .27], "wristN", g="handle", tower=False)],
    [meas("Knee", "hip", "kneeN", "ankleN"), meas("Torso lean", "lean")], ["thigh", "back"], dur=3200, ease="smooth", view=[-0.6, 0.75, -0.08, 0.95])

# ---------------------------------------------------------------- STATIONARY CYCLING
_BB = (0.12, 0.30)
_R = 0.09


def pedal(theta):
    return (_BB[0] + _R * math.cos(theta), _BB[1] + _R * math.sin(theta) + 0.03)


def cycle_keys():
    ks = []
    N = 8
    hip = (-0.08, 0.62)
    T = 38
    sh = sh_of(hip, T)
    for i in range(N):
        th = 2 * math.pi * i / N
        ks.append(key(i / N, hip, T, foot(*pedal(th), ft=80, b=1), hand(.35, .80), legF=foot(*pedal(th + math.pi), ft=80, b=1),
                      label="Pedal", cue="Smooth circles, saddle height so knee has a slight bend at the bottom"))
    return ks


reg("Stationary Cycling", cycle_keys(), [
    prop_line((-0.10, .64), (0.12, .30), .03), prop_line((.12, .30), (.36, .66), .03), prop_line((.36, .66), (.36, .82), .03),
    prop_line((-0.17, .66), (-0.02, .66), .045), prop_line((.30, .82), (.42, .82), .03),
    prop_circle([.12, .30], .028, "metal", True, 0), prop_circle([.52, .22], .20, "furn", False, 0), prop_line([.12, .30], [.52, .22], .02),
    prop_line([.12, .30], at("ankleN", (0, -.03)), .02, "furn", 0), prop_line([.12, .30], at("ankleF", (0, -.03)), .02, "furn", 0),
    prop_line(at("ankleN", (-.04, -.035)), at("ankleN", (.06, -.035)), .02, "metal", 0)],
    [meas("Knee", "hip", "kneeN", "ankleN")], ["thigh"], dur=1800, ease="linear", view=[-0.4, 0.8, -0.08, 1.05])

# ---------------------------------------------------------------- STAIR CLIMBING
def stair_key(t, ph):
    yn = 0.04 + 0.09 * (1 + math.sin(2 * math.pi * ph))
    yf = 0.04 + 0.09 * (1 - math.sin(2 * math.pi * ph))
    hip = (0.0, 0.50 + 0.09 * (1 + math.sin(2 * math.pi * ph)) * .5 + .02)
    return key(t, hip, 8, foot(0.02, yn, b=1), hand(.28, .60), legF=foot(0.02, yf, b=1), label="Step", cue="Stand tall, light hands on rails, full foot on the step")


reg("Stair Climbing", [stair_key(i / 8, i / 8) for i in range(8)], [
    prop_line([.44, 0], [.44, .80], .035), prop_line([.26, .62], [.44, .62], .03),
    prop_line(at("ankleN", (-.06, -.045)), at("ankleN", (.12, -.045)), .03, "metal", 0), prop_line(at("ankleF", (-.06, -.045)), at("ankleF", (.12, -.045)), .03, "metal", 0)],
    [meas("Knee", "hip", "kneeN", "ankleN")], ["thigh", "glutes"], dur=1800, ease="linear", view=[-0.5, 0.65, -0.08, 1.2])

# ---------------------------------------------------------------- BURPEES
# alpha=22 (like Plank/Mountain Climbers use) puts the hand-to-shoulder distance at ~0.348 pose units -
# just inside the arm's 0.35 max reach in this flat 2D math, but the 3D lift adds the grip's sideways
# offset on top of that, which was tipping the real 3D distance past max reach: the IK then clamped
# short of the floor, so the hand looked like it was floating instead of planted. A shallower fold
# (alpha=18) leaves real slack.
_h18, _T18, _a18 = pushup_geom(18, 0.0)
_sh18 = sh_of(_h18, _T18)
_handx = 0.22
_dx = _handx - _sh18[0]
_plank_hip = (_h18[0] + _dx, _h18[1])
_plank_ankle = (_a18[0] + _dx, _a18[1])
_FLOORHAND = hand(_handx, 0.035)

# The squatted "hands down" position is a different body shape than the plank (hips dropped low, torso
# folded forward over the feet, not stretched out) so it needs its own, closer hand target - reusing the
# plank's far-away target put the hand well past the arm's actual max reach, so it hovered above the
# floor with the arm rigid-straight instead of resting on the mat with a natural elbow bend.
_squat_hip, _squat_T = (-.10, .17), 60
_SQUATHAND = hand(.05, .035)


def squat_hand_key(t, label, cue):
    return key(t, _squat_hip, _squat_T, foot(0.0, 0.04), _SQUATHAND, label=label, cue=cue)


def abs_hand(hip, T, off):
    sh = sh_of(hip, T)
    return hand(sh[0] + off[0], sh[1] + off[1])


reg("Burpees", [
    key(0.00, (0, .5295), 0, foot(0.0), abs_hand((0, .5295), 0, (0.02, -.34)), label="Stand", cue="Feet shoulder width"),
    squat_hand_key(0.14, "Squat, hands down", "Hands flat on the floor"),
    key(0.30, _plank_hip, _T18, foot(*_plank_ankle, ft=150), _FLOORHAND, label="Kick back to plank", cue="Land in a strong plank"),
    squat_hand_key(0.50, "Jump feet in", "Feet land outside your hands"),
    key(0.68, (0.0, .50), 5, foot(0.0, 0.05), abs_hand((0.0, .50), 5, (0.0, .20)), label="Explode up", cue="Drive through your heels"),
    key(0.80, (0.0, .72), 0, foot(0.0, 0.25), abs_hand((0.0, .72), 0, (0.0, .34)), label="Jump, arms overhead", cue="Land softly"),
    key(0.92, (0.0, .46), 12, foot(0.0), abs_hand((0.0, .46), 12, (0.02, -.30)), label="Land softly", cue="Bend your knees to absorb"),
], [FLOOR_MAT], [meas("Knee", "hip", "kneeN", "ankleN")], ["thigh", "chest", "abs"], dur=4600, view=[-0.5, 0.7, -0.08, 1.35])

# ---------------------------------------------------------------- CLEAN AND PRESS
reg("Kettlebell/Barbell Clean and Press", [
    key(0.00, (-.12, .50), 32, foot(0.0), handrel(.02, -.35), label="Hang", cue="Hinge at the hips, flat back, bar close to legs"),
    key(0.16, (0.02, .53), 0, foot(0.0), handrel(.08, -.10), label="Explode", cue="Jump the hips through, elbows high"),
    # The rack position itself (elbows up, bar resting across the front delts) sat too close to the
    # head even before any pressing starts - .07/.06 forward wasn't enough clearance for an 0.08-radius
    # plate at roughly chin height, so it read as clipping the head at rest, not just mid-press. Pushing
    # the elbows further forward (matching the "elbows high" cue already used at the catch) gives the
    # plate real clearance at every rack-position keyframe.
    key(0.30, (0.0, .47), 2, foot(0.0), handrel(.15, .02), label="Catch in rack", cue="Dip and catch the bar on your shoulders"),
    key(0.45, (0.0, .5295), 0, foot(0.0), handrel(.15, .02), label="Stand", cue="Squeeze glutes, ribs down"),
    # A real strict press keeps the BAR on a close-to-straight line from the rack to overhead - it's
    # the lifter's head that moves (tips back, "through the window") to let the bar pass, not the bar
    # swinging out around the face. Swinging the bar itself forward read as an odd forward-then-up
    # curve instead of a direct press. This keyframe sits almost exactly on the straight line between
    # the rack and the lockout (barely nudged forward) and tilts the head back instead for clearance.
    key(0.55, (0.0, .5295), -1, foot(0.0), handrel(.115, .185), head=-22, label="Drive past your face", cue="Tip your head back slightly, press the bar straight up"),
    key(0.65, (0.0, .5295), -3, foot(0.0), handrel(.0, .35), label="Press overhead", cue="Lock out with biceps by your ears"),
    key(0.72, (0.0, .5295), -1, foot(0.0), handrel(.115, .185), head=-22, label="Lower past your face", cue="Tip your head back slightly, lower the bar straight down"),
    key(0.80, (0.0, .5295), 0, foot(0.0), handrel(.15, .02), label="Lower to rack", cue="Control the bar down"),
    key(0.90, (-.08, .50), 15, foot(0.0), handrel(.03, -.28), label="Return", cue="Slide the bar back to your thighs"),
], [plate("wristN", .08)], [meas("Knee", "hip", "kneeN", "ankleN"), meas("Elbow", "shoulder", "elbowN", "wristN")], ["thigh", "glutes", "shoulder"], dur=5000, view=[-0.6, 0.6, -0.08, 1.55])

# ---------------------------------------------------------------- MOUNTAIN CLIMBERS
_mc_hip, _mc_T, _mc_ankle = pushup_geom(22, -0.55)
_mc_sh = sh_of(_mc_hip, _mc_T)
_mc_handx = _mc_sh[0]
_mc_h = hand(_mc_handx, 0.035)
_mc_back = foot(_mc_ankle[0], _mc_ankle[1], ft=150)
_mc_fwd = foot(_mc_hip[0] + .15, 0.11, ft=120, b=1)
reg("Mountain Climbers", [
    key(0.0, _mc_hip, _mc_T, _mc_fwd, _mc_h, legF=_mc_back, armF=_mc_h, label="Drive knee", cue="Hips level, shoulders over wrists"),
    key(0.5, _mc_hip, _mc_T, _mc_back, _mc_h, legF=_mc_fwd, armF=_mc_h, label="Switch legs", cue="Fast, controlled, core braced"),
], [FLOOR_MAT], [meas("Knee", "hip", "kneeN", "ankleN")], ["abs", "thigh"], dur=900, view=[-0.8, 0.6, -0.08, 0.8])
