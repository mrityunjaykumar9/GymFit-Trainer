import math
from lib import *
from engine import vec, add

FLAT = foot(0.0)
MAT = prop_line((-.75, .02), (.75, .02), .03)
STAND_ARMS = fkarm(180, 180)
STAND = lambda t, arms=STAND_ARMS, T=0, **kw: key(t, (0, .525), T, FLAT, arms, **kw)
HIPANG = lambda: meas("Hip", "shoulder", "hip", "kneeN")

# ---------------------------------------------------------------- STANDING
reg("Mountain Pose (Tadasana)", [
    STAND(0.0, STAND_ARMS, label="Exhale: arms down", cue="Feet grounded, spine long, shoulders relaxed"),
    STAND(0.5, fkarm(2, 0), T=0, head=-6, label="Inhale: reach up", cue="Palms together overhead, ribs soft"),
], [MAT], [], ["back", "thigh"], dur=5000, view=[-0.6, 0.6, -0.08, 1.35],
    tip="Mountain Pose is stillness: stand tall, weight even on both feet, and breathe slowly.")

# Warrior geometry (front = near leg)
_wr_hip = (-.02, .33)
_wr_front = foot(.18, .04)
_wr_back = fkleg(233.7, 233.7, ft=90)
_st_far = fkleg(180, 180)

reg("Warrior I (Virabhadrasana I)", [
    key(0.0, (0, .525), 0, FLAT, fkarm(180, 180), legF=_st_far, armF=fkarm(180, 180), label="Stand", cue="Feet together, arms by sides"),
    key(0.5, _wr_hip, -3, _wr_front, fkarm(4, 2), legF=_wr_back, armF=fkarm(4, 2), head=-12, label="Lunge, arms up", cue="Front knee over ankle, back leg strong, hips square"),
], [MAT], [meas("Front knee", "hip", "kneeN", "ankleN")], ["thigh", "shoulder"], dur=4600, view=[-0.65, 0.6, -0.08, 1.4])

reg("Warrior II (Virabhadrasana II)", [
    key(0.0, (0, .525), 0, FLAT, fkarm(180, 180), legF=_st_far, armF=fkarm(180, 180), label="Stand", cue="Feet together, arms by sides"),
    key(0.5, _wr_hip, 0, _wr_front, fkarm(90, 90), legF=_wr_back, armF=fkarm(270, 270), head=8, label="Wide arms, deep lunge", cue="Gaze over front hand, shoulders stacked over hips"),
], [MAT], [meas("Front knee", "hip", "kneeN", "ankleN")], ["thigh", "shoulder"], dur=4600, view=[-0.8, 0.8, -0.08, 1.2])

# ---------------------------------------------------------------- FLOOR: HANDS & KNEES
_tab_hip, _tab_T = (-.035, .285), 71.6
_tab_legs = fkleg(180, 270, ft=270)
_HAND = hand(.25, .03)

reg("Downward-Facing Dog (Adho Mukha Svanasana)", [
    key(0.0, _tab_hip, _tab_T, _tab_legs, _HAND, head=8, label="Tabletop", cue="Wrists under shoulders, knees under hips"),
    key(0.5, (-.19, .494), 121, fkleg(205.3, 205.3, ft=105), _HAND, head=45, label="Lift hips up & back", cue="Press the floor away, heels reach down, spine long"),
], [MAT], [HIPANG()], ["back", "shoulder", "shin"], dur=4600, view=[-0.6, 0.55, -0.08, 0.8])

reg("Child's Pose (Balasana)", [
    key(0.0, _tab_hip, _tab_T, _tab_legs, _HAND, head=8, label="Tabletop", cue="Big toes together, knees wide as comfortable"),
    key(0.5, (-.20, .11), 80, fkleg(107, 270, ft=270), hand(.46, .03), spine=-.025, head=20, label="Sit back, fold forward", cue="Hips to heels, forehead down, breathe into your back"),
], [MAT], [], ["back"], dur=5000, view=[-0.5, 0.65, -0.08, 0.7])

reg("Cat-Cow Stretch (Marjaryasana-Bitilasana)", [
    key(0.0, _tab_hip, _tab_T, _tab_legs, _HAND, spine=-.065, head=62, label="Cat: round your spine", cue="Exhale, push the floor away, chin to chest"),
    key(0.5, _tab_hip, _tab_T, _tab_legs, _HAND, spine=.055, head=-32, label="Cow: belly drops", cue="Inhale, lift tailbone and chest, look forward"),
], [MAT], [], ["back", "abs"], dur=4400, view=[-0.5, 0.55, -0.08, 0.7])

# ---------------------------------------------------------------- FLOOR: PRONE / SUPINE
_cb_hip = (0.0, .07)
reg("Cobra Pose (Bhujangasana)", [
    key(0.0, _cb_hip, 90, fkleg(270, 270, ft=270), hand(.20, .035, b=-1), head=0, label="Lie face down", cue="Hands under shoulders, legs together"),
    key(0.5, _cb_hip, 42, fkleg(270, 270, ft=270), hand(.20, .035, b=-1), spine=.04, head=-18, label="Lift your chest", cue="Elbows soft, shoulders away from ears, hips stay down"),
], [MAT], [], ["back"], dur=4400, view=[-0.65, 0.6, -0.08, 0.65])

_br_s = (-.30, .075)
_br_hip = lambda T: add(_br_s, vec(T), -.30)
reg("Bridge Pose (Setu Bandhasana)", [
    key(0.0, _br_hip(-90), -90, foot(.27, .04), hand(-.02, .04), label="Lie back", cue="Feet hip-width apart, close to your hips"),
    key(0.5, _br_hip(-122), -122, foot(.27, .04), hand(-.02, .04), spine=-.0, label="Lift hips high", cue="Press into feet and arms, keep knees over ankles"),
], [MAT], [meas("Knee", "hip", "kneeN", "ankleN")], ["glutes", "back"], dur=4600, view=[-0.65, 0.55, -0.08, 0.7])

_ps_hip = (-.10, .065)
_ps_a = shoulder_of(_ps_hip, 0)
_ps_b = shoulder_of(_ps_hip, 80)
reg("Seated Forward Bend (Paschimottanasana)", [
    key(0.0, _ps_hip, 0, fkleg(90, 90, ft=35), hand(_ps_a[0] + .02, _ps_a[1] + .34), label="Sit tall", cue="Legs straight, lengthen your spine first"),
    key(0.5, _ps_hip, 80, fkleg(90, 90, ft=35), hand(.37, .09), spine=-.03, head=24, label="Hinge forward", cue="Fold from the hips, hold shins or feet, soft knees if needed"),
], [MAT], [meas("Hip", "shoulder", "hip", "kneeN")], ["hamstrings", "back"], dur=5000, view=[-0.55, 0.65, -0.08, 0.7])

reg("Relaxation Pose", [
    key(0.0, (0, .07), -90, fkleg(92, 92, ft=100), fkarm(90, 90), spine=0.0, label="Exhale", cue="Let every muscle soften"),
    key(0.5, (0, .07), -90, fkleg(92, 92, ft=100), fkarm(90, 90), spine=0.022, label="Inhale (belly rises)", cue="Breathe naturally, let the floor hold you"),
], [MAT], [], ["back"], dur=5200, view=[-0.65, 0.6, -0.08, 0.65])
