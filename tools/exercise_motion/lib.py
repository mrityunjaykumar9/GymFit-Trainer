"""Authoring helpers shared by the m_*.py motion definition modules."""
from engine import ANKLE_H

MOTIONS = {}
STAND_HIP = 0.5295  # hip height with legs (almost) straight, feet flat


def key(t, hip, T, legN, armN, legF=None, armF=None, head=0, spine=0, sl=0, label="", cue="", lat=None, grip=None, stance=None):
    """lat = [armN, armF, legN, legF] degrees of sideways swing; grip / stance = half-widths (3D view only)."""
    k = {"t": t, "hip": list(hip), "T": T, "legN": legN, "armN": armN, "head": head, "spine": spine, "sl": sl}
    if lat is not None: k["lat"] = list(lat)
    if grip is not None: k["grip"] = grip
    if stance is not None: k["stance"] = stance
    if legF is not None: k["legF"] = legF
    if armF is not None: k["armF"] = armF
    if label: k["label"] = label
    if cue: k["cue"] = cue
    return k


def foot(x, y=ANKLE_H, **kw):
    d = {"a": [x, y]}; d.update(kw); return d


def fkleg(th, sh, **kw):
    d = {"th": th, "sh": sh}; d.update(kw); return d


def hand(x, y, **kw):
    d = {"h": [x, y]}; d.update(kw); return d


def handrel(dx, dy, **kw):
    d = {"hs": [dx, dy]}; d.update(kw); return d


def fkarm(u, f, rel=False, **kw):
    d = {"u": u, "f": f}
    if rel: d["rt"] = 1
    d.update(kw); return d


def prop_line(a, b, w=.03, c="furn", z=0): return {"k": "line", "a": a, "b": b, "w": w, "c": c, "z": z}
def prop_circle(at, r, c="furn", fill=True, z=0): return {"k": "circle", "at": at, "r": r, "c": c, "fill": fill, "z": z}
def plate(at, r=.085, z=1): return {"k": "plate", "at": at, "r": r, "z": z}
def db(at, r=.045, z=1): return {"k": "db", "at": at, "r": r, "z": z}
def cable(frm, at, z=0, g="handle", tower=True): return {"k": "cable", "from": frm, "at": at, "z": z, "g": g, "tower": tower}
def at(p, off=(0, 0)): return {"p": p, "off": list(off)}


def reg(name, keys, props=(), measure=(), focus=(), dur=3200, ease="smooth", view=None, tip="", frontal=False):
    m = {"keys": keys, "props": list(props), "measure": list(measure), "focus": list(focus),
         "dur": dur, "ease": ease, "tip": tip}
    if frontal: m["frontal"] = True
    if view: m["view"] = view
    MOTIONS[name] = m


def meas(name, a, b=None, c=None):
    return {"name": name, "j": [a] if b is None else [a, b, c]} if a != "lean" else {"name": name, "j": ["lean"]}


import math
from engine import vec, add, TORSO


def shoulder_of(hip, T):
    return add(tuple(hip), vec(T), TORSO)


def normal(T):
    return (math.cos(math.radians(T)), -math.sin(math.radians(T)))


def hiprel(dx, dy, **kw):
    d = {"hh": [dx, dy]}; d.update(kw); return d
