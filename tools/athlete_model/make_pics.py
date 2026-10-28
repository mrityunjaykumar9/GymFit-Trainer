"""Cuts phone screenshots of the 3D athlete into the small WebP pictures the app shows (exercise_pics).

Usage: python make_pics.py <raw_dir> app/src/main/assets/exercise_pics [x0 y0 x1 y1]

<raw_dir> holds screenshots named <slug>_a.png (start of the rep) and <slug>_b.png (peak), taken from a debug
build with `adb shell am start ... --es debug_exercise "<name>" --ef debug_yaw -35 --ef debug_t 0|0.5
--ez debug_clean true` (see CLAUDE.md). The default crop box is the 3D panel on the exercise screen of a 1080x2400 phone.
"""
import os, sys
import numpy as np
from PIL import Image

raw, out = sys.argv[1], sys.argv[2]
box = tuple(int(v) for v in sys.argv[3:7]) if len(sys.argv) >= 7 else (74, 340, 1006, 1272)
os.makedirs(out, exist_ok=True)
total = 0
for f in sorted(os.listdir(raw)):
    if not f.endswith(".png"): continue
    im = Image.open(os.path.join(raw, f)).convert("RGB").crop(box).resize((360, 360), Image.LANCZOS)
    dst = os.path.join(out, f.replace(".png", ".webp"))
    im.save(dst, "WEBP", quality=76, method=6)
    total += os.path.getsize(dst)


def subject_box(im):
    """Bounding box of the athlete (skin-coloured pixels), used to zoom the thumbnails in on the movement."""
    a = np.asarray(im.resize((180, 180))).astype(int)
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    skin = (r > 140) & (r < 250) & (g > 80) & (g < 200) & (b > 50) & (b < 170) & (r > g) & (g > b)
    ys, xs = np.where(skin)
    if len(xs) < 20: return (0, 0, 180, 180)
    return xs.min(), ys.min(), xs.max(), ys.max()

# thumbnails: square crop around the athlete from the peak picture
for f in sorted(os.listdir(raw)):
    if not f.endswith("_b.png"): continue
    im = Image.open(os.path.join(raw, f)).convert("RGB").crop(box)
    x0, y0, x1, y1 = subject_box(im)
    k = im.width / 180.0
    cx, cy = (x0 + x1) / 2 * k, (y0 + y1) / 2 * k
    side = max((x1 - x0), (y1 - y0)) * k * 1.45
    side = min(max(side, im.width * 0.55), im.width)
    l = min(max(cx - side / 2, 0), im.width - side); t = min(max(cy - side / 2, 0), im.height - side)
    th = im.crop((int(l), int(t), int(l + side), int(t + side))).resize((200, 200), Image.LANCZOS)
    dst = os.path.join(out, f.replace("_b.png", "_t.webp"))
    th.save(dst, "WEBP", quality=74, method=6)
    total += os.path.getsize(dst)
print("wrote", len(os.listdir(out)), "pictures,", total // 1024, "KB")
