#!/usr/bin/env python3
"""0.1.8.2 weapon textures (weapons only; tool textures are not changed).

Scope (after the 0.1.8.2 audit of all weapon models):
* weapon/bronze_sword, weapon/nanosaber, weapon/quantum_sword: UV maps (32x32) of the 3D sword
  models. The 0.1.8.1 maps were flat single colours. Each UV region of the model is painted for its
  part: blade [0,0,4,16], guard [4,0,10,3], grip [4,3,6,10], pommel [6,3,8,5], strip A [8,3,10,16]
  (core / light), strip B [10,3,12,16] (fuller / emitter / point). Model UV units are 2 px. The
  model files are not changed.
* item_tool_plasma_launcher: 10-frame hand icon (old IC2-style frames); the .mcmeta is kept.
* item_tool_nano_bow (+ pull0..2): 32x32 icons (the 24x24 icons were low-detail).
* Not changed: the guns (64x64 3D textures and 40x40 icons are already modern), the unused 16x16
  icons (item_nanosaber_deactive, item_tool_quantum_sword, item_tool_bronze_sword,
  item_tool_laserminer, item_tool_plasma_launcher_deactivate; no model uses them).

Usage: weapons_182.py <repo root> [preview dir]
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixart import Canvas, hexc, mix, shade, ramp, sheet  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
OUT = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/item')
N = 32


def mat(c):
    return ramp(hexc(c) if isinstance(c, str) else c, 5, -0.55, 0.42)


IRON, STEEL, RUBBER, WOOD = mat('aab0b8'), mat('5a6270'), mat('34343c'), mat('9a6a3a')
WHITE, RED = mat('e8ecf0'), mat('c8382e')
LED_G, LED_C = hexc('6af06a'), hexc('6ae8ff')


# ------------------------------------------------------------------ icon primitives
def shade_mask(c, mask, m):
    if not mask:
        return
    xs = [p[0] for p in mask]
    ys = [p[1] for p in mask]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    w, h = max(1, x1 - x0), max(1, y1 - y0)
    for (x, y) in mask:
        g = ((x - x0) / w + (y - y0) / h) / 2
        idx = 3 - int(round(2 * g))
        if (x - 1, y) not in mask or (x, y - 1) not in mask:
            idx = 4
        elif (x + 1, y) not in mask or (x, y + 1) not in mask:
            idx = 0 if idx <= 2 else 1
        c.put(x, y, m[max(0, min(4, idx))])


def region(test):
    return {(x, y) for y in range(N) for x in range(N) if test(x + 0.5, y + 0.5)}


def local(x, y, cx, cy, ang):
    a = math.radians(ang)
    dx, dy = x - cx, y - cy
    return dx * math.cos(a) + dy * math.sin(a), -dx * math.sin(a) + dy * math.cos(a)


def box(c, cx, cy, ang, hw, hh, m, r=0.0):
    def t(x, y):
        u, v = local(x, y, cx, cy, ang)
        if abs(u) > hw or abs(v) > hh:
            return False
        if r > 0:
            ex, ey = abs(u) - (hw - r), abs(v) - (hh - r)
            if ex > 0 and ey > 0 and ex * ex + ey * ey > r * r:
                return False
        return True
    mask = region(t)
    shade_mask(c, mask, m)
    return mask


def rod(c, x0, y0, x1, y1, w, m):
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy) or 1
    ux, uy = dx / L, dy / L
    nx, ny = -uy, ux
    if nx + ny > 0:
        nx, ny = -nx, -ny
    half = w / 2
    for y in range(N):
        for x in range(N):
            px, py = x + 0.5 - x0, y + 0.5 - y0
            t = px * ux + py * uy
            if t < -0.3 or t > L + 0.3:
                continue
            off = px * nx + py * ny
            if abs(off) > half:
                continue
            k = (off / half + 1) / 2
            idx = int(round(1 + 3 * k)) if w >= 2 else 2
            c.put(x, y, m[max(0, min(4, idx))])


def outline(c, k=-0.68):
    src = list(c.px)
    for y in range(c.h):
        for x in range(c.w):
            if src[y * c.w + x][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < c.w and 0 <= ny < c.h and src[ny * c.w + nx][3] > 0:
                    o = shade(src[ny * c.w + nx], k)
                    c.put(x, y, (o[0], o[1], o[2], 255))
                    break


def led(c, x, y, col):
    c.put(x, y, shade(col, 0.45))
    c.put(x + 1, y, col)


def box_dims(c, x0, y0, w, h, m):
    return box(c, x0 + w / 2, y0 + h / 2, 0, w / 2, h / 2, m)


# ------------------------------------------------------------------ nano bow
def nano_bow(pull):
    c = Canvas(N, N)
    limb = mat('2e3440')
    k = 8.5 - pull * 0.6
    ax, ay, bx, by = 5.0, 4.0, 27.0, 26.0
    cx, cy = 16 + k, 15 - k
    pts = []
    for i in range(33):
        t = i / 32
        pts.append(((1 - t) ** 2 * ax + 2 * (1 - t) * t * cx + t * t * bx, (1 - t) ** 2 * ay + 2 * (1 - t) * t * cy + t * t * by))
    for (x, y) in pts:
        c.put(int(x), int(y), limb[2]); c.put(int(x) - 1, int(y), limb[3]); c.put(int(x), int(y) + 1, limb[1])
    for i in (7, 25):
        x, y = pts[i]
        c.put(int(x), int(y), LED_C)
    gx, gy = pts[16]
    box(c, gx, gy, -45, 1.8, 2.6, RUBBER)
    px, py = 16.0 - pull * 2.0, 15.0 + pull * 2.0
    glow = (140, 240, 255, 255)
    c.line(ax, ay, px, py, glow)
    c.line(px, py, bx, by, glow)
    if pull > 0:
        tx, ty = px + 12 + pull, py - 12 - pull
        c.line(px, py, tx, ty, WOOD[2])
        c.put(int(tx), int(ty), IRON[4]); c.put(int(tx) - 1, int(ty), IRON[2]); c.put(int(tx), int(ty) + 1, IRON[2])
        c.put(int(px) - 1, int(py), WHITE[3]); c.put(int(px), int(py) + 1, WHITE[3])
    outline(c)
    return c


# ------------------------------------------------------------------ plasma launcher (10 frames)
def plasma_launcher(frame, frames=10):
    c = Canvas(N, N)
    gun = mat('3a3f4a')
    pulse = 0.5 + 0.5 * math.sin(2 * math.pi * frame / frames)
    rod(c, 5, 28, 10, 21, 4, RUBBER)                       # grip
    box(c, 15, 16, -45, 10.5, 4.2, gun, 1.6)               # body
    box(c, 15, 16, -45, 10.5, 1.0, RED, 0.3)               # accent stripe
    box(c, 25.5, 5.5, -45, 3.4, 2.6, STEEL, 0.8)           # muzzle
    box(c, 9.5, 21.5, -45, 3.0, 3.0, gun, 1.0)             # rear block
    # EN: glass chamber with a pulsing plasma core. ZH: 带脉动等离子核心的玻璃腔。JA: 脈動するプラズマ核を収めたガラス室。
    cx, cy = 16.5, 14.5
    glass = (150, 200, 230, 255)
    for y in range(N):
        for x in range(N):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= 4.2:
                c.put(x, y, mix(hexc('1a1030'), glass, 0.15))
            if d <= 4.2 and d > 3.4:
                c.put(x, y, mix(gun[1], glass, 0.35))
    core = 1.6 + 1.3 * pulse
    for y in range(N):
        for x in range(N):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= core:
                t = d / core
                c.put(x, y, mix(hexc('fff0ff'), hexc('d040ff'), t))
            elif d <= core + 1.0 and d <= 3.4:
                c.put(x, y, mix(hexc('d040ff'), hexc('3a1060'), 0.4 + 0.6 * (1 - pulse)))
    c.put(int(cx - 2), int(cy - 2), (255, 255, 255, 255))
    # muzzle glow follows the pulse
    mg = mix(hexc('6a2a8a'), hexc('f0a0ff'), pulse)
    c.put(27, 3, mg); c.put(28, 4, mg); c.put(27, 4, shade(mg, 0.3))
    led(c, 9, 20, LED_G if frame % 5 else hexc('b0ffb0'))
    outline(c)
    return c


# ------------------------------------------------------------------ sword UV maps
def uv_canvas():
    return Canvas(32, 32)


def blade_strip(c, profile, tip_light=0.10, noise=0.035, seed=1, lines=None):
    """Blade region x 0..7, y 0..31: profile = 8 colours across the width."""
    rng = random.Random(seed)
    for y in range(32):
        f = tip_light * (1 - y / 31)
        for x in range(8):
            col = shade(profile[x], f + (rng.random() - 0.5) * noise)
            c.put(x, y, col)
        if lines and y % lines[0] == lines[1]:
            for x in range(2, 6):
                c.put(x, y, mix(c.get(x, y), lines[2], 0.45))


def strip(c, x0, cols, glow=None, seed=2, rings=None, ring_col=None):
    rng = random.Random(seed)
    for y in range(6, 32):
        k = 0.0
        if glow:
            k = glow * math.sin(y * 0.55)
        for i, col in enumerate(cols):
            c.put(x0 + i, y, shade(col, k + (rng.random() - 0.5) * 0.03))
        if rings and (y - 6) % rings == 0:
            for i in range(len(cols)):
                c.put(x0 + i, y, ring_col)


def guard(c, m, rivets=None, line=None):
    for y in range(0, 6):
        for x in range(8, 20):
            idx = 4 if y == 0 else 0 if y == 5 else 3 if y == 1 else 1 if y == 4 else 2
            if x in (8, 19):
                idx = max(0, idx - 1)
            c.put(x, y, m[idx])
    if line:
        for x in range(9, 19):
            if x % 3 != 1:
                c.put(x, 2, line)
                c.put(x, 3, shade(line, -0.35))
    for (x, y, col) in rivets or []:
        c.put(x, y, col)


def grip(c, m, wrap=None, ring=None):
    for y in range(6, 20):
        for x in range(8, 12):
            idx = (3 if x == 8 else 2 if x == 9 else 1 if x == 10 else 0)
            col = m[idx]
            if wrap and (x + y) % 4 in (0, 1):
                col = shade(col, -0.18)
            if ring and (y - 6) % 4 == 0:
                col = ring
            c.put(x, y, col)


def pommel(c, m, gem=None):
    for y in range(6, 10):
        for x in range(12, 16):
            idx = 4 - ((x - 12) + (y - 6)) // 2
            c.put(x, y, (gem or m)[max(0, min(4, idx))])
    c.put(12, 6, (255, 255, 255, 255))


def sword_bronze():
    c = uv_canvas()
    b = mat('cc8446')
    edge = shade(b[4], 0.25)
    blade_strip(c, [edge, b[3], b[2], b[1], b[1], b[2], b[3], edge], seed=11)
    strip(c, 20, [b[0], shade(b[0], -0.25), b[1], b[3]], seed=12)        # fuller (groove)
    guard(c, mat('a8653a'), rivets=[(10, 2, b[4]), (10, 3, b[2]), (17, 2, b[4]), (17, 3, b[2]), (13, 2, mat('3a8a9a')[3]), (14, 2, mat('3a8a9a')[2])])
    grip(c, mat('6a4226'), wrap=True)
    pommel(c, b)
    strip(c, 16, [b[2], b[3], b[3], b[2]], seed=13)
    return c


def sword_quantum():
    c = uv_canvas()
    w = mat('dfe4ee')
    cyan = hexc('5ad8ff')
    edge = hexc('b8f4ff')
    blade_strip(c, [edge, w[4], w[3], w[2], w[2], w[3], w[4], edge], seed=21, lines=(6, 3, cyan))
    strip(c, 16, [hexc('2ab0ff'), hexc('d8ffff'), hexc('f4ffff'), hexc('2ab0ff')], glow=0.12, seed=22)   # energy core
    strip(c, 20, [edge, w[4], w[3], edge], seed=23)                                                       # point
    guard(c, mat('2a3040'), line=cyan, rivets=[(13, 1, edge), (14, 1, edge)])
    grip(c, mat('23262e'), ring=hexc('3aa8d8'))
    pommel(c, w, gem=mat('3ac8f0'))
    return c


def sword_nanosaber():
    c = uv_canvas()
    g = [hexc('1f8a2c'), hexc('3ad84a'), hexc('9aff9a'), hexc('eaffea'), hexc('eaffea'), hexc('9aff9a'), hexc('3ad84a'), hexc('1f8a2c')]
    blade_strip(c, g, tip_light=0.05, noise=0.06, seed=31)                                                 # energy blade
    strip(c, 16, [hexc('5aff6a'), hexc('f0fff0'), hexc('f0fff0'), hexc('5aff6a')], glow=0.10, seed=32)   # core / light ring
    st = mat('6a727e')
    strip(c, 20, [st[3], st[2], st[1], st[0]], seed=33, rings=3, ring_col=hexc('1e2228'))                 # emitter
    guard(c, mat('3a4048'), rivets=[(10, 2, LED_G), (17, 2, LED_G)], line=hexc('5a626c'))
    grip(c, mat('2a2c32'), ring=hexc('16181c'))
    pommel(c, st)
    return c


def strip_frames(frames):
    out = Canvas(N, N * len(frames))
    for i, f in enumerate(frames):
        out.paste(f, 0, i * N)
    return out


WEAPONS = {
    'item_tool_nano_bow': lambda: nano_bow(0),
    'item_tool_nano_bow_pull0': lambda: nano_bow(1),
    'item_tool_nano_bow_pull1': lambda: nano_bow(2),
    'item_tool_nano_bow_pull2': lambda: nano_bow(3),
    'item_tool_plasma_launcher': lambda: strip_frames([plasma_launcher(i) for i in range(10)]),
    'weapon/bronze_sword': sword_bronze,
    'weapon/quantum_sword': sword_quantum,
    'weapon/nanosaber': sword_nanosaber,
}


def main():
    made = []
    for name, fn in WEAPONS.items():
        img = fn()
        path = os.path.join(OUT, name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
        made.append((name, img))
    print('weapon textures written:', len(made))
    if PREVIEW:
        os.makedirs(PREVIEW, exist_ok=True)
        firsts = []
        for name, img in made:
            f = Canvas(img.w, min(img.h, N))
            f.px = img.px[:img.w * f.h]
            firsts.append(f)
        sheet(firsts, 8, 5).save(os.path.join(PREVIEW, 'weapons_new.png'))
        pl = dict(made)['item_tool_plasma_launcher']
        frames = []
        for k in range(10):
            f = Canvas(N, N)
            f.px = pl.px[k * N * N:(k + 1) * N * N]
            frames.append(f)
        sheet(frames, 10, 3).save(os.path.join(PREVIEW, 'weapons_plasma_frames.png'))


if __name__ == '__main__':
    main()
