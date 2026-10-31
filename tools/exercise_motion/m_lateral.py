"""Sideways (front-view) data for every exercise, applied on top of the side-view key poses in the other m_*.py files.

The authoring engine is a side-view (sagittal) model, so by itself it cannot say how wide a grip is, how far apart the
feet are, or that an arm goes out to the SIDE. This table adds that for the 3D athlete. All distances are in pose
units (a standing figure is 1.0 tall, so 0.10 = about 18 cm):

  grip    half-distance between the hands (0.135 = shoulder width, bars are wider)
  stance  half-distance between the feet (0.075 = hip width)
  lat     [armN, armF, legN, legF] degrees: elbows flare out when a grip is set, otherwise the arm swings out
          sideways (negative = inward, e.g. hands coming together in a fly)
  frontal the whole movement happens in the plane facing the viewer (Warrior II, Triangle, Tree, Lateral Raise)

Any value can be one number (all key poses) or a list with one entry per key pose.
"""
from lib import MOTIONS


def L(name, grip=None, stance=None, lat=None, frontal=False):
    m = MOTIONS[name]
    n = len(m["keys"])

    def at(v, i, lat_list=False):
        if v is None: return None
        if lat_list:
            return v[i] if isinstance(v[0], (list, tuple)) else v
        return v[i] if isinstance(v, (list, tuple)) else v

    for i, k in enumerate(m["keys"]):
        g, s, l = at(grip, i), at(stance, i), at(lat, i, True)
        if g is not None: k["grip"] = g
        if s is not None: k["stance"] = s
        if l is not None: k["lat"] = list(l)
    if frontal: m["frontal"] = True


# ------------------------------------------------------------------ chest
L("Barbell Bench Press", grip=.20, stance=.11, lat=[35, 35, 0, 0])
L("Incline Barbell Bench Press", grip=.20, stance=.11, lat=[35, 35, 0, 0])
L("Dumbbell Bench Press", grip=.15, stance=.11, lat=[40, 40, 0, 0])
L("Incline Dumbbell Press", grip=.15, stance=.11, lat=[40, 40, 0, 0])
L("Close Grip Bench Press", grip=.085, stance=.11, lat=[8, 8, 0, 0])
L("Cable Fly", stance=.08, lat=[[82, 82, 0, 0], [-26, -26, 0, 0]])
L("Pec Deck", grip=[.30, .05], stance=.09, lat=[[60, 60, 0, 0], [10, 10, 0, 0]])
L("Push Ups", grip=.17, stance=.06, lat=[30, 30, 0, 0])
# ------------------------------------------------------------------ back
L("Pull Ups", grip=.26, stance=.04, lat=[12, 12, 0, 0])
L("Lat Pulldown", grip=.27, stance=.11, lat=[10, 10, 0, 0])
L("Barbell Row", grip=.16, stance=.085, lat=[30, 30, 0, 0])
L("T-Bar Row", grip=.05, stance=.12, lat=[40, 40, 0, 0])
L("Seated Cable Row", grip=.05, stance=.075, lat=[20, 20, 0, 0])
L("Deadlift", grip=.15, stance=.075)
# ------------------------------------------------------------------ shoulders
L("Overhead Press", grip=.16, stance=.075, lat=[15, 15, 0, 0])
L("Dumbbell Shoulder Press", grip=[.17, .15], stance=.09, lat=[20, 20, 0, 0])
L("Lateral Raise", grip=.12, stance=.075, frontal=True)
L("Front Raise", grip=.12)
L("Rear Delt Fly", stance=.085, lat=[[0, 0, 0, 0], [60, 60, 0, 0]])
L("Face Pull", grip=[.06, .17], stance=.075, lat=[[10, 10, 0, 0], [60, 60, 0, 0]])
L("Barbell Shrug", grip=.19, stance=.075)
# ------------------------------------------------------------------ arms
L("Barbell Curl", grip=.11, stance=.075, lat=[8, 8, 0, 0])
L("Dumbbell Curl", grip=.13, stance=.075)
L("Hammer Curl", grip=.12, stance=.075)
L("Cable Curl", grip=.11, stance=.075, lat=[8, 8, 0, 0])
L("Preacher Curl", grip=.12, stance=.09)
L("Wrist Curl", grip=.11, stance=.10)
L("Tricep Pushdown", grip=.09, stance=.075, lat=[5, 5, 0, 0])
L("Skull Crushers", grip=.10, stance=.11, lat=[8, 8, 0, 0])
L("Overhead Tricep Extension", grip=.09, stance=.075)
# ------------------------------------------------------------------ legs & glutes
L("Barbell Back Squat", grip=.17, stance=.135, lat=[20, 20, 0, 0])
L("Front Squat", grip=.14, stance=.125)
L("Leg Press", stance=.12)
L("Romanian Deadlift", grip=.14, stance=.075)
L("Leg Extension", stance=.10)
L("Walking Lunges", grip=.14, stance=.06)
L("Bulgarian Split Squat", grip=.14, stance=.06)
L("Calf Raise", grip=.14, stance=.075)
L("Hip Thrust", grip=.17, stance=.09)
L("Glute Bridge", grip=.13, stance=.08)
# ------------------------------------------------------------------ core & cardio
L("Plank", grip=.07, stance=.06)
L("Crunch", grip=.075, stance=.09)
L("Hanging Leg Raise", grip=.22, stance=.04)
L("Cable Crunch", grip=.06, stance=.09)
L("Running", stance=.08)
L("Farmer's Carry", grip=.15, stance=.08)
L("Rowing Machine", grip=.18, stance=.07)
L("Stationary Cycling", grip=.17, stance=.07)
L("Stair Climbing", grip=.15, stance=.08)
L("Burpees", grip=.15, stance=.075)
L("Kettlebell/Barbell Clean and Press", grip=.15, stance=.075)
L("Mountain Climbers", grip=.15, stance=.07)
# ------------------------------------------------------------------ yoga
L("Mountain Pose (Tadasana)", grip=[.13, .035], stance=.04)
L("Warrior I (Virabhadrasana I)", grip=.10, stance=.075)
L("Warrior II (Virabhadrasana II)", grip=.02, stance=.02, frontal=True)
L("Downward-Facing Dog (Adho Mukha Svanasana)", grip=.15, stance=.08)
L("Child's Pose (Balasana)", grip=.14, stance=.09)
L("Cat-Cow Stretch (Marjaryasana-Bitilasana)", grip=.15, stance=.09)
L("Cobra Pose (Bhujangasana)", grip=.14, stance=.05)
L("Bridge Pose (Setu Bandhasana)", grip=.13, stance=.08)
L("Seated Forward Bend (Paschimottanasana)", stance=.06)
L("Relaxation Pose", grip=.15, stance=.09)
# ------------------------------------------------------------------ warm-up & mobility
L("Leg Swings", stance=.08)
L("Jumping Jacks", stance=.09, lat=[[0, 0, 0, 0], [70, 70, 55, 55]])
L("High Knees", stance=.07, lat=[15, 15, 0, 0])
L("Standing Side Bend", grip=.09, stance=.075, frontal=True)
L("Standing Toe Touch", grip=.09, stance=.08)
L("Inchworm", grip=.14, stance=.08)
