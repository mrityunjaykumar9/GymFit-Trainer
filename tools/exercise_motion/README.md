# Exercise motion authoring

Source of truth for the in-app exercise animations. `m_*.py` define key poses per exercise
(joint angles / IK targets, props, measured angles, coaching cues); `build.py` validates them and
writes `app/src/main/assets/exercise_motions.json`. `engine.py` is the Python mirror of
`MotionModel.kt`; `python preview.py out.png "Exercise Name" ...` renders key poses to PNG
(needs `pip install pillow`) so poses can be checked without running the app.
Keep engine.py and MotionModel.kt in sync. Exercise names must match `ExerciseSeedData.kt`.

Cable props: `cable(from, at, g="rope"|"bar"|"handle", tower=True)` draws a tower with weight stack, pulley, cable and the grip in the hands (see `MachineProps.kt`); use `tower=False` for machines without a tower (e.g. the rowing machine chain).


## Front-view data (`m_lateral.py`)

The engine itself is a side-view model. `m_lateral.py` runs last (see `build.py`) and adds what the side view cannot say
for the 3D athlete: `grip` (half-distance between the hands), `stance` (between the feet), `lat` (elbow flare, or an
arm swinging out to the side / in, in degrees) and `frontal=True` for movements that happen in the plane facing the
viewer (Warrior II, Triangle, Tree, Lateral Raise - there pose +x is the body's side-to-side axis and the near side is
the character's left). Distances are pose units (1.0 = standing height, 0.135 = shoulder width). After changing it run
`python build.py`, then check the exercise in the app from the Front and Side chips (or with the debug intent in
CLAUDE.md).

## Coaching guides (`m_guides.py`)

People look at the animation, they don't read the text. `m_guides.py` (runs last, after `m_lateral.py`) adds up to
three marks per exercise that are drawn over BOTH the 3D athlete and the 2D side view when the "Guides" chip is on:
`plumb(joint, label)` vertical line to the floor (wrist over elbow, bar over mid-foot), `level(joint, label)`
horizontal line (elbows at shoulder height), `line(a, b, label)` straight line between two joints (head to heels),
`ring(joint, label)` pulsing marker (elbow stays pinned) and `tilt(a, b, label)` the angle of a segment against the
horizontal with a live degree readout (back angle). Keep labels short (about 20 characters). The kotlin side is
`GuideOverlay.kt` (drawing, label placement) with the 3D joint projection in `AthleteStage.guideSpace`. `build.py`
rejects unknown joints or more than three guides.
