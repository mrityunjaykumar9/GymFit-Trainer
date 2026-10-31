import math
from lib import *
from engine import vec, add

FLAT = foot(0.0)
BENCH = [prop_line((-0.55, 0.195), (0.07, 0.195), .05), prop_line((-0.48, 0.17), (-0.48, 0.02), .03), prop_line((0.0, 0.17), (0.0, 0.02), .03)]
SEAT = [prop_line((-.2, .20), (.08, .20), .05), prop_line((-.15, .22), (-.15, .90), .05), prop_line((-.02, .17), (-.02, 0), .03)]
ELB = lambda: meas("Elbow", "shoulder", "elbowN", "wristN")
SIDE_TIP = "Raise your arms out to the SIDES. The side view makes the arm look shorter because it moves toward the camera."

# ---------------------------------------------------------------- CHEST (non-press)
reg("Cable Fly", [
    key(0.0, (0.0, .5295), 12, foot(-.04), fkarm(172, 168), legF=foot(.10), label="Arms wide", cue="Slight elbow bend, chest open"),
    key(0.5, (0.0, .5295), 12, foot(-.04), fkarm(98, 96), legF=foot(.10), label="Squeeze", cue="Hug a big tree - hands meet in front of chest"),
], [cable([-.16, 1.15], "wristN", g="handle")],
    [], ["chest"], dur=3000, view=[-0.75, 0.75, -0.08, 1.3])

reg("Pec Deck", [
    key(0.0, (-.06, .27), 0, foot(.22, .04), handrel(-.03, .03), label="Open", cue="Elbows level with shoulders, chest tall"),
    key(0.5, (-.06, .27), 0, foot(.22, .04), handrel(.30, .02), label="Squeeze", cue="Bring the pads together, pause 1 second"),
], SEAT + [prop_line(at("wristN", (0, -.10)), at("wristN", (0, .14)), .045, "metal", 1)],
    [], ["chest"], dur=3000, view=[-0.65, 0.65, -0.08, 1.15])


def pushup_pose(alpha_deg, toe_x=-0.55):
    ang = math.radians(alpha_deg)
    ankle = (toe_x - 0.05, 0.0866)
    d = (math.cos(ang), math.sin(ang))
    hip = (ankle[0] + d[0] * 0.49, ankle[1] + d[1] * 0.49)
    return hip, 90 - alpha_deg, 270 - alpha_deg


_PU_TOP_ALPHA = 21.6
_h_top, _T_top, _ = pushup_pose(_PU_TOP_ALPHA)
_sh_top = shoulder_of(_h_top, _T_top)
# At the old alpha=22 the hand-to-shoulder distance (0.3525) was *past* the arm's actual max reach
# (0.165+0.185=0.35), so the 2D solver clamped it - the elbow ended up locked dead straight right at
# that clamp boundary, which is numerically unstable and is what made the wrist flip/droop oddly as
# the body moved. Easing the top alpha back a couple degrees keeps the body reading as "straight
# line, arms extended" while leaving the arm just enough slack to resolve a normal, stable pose.
_HAND = (_sh_top[0], 0.03)


def pushup_key(t, alpha, label="", cue=""):
    hip, T, leg_b = pushup_pose(alpha)
    return key(t, hip, T, fkleg(leg_b, leg_b, ft=150), hand(*_HAND), label=label, cue=cue)


reg("Push Ups", [
    pushup_key(0.0, _PU_TOP_ALPHA, "Top", "Body in one straight line, core tight"),
    pushup_key(0.5, 12, "Chest to floor", "Elbows ~45° from your body, don't flare"),
], [], [ELB(), meas("Body line", "shoulder", "hip", "ankleN")], ["chest", "uarm"], dur=2800, view=[-0.8, 0.8, -0.08, 0.8])

# ---------------------------------------------------------------- BACK
_hang_hand = hand(0.05, 1.40)
_hang_legs = fkleg(188, 212)
reg("Pull Ups", [
    key(0.0, (0.0, 0.74), 2, _hang_legs, _hang_hand, label="Dead hang", cue="Arms fully straight, shoulders packed down"),
    key(0.5, (0.0, 0.98), 8, _hang_legs, hand(0.05, 1.40, b=1), label="Chin over bar", cue="Lead with your chest, elbows drive down and back"),
], [prop_line((-0.55, 1.40), (0.55, 1.40), .022, "metal"), prop_line((-0.55, 1.40), (-0.55, 0), .03), prop_line((0.55, 1.40), (0.55, 0), .03)],
    [ELB()], ["back", "uarm"], dur=3400, view=[-0.7, 0.7, -0.08, 1.58])

_sh_ld = shoulder_of((-.05, .27), -10)
reg("Lat Pulldown", [
    key(0.0, (-.05, .27), -10, foot(.20), hand(_sh_ld[0] + .09, _sh_ld[1] + .34), label="Arms stretched", cue="Chest up, slight lean back"),
    key(0.5, (-.05, .27), -14, foot(.20), hand(_sh_ld[0] + .13, _sh_ld[1] - .02), label="Bar to upper chest", cue="Pull elbows down and back, don't swing"),
], [prop_line((-.20, .20), (.10, .20), .05), prop_line((.0, .17), (0, 0), .03), prop_line((.08, .355), (.27, .33), .05),
    cable([.08, 1.5], "wristN", g="bar")],
    [ELB()], ["back"], dur=3200, view=[-0.6, 0.6, -0.08, 1.55])

_sh_row = shoulder_of((-.22, .47), 58)
_hang_row = hand(_sh_row[0], _sh_row[1] - .35)
reg("Barbell Row", [
    key(0.0, (-.22, .47), 58, foot(0.0), _hang_row, label="Arms hang", cue="Hinge at hips, flat back, neutral neck"),
    key(0.5, (-.22, .47), 58, foot(0.0), hand(_sh_row[0] - .02, _sh_row[1] - .13), label="Row to lower ribs", cue="Drive elbows back, squeeze shoulder blades"),
], [plate("wristN", .09)], [ELB(), meas("Back lean", "lean")], ["back"], dur=3000)

reg("T-Bar Row", [
    key(0.0, (-.22, .47), 58, foot(0.0), hand(_sh_row[0] + .03, _sh_row[1] - .35), label="Arms hang", cue="Chest up, back flat, knees soft"),
    key(0.5, (-.22, .47), 58, foot(0.0), hand(_sh_row[0] + .0, _sh_row[1] - .14), label="Pull to chest", cue="Elbows close to body, pause at the top"),
], [prop_line((-.85, .06), "wristN", .03, "metal"), plate(at("wristN", (-.12, -.03)), .085)],
    [ELB(), meas("Back lean", "lean")], ["back"], dur=3000, view=[-0.95, 0.6, -0.08, 1.1])

_sh_sr_a = shoulder_of((-.12, .22), 28)
_sh_sr_b = shoulder_of((-.12, .22), -6)
_sr_foot = foot(.30, .16, ft=75)
reg("Seated Cable Row", [
    key(0.0, (-.12, .22), 28, _sr_foot, hand(_sh_sr_a[0] + .35, _sh_sr_a[1] - .02), label="Reach", cue="Back flat, let shoulder blades spread"),
    key(0.5, (-.12, .22), -6, _sr_foot, hand(_sh_sr_b[0] + .13, _sh_sr_b[1] - .20), label="Pull to belly", cue="Chest tall, squeeze shoulder blades together"),
], [prop_line((-.30, .17), (.2, .17), .05), prop_line((.18, .02), (.34, .26), .055), cable([.62, .43], "wristN", g="handle")],
    [ELB(), meas("Back lean", "lean")], ["back"], dur=3200, view=[-0.6, 0.8, -0.08, 1.0])

reg("Deadlift", [
    key(0.0, (-.25, .40), 58, foot(0.0), handrel(0.0, -.35), label="Set up", cue="Bar over mid-foot, chest up, back flat"),
    key(0.5, (0.0, .5295), 4, foot(0.0), handrel(0.03, -.35), label="Stand tall", cue="Hips and shoulders rise together, squeeze glutes"),
], [plate("wristN", .12)], [meas("Knee", "hip", "kneeN", "ankleN"), meas("Back lean", "lean")], ["back", "glutes", "thigh"], dur=3600)

# ---------------------------------------------------------------- SHOULDERS
_ohp_sh = shoulder_of((0.0, .5295), 0)
reg("Overhead Press", [
    key(0.0, (0, .5295), 0, FLAT, hand(.06, _ohp_sh[1] + .03), label="Rack position", cue="Squeeze glutes, ribs down"),
    key(0.5, (0, .5295), -3, FLAT, hand(.0, _ohp_sh[1] + .35), label="Lockout overhead", cue="Head through the window, bar over mid-foot"),
], [plate("wristN", .085)], [ELB()], ["shoulder", "uarm"], dur=3000)

_dsp = shoulder_of((-.06, .27), 0)
reg("Dumbbell Shoulder Press", [
    key(0.0, (-.06, .27), 0, foot(.22, .04), hand(.03, _dsp[1] + .04), label="Start", cue="Dumbbells at ear height, wrists over elbows"),
    key(0.5, (-.06, .27), 0, foot(.22, .04), hand(-.02, _dsp[1] + .35), label="Press overhead", cue="Lock out without arching your back"),
], SEAT + [db("wristN", .05)], [ELB()], ["shoulder", "uarm"], dur=3000, view=[-0.65, 0.65, -0.08, 1.35])

reg("Lateral Raise", [
    key(0.0, (0, .5295), 0, FLAT, fkarm(184, 182), armF=fkarm(176, 178), label="Arms at sides", cue="Slight elbow bend, palms facing down"),
    key(0.5, (0, .5295), 0, FLAT, fkarm(98, 94), armF=fkarm(262, 266), label="Raise to shoulder height", cue="Lead with your elbows, stop at shoulder level"),
], [db("wristN", .045), db("wristF", .045)], [], ["shoulder"], dur=3000)

reg("Front Raise", [
    # u and f were equal (zero relative bend) at both keys, so the elbow sat exactly on the
    # shoulder-to-wrist line - a perfectly rigid rod with no visible joint. Lateral Raise (the same
    # kind of raise, just out to the side) keeps a small but real bend throughout for exactly this
    # reason; matching that here instead of a hard-locked elbow.
    key(0.0, (0, .5295), 0, FLAT, fkarm(180, 178), label="Dumbbells at thighs", cue="Brace your core, no swinging"),
    key(0.5, (0, .5295), 0, FLAT, fkarm(92, 88), label="Raise to eye level", cue="Slight elbow bend, stop at shoulder height"),
], [db("wristN", .045)], [meas("Arm lift", "hip", "shoulder", "wristN")], ["shoulder"], dur=3000)

reg("Rear Delt Fly", [
    key(0.0, (-.2, .475), 68, foot(0.0), fkarm(180, 180), label="Arms hang", cue="Hinge forward, flat back"),
    # The free-swing that carries the arm out to the side (see m_lateral.py) rotates the whole,
    # still-2D-straight arm toward the camera axis by the lat angle - at a swing this wide, that
    # flattens almost any 2D elbow bend down to a couple of degrees in the final 3D pose, so the
    # old forearm bearing (174, only 6deg off the upper arm's 180) rendered as a dead-locked elbow.
    # A bigger 2D bend here is what survives the swing as the "soft elbow" the cue calls for.
    key(0.5, (-.2, .475), 68, foot(0.0), fkarm(180, 145), label="Open arms wide", cue="Squeeze shoulder blades, elbows soft"),
], [db("wristN", .045)], [meas("Back lean", "lean")], ["shoulder", "back"], dur=3000)

# Face pull: stand tall (leaning back a touch as you pull), rope at eye level, pull to the face with the
# elbows high and BEHIND the hands, hands finishing beside the ears. Cable height = eye height.
_FP_A = ((-.03, .5295), -2)
_FP_B = ((-.045, .5295), -8)
_sfa = shoulder_of(*_FP_A)
_sfb = shoulder_of(*_FP_B)
_EYE = _sfa[1] + .115
reg("Face Pull", [
    key(0.0, _FP_A[0], _FP_A[1], foot(-.03), hand(_sfa[0] + .335, _EYE - .02), legF=foot(.07),
        label="Arms extended", cue="Rope at eye level, feet shoulder width, ribs down"),
    # The hand sitting forward of the shoulder (not just above it) was pulling the IK elbow solution
    # way up above shoulder height - almost overhead - instead of staying level with it. Directly
    # above the shoulder keeps the elbow right where the cue says: "elbows at shoulder height".
    key(0.5, _FP_B[0], _FP_B[1], foot(-.03), hand(_sfb[0], _sfb[1] + .08, b=1), legF=foot(.07),
        label="Pull to face", cue="Elbows high and wide, hands finish beside your ears"),
], [cable([.78, _EYE], "wristN", g="rope")],
    [ELB()], ["shoulder", "back"], dur=3000, view=[-0.5, 1.0, -0.08, 1.3],
    tip="Pull the rope toward your face and split it so your hands end up beside your ears, elbows at shoulder height.")

reg("Barbell Shrug", [
    # The old target was 99% of max arm reach (l1+l2) - fine for the near arm, but the far hand's
    # own 3D distance (after the grip-width lateral offset gets lifted into 3D) came out just over
    # max reach, so its IK clamped to a different, asymmetric elbow bend than the near arm, and the
    # near-degenerate triangle also read as an odd kinked elbow instead of a straight hanging arm.
    # Pulling the target in to ~90% of reach fixes both and leaves room for a bigger, clearly visible
    # shrug (the model has no separate shoulder-raise bone, so the lift has to come from the arm
    # reaching higher toward a now-closer target rather than a static one).
    key(0.0, (0, .5295), 0, FLAT, handrel(.07, -.31), sl=0.0, label="Arms straight", cue="Bar at thighs, shoulders relaxed"),
    key(0.5, (0, .5295), 0, FLAT, handrel(.07, -.31), sl=0.16, label="Shrug up", cue="Lift straight up toward your ears, no rolling"),
], [plate("wristN", .085)], [], ["traps"], dur=2400)


# ---------------------------------------------------------------- BICEPS
def curl_keys(top_f, bottom_f, alternate=False, legs=FLAT, u=182, cue_top="Elbows stay glued to your sides"):
    near_b, near_t = fkarm(u, bottom_f), fkarm(u, top_f)
    far_b, far_t = (near_t, near_b) if alternate else (near_b, near_t)
    return [key(0.0, (0, .5295), 0, legs, near_b, armF=far_b, label="Lower slowly", cue="Full stretch, control the descent"),
            key(0.5, (0, .5295), 0, legs, near_t, armF=far_t, label="Squeeze", cue=cue_top)]


reg("Barbell Curl", curl_keys(28, 178), [plate("wristN", .08)], [ELB()], ["uarm"], dur=2800)
reg("Dumbbell Curl", curl_keys(25, 178), [db("wristN", .045), db("wristF", .045)], [ELB()], ["uarm"], dur=2800)
#  22 put the dumbbell's forearm bearing close enough to vertical that it curled up past the chin
#  almost into the face/behind the head - fixed to stop level with the upper chest instead.
reg("Hammer Curl", curl_keys(50, 178, cue_top="Thumbs up (neutral grip), elbows pinned"), [db("wristN", .045), db("wristF", .045)], [ELB()], ["uarm", "farm"], dur=2800)
reg("Cable Curl", curl_keys(30, 170), [cable([.52, .08], "wristN", g="bar")],
    [ELB()], ["uarm"], dur=2800, view=[-0.5, 0.8, -0.08, 1.2])

reg("Preacher Curl", [
    key(0.0, (-.12, .27), 14, foot(.16, .04), fkarm(138, 150), label="Lower slowly", cue="Almost straight - don't hyperextend the elbow"),
    key(0.5, (-.12, .27), 14, foot(.16, .04), fkarm(138, -25), label="Squeeze", cue="Upper arms stay flat on the pad"),
], [prop_line((-.26, .20), (.04, .20), .05), prop_line((-.10, .17), (-.10, 0), .03), prop_line((-.01, .47), (.17, .30), .07), prop_line((.12, .34), (.12, 0), .03), db("wristN", .05)],
    [ELB()], ["uarm"], dur=3000, view=[-0.5, 0.6, -0.08, 0.9])

reg("Wrist Curl", [
    key(0.0, (-.12, .27), 28, foot(.2, .04), fkarm(155, 98), label="Lower", cue="Let the bar roll down to your fingertips"),
    key(0.5, (-.12, .27), 28, foot(.2, .04), fkarm(155, 82), label="Curl up", cue="Only your wrists move, forearms stay on thighs"),
], [prop_line((-.26, .20), (.06, .20), .05), prop_line((-.10, .17), (-.10, 0), .03), plate("wristN", .06)],
    [], ["farm"], dur=2400, view=[-0.5, 0.6, -0.08, 0.9])

# ---------------------------------------------------------------- TRICEPS
reg("Tricep Pushdown", [
    # f=177 put the locked-out wrist 0.054 pose units BELOW the hip - the hand pressed down past the
    # hip and disappeared into the thigh. f=128 (the first fix) overcorrected the other way, stopping
    # 0.012 units ABOVE the hip with a visible gap instead of reaching it. f=140 lands right at the
    # hip/thigh line (0.011 below hip) - the hand reaches the thigh without sinking into it. T=4
    # (almost perfectly upright) also read as too stiff - a real pushdown leans forward slightly from
    # the hips to brace against the cable, so both keys lean a bit more.
    key(0.0, (0, .5295), 10, FLAT, fkarm(180, 62), label="Elbows bent", cue="Elbows pinned to your sides, slight forward lean"),
    key(0.5, (0, .5295), 10, FLAT, fkarm(180, 140), label="Press down & squeeze", cue="Lock out at your hips, keep upper arms still"),
], [cable([.12, 1.48], "wristN", g="rope")],
    [ELB()], ["uarm"], dur=2800, view=[-0.5, 0.6, -0.08, 1.6])

reg("Skull Crushers", [
    key(0.0, (0, .27), -90, foot(.30, .05), fkarm(0, 0), label="Arms extended", cue="Upper arms vertical, elbows tucked"),
    key(0.5, (0, .27), -90, foot(.30, .05), fkarm(-6, -88), label="Lower to forehead", cue="Only your forearms move"),
], BENCH + [plate("wristN", .07)], [ELB()], ["uarm"], dur=3000, view=[-0.75, 0.55, -0.08, 0.9])

reg("Overhead Tricep Extension", [
    key(0.0, (0, .5295), 0, FLAT, fkarm(-4, 2), label="Extended", cue="Biceps by your ears, core tight"),
    key(0.5, (0, .5295), 0, FLAT, fkarm(-14, -165), label="Lower behind head", cue="Elbows point forward, don't flare"),
], [db("wristN", .06)], [ELB()], ["uarm"], dur=3000, view=[-0.6, 0.6, -0.08, 1.45])
