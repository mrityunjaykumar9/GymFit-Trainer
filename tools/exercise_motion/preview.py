import sys, json, math
from PIL import Image, ImageDraw
import engine

def sheet(names, motions, out, frames=4, size=240):
    frames = max(frames, max(len(motions[n]['keys']) for n in names) if False else frames)
    cols = frames
    rows = len(names)
    img = Image.new("RGB", (cols * size, rows * size), (255, 255, 255))
    for r, n in enumerate(names):
        m = motions[n]
        ts = [k["t"] for k in m["keys"]]
        if len(ts) != frames:
            ts = [c / frames for c in range(frames)]
        for c in range(frames):
            t = ts[c] if c < len(ts) else 0
            img.paste(engine.render(m, t, size, n), (c * size, r * size))
    img.save(out)
    print(out)

if __name__ == "__main__":
    import build
    motions = build.load_all()
    names = sys.argv[2:] or list(motions)
    sheet(names, motions, sys.argv[1])
