"""Generates the gym-prop meshes (unit cylinder / box / sphere in several materials) as tiny glb files.

Usage: python build_props.py app/src/main/assets/models/props

Every prop is original procedural geometry. The app instances these, scaling/rotating each one per frame
(see presentation/components/body3d/PropKit.kt). Cylinders run along +Y with radius 0.5 and height 1,
boxes are 1x1x1, spheres have radius 0.5 - all centred on the origin.
"""
import json, math, os, struct, sys
import numpy as np

# name -> (shape, baseColor rgba, metallic, roughness)
KINDS = {
    "cyl_chrome": ("cyl", (0.80, 0.81, 0.84, 1), 1.0, 0.22),
    "cyl_steel": ("cyl", (0.16, 0.17, 0.19, 1), 0.7, 0.42),
    "cyl_black": ("cyl", (0.045, 0.045, 0.05, 1), 0.1, 0.55),
    "cyl_plate": ("cyl", (0.09, 0.09, 0.10, 1), 0.3, 0.5),
    "cyl_accent": ("cyl", (0.90, 0.36, 0.08, 1), 0.0, 0.5),
    "box_pad": ("box", (0.11, 0.11, 0.13, 1), 0.0, 0.5),
    "box_steel": ("box", (0.20, 0.21, 0.23, 1), 0.6, 0.45),
    "box_mat": ("box", (0.16, 0.17, 0.19, 1), 0.0, 0.8),
    "sphere_steel": ("sph", (0.55, 0.56, 0.60, 1), 1.0, 0.3),
}

def cylinder(seg=28):
    P, N, I = [], [], []
    def ring(y, ny, rad):
        base = len(P)
        for i in range(seg + 1):
            a = 2 * math.pi * i / seg
            c, s = math.cos(a), math.sin(a)
            P.append((0.5 * c, y, 0.5 * s)); N.append((c, 0, s) if ny == 0 else (0, ny, 0))
        return base
    a = ring(-0.5, 0, 0.5); b = ring(0.5, 0, 0.5)
    for i in range(seg):
        I += [a + i, b + i, a + i + 1, a + i + 1, b + i, b + i + 1]
    for y, ny in ((0.5, 1), (-0.5, -1)):
        c0 = len(P); P.append((0, y, 0)); N.append((0, ny, 0))
        r0 = len(P)
        for i in range(seg + 1):
            ang = 2 * math.pi * i / seg
            P.append((0.5 * math.cos(ang), y, 0.5 * math.sin(ang))); N.append((0, ny, 0))
        for i in range(seg):
            I += ([c0, r0 + i + 1, r0 + i] if ny > 0 else [c0, r0 + i, r0 + i + 1])
    return P, N, I

def box():
    P, N, I = [], [], []
    for axis in range(3):
        for sign in (1, -1):
            n = [0, 0, 0]; n[axis] = sign
            u = [0, 0, 0]; u[(axis + 1) % 3] = 1
            v = [0, 0, 0]; v[(axis + 2) % 3] = 1
            base = len(P)
            for su, sv in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
                P.append(tuple(0.5 * n[k] + 0.5 * su * u[k] + 0.5 * sv * v[k] for k in range(3))); N.append(tuple(n))
            I += [base, base + 1, base + 2, base, base + 2, base + 3] if sign > 0 else [base, base + 2, base + 1, base, base + 3, base + 2]
    return P, N, I

def sphere(seg=24, rings=14):
    P, N, I = [], [], []
    for r in range(rings + 1):
        th = math.pi * r / rings
        for s in range(seg + 1):
            ph = 2 * math.pi * s / seg
            n = (math.sin(th) * math.cos(ph), math.cos(th), math.sin(th) * math.sin(ph))
            P.append(tuple(0.5 * k for k in n)); N.append(n)
    for r in range(rings):
        for s in range(seg):
            a = r * (seg + 1) + s; b = a + seg + 1
            I += [a, a + 1, b, a + 1, b + 1, b]
    return P, N, I

def write(path, shape, color, metallic, rough):
    P, N, I = {"cyl": cylinder, "box": box, "sph": sphere}[shape]()
    pos = np.array(P, np.float32); nor = np.array(N, np.float32); idx = np.array(I, np.uint16)
    blob = pos.tobytes() + nor.tobytes() + idx.tobytes()
    while len(blob) % 4: blob += b"\0"
    g = {
        "asset": {"version": "2.0", "generator": "FreeGymTrainer build_props.py"},
        "scene": 0, "scenes": [{"nodes": [0]}], "nodes": [{"mesh": 0, "name": os.path.basename(path)}],
        "meshes": [{"primitives": [{"attributes": {"POSITION": 0, "NORMAL": 1}, "indices": 2, "material": 0}]}],
        "materials": [{"pbrMetallicRoughness": {"baseColorFactor": list(color), "metallicFactor": metallic, "roughnessFactor": rough}}],
        "buffers": [{"byteLength": len(blob)}],
        "bufferViews": [
            {"buffer": 0, "byteOffset": 0, "byteLength": pos.nbytes, "target": 34962},
            {"buffer": 0, "byteOffset": pos.nbytes, "byteLength": nor.nbytes, "target": 34962},
            {"buffer": 0, "byteOffset": pos.nbytes + nor.nbytes, "byteLength": idx.nbytes, "target": 34963}],
        "accessors": [
            {"bufferView": 0, "componentType": 5126, "count": len(P), "type": "VEC3", "min": pos.min(0).tolist(), "max": pos.max(0).tolist()},
            {"bufferView": 1, "componentType": 5126, "count": len(P), "type": "VEC3"},
            {"bufferView": 2, "componentType": 5123, "count": len(I), "type": "SCALAR"}],
    }
    js = json.dumps(g, separators=(",", ":")).encode()
    while len(js) % 4: js += b" "
    with open(path, "wb") as f:
        f.write(struct.pack("<III", 0x46546C67, 2, 12 + 8 + len(js) + 8 + len(blob)))
        f.write(struct.pack("<II", len(js), 0x4E4F534A)); f.write(js)
        f.write(struct.pack("<II", len(blob), 0x004E4942)); f.write(blob)

def write_floor(path):
    """A flat unit-radius disc (y = 0) whose alpha fades to nothing at the rim, so it needs no visible edge."""
    import io
    from PIL import Image
    n = 256
    yy, xx = np.mgrid[0:n, 0:n]
    r = np.hypot((xx - n / 2 + 0.5) / (n / 2), (yy - n / 2 + 0.5) / (n / 2))
    a = np.clip(1 - r, 0, 1)
    a = (a * a * (3 - 2 * a))                      # smoothstep fade
    rgba = np.zeros((n, n, 4), np.uint8); rgba[..., :3] = 255; rgba[..., 3] = (a * 255).astype(np.uint8)
    b = io.BytesIO(); Image.fromarray(rgba, "RGBA").save(b, "PNG"); png = b.getvalue()
    seg = 48
    P = [(0, 0, 0)] + [(math.cos(2 * math.pi * i / seg), 0, math.sin(2 * math.pi * i / seg)) for i in range(seg + 1)]
    UV = [(0.5, 0.5)] + [(0.5 + 0.5 * x, 0.5 + 0.5 * z) for x, _, z in P[1:]]
    I = []
    for i in range(seg): I += [0, i + 2, i + 1]
    pos = np.array(P, np.float32); nor = np.tile(np.array([0, 1, 0], np.float32), (len(P), 1)); uv = np.array(UV, np.float32); idx = np.array(I, np.uint16)
    blob = pos.tobytes() + nor.tobytes() + uv.tobytes() + idx.tobytes()
    while len(blob) % 4: blob += b"\0"
    png_off = len(blob); blob += png
    while len(blob) % 4: blob += b"\0"
    o1, o2, o3 = pos.nbytes, pos.nbytes + nor.nbytes, pos.nbytes + nor.nbytes + uv.nbytes
    g = {
        "asset": {"version": "2.0", "generator": "FreeGymTrainer build_props.py"},
        "scene": 0, "scenes": [{"nodes": [0]}], "nodes": [{"mesh": 0, "name": "floor"}],
        "meshes": [{"primitives": [{"attributes": {"POSITION": 0, "NORMAL": 1, "TEXCOORD_0": 2}, "indices": 3, "material": 0}]}],
        "materials": [{"alphaMode": "BLEND", "pbrMetallicRoughness": {"baseColorFactor": [1, 1, 1, 1], "baseColorTexture": {"index": 0}, "metallicFactor": 0.0, "roughnessFactor": 0.9}}],
        "textures": [{"source": 0}], "images": [{"bufferView": 4, "mimeType": "image/png"}],
        "buffers": [{"byteLength": len(blob)}],
        "bufferViews": [
            {"buffer": 0, "byteOffset": 0, "byteLength": pos.nbytes}, {"buffer": 0, "byteOffset": o1, "byteLength": nor.nbytes},
            {"buffer": 0, "byteOffset": o2, "byteLength": uv.nbytes}, {"buffer": 0, "byteOffset": o3, "byteLength": idx.nbytes},
            {"buffer": 0, "byteOffset": png_off, "byteLength": len(png)}],
        "accessors": [
            {"bufferView": 0, "componentType": 5126, "count": len(P), "type": "VEC3", "min": pos.min(0).tolist(), "max": pos.max(0).tolist()},
            {"bufferView": 1, "componentType": 5126, "count": len(P), "type": "VEC3"},
            {"bufferView": 2, "componentType": 5126, "count": len(P), "type": "VEC2"},
            {"bufferView": 3, "componentType": 5123, "count": len(I), "type": "SCALAR"}],
    }
    js = json.dumps(g, separators=(",", ":")).encode()
    while len(js) % 4: js += b" "
    with open(path, "wb") as f:
        f.write(struct.pack("<III", 0x46546C67, 2, 12 + 8 + len(js) + 8 + len(blob)))
        f.write(struct.pack("<II", len(js), 0x4E4F534A)); f.write(js)
        f.write(struct.pack("<II", len(blob), 0x004E4942)); f.write(blob)

out = sys.argv[1]
os.makedirs(out, exist_ok=True)
write_floor(os.path.join(out, "floor.glb"))
for name, (shape, color, m, r) in KINDS.items():
    write(os.path.join(out, name + ".glb"), shape, color, m, r)
print("wrote", len(KINDS), "props to", out)
