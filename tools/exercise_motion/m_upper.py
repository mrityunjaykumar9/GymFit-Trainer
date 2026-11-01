from lib import *
from engine import vec, add

ELBOW = lambda: meas("Elbow", "shoulder", "elbowN", "wristN")


def press_family(name, incline, dumbbell, label_top, label_bot, cue_bot, focus, tip="", close=False):
    """Supine/incline press. incline = back elevation in degrees (0 = flat)."""
    T = incline - 90
    hip = (0.0, 0.27 if incline == 0 else 0.30)
    sh = shoulder_of(hip, T)
    n = normal(T)
    toward_feet = (-vec(T)[0], -vec(T)[1])
    top = add(sh, n, 0.35)
    bot = add(add(sh, n, 0.07), toward_feet, 0.12)
    legs = foot(0.30 if incline == 0 else 0.26, 0.05)
    if incline == 0:
        furn = [prop_line((-0.55, 0.195), (0.07, 0.195), .05), prop_line((-0.48, 0.17), (-0.48, 0.02), .03), prop_line((0.0, 0.17), (0.0, 0.02), .03)]
    else:
        back_end = add(hip, vec(T), 0.52)
        furn = [prop_line((hip[0] - .03, hip[1] - .05), back_end, .05), prop_line((hip[0] - .03, hip[1] - .05), (0.13, hip[1] - .05), .05),
                prop_line((0.0, hip[1] - .07), (0.0, 0.02), .03), prop_line(add(hip, vec(T), .4), (add(hip, vec(T), .4)[0] - .1, 0.02), .03)]
    load = db("wristN") if dumbbell else plate("wristN")
    keys = [
        key(0.0, hip, T, legs, hand(*top), label=label_top, cue="Wrists stacked over elbows, shoulder blades pinched"),
        key(0.5, hip, T, legs, hand(*bot), label=label_bot, cue=cue_bot),
    ]
    reg(name, keys, furn + [load], [ELBOW()], focus, dur=3000, tip=tip,
        view=[-0.75, 0.55, -0.08, 0.9] if incline == 0 else [-0.7, 0.6, -0.08, 1.0])


press_family("Barbell Bench Press", 0, False, "Lockout", "Bar to mid-chest", "Elbows ~45-75° from body, forearms vertical", ["chest"])
press_family("Incline Barbell Bench Press", 35, False, "Lockout", "Bar to upper chest", "Touch upper chest, don't bounce", ["chest"])
press_family("Dumbbell Bench Press", 0, True, "Lockout", "Deep stretch", "Lower until upper arms are about level with the bench", ["chest"])
press_family("Incline Dumbbell Press", 35, True, "Lockout", "Deep stretch", "Control the stretch, elbows slightly tucked", ["chest"])
press_family("Close Grip Bench Press", 0, False, "Lockout", "Bar to lower chest", "Keep elbows tucked close to your ribs", ["uarm", "chest"])
# Negative incline = head end lower than hips. The press_family math (T = incline - 90) extends
# cleanly to a decline angle without any special-casing - verified visually, bench and bar path
# both slope the right way.
press_family("Decline Bench Press", -15, False, "Lockout", "Bar to lower chest", "Head lower than hips, bar drifts toward your ribs", ["chest"])

# ---------------------------------------------------------------- DIPS
# Same "fixed hand, hip translates" trick as Pull Ups, just upright instead of hanging: the bars
# are fixed in world space, and the body rises/sinks between them as the elbow bends. The IK elbow
# angle is very non-linear near full extension (a shoulder-to-hand distance of 94% of max reach
# only gave a 141deg elbow, not the near-180deg lockout it looks like at first glance) - like Pull
# Ups' dead hang, the fix is to target slightly PAST max reach so the IK clamps to a fully straight
# arm instead of trying to hit an exact soft-lockout distance. The bottom targets ~95-100deg instead.
_dip_hand = hand(0.0, 0.50)
_dip_legs = fkleg(185, 205)
reg("Dips", [
    key(0.0, (0.0, 0.60), -4, _dip_legs, _dip_hand, label="Lockout", cue="Arms straight, shoulders down, body tall"),
    key(0.5, (0.0, 0.45), -22, _dip_legs, _dip_hand, label="Lower to ~90°", cue="Lean forward slightly, elbows drive back"),
], [prop_line((-0.4, 0.50), (0.4, 0.50), .03, "metal"), prop_line((-0.4, 0.50), (-0.4, 0), .03), prop_line((0.4, 0.50), (0.4, 0), .03)],
    [ELBOW()], ["chest", "uarm"], dur=3000, view=[-0.6, 0.6, -0.08, 1.0])
