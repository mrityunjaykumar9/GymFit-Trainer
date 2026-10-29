import importlib, json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from lib import MOTIONS
MODULES = ["m_upper", "m_upper2", "m_lower", "m_core_cardio", "m_yoga", "m_warmup", "m_lateral", "m_guides"]

def load_all():
    for m in MODULES:
        try:
            importlib.import_module(m)
        except ModuleNotFoundError as e:
            if e.name != m: raise
    return MOTIONS

def validate(ms):
    from engine import normalize
    bad = []
    for name, m in ms.items():
        for limb in ("legN", "legF", "armN", "armF"):
            modes = set()
            for k in m["keys"]:
                spec = normalize(k)[limb]
                modes.add(next((x for x in ("a", "h", "hs", "hh") if x in spec), "fk"))
            if len(modes) > 1:
                bad.append("%s.%s mixes %s" % (name, limb, sorted(modes)))
        ts = [k["t"] for k in m["keys"]]
        if ts != sorted(ts) or ts[0] != 0:
            bad.append("%s key times must start at 0 and ascend" % name)
    joints = {"hip", "shoulder", "head", "elbowN", "wristN", "kneeN", "ankleN", "toeN", "elbowF", "wristF", "kneeF", "ankleF", "toeF"}
    for name, m in ms.items():
        for g in m.get("guides", []):
            if g["k"] not in ("plumb", "level", "line", "ring", "tilt") or g["j"] not in joints or (g["k"] in ("line", "tilt") and g.get("a") not in joints):
                bad.append("%s has a bad guide %s" % (name, g))
        if len(m.get("guides", [])) > 3:
            bad.append("%s has more than 3 guides" % name)
    if bad:
        raise SystemExit("; ".join(bad))


if __name__ == "__main__":
    ms = load_all()
    validate(ms)
    out = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "exercise_motions.json")
    with open(out, "w") as f:
        json.dump(ms, f, separators=(",", ":"))
    print(len(ms), "motions ->", os.path.getsize(out) // 1024, "KB")
