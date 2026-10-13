#!/usr/bin/env python3
"""0.1.8.3 weapon icons: the bronze sword, nanosaber and quantum sword go back to 2D item icons
(the 3D sword models are removed), and the nano bow gets a new compound-bow icon set.

All icons are 32x32 and drawn at 45 degrees (pommel or grip at the lower left, tip at the upper
right), lit from the upper left. Each part is a band around the weapon axis: u runs along the
axis from the pommel, v across it (v > 0 is the lit upper-left side).

Outputs (textures/item/...):
  weapon/bronze_sword.png         heavy-duty tactical blade
  weapon/nanosaber.png            nano HF vibration cutter, generator off
  weapon/nanosaber_active.png     6-frame strip, vibrating green edge (+ .mcmeta)
  weapon/quantum_sword.png        8-frame strip, field blade over a singularity (+ .mcmeta)

The item names and ids do not change; only the sprites follow the new design brief.
  item_tool_nano_bow.png, item_tool_nano_bow_pull0..2.png

Usage: weapons_183.py <repo root> [preview dir]
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixart import Canvas, hexc, mix, shade, sheet  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
OUT = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/item')
N = 32
SQ = math.sqrt(0.5)

# bronze toolset palette (the 0.1.8.3 bronze tool icons)
BRONZE = [hexc('452806'), hexc('6b3f0a'), hexc('96580d'), hexc('e88515'), hexc('ed993b'), hexc('ffb766'), hexc('ffd5a6')]
BRONZE_OUT = hexc('331b0a')
LEATHER = [hexc('281e0b'), hexc('442b19'), hexc('684e1e'), hexc('896727')]
PATINA = [hexc('1d4f4a'), hexc('2f8a7a'), hexc('6fd6c0'), hexc('d8fff4')]
G10 = [hexc('2a2e22'), hexc('3d4331'), hexc('565e45'), hexc('737c5c')]


class Frame:
    """Axis frame: origin (pommel end) and unit direction toward the tip (45 degrees up-right)."""

    def __init__(self, x0, y0, x1, y1):
        self.x0, self.y0 = x0, y0
        dx, dy = x1 - x0, y1 - y0
        self.L = math.hypot(dx, dy)
        self.ux, self.uy = dx / self.L, dy / self.L
        # v points to the upper left (lit side)
        self.vx, self.vy = self.uy, -self.ux
        if self.vx > 0:
            self.vx, self.vy = -self.vx, -self.vy

    def uv(self, x, y):
        px, py = x + 0.5 - self.x0, y + 0.5 - self.y0
        return px * self.ux + py * self.uy, px * self.vx + py * self.vy


def outline(c, col=None, k=-0.72):
    """1 px outline around fully opaque pixels: a darker shade of the neighbour or a fixed colour."""
    src = list(c.px)
    for y in range(c.h):
        for x in range(c.w):
            if src[y * c.w + x][3] == 255:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < c.w and 0 <= ny < c.h and src[ny * c.w + nx][3] == 255:
                    n = src[ny * c.w + nx]
                    o = col if col else shade(n, k)
                    c.put(x, y, (o[0], o[1], o[2], 255))
                    break


def soft_glow(c, col, strength, radius=1, source=None):
    """Translucent halo around opaque pixels (used under energy blades). Drawn before the outline."""
    src = list(c.px)
    for y in range(c.h):
        for x in range(c.w):
            if src[y * c.w + x][3]:
                continue
            best = 0
            for dy in range(-radius, radius + 1):
                for dx in range(-radius, radius + 1):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < c.w and 0 <= ny < c.h and src[ny * c.w + nx][3] == 255 and src[ny * c.w + nx][1] > 150 \
                            and (source is None or source(nx, ny)):
                        d = math.hypot(dx, dy)
                        best = max(best, 1 - (d - 1) / radius if d >= 1 else 1)
            if best > 0:
                c.put(x, y, (col[0], col[1], col[2], int(255 * strength * best)))


def ramp_pick(r, t):
    """t in 0..1 -> colour of ramp r (0 = darkest)."""
    return r[max(0, min(len(r) - 1, int(round(t * (len(r) - 1)))))]


# ------------------------------------------------------------------ swords
# Design brief (0.1.8.3): the names stay, the silhouettes leave the classic long sword behind.
#   bronze sword    -> heavy-duty tactical blade: tanto point, sawback spine, bolted full-tang grip
#   nanosaber       -> nano HF vibration cutter: generator housing, emitter rail, vibrating edge
#   quantum sword   -> quantum field emitter: forked emitter, singularity core, a blade of light


def blade_profile(u, start, tip, w, taper=6.5):
    """Half width of a blade at u: full width w, tapering to a point over the last `taper` px."""
    if u < start or u > tip:
        return -1
    if u > tip - taper:
        return w * max(0.0, (tip - u) / taper) ** 0.85
    return w


NANO_STEEL = [hexc('15181d'), hexc('262b33'), hexc('3c434e'), hexc('5c6672'), hexc('8a96a3'), hexc('c4ccd6')]
NANO_GREEN = [hexc('0e5a1a'), hexc('1f9a2e'), hexc('3fe050'), hexc('9cff9a'), hexc('effff0')]
QUANTUM_WHITE = [hexc('5e6878'), hexc('8d98aa'), hexc('b9c3d2'), hexc('dfe6ef'), hexc('f6f9fc')]
QUANTUM_CYAN = [hexc('0c3c66'), hexc('1a76c2'), hexc('3ab4ff'), hexc('9be6ff'), hexc('f0fdff')]
QUANTUM_DARK = [hexc('101320'), hexc('1d2233'), hexc('2e3550'), hexc('454f72')]


def band(v, lo, hi):
    return lo <= v <= hi


def bronze_sword():
    """Tactical blade. Edge on the lower right (v < 0), spine on the lit upper left (v > 0)."""
    c = Canvas(N, N)
    f = Frame(3.5, 28.5, 29.5, 2.5)
    tip = f.L
    steel = [hexc('3a3f46'), hexc('5b636d'), hexc('8d97a3'), hexc('c9d2dc'), hexc('f2f6fa')]
    for y in range(N):
        for x in range(N):
            u, v = f.uv(x, y)
            col = None
            # pommel with lanyard hole
            if u < 2.3 and abs(v) <= 1.5:
                col = BRONZE_OUT if (abs(u - 1.1) < 0.5 and abs(v) < 0.4) else (steel[3] if v > 0 else steel[2])
            # bolted full-tang grip: bronze tang on the spine side, dark scales, two steel bolts
            elif 2.3 <= u < 9.0 and abs(v) <= 1.45:
                if v > 0.75:
                    col = BRONZE[4]
                else:
                    col = G10[2] if (int(u * 1.41) % 2) else G10[1]          # textured G10 scales
                    if v < -0.9:
                        col = G10[0]
                for bu in (4.2, 7.0):
                    if abs(u - bu) < 0.55 and abs(v + 0.1) < 0.45:
                        col = steel[3]
            # asymmetric guard: a finger lug on the edge side
            elif 9.0 <= u < 10.5 and -3.1 <= v <= 1.9:
                col = steel[1] if v < -0.8 else steel[2]                       # steel guard plate
                if u < 9.6:
                    col = steel[3]
            # blade
            elif u >= 10.5:
                tanto = tip - 7.0
                spine = 1.75 if u < tip - 1.2 else 1.75 - (u - (tip - 1.2)) * 1.6
                edge = -2.25 if u < tanto else -2.25 + (u - tanto) * (4.0 / 7.0)
                # sawback: notches cut into the spine
                if 13.0 <= u <= 21.0 and (int((u - 13.0) * 1.42) % 2 == 1):
                    spine -= 0.75
                if edge <= v <= spine:
                    if v > spine - 0.75:
                        col = BRONZE[2]                       # brushed spine
                    elif v < edge + 0.85:
                        col = BRONZE[6]                       # honed edge
                    elif v < edge + 1.55:
                        col = BRONZE[5]                       # grind line
                    else:
                        col = BRONZE[4] if v > 0 else BRONZE[3]
                    if abs(u - tanto) < 0.5:
                        col = BRONZE[6]                       # tanto ridge
                    for hu in (14.5, 17.4):                   # lightening holes
                        if abs(u - hu) < 0.5 and abs(v - 0.2) < 0.4:
                            col = BRONZE_OUT
            if col:
                c.put(x, y, col)
    outline(c, BRONZE_OUT)
    return c


def nanosaber(active, frame=0, frames=6):
    """Nano HF vibration cutter. Off: steel edge. On: the edge vibrates (green afterimages)."""
    c = Canvas(N, N)
    f = Frame(3.5, 28.5, 29.5, 2.5)
    tip = f.L
    jitter = (1, 2, 1, 0, 1, 2)[frame % 6] if active else 0
    for y in range(N):
        for x in range(N):
            u, v = f.uv(x, y)
            col = None
            if u < 1.6 and abs(v) <= 1.3:                                   # pommel cap
                col = NANO_STEEL[4] if v > 0 else NANO_STEEL[3]
            elif 1.6 <= u < 8.2 and abs(v) <= 1.25:                         # ridged grip
                col = NANO_STEEL[1] if int(u * 1.41) % 2 else NANO_STEEL[2]
                if v > 0.8:
                    col = NANO_STEEL[3]
            elif 6.0 <= u < 8.2 and -2.5 <= v < -1.25:                      # trigger
                col = NANO_GREEN[2] if active else NANO_GREEN[1]
            elif 8.2 <= u < 13.6 and -2.5 <= v <= 2.1:                      # HF generator housing
                col = NANO_STEEL[4] if v > 1.3 else NANO_STEEL[3] if v > -1.0 else NANO_STEEL[2]
                if v > -0.2 and any(abs(u - s) < 0.4 for s in (9.4, 10.8, 12.2)):
                    col = NANO_STEEL[0]                                      # vent slits
                if band(v, -2.0, -1.3) and any(abs(u - s) < 0.4 for s in (9.6, 11.0)):
                    col = NANO_GREEN[3] if active else NANO_GREEN[1]          # status LEDs
            elif u >= 13.6:
                chisel = tip - 4.0
                spine = 1.25
                edge = -1.65 if u < chisel else -1.65 + (u - chisel) * (2.9 / 4.0)
                if edge <= v <= spine:
                    col = NANO_STEEL[2] if v > 0.6 else NANO_STEEL[1]
                    if v > spine - 0.7:
                        node = (u - 14.5) % 2.1 < 0.75
                        col = (NANO_GREEN[3] if active else NANO_GREEN[1]) if node else NANO_STEEL[3]   # emitter rail
                    if v < edge + 0.8:
                        col = (NANO_GREEN[4] if active else NANO_STEEL[5])                              # cutting edge
                    elif v < edge + 1.4 and active:
                        col = NANO_GREEN[2]
            if col:
                c.put(x, y, col)
    outline(c, hexc('0b0d10'))
    if active:
        # vibration afterimages: translucent copies of the edge just outside it, shifting each frame
        for y in range(N):
            for x in range(N):
                if c.get(x, y)[3]:
                    continue
                u, v = f.uv(x, y)
                if u < 14.0 or u > tip - 0.5:
                    continue
                chisel = tip - 4.0
                edge = -1.65 if u < chisel else -1.65 + (u - chisel) * (2.9 / 4.0)
                d = edge - v                                  # distance outside the edge
                if 0.6 < d <= 0.6 + 0.75 * (jitter + 1):
                    a = 150 if d < 1.4 else 80
                    c.put(x, y, (NANO_GREEN[3][0], NANO_GREEN[3][1], NANO_GREEN[3][2], a))
                elif v > 1.25 + 0.6 and v <= 1.25 + 0.6 + 0.75 * (jitter > 1) and u < tip - 2:
                    c.put(x, y, (NANO_GREEN[2][0], NANO_GREEN[2][1], NANO_GREEN[2][2], 70))
    return c


VOID = [hexc('05030c'), hexc('1a0c33'), hexc('5a2ca8'), hexc('b88cff'), hexc('f4ecff')]


def quantum_sword(frame=0, frames=8):
    """Quantum field emitter: forked emitter around a singularity, the blade is a light field."""
    c = Canvas(N, N)
    f = Frame(2.5, 29.5, 29.5, 2.5)
    tip = f.L
    phase = frame / frames
    core_u = 12.0
    # hilt and emitter fork (opaque, outlined)
    for y in range(N):
        for x in range(N):
            u, v = f.uv(x, y)
            col = None
            if u < 2.2 and math.hypot(u - 1.2, v) <= 1.5:                 # pommel gem
                col = QUANTUM_CYAN[3] if v > 0.2 else QUANTUM_CYAN[2]
            elif 2.2 <= u < 8.6 and abs(v) <= 1.1:                        # grip
                ring = abs((u - 2.2) % 2.1 - 1.05) < 0.35
                col = QUANTUM_CYAN[1] if ring else (QUANTUM_DARK[2] if v > 0 else QUANTUM_DARK[1])
            elif 8.6 <= u < 10.4 and abs(v) <= 1.9:                       # emitter base
                col = QUANTUM_WHITE[3] if v > 0.7 else QUANTUM_WHITE[2] if v > -0.7 else QUANTUM_WHITE[1]
            elif 10.4 <= u < 14.2 and 1.5 <= abs(v) <= 2.6:              # fork prongs
                col = QUANTUM_WHITE[3] if v > 0 else QUANTUM_WHITE[1]
                if u > 13.3:
                    col = QUANTUM_CYAN[3]
            if col:
                c.put(x, y, col)
    outline(c, hexc('0a0d18'))
    # singularity between the prongs: black core, accretion ring with a hot spot that orbits
    for y in range(N):
        for x in range(N):
            u, v = f.uv(x, y)
            d = math.hypot(u - core_u, v)
            if d <= 0.75:
                c.put(x, y, VOID[0])
            elif d <= 1.45:
                ang = math.atan2(v, u - core_u) / (2 * math.pi)
                hot = (ang - phase) % 1.0
                c.put(x, y, VOID[4] if hot < 0.18 else VOID[3] if hot < 0.5 else VOID[2])
    # light blade: opaque bright rims and core line, translucent interference field inside
    start = 13.4
    for y in range(N):
        for x in range(N):
            u, v = f.uv(x, y)
            if u < start or u > tip:
                continue
            w = blade_profile(u, start, tip, 2.05, taper=10.0)
            if w < 0 or abs(v) > w:
                continue
            if c.get(x, y)[3] == 255 and u < 14.3:
                continue
            gap = any(abs(u - g) < 0.4 for g in (21.0, 28.5))
            if abs(v) < 0.45:
                col = QUANTUM_CYAN[4]
            elif abs(v) > w - 0.72 and not gap:
                col = QUANTUM_CYAN[3] if v > 0 else QUANTUM_CYAN[2]
            else:
                ripple = 0.5 + 0.5 * math.sin(u * 1.1 - phase * 2 * math.pi)
                a = int(70 + 95 * ripple)
                base = QUANTUM_CYAN[2] if ripple > 0.5 else VOID[3]
                col = (base[0], base[1], base[2], a)
            c.put(x, y, col)
    # field sparks that drift toward the tip
    for k in range(3):
        su = start + ((phase + k / 3.0) % 1.0) * (tip - start - 2)
        sv = (2.6 if k % 2 else -2.6) * (1 - (su - start) / (tip - start))
        sx = f.x0 + su * f.ux + sv * f.vx - 0.5
        sy = f.y0 + su * f.uy + sv * f.vy - 0.5
        ix, iy = int(round(sx)), int(round(sy))
        if 0 <= ix < N and 0 <= iy < N and not c.get(ix, iy)[3]:
            c.put(ix, iy, (QUANTUM_CYAN[3][0], QUANTUM_CYAN[3][1], QUANTUM_CYAN[3][2], 200))
    soft_glow(c, QUANTUM_CYAN[2], 0.22, radius=1, source=lambda x, y: f.uv(x, y)[0] >= start)
    return c


# ------------------------------------------------------------------ nano bow
BOW_LIMB = [hexc('1a1f26'), hexc('2c333d'), hexc('46505d'), hexc('6b7888'), hexc('a3b2c2')]


def nano_bow(pull):
    """Compound bow: carbon limbs with cams at the tips, a riser with grip and sight, an energy
    string. The bow runs from the upper left to the lower right and bulges toward the upper right;
    the string is pulled toward the lower left, the nocked arrow points to the upper right."""
    c = Canvas(N, N)
    # Bow frame: s along the bow (upper left -> lower right), t across it (+ = upper right, the belly).
    ox, oy = 15.6, 16.4

    def st(x, y):
        px, py = x + 0.5 - ox, y + 0.5 - oy
        return (px + py) * SQ, (px - py) * SQ

    def xy(s, t):
        return ox + (s + t) * SQ, oy + (s - t) * SQ

    half = 14.2                                       # half length (cam to cam)
    bend = 4.6 - 0.55 * pull                         # belly bulge at the middle
    limb_t = lambda s: bend * (1 - (s / half) ** 2)  # limb centre line
    draw = 1.2 + pull * 1.55                         # string position behind the cams (t < 0)
    string_col = hexc('7dffc4')
    string_hi = hexc('dcfff0')
    for y in range(N):
        for x in range(N):
            s, t = st(x, y)
            if abs(s) > half + 0.6:
                continue
            col = None
            lt = limb_t(s)
            # riser (middle): thick, rubber grip, sight LED
            if abs(s) <= 4.6 and lt - 1.9 <= t <= lt + 0.9:
                col = BOW_LIMB[3] if t > lt - 0.4 else BOW_LIMB[2]
                if abs(s) <= 1.5 and t < lt - 0.3:
                    col = hexc('2c2c33') if (x + y) % 2 else hexc('3a3a43')
                if -3.6 <= s <= -2.6 and t > lt - 0.2:
                    col = hexc('5aff9a')
            # limbs: tapered, lit edge on the belly side
            elif abs(t - lt) <= 1.25 - 0.45 * (abs(s) / half):
                col = BOW_LIMB[4] if t > lt + 0.1 else BOW_LIMB[2]
                if 7.5 <= abs(s) <= 8.5:
                    col = hexc('3fd88a')                                # limb light strip
            if col:
                c.put(x, y, col)
    # string: from each cam to the nock point (straight lines)
    ax, ay = xy(-half, 0.2)
    bx, by = xy(half, 0.2)
    nx, ny = xy(0, -draw)
    c.line(ax, ay, nx, ny, string_col)
    c.line(nx, ny, bx, by, string_col)
    # cams (pulleys) at the limb tips
    for s in (-half, half):
        qx, qy = xy(s, 0.2)
        for y in range(N):
            for x in range(N):
                d = math.hypot(x + 0.5 - qx, y + 0.5 - qy)
                if d <= 1.75:
                    c.put(x, y, BOW_LIMB[4] if d > 0.9 else hexc('c4d0dc'))
    # nocked arrow while drawing
    if pull > 0:
        hx, hy = xy(0, bend + 3.2 + 0.4 * pull)       # head beyond the riser
        c.line(nx, ny, hx, hy, hexc('aab0ba'))
        tx, ty = int(round(hx)), int(round(hy))
        c.put(tx, ty, hexc('f2fff8'))
        for k in (1, 2):
            c.put(tx - k, ty, string_col)
            c.put(tx, ty + k, string_col)
        fx, fy = int(round(nx)), int(round(ny))
        c.put(fx - 1, fy, hexc('3fd88a'))              # fletching
        c.put(fx, fy + 1, hexc('3fd88a'))
        c.put(fx, fy, string_hi)
    outline(c, hexc('07090c'))
    return c


# ------------------------------------------------------------------ output
def strip(frames):
    out = Canvas(N, N * len(frames))
    for i, f in enumerate(frames):
        out.paste(f, 0, i * N)
    return out


def write_mcmeta(path, frametime, interpolate):
    with open(path + '.mcmeta', 'w', encoding='utf-8') as f:
        json.dump({'animation': {'frametime': frametime, 'interpolate': interpolate}}, f, indent=2)
        f.write('\n')


ICONS = {
    'weapon/bronze_sword': (bronze_sword, None),
    'weapon/nanosaber': (lambda: nanosaber(False), None),
    'weapon/nanosaber_active': (lambda: strip([nanosaber(True, i) for i in range(6)]), (1, False)),
    'weapon/quantum_sword': (lambda: strip([quantum_sword(i) for i in range(8)]), (2, False)),
    'item_tool_nano_bow': (lambda: nano_bow(0), None),
    'item_tool_nano_bow_pull0': (lambda: nano_bow(1), None),
    'item_tool_nano_bow_pull1': (lambda: nano_bow(2), None),
    'item_tool_nano_bow_pull2': (lambda: nano_bow(3), None),
}


def main():
    made = []
    for name, (fn, anim) in ICONS.items():
        img = fn()
        path = os.path.join(OUT, name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
        if anim:
            write_mcmeta(path, *anim)
        elif os.path.exists(path + '.mcmeta'):
            os.remove(path + '.mcmeta')
        made.append((name, img))
    print('weapon icons written:', len(made))
    if PREVIEW:
        os.makedirs(PREVIEW, exist_ok=True)
        firsts = []
        for _, img in made:
            f = Canvas(N, N)
            f.px = img.px[:N * N]
            firsts.append(f)
        sheet(firsts, 8, 6).save(os.path.join(PREVIEW, 'weapons_183.png'))
        for name in ('weapon/nanosaber_active', 'weapon/quantum_sword'):
            img = dict(made)[name]
            frames = []
            for k in range(img.h // N):
                f = Canvas(N, N)
                f.px = img.px[k * N * N:(k + 1) * N * N]
                frames.append(f)
            sheet(frames, 8, 4).save(os.path.join(PREVIEW, name.replace('/', '_') + '_frames.png'))


if __name__ == '__main__':
    main()
