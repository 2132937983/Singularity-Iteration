#!/usr/bin/env python3
"""
Singularity Iteration GUI theme v2 - "clean modern IC2".

Matte light-grey / off-white panels, thin flat outlines with fine inset shading, minimal
slot frames, light inset display wells with dark text, one restrained steel-blue accent
and semantic status colours only. No rivets, rust, hazard tape, circuit traces or glow.

restyle(): rebuilds each machine GUI from the upstream original texture - every functional
pixel keeps its coordinates (slots, arrows, tank wells, widget backgrounds), decoration is
replaced by a flat panel. new_*(): backgrounds for the code-drawn screens.
"""
from __future__ import annotations
import os, sys
from collections import deque
from PIL import Image

# ------------------------------------------------------------------ palette
PANEL_TOP = (224, 226, 227)
PANEL_BOT = (211, 213, 215)
OUTLINE = (88, 93, 99)
EDGE_HI = (248, 249, 249)
EDGE_LO = (170, 174, 179)
GROOVE_LO = (164, 168, 173)
GROOVE_HI = (245, 246, 247)
SLOT_FILL = (168, 172, 177)
SLOT_FILL_IN = (156, 160, 165)
SLOT_LO = (122, 127, 133)
SLOT_HI = (247, 248, 249)
WELL_FILL = (238, 240, 241)
WELL_GRID = (229, 231, 233)
SCREEN = (40, 44, 49)            # dark graphite (only for screens that carry light text)
ACCENT = (58, 110, 165)
ACCENT_LT = (126, 160, 198)


def clamp(v):
    return max(0, min(255, int(round(v))))


def lerp(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def sat(c):
    return max(c[:3]) - min(c[:3])


def h32(n):
    n &= 0xFFFFFFFF
    n = ((n >> 16) ^ n) * 0x45d9f3b & 0xFFFFFFFF
    n = ((n >> 16) ^ n) * 0x45d9f3b & 0xFFFFFFFF
    return (n >> 16) ^ n


def panel_color(x, y, h):
    c = lerp(PANEL_TOP, PANEL_BOT, y / max(1, h - 1))
    d = (h32(x * 92821 + y * 68917) % 3) - 1          # +-1 matte grain, invisible at 1x
    return tuple(clamp(v + d * 0.6) for v in c)


# ------------------------------------------------------------------ primitives
def new_canvas(w=256, h=256):
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))


def put(img, x, y, c):
    img.putpixel((x, y), tuple(c[:3]) + (255,))


def panel(img, x0, y0, w, h):
    px = img.load()
    for y in range(h):
        for x in range(w):
            px[x0 + x, y0 + y] = panel_color(x, y, h) + (255,)
    edge(img, x0, y0, w, h)


def edge(img, x0, y0, w, h):
    """Thin flat outline with 2-px chamfered corners and a fine inner highlight/shadow."""
    px = img.load()
    x1, y1 = x0 + w - 1, y0 + h - 1
    for x in range(x0 + 2, x1 - 1):
        px[x, y0] = OUTLINE + (255,); px[x, y1] = OUTLINE + (255,)
        px[x, y0 + 1] = EDGE_HI + (255,); px[x, y1 - 1] = EDGE_LO + (255,)
    for y in range(y0 + 2, y1 - 1):
        px[x0, y] = OUTLINE + (255,); px[x1, y] = OUTLINE + (255,)
        px[x0 + 1, y] = EDGE_HI + (255,); px[x1 - 1, y] = EDGE_LO + (255,)
    for (cx, cy, sx, sy) in ((x0, y0, 1, 1), (x1, y0, -1, 1), (x0, y1, 1, -1), (x1, y1, -1, -1)):
        for (ax, ay) in ((cx, cy), (cx + sx, cy), (cx, cy + sy)):
            px[ax, ay] = (0, 0, 0, 0)
        px[cx + sx, cy + sy] = OUTLINE + (255,)
        px[cx + 2 * sx, cy] = OUTLINE + (255,)
        px[cx, cy + 2 * sy] = OUTLINE + (255,)


def groove(img, x, y, w):
    for i in range(w):
        put(img, x + i, y, GROOVE_LO)
        put(img, x + i, y + 1, GROOVE_HI)


def slot(img, x, y, size=18):
    for j in range(size):
        for i in range(size):
            if i == size - 1 or j == size - 1:
                c = SLOT_HI
            elif i == 0 or j == 0:
                c = SLOT_LO
            elif i == 1 or j == 1:
                c = SLOT_FILL_IN
            else:
                c = SLOT_FILL
            put(img, x + i, y + j, c)
    put(img, x + size - 1, y, lerp(SLOT_LO, SLOT_HI, 0.5))
    put(img, x, y + size - 1, lerp(SLOT_LO, SLOT_HI, 0.5))


def well(img, x, y, w, h, grid=True):
    """Light inset display (dark text is drawn on it)."""
    for j in range(h):
        for i in range(w):
            if i == 0 or j == 0:
                c = SLOT_LO
            elif i == w - 1 or j == h - 1:
                c = SLOT_HI
            else:
                c = WELL_FILL
                if grid and (i % 8 == 0 or j % 8 == 0):
                    c = WELL_GRID
            put(img, x + i, y + j, c)


def inventory(img, x, y):
    for r in range(3):
        for c in range(9):
            slot(img, x + c * 18, y + r * 18)
    for c in range(9):
        slot(img, x + c * 18, y + 58)


# ------------------------------------------------------------------ restyle of upstream textures
def main_mask(img):
    w, h = img.size
    px = img.load()
    start = None
    for s in range(min(w, h)):
        for x, y in ((s, s), (s + 1, s), (s, s + 1), (s + 2, s), (s, s + 2)):
            if x < w and y < h and px[x, y][3] > 0:
                start = (x, y); break
        if start:
            break
    if not start:
        return set(), None
    seen = {start}; q = deque([start])
    while q:
        x, y = q.popleft()
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in seen and px[nx, ny][3] > 0:
                seen.add((nx, ny)); q.append((nx, ny))
    xs = [p[0] for p in seen]; ys = [p[1] for p in seen]
    return seen, (min(xs), min(ys), max(xs), max(ys))


def near(c, ref, tol=3):
    return all(abs(c[i] - ref[i]) <= tol for i in range(3))


def components(cells):
    """Connected components of a pixel set (4-neighbour)."""
    cells = set(cells); out = []
    while cells:
        s = cells.pop(); comp = [s]; q = deque([s])
        while q:
            x, y = q.popleft()
            for n in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                if n in cells:
                    cells.remove(n); comp.append(n); q.append(n)
        out.append(comp)
    return out


def restyle(src, out, dark_mode=None, wells=()):
    img = Image.open(src).convert('RGBA')
    op = img.load()
    mask, bbox = main_mask(img)
    if not bbox or bbox[2] - bbox[0] < 60:
        return False
    x0, y0, x1, y1 = bbox
    W, H = x1 - x0 + 1, y1 - y0 + 1
    if dark_mode is None:
        bgish = sum(1 for p in mask if lum(op[p]) >= 176 and not near(op[p], (255, 255, 255), 0))
        dark_mode = bgish < len(mask) * 0.12
    res = img.copy(); px = res.load()

    slotfam = {p for p in mask if near(op[p], (139, 139, 139)) or near(op[p], (55, 55, 55), 2)}
    near_slot = set()
    for (x, y) in slotfam:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                near_slot.add((x + dx, y + dy))
    blacks = [p for p in mask if lum(op[p]) < 24 and sat(op[p]) < 20]
    big_black = set()
    for comp in components(blacks):
        xs = [p[0] for p in comp]; ys = [p[1] for p in comp]
        if len(comp) > 300 and (max(xs) - min(xs)) > 12 and (max(ys) - min(ys)) > 12 \
                and len(comp) > 0.6 * (max(xs) - min(xs) + 1) * (max(ys) - min(ys) + 1):
            big_black.update(comp)

    # small isolated grey marks (trace ends, screw heads, dashes) are decoration, not widgets
    greys = [p for p in mask if 24 <= lum(op[p]) < 176 and sat(op[p]) <= 24 and p not in slotfam]
    deco = set()
    for comp in components(greys):
        xs = [q[0] for q in comp]; ys = [q[1] for q in comp]
        if len(comp) <= 14 or (max(xs) - min(xs) <= 1 and max(ys) - min(ys) > 3 and len(comp) < 40) \
                or (max(ys) - min(ys) <= 1 and max(xs) - min(xs) > 3 and len(comp) < 40 and not any(q in near_slot for q in comp)):
            if not any(q in near_slot for q in comp):
                deco.update(comp)

    # large saturated decorative areas (illustrations) -> neutral light-grey relief
    sats = [p for p in mask if sat(op[p]) > 24 and lum(op[p]) >= 24]
    big_sat = set()
    for comp in components(sats):
        if len(comp) > 600:
            big_sat.update(comp)

    def bg(x, y):
        return panel_color(x - x0, y - y0, H)

    for (x, y) in mask:
        c = op[x, y][:3]; a = op[x, y][3]
        L, S = lum(c), sat(c)
        if near(c, (139, 139, 139)):
            nc = SLOT_FILL
        elif near(c, (55, 55, 55), 2):
            nc = SLOT_LO
        elif L >= 250 and (x, y) in near_slot:
            nc = SLOT_HI
        elif (x, y) in big_black:
            # display screens become light inset wells (dark text is drawn on them)
            nb = [(x - 1, y) in big_black, (x, y - 1) in big_black, (x + 1, y) in big_black, (x, y + 1) in big_black]
            nc = SLOT_LO if not (nb[0] and nb[1]) else SLOT_HI if not (nb[2] and nb[3]) else WELL_FILL
        elif near(c, (42, 80, 55), 4) or near(c, (26, 110, 61), 4):
            nc = WELL_FILL                    # green LCD field -> light display
        elif near(c, (59, 113, 77), 4):
            nc = WELL_GRID
        elif near(c, (100, 243, 67), 6) or near(c, (28, 161, 72), 6):
            nc = SLOT_LO                      # LCD bezel lines
        elif (x, y) in deco:
            nc = bg(x, y)
        elif dark_mode and (S > 24 or L < 120):
            nc = bg(x, y)                     # dark-theme decoration -> flat panel
        elif (x, y) in big_sat:
            nc = lerp((150, 154, 160), (232, 234, 236), L / 255)
        elif S > 24:
            nc = c                            # functional colour (arrows, icons, indicators)
        elif L >= 176:
            nc = bg(x, y)                     # background, traces, screws -> clean panel
        elif L < 24:
            nc = OUTLINE
        else:                                 # functional greys: wells, arrow bodies, frames
            nc = lerp((70, 75, 81), (168, 172, 177), (L - 24) / 152)
        px[x, y] = nc + (a,)

    # slot inner shading (second row/col inside each slot fill)
    for (x, y) in mask:
        if near(op[x, y], (139, 139, 139)) and (near(op[x - 1, y], (55, 55, 55), 2) or near(op[x, y - 1], (55, 55, 55), 2)):
            px[x, y] = SLOT_FILL_IN + (255,)

    # outer edge of the main panel
    for (x, y) in mask:
        border = any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if border:
            px[x, y] = OUTLINE + (255,)
    # 1-px highlight inside top/left outline, shadow inside bottom/right
    for (x, y) in mask:
        if px[x, y][:3] == OUTLINE or (op[x, y][3] and lum(op[x, y]) < 176 and not dark_mode):
            continue
        if (x, y - 1) in mask and px[x, y - 1][:3] == OUTLINE and (x, y - 2) not in mask:
            px[x, y] = EDGE_HI + (255,)
        elif (x - 1, y) in mask and px[x - 1, y][:3] == OUTLINE and (x - 2, y) not in mask:
            px[x, y] = EDGE_HI + (255,)
        elif (x, y + 1) in mask and px[x, y + 1][:3] == OUTLINE and (x, y + 2) not in mask:
            px[x, y] = EDGE_LO + (255,)
        elif (x + 1, y) in mask and px[x + 1, y][:3] == OUTLINE and (x + 2, y) not in mask:
            px[x, y] = EDGE_LO + (255,)

    def plain(x, y):
        return (x, y) in mask and (lum(op[x, y]) >= 176 or (dark_mode and (sat(op[x, y]) > 24 or lum(op[x, y]) < 120))) \
            and (x, y) not in near_slot and (x, y) not in big_black

    # groove above the player inventory (top row of a 9-wide slot grid)
    inv_top = None
    for y in range(y0, y1):
        run = sum(1 for k in range(9) if near(op[x0 + 8 + 18 * k, y], (139, 139, 139))) if x0 + 8 + 18 * 8 < img.width else 0
        if run == 9:
            inv_top = y - 1
            break
    if inv_top:
        gy = inv_top - 4
        for x in range(x0 + 4, x1 - 3):
            if plain(x, gy) and plain(x, gy + 1):
                px[x, gy] = GROOVE_LO + (255,)
                px[x, gy + 1] = GROOVE_HI + (255,)
    for (wx, wy, ww, wh) in wells:
        well(res, x0 + wx, y0 + wy, ww, wh, grid=False)
    res.save(out)
    return True


def restyle_atlas(src, out):
    """Re-tint the cyan progress/arrow fills to the steel-blue accent; slot greys to v2 slots."""
    img = Image.open(src).convert('RGBA'); px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            c = (r, g, b)
            if b > 150 and g > 120 and r < 140 and b - r > 60:           # cyan-ish
                L = lum(c) / 255
                px[x, y] = lerp((34, 74, 120), ACCENT_LT, max(0, min(1, (L - 0.35) / 0.55))) + (a,)
            elif near(c, (139, 139, 139)):
                px[x, y] = SLOT_FILL + (a,)
            elif near(c, (55, 55, 55), 2):
                px[x, y] = SLOT_LO + (a,)
    img.save(out)


if __name__ == '__main__':
    for p in sys.argv[1:]:
        print(p, restyle(p, '/tmp/claude-0/' + os.path.basename(p)))


GUI_DIR = '/home/claude/repo/src/main/resources/assets/mio_icif/textures/gui/'


def new_laser_tower():
    img = new_canvas()
    panel(img, 0, 0, 176, 200)
    groove(img, 4, 15, 168)          # under the tab bar
    slot(img, 7, 85)
    groove(img, 4, 103, 168)         # above the player inventory
    inventory(img, 7, 117)
    img.save(GUI_DIR + 'gui_laser_tower.png')


def new_energy_terminal():
    img = new_canvas()
    panel(img, 0, 0, 240, 210)
    groove(img, 4, 15, 232)
    img.save(GUI_DIR + 'gui_energy_terminal.png')


def new_generic():
    img = new_canvas()
    panel(img, 0, 0, 176, 166)
    slot(img, 79, 42)
    groove(img, 4, 77, 168)
    inventory(img, 7, 83)
    img.save(GUI_DIR + 'gui_generic.png')


# atlas widget regions (x0, y0, x1, y1) whose dark "LCD" backgrounds become flat light meters
ATLAS_METER_REGIONS = [(36, 0, 128, 60), (158, 0, 236, 60), (0, 188, 70, 228), (0, 268, 104, 326)]


def lighten_atlas_meters(path):
    img = Image.open(path).convert('RGBA'); px = img.load()
    for (x0, y0, x1, y1) in ATLAS_METER_REGIONS:
        for y in range(y0, y1):
            for x in range(x0, x1):
                r, g, b, a = px[x, y]
                if not a:
                    continue
                c = (r, g, b); L, S = lum(c), sat(c)
                if S < 20 and L < 90:
                    nc = (112, 117, 123) if L < 18 else lerp((236, 238, 240), (196, 200, 205), min(1, L / 90))
                    px[x, y] = nc + (a,)
                elif g > r + 40 and g > b + 40 and r < 120:          # green scale ticks -> dark grey ticks
                    px[x, y] = (96, 101, 107, a)
    img.save(path)
