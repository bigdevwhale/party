"""Draws the launcher icon (a heater shield, gules, a gold chevron between two gold stars and a
crescent, on lapis) and docs/logo.png.

python tools/icon.py
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
LAPIS = (44, 76, 140)
GULES = (176, 48, 42)
GOLD = (214, 169, 58)
GOLD_LIGHT = (240, 210, 122)
INK = (43, 32, 24)
S = 4  # supersampling


def shield(u, cx, top, w, h):
    """Points of a heater shield, in pixels."""
    pts = [(cx - w / 2, top), (cx + w / 2, top), (cx + w / 2, top + h * 0.42)]
    # The curved sides down to the point, sampled.
    for i in range(1, 21):
        t = i / 20
        x = (1 - t) ** 3 * (cx + w / 2) + 3 * (1 - t) ** 2 * t * (cx + w / 2) + 3 * (1 - t) * t ** 2 * (cx + w * 0.22) + t ** 3 * cx
        y = (1 - t) ** 3 * (top + h * 0.42) + 3 * (1 - t) ** 2 * t * (top + h * 0.78) + 3 * (1 - t) * t ** 2 * (top + h * 0.9) + t ** 3 * (top + h)
        pts.append((x, y))
    left = [(2 * cx - x, y) for x, y in reversed(pts[3:])]
    return pts + left[1:] + [(cx - w / 2, top + h * 0.42)]


def star(cx, cy, r, inner):
    out = []
    for i in range(10):
        rr = r if i % 2 == 0 else inner
        a = -math.pi / 2 + i * math.pi / 5
        out.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    return out


def draw(size: int, background: bool) -> Image.Image:
    W = size * S
    u = W / 108
    im = Image.new("RGBA", (W, W), LAPIS + (255,) if background else (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    cx, top, w, h = 54 * u, 27 * u, 46 * u, 54 * u
    pts = shield(u, cx, top, w, h)
    d.polygon(pts, fill=GULES)
    # The chevron, clipped to the shield by drawing on a mask.
    mask = Image.new("L", (W, W), 0)
    ImageDraw.Draw(mask).polygon(pts, fill=255)
    layer = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer)
    t = 8 * u
    ld.line([(cx - w * 0.7, top + h * 0.95), (cx, top + h * 0.45), (cx + w * 0.7, top + h * 0.95)], fill=INK, width=int(t + 2.4 * u), joint="curve")
    ld.line([(cx - w * 0.7, top + h * 0.95), (cx, top + h * 0.45), (cx + w * 0.7, top + h * 0.95)], fill=GOLD, width=int(t), joint="curve")
    for sx in (-1, 1):
        ld.polygon(star(cx + sx * w * 0.24, top + h * 0.2, 6.2 * u, 2.7 * u), fill=GOLD, outline=INK, width=int(0.8 * u))
    # A crescent under the chevron.
    r = 5 * u
    ccx, ccy = cx, top + h * 0.79
    ld.ellipse([ccx - r, ccy - r, ccx + r, ccy + r], fill=GOLD_LIGHT, outline=INK, width=int(0.8 * u))
    ld.ellipse([ccx - r * 0.8, ccy - r * 1.25, ccx + r * 0.8, ccy + r * 0.35], fill=GULES)
    im.paste(layer, (0, 0), Image.composite(layer, Image.new("RGBA", (W, W)), mask).split()[3])
    d.line(pts + [pts[0]], fill=INK, width=int(2 * u), joint="curve")
    return im.resize((size, size), Image.LANCZOS)


DENS = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
for name, k in DENS.items():
    folder = RES / f"mipmap-{name}"
    folder.mkdir(parents=True, exist_ok=True)
    fg = int(108 * k)
    draw(fg, False).save(folder / "ic_launcher_foreground.png")
    legacy = int(48 * k)
    full = draw(fg, True).resize((legacy, legacy), Image.LANCZOS)
    full.save(folder / "ic_launcher.png")
    m = Image.new("L", (legacy, legacy), 0)
    ImageDraw.Draw(m).ellipse([0, 0, legacy - 1, legacy - 1], fill=255)
    rnd = Image.new("RGBA", (legacy, legacy), (0, 0, 0, 0))
    rnd.paste(full, (0, 0), m)
    rnd.save(folder / "ic_launcher_round.png")
(ROOT / "docs").mkdir(exist_ok=True)
logo = draw(512, True)
m = Image.new("L", (512, 512), 0)
ImageDraw.Draw(m).rounded_rectangle([0, 0, 511, 511], 110, fill=255)
out = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
out.paste(logo, (0, 0), m)
out.save(ROOT / "docs/logo.png")
print("icons written")
