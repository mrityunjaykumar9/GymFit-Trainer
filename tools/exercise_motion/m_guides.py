"""Coaching guides drawn over the animation (dashed lines, rings, angle readouts) - runs after m_lateral.

People look at the animation, they don't read the text, so each exercise gets two or three marks that show the
one or two things that matter most (see Guide in MotionModel.kt / GuideOverlay.kt):

  plumb(j, label)       vertical dashed line from joint j down to the floor ("wrist over elbow", "bar over mid-foot")
  level(j, label)       horizontal dashed line through joint j ("elbows at shoulder height", "stop at shoulder level")
  line(a, b, label)     straight dashed line between two joints ("head to heels in one line")
  ring(j, label)        pulsing ring around joint j ("elbow stays pinned", "hips stay down")
  tilt(a, b, label)     the segment a -> b against the horizontal, with a live angle readout ("back angle 35")

Joints: hip, shoulder, head, elbowN, wristN, kneeN, ankleN, toeN (N = the near side, F = the far side).
Keep labels short (about 24 characters) and use no more than three guides per exercise so the picture stays clean.
"""
from lib import MOTIONS


def plumb(j, l=""): return {"k": "plumb", "j": j, "l": l}
def level(j, l=""): return {"k": "level", "j": j, "l": l}
def line(a, b, l=""): return {"k": "line", "j": a, "a": b, "l": l}
def ring(j, l=""): return {"k": "ring", "j": j, "l": l}
def tilt(a, b, l=""): return {"k": "tilt", "j": a, "a": b, "l": l}


def G(name, *guides):
    assert name in MOTIONS, name
    MOTIONS[name]["guides"] = list(guides)


# ------------------------------------------------------------------ chest
G("Barbell Bench Press", plumb("elbowN", "Wrist over elbow"), ring("shoulder", "Blades pinched"))
G("Incline Barbell Bench Press", plumb("elbowN", "Wrist over elbow"), tilt("hip", "shoulder", "Incline"))
G("Dumbbell Bench Press", plumb("elbowN", "Wrist over elbow"), ring("shoulder", "Blades pinched"))
G("Incline Dumbbell Press", plumb("elbowN", "Wrist over elbow"), tilt("hip", "shoulder", "Incline"))
G("Close Grip Bench Press", plumb("elbowN", "Wrist over elbow"), ring("elbowN", "Elbows tucked"))
G("Cable Fly", level("shoulder", "Hands at chest height"), ring("elbowN", "Soft, fixed elbow"))
G("Pec Deck", level("shoulder", "Elbows shoulder-high"))
G("Push Ups", line("shoulder", "ankleN", "Straight line"), plumb("wristN", "Hands under shoulders"))
# ------------------------------------------------------------------ back
G("Pull Ups", level("wristN", "Chin over the bar"), ring("shoulder", "Shoulders down"))
G("Lat Pulldown", plumb("hip", "Lean back a little"), ring("elbowN", "Elbows drive down"))
G("Barbell Row", tilt("hip", "shoulder", "Back angle"), plumb("wristN", "Bar close to body"))
G("T-Bar Row", tilt("hip", "shoulder", "Back angle"), ring("shoulder", "Squeeze blades"))
G("Seated Cable Row", plumb("hip", "Chest tall"), level("wristN", "Pull to belly"))
G("Deadlift", plumb("ankleN", "Bar over mid-foot"), tilt("hip", "shoulder", "Flat back"))
# ------------------------------------------------------------------ shoulders
G("Overhead Press", plumb("ankleN", "Bar over mid-foot"), ring("hip", "Glutes tight"))
G("Dumbbell Shoulder Press", plumb("shoulder", "Press straight up"))
G("Lateral Raise", level("shoulder", "Up to shoulder height"))
G("Front Raise", level("shoulder", "Up to shoulder height"))
G("Rear Delt Fly", tilt("hip", "shoulder", "Back angle"), ring("elbowN", "Lead with the elbows"))
G("Face Pull", level("shoulder", "Elbows shoulder-high"), ring("wristN", "Hands by ears"))
G("Barbell Shrug", plumb("shoulder", "Lift straight up"))
# ------------------------------------------------------------------ arms
G("Barbell Curl", ring("elbowN", "Elbow pinned"), plumb("shoulder", "No swinging"))
G("Dumbbell Curl", ring("elbowN", "Elbow pinned"), plumb("shoulder", "No swinging"))
G("Hammer Curl", ring("elbowN", "Elbow pinned"), plumb("shoulder", "No swinging"))
G("Cable Curl", ring("elbowN", "Elbow pinned"), plumb("shoulder", "No swinging"))
G("Preacher Curl", ring("elbowN", "Arm stays on pad"))
G("Wrist Curl", ring("elbowN", "Forearm on thigh"), ring("wristN", "Only wrist moves"))
G("Tricep Pushdown", ring("elbowN", "Elbow pinned"), plumb("shoulder", "Upper arm vertical"))
G("Skull Crushers", ring("elbowN", "Elbows point up"), plumb("shoulder", "Arms near vertical"))
G("Overhead Tricep Extension", plumb("elbowN", "Elbows by your ears"))
# ------------------------------------------------------------------ legs
G("Barbell Back Squat", plumb("ankleN", "Bar over mid-foot"), tilt("hip", "shoulder", "Chest up"))
G("Front Squat", plumb("ankleN", "Bar over mid-foot"), ring("elbowN", "Elbows stay high"))
G("Leg Press", ring("kneeN", "Knees over toes"), ring("hip", "Hips on seat"))
G("Romanian Deadlift", tilt("hip", "shoulder", "Flat back"), plumb("ankleN", "Bar close to legs"))
G("Leg Extension", ring("kneeN", "Knee is the hinge"), ring("ankleN", "Squeeze at the top"))
G("Walking Lunges", plumb("ankleN", "Front knee over ankle"), plumb("hip", "Torso upright"))
G("Bulgarian Split Squat", plumb("ankleN", "Front knee over ankle"), plumb("hip", "Torso upright"))
G("Calf Raise", ring("ankleN", "Rise high, pause"), plumb("hip", "No leaning"))
G("Hip Thrust", line("shoulder", "kneeN", "Straight line"), plumb("ankleN", "Shins vertical"))
G("Glute Bridge", line("shoulder", "kneeN", "Straight line"), ring("hip", "Squeeze glutes"))
# ------------------------------------------------------------------ core & cardio
G("Plank", line("shoulder", "ankleN", "Straight line"), plumb("shoulder", "Elbow under shoulder"))
G("Crunch", ring("hip", "Back stays down"), ring("head", "Don't pull neck"))
G("Hanging Leg Raise", ring("shoulder", "No swinging"))
G("Cable Crunch", ring("hip", "Hips stay still"))
G("Running", plumb("hip", "Land under hips"))
G("Farmer's Carry", plumb("shoulder", "Stand tall"))
G("Jump Rope", ring("wristN", "Small wrist circles"))
G("Rowing Machine", plumb("ankleN", "Shins vertical"))
G("Stationary Cycling", ring("hip", "Hips steady"))
G("Stair Climbing", plumb("hip", "Stand tall"))
G("Burpees", plumb("wristN", "Hands under shoulders"))
G("Kettlebell/Barbell Clean and Press", plumb("ankleN", "Bar close"))
G("Mountain Climbers", plumb("wristN", "Hands under shoulders"), ring("hip", "Hips low and level"))
# ------------------------------------------------------------------ yoga
G("Mountain Pose (Tadasana)", plumb("shoulder", "Stack up in one line"))
G("Warrior I (Virabhadrasana I)", plumb("ankleN", "Front knee over ankle"))
G("Warrior II (Virabhadrasana II)", level("shoulder", "Arms shoulder-high"), plumb("ankleN", "Front knee over ankle"))
G("Downward-Facing Dog (Adho Mukha Svanasana)", line("wristN", "hip", "Long straight back"))
G("Child's Pose (Balasana)", ring("hip", "Sit back"))
G("Cat-Cow Stretch (Marjaryasana-Bitilasana)", plumb("shoulder", "Hands under shoulders"), plumb("hip", "Knees under hips"))
G("Cobra Pose (Bhujangasana)", ring("hip", "Hips down"), ring("elbowN", "Elbows close"))
G("Bridge Pose (Setu Bandhasana)", line("shoulder", "kneeN", "One long line"))
G("Seated Forward Bend (Paschimottanasana)", ring("hip", "Hinge from the hips"))
# ------------------------------------------------------------------ warm-up & mobility
G("Leg Swings", plumb("hip", "Swing from hip"))
G("High Knees", level("hip", "Knee to hip height"))
G("Standing Side Bend", ring("hip", "Hips stay still"))
G("Standing Toe Touch", ring("kneeN", "Soft knees"))
G("Jumping Jacks", level("shoulder", "Hands overhead"))
G("Inchworm", plumb("wristN", "Hands under shoulders"))
