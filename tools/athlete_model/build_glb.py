"""Packs the Quaternius "Universal Base Characters" Superhero_Male glTF into one compact athlete.glb.

Usage: python build_glb.py "<...>/Universal Base Characters[Standard]/Base Characters" out/athlete.glb

The source pack is licensed for use inside apps but not for redistribution as files, so neither the
pack nor the generated athlete.glb should go into a public repository (see .gitignore).
Textures are downscaled to 1024px JPEG; roughness is packed into the glTF metallicRoughness layout.
"""
import io, json, os, struct, sys
import numpy as np
from PIL import Image

src, out = sys.argv[1], sys.argv[2]
godot = os.path.join(src, "Godot - UE")
tex = os.path.join(src, "Textures")
g = json.load(open(os.path.join(godot, "Superhero_Male_FullBody.gltf")))
buf = bytearray(open(os.path.join(godot, "Superhero_Male_FullBody.bin"), "rb").read())

def jpg(im, size, q):
    b = io.BytesIO()
    im.convert("RGB").resize((size, size), Image.LANCZOS).save(b, "JPEG", quality=q, optimize=True)
    return b.getvalue()

def rough(path):
    r = np.asarray(Image.open(path).convert("L"))
    rgb = np.zeros(r.shape + (3,), np.uint8)
    rgb[..., 0] = 255
    rgb[..., 1] = r          # G = roughness
    return Image.fromarray(rgb)

files = {
    "T_Hair_1_Normal_png.png": (os.path.join(godot, "T_Hair_1_Normal.png"), 256, 90),
    "T_Hair_1_BaseColor.png": (os.path.join(godot, "T_Hair_1_BaseColor.png"), 256, 90),
    "T_Eye_Normal_png.png": (os.path.join(godot, "T_Eye_Normal.png"), 256, 90),
    "T_Eye_Brown.png": (os.path.join(godot, "T_Eye_Brown.png"), 256, 92),
    "T_Superhero_Male_Normal.png": (os.path.join(godot, "T_Superhero_Male_Normal.png"), 1024, 92),
    "T_Superhero_Male_Dark.png": (os.path.join(godot, "T_Superhero_Male_Dark.png"), 1024, 90),
}
rough_path = os.path.join(godot, "T_Superhero_Male_Roughness.png")

# ---- short buzz-cut hair: a static mesh in model space, re-parented to the Head bone ----------------
def rest_world(g):
    nodes = g["nodes"]
    def qmat(q):
        x, y, z, w = q
        return np.array([[1-2*(y*y+z*z), 2*(x*y-z*w), 2*(x*z+y*w)], [2*(x*y+z*w), 1-2*(x*x+z*z), 2*(y*z-x*w)], [2*(x*z-y*w), 2*(y*z+x*w), 1-2*(x*x+y*y)]])
    def local(nd):
        m = np.eye(4); m[:3, :3] = qmat(nd.get("rotation", [0, 0, 0, 1])) @ np.diag(nd.get("scale", [1, 1, 1])); m[:3, 3] = nd.get("translation", [0, 0, 0]); return m
    out = {}
    def walk(i, parent):
        out[i] = parent @ local(nodes[i])
        for c in nodes[i].get("children", []): walk(c, out[i])
    for r in g["scenes"][0]["nodes"]: walk(r, np.eye(4))
    return out

def attach_static_mesh(g, buf, gltf_path, bone_name, hair_color=None):
    h = json.load(open(gltf_path))
    hb = open(gltf_path.replace(".gltf", ".bin"), "rb").read()
    prim = h["meshes"][0]["primitives"][0]
    def copy_accessor(ai):
        a = dict(h["accessors"][ai]); bv = h["bufferViews"][a["bufferView"]]
        data = hb[bv.get("byteOffset", 0): bv.get("byteOffset", 0) + bv["byteLength"]]
        while len(buf) % 4: buf.append(0)
        g["bufferViews"].append({"buffer": 0, "byteOffset": len(buf), "byteLength": len(data), **({"target": bv["target"]} if "target" in bv else {}),
                                 **({"byteStride": bv["byteStride"]} if "byteStride" in bv else {})})
        buf.extend(data)
        a["bufferView"] = len(g["bufferViews"]) - 1
        g["accessors"].append(a)
        return len(g["accessors"]) - 1
    attrs = {k: copy_accessor(v) for k, v in prim["attributes"].items() if k in ("POSITION", "NORMAL", "TEXCOORD_0")}
    mat = next(i for i, m in enumerate(g["materials"]) if m["name"] == "MI_Hair_1")
    g["meshes"].append({"name": h["meshes"][0].get("name", "hair"), "primitives": [{"attributes": attrs, "indices": copy_accessor(prim["indices"]), "material": mat}]})
    bone = next(i for i, n in enumerate(g["nodes"]) if n.get("name") == bone_name)
    inv = np.linalg.inv(rest_world(g)[bone])
    g["nodes"].append({"name": h["nodes"][0]["name"], "mesh": len(g["meshes"]) - 1, "matrix": [float(v) for v in inv.T.reshape(-1)]})
    g["nodes"][bone].setdefault("children", []).append(len(g["nodes"]) - 1)


# ---- procedural sneakers (original geometry), fitted to the model's feet and parented to the foot bones ---------
def _superellipse(t, n):
    c, s_ = np.cos(t), np.sin(t)
    return np.sign(c) * np.abs(c) ** (2.0 / n), np.sign(s_) * np.abs(s_) ** (2.0 / n)

def _loft(zs, width, ybot, ytop, cx, n, k=28):
    """Closed lofted surface: one superellipse ring per z. width/ytop are callables of z. Returns (V, I)."""
    V = []
    for z in zs:
        w = width(z); top = ytop(z) if callable(ytop) else ytop; bot = ybot(z) if callable(ybot) else ybot
        for i in range(k):
            ex, ey = _superellipse(2 * np.pi * i / k, n)
            V.append((cx(z) + w * ex, bot + (top - bot) * (0.5 + 0.5 * ey), z))
    V = np.array(V, np.float32)
    I = []
    for r in range(len(zs) - 1):
        for i in range(k):
            a = r * k + i; b = r * k + (i + 1) % k; c = (r + 1) * k + i; d = (r + 1) * k + (i + 1) % k
            I += [a, c, b, b, c, d]
    # cap both ends (rings collapse to a point there, so a fan is enough)
    for ring, flip in ((0, True), (len(zs) - 1, False)):
        centre = len(V); V = np.vstack([V, V[ring * k:(ring + 1) * k].mean(0)[None]])
        for i in range(k):
            a = ring * k + i; b = ring * k + (i + 1) % k
            I += ([centre, b, a] if flip else [centre, a, b])
    return V, np.array(I, np.uint32)

def _normals(V, I):
    N = np.zeros_like(V)
    for t in I.reshape(-1, 3):
        fn = np.cross(V[t[1]] - V[t[0]], V[t[2]] - V[t[0]])
        N[t] += fn
    L = np.linalg.norm(N, axis=1, keepdims=True); L[L == 0] = 1
    return (N / L).astype(np.float32)

def build_shoe_parts():
    """Left shoe in model space (rest pose). Returns {material: (V, I)}; the right shoe is the mirror image."""
    z0, z1 = -0.160, 0.170
    zs = np.linspace(z0, z1, 44)
    def planform(z):   # superellipse outline of the sole as seen from above
        zm = (z0 + z1) / 2; half = (z1 - z0) / 2
        u = min(abs((z - zm) / half), 1.0)
        base = 0.052 + 0.016 * np.clip((z + 0.10) / 0.14, 0, 1)      # wider at the ball of the foot than the heel
        return base * (1 - u ** 3.2) ** (1 / 3.2)
    def top(z):        # upper: tall collar at the heel, dropping over the instep to the toe box
        pts = [(-0.16, 0.118), (-0.05, 0.118), (-0.02, 0.098), (0.02, 0.076), (0.06, 0.060), (0.10, 0.046), (0.14, 0.036), (0.17, 0.030)]
        return float(np.interp(z, [p[0] for p in pts], [p[1] for p in pts]))
    cx = lambda z: 0.128
    parts = {}
    parts["upper"] = _loft(zs, lambda z: planform(z) * 0.97 + 0.0, 0.004, top, cx, 3.0)
    parts["mid"] = _loft(zs, lambda z: planform(z) + 0.0035, 0.004, 0.022, cx, 6.0)
    parts["sole"] = _loft(zs, lambda z: planform(z) + 0.0045, -0.012, 0.006, cx, 8.0)
    return parts

SHOE_MATERIALS = {
    "upper": {"baseColorFactor": [0.93, 0.94, 0.96, 1.0], "metallicFactor": 0.0, "roughnessFactor": 0.55},
    "mid": {"baseColorFactor": [0.95, 0.40, 0.09, 1.0], "metallicFactor": 0.0, "roughnessFactor": 0.5},
    "sole": {"baseColorFactor": [0.07, 0.07, 0.08, 1.0], "metallicFactor": 0.0, "roughnessFactor": 0.85},
}

def attach_shoes(g, buf):
    parts = build_shoe_parts()
    mat_index = {}
    for key, pbr in SHOE_MATERIALS.items():
        g["materials"].append({"name": "Shoe_" + key, "pbrMetallicRoughness": pbr, "doubleSided": True})
        mat_index[key] = len(g["materials"]) - 1
    def add_accessor(arr, ctype, atype, target, with_minmax=False):
        data = arr.tobytes()
        while len(buf) % 4: buf.append(0)
        g["bufferViews"].append({"buffer": 0, "byteOffset": len(buf), "byteLength": len(data), "target": target})
        buf.extend(data)
        acc = {"bufferView": len(g["bufferViews"]) - 1, "componentType": ctype, "count": int(arr.shape[0]) if arr.ndim > 1 else int(arr.shape[0]), "type": atype}
        if with_minmax: acc["min"] = arr.min(0).tolist(); acc["max"] = arr.max(0).tolist()
        g["accessors"].append(acc)
        return len(g["accessors"]) - 1
    world = rest_world(g)
    for side, mirror in (("l", 1.0), ("r", -1.0)):
        prims = []
        for key, (V, I) in parts.items():
            V2 = V.copy(); I2 = I.copy()
            if mirror < 0:
                V2[:, 0] *= -1; I2 = I2.reshape(-1, 3)[:, ::-1].reshape(-1)
            N = _normals(V2, I2)
            prims.append({"attributes": {"POSITION": add_accessor(V2, 5126, "VEC3", 34962, True), "NORMAL": add_accessor(N, 5126, "VEC3", 34962)},
                          "indices": add_accessor(I2.astype(np.uint32), 5125, "SCALAR", 34963), "material": mat_index[key]})
        g["meshes"].append({"name": "Shoe_" + side, "primitives": prims})
        bone = next(i for i, n in enumerate(g["nodes"]) if n.get("name") == "foot_" + side)
        inv = np.linalg.inv(world[bone])
        g["nodes"].append({"name": "Shoe_" + side, "mesh": len(g["meshes"]) - 1, "matrix": [float(v) for v in inv.T.reshape(-1)]})
        g["nodes"][bone].setdefault("children", []).append(len(g["nodes"]) - 1)

hair_dir = os.path.join(src, "..", "Hairstyles", "Origin at 0", "glTF (Godot)")
attach_static_mesh(g, buf, os.path.join(hair_dir, "Hair_Buzzed.gltf"), "Head")
attach_shoes(g, buf)
for m in g["materials"]:
    if m["name"] == "MI_Hair_1":
        m["pbrMetallicRoughness"]["baseColorFactor"] = [0.075, 0.05, 0.035, 1.0]   # dark brown hair and eyebrows

for img in g["images"]:
    uri = img.pop("uri")
    if uri.startswith("T_Superhero_Male_Roughness"):
        data = jpg(rough(rough_path), 1024, 90)
    else:
        p, size, q = files[uri]
        data = jpg(Image.open(p), size, q)
    while len(buf) % 4: buf.append(0)
    g["bufferViews"].append({"buffer": 0, "byteOffset": len(buf), "byteLength": len(data)})
    buf += data
    img["bufferView"] = len(g["bufferViews"]) - 1
    img["mimeType"] = "image/jpeg"

while len(buf) % 4: buf.append(0)
g["buffers"] = [{"byteLength": len(buf)}]
js = json.dumps(g, separators=(",", ":")).encode()
while len(js) % 4: js += b" "
total = 12 + 8 + len(js) + 8 + len(buf)
os.makedirs(os.path.dirname(out) or ".", exist_ok=True)
with open(out, "wb") as f:
    f.write(struct.pack("<III", 0x46546C67, 2, total))
    f.write(struct.pack("<II", len(js), 0x4E4F534A)); f.write(js)
    f.write(struct.pack("<II", len(buf), 0x004E4942)); f.write(buf)
print("wrote", out, total // 1024, "KB")
