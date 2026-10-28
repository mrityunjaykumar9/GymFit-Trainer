"""
Reference implementation of the exercise-motion model (kinematics + preview renderer).

The Kotlin runtime (presentation/components/motion/MotionEngine.kt) mirrors resolve() below
exactly; this Python copy exists so poses can be authored and checked visually as PNG contact
sheets before they ship. Coordinates: world units where a standing figure is ~1.0 tall, +x is
the direction the figure faces, y is up, ground is y=0. Angles are compass bearings in degrees:
0 = straight up, 90 = forward (+x), 180 = straight down, -90/270 = backward.
"""
import math

THIGH, SHIN, TORSO = 0.245, 0.245, 0.30
UARM, FARM, FOOT = 0.165, 0.185, 0.10
HEAD_D, HEAD_R, ANKLE_H = 0.115, 0.062, 0.04


def vec(a):
    r = math.radians(a)
    return (math.sin(r), math.cos(r))


def add(p, q, k=1.0):
    return (p[0] + q[0] * k, p[1] + q[1] * k)


def ik(root, target, l1, l2, s):
    """Two-bone IK. Returns (mid, end). s=+1 bends the mid joint counter-clockwise of the line."""
    dx, dy = target[0] - root[0], target[1] - root[1]
    d = math.hypot(dx, dy)
    d = min(max(d, abs(l1 - l2) + 1e-4), l1 + l2 - 1e-4)
    b = math.degrees(math.atan2(dx, dy))
    a = math.degrees(math.acos(max(-1.0, min(1.0, (l1 * l1 + d * d - l2 * l2) / (2 * l1 * d)))))
    mid = add(root, vec(b - s * a), l1)
    end = add(root, vec(b), d)
    return mid, end


def resolve_leg(hip, spec):
    if "a" in spec:
        knee, ankle = ik(hip, spec["a"], THIGH, SHIN, spec.get("b", 1))
    else:
        knee = add(hip, vec(spec["th"]), THIGH)
        ankle = add(knee, vec(spec["sh"]), SHIN)
    toe = add(ankle, vec(spec.get("ft", 90)), FOOT)
    return knee, ankle, toe


def resolve_arm(shoulder, spec, T=0, hip=(0, 0)):
    if "h" in spec or "hs" in spec or "hh" in spec:
        tgt = spec["h"] if "h" in spec else add(shoulder, spec["hs"]) if "hs" in spec else add(hip, spec["hh"])
        elbow, wrist = ik(shoulder, tgt, UARM, FARM, spec.get("b", -1))
    else:
        rt = T if spec.get("rt") else 0
        sc = spec.get("sc", 1)
        elbow = add(shoulder, vec(spec["u"] + rt), UARM * sc)
        wrist = add(elbow, vec(spec["f"] + rt), FARM * sc)
    return elbow, wrist


def resolve(p):
    """p: normalized pose dict -> dict of named joint points."""
    hip = tuple(p["hip"])
    shoulder = add(add(hip, vec(p["T"]), TORSO), (0, p.get("sl", 0)))
    head = add(shoulder, vec(p["T"] + p.get("head", 0)), HEAD_D)
    out = {"hip": hip, "shoulder": shoulder, "head": head, "T": p["T"], "spine": p.get("spine", 0)}
    for side in ("N", "F"):
        k, a, t = resolve_leg(hip, p["leg" + side])
        out["knee" + side], out["ankle" + side], out["toe" + side] = k, a, t
        e, w = resolve_arm(shoulder, p["arm" + side], p["T"], hip)
        out["elbow" + side], out["wrist" + side] = e, w
    return out


DEFAULTS = {"sc": 1, "ft": 90}


def lerp_val(a, b, t):
    if isinstance(a, (list, tuple)):
        return [lerp_val(x, y, t) for x, y in zip(a, b)]
    if isinstance(a, dict):
        out = {}
        for k in set(a) | set(b):
            d = DEFAULTS.get(k)
            x, y = a.get(k, d if d is not None else b.get(k)), b.get(k, d if d is not None else a.get(k))
            out[k] = lerp_val(x, y, t)
        return out
    return a + (b - a) * t


def normalize(k):
    q = dict(k)
    q.setdefault("head", 0)
    q.setdefault("spine", 0)
    q.setdefault("sl", 0)
    q.setdefault("legF", q["legN"])
    q.setdefault("armF", q["armN"])
    for side in ("legN", "legF"):
        if "a" in q[side] and "b" not in q[side]:
            q[side] = dict(q[side], b=1)
    for side in ("armN", "armF"):
        if any(m in q[side] for m in ("h", "hs", "hh")) and "b" not in q[side]:
            q[side] = dict(q[side], b=-1)
    return q


def ease(t, mode):
    return t * t * (3 - 2 * t) if mode == "smooth" else t


def pose_at(motion, t):
    keys = [normalize(k) for k in motion["keys"]]
    times = [k["t"] for k in keys]
    keys.append(dict(keys[0])); times.append(1.0)
    t = t % 1.0
    lab = ""
    for k in keys[:-1]:
        if k["t"] <= t + 1e-9:
            lab = k.get("label", "")
    for i in range(len(keys) - 1):
        if times[i] <= t <= times[i + 1]:
            span = times[i + 1] - times[i]
            u = 0 if span == 0 else (t - times[i]) / span
            a, b = keys[i], keys[i + 1]
            u = ease(u, motion.get("ease", "smooth"))
            p = {}
            for name in ("hip", "T", "head", "spine", "sl", "legN", "legF", "armN", "armF"):
                p[name] = lerp_val(a[name], b[name], u)
            return p, lab
    return keys[0], ""


def joint_angle(pts, a, b, c):
    v1 = (pts[a][0] - pts[b][0], pts[a][1] - pts[b][1])
    v2 = (pts[c][0] - pts[b][0], pts[c][1] - pts[b][1])
    n = math.hypot(*v1) * math.hypot(*v2)
    if n == 0:
        return 0
    return math.degrees(math.acos(max(-1, min(1, (v1[0] * v2[0] + v1[1] * v2[1]) / n))))


def measure_value(pts, m):
    if m[0] == "lean":
        return abs(((pts["T"] + 180) % 360) - 180)
    return joint_angle(pts, m[0], m[1], m[2])


# ----------------------------------------------------------------- preview renderer (PIL)
def render(motion, t, size=300, title=None):
    from PIL import Image, ImageDraw
    SS = 2
    W = size * SS
    img = Image.new("RGB", (W, W), (255, 246, 238))
    d = ImageDraw.Draw(img)
    x0, x1, y0, y1 = motion.get("view", [-0.7, 0.7, -0.08, 1.32])
    sc = min(W / (x1 - x0), W / (y1 - y0))
    ox = W / 2 - (x0 + x1) / 2 * sc
    oy = W / 2 + (y0 + y1) / 2 * sc

    def P(p):
        return (ox + p[0] * sc, oy - p[1] * sc)

    def line(a, b, w, col):
        pa, pb = P(a), P(b)
        d.line([pa, pb], fill=col, width=int(w * sc))
        r = w * sc / 2
        for q in (pa, pb):
            d.ellipse([q[0] - r, q[1] - r, q[0] + r, q[1] + r], fill=col)

    def circle(c, r, col, fill=True, wid=3):
        q = P(c); rr = r * sc
        box = [q[0] - rr, q[1] - rr, q[0] + rr, q[1] + rr]
        if fill:
            d.ellipse(box, fill=col)
        else:
            d.ellipse(box, outline=col, width=wid)

    p, label = pose_at(motion, t)
    pts = resolve(p)

    def ref(r):
        if isinstance(r, str):
            return pts[r]
        if isinstance(r, dict):
            return add(pts[r["p"]], r.get("off", (0, 0)))
        return tuple(r)

    FURN, METAL, BODY, BODYF, ACC = (200, 190, 184), (110, 114, 140), (27, 31, 59), (92, 98, 138), (255, 107, 53)
    d.line([P((-9, 0)), P((9, 0))], fill=(170, 160, 150), width=3)
    for pr in motion.get("props", []):
        if pr.get("z", 0) > 0:
            continue
        draw_prop(pr, ref, line, circle, FURN, METAL, ACC, sc, d, P)
    BG = (255, 246, 238)
    TORSOC = (58, 66, 110)

    def limb(a, b, w, col):
        line(a, b, w + .014, BG)
        line(a, b, w, col)
    # far limbs
    line(pts["hip"], pts["kneeF"], .052, BODYF); line(pts["kneeF"], pts["ankleF"], .042, BODYF)
    line(pts["ankleF"], pts["toeF"], .034, BODYF)
    line(pts["shoulder"], pts["elbowF"], .042, BODYF); line(pts["elbowF"], pts["wristF"], .034, BODYF)
    spine_line(pts, line, TORSOC)
    limb(pts["hip"], pts["kneeN"], .06, BODY); limb(pts["kneeN"], pts["ankleN"], .046, BODY)
    limb(pts["ankleN"], pts["toeN"], .036, BODY)
    circle(pts["head"], HEAD_R + .007, BG)
    circle(pts["head"], HEAD_R, TORSOC)
    limb(pts["shoulder"], pts["elbowN"], .046, BODY); limb(pts["elbowN"], pts["wristN"], .036, BODY)
    circle(pts["wristN"], .026, BODY)
    for pr in motion.get("props", []):
        if pr.get("z", 0) > 0:
            draw_prop(pr, ref, line, circle, FURN, METAL, ACC, sc, d, P)
    ms = motion.get("measure", [])
    txt = []
    for m in ms:
        txt.append("%s %d" % (m["name"], round(measure_value(pts, m["j"]))))
    d.text((10, 10), (title or "") + "  " + label, fill=(0, 0, 0))
    d.text((10, 30), " | ".join(txt), fill=(150, 40, 0))
    return img.resize((size, size), Image.LANCZOS)


def spine_line(pts, line, col):
    hip, sh, s = pts["hip"], pts["shoulder"], pts["spine"]
    T = pts["T"]
    n = (math.cos(math.radians(T)), -math.sin(math.radians(T)))
    c = ((hip[0] + sh[0]) / 2 + n[0] * 2 * s, (hip[1] + sh[1]) / 2 + n[1] * 2 * s)
    prev = hip
    for i in range(1, 9):
        u = i / 8
        q = ((1 - u) ** 2 * hip[0] + 2 * u * (1 - u) * c[0] + u * u * sh[0],
             (1 - u) ** 2 * hip[1] + 2 * u * (1 - u) * c[1] + u * u * sh[1])
        line(prev, q, .10, col)
        prev = q


def draw_prop(pr, ref, line, circle, FURN, METAL, ACC, sc, d, P):
    k = pr["k"]
    col = {"furn": FURN, "metal": METAL, "acc": ACC}[pr.get("c", "furn")]
    if k == "line":
        line(ref(pr["a"]), ref(pr["b"]), pr.get("w", .03), col)
    elif k == "circle":
        circle(ref(pr["at"]), pr["r"], col, pr.get("fill", True))
    elif k == "plate":
        c = ref(pr["at"])
        circle(c, pr.get("r", .085), METAL); circle(c, pr.get("r", .085) * .72, ACC); circle(c, .02, METAL)
    elif k == "db":
        c = ref(pr["at"])
        circle(c, pr.get("r", .045), METAL)
    elif k == "cable":
        a, b = ref(pr["from"]), ref(pr["at"])
        d.line([P(a), P(b)], fill=METAL, width=3)
        circle(a, .025, METAL)
    elif k == "ropeloop":
        c = ref(pr["at"])
        cx, cy = c[0], c[1] / 2
        p0, p1 = P((cx - pr["rx"], cy + cy)), P((cx + pr["rx"], 0))
        d.ellipse([p0[0], p0[1], p1[0], p1[1]], outline=METAL, width=3)
    elif k == "ball":
        circle(ref(pr["at"]), pr["r"], col)
