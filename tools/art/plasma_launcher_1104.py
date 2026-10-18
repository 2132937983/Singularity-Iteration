#!/usr/bin/env python3
"""
0.1.10.4: plasma launcher (item_tool_plasma_launcher) art: 3D hand model, animated texture, GUI icon.

Writes (under <repo>/src/main/resources/assets/mio_icif):
  models/item/3d/item_tool_plasma_launcher.json   element model, barrel to the north (-z), grip at the back
  textures/item/3d/item_tool_plasma_launcher.png  128 x 128 per frame, 6 frames (+ .mcmeta)
  models/item/item_tool_plasma_launcher.json      neoforge:separate_transforms (gui and fixed: the 2D icon)
  textures/item/item_tool_plasma_launcher.png     32 x 32 icon, 8 frames (+ .mcmeta)

Design: a heavy plasma lance. Gunmetal receiver under white armour plates with a violet stripe;
three copper accelerator rings around an exposed, glowing plasma chamber; a vented emitter with
two forked prongs and a cyan focusing ring at the muzzle; an energy cell with violet charge lights
under the receiver; a holographic sight; heat-sink fins; an angled grip and a stock. The plasma,
the prong tips, the side conduits, the charge lights and the sight lens are emissive
(neoforge_data block_light 15) and pulse in the texture animation.

Texture density: 2 px per model unit (128 px texture = 16 UV units, 1 UV unit = 8 px).

Usage: plasma_launcher_1104.py <repo root> [--preview=<out.png>]
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixart import Canvas, hexc, mix, shade, write_png  # noqa: E402

ROOT = sys.argv[1]
A = os.path.join(ROOT, 'src/main/resources/assets/mio_icif')
TEX = 128
PX = 2            # texture pixels per model unit
FRAMES = 6

# ------------------------------------------------------------------ palette
GUNMETAL = hexc('3b414b')
GUNMETAL_D = hexc('262a31')
ARMOR = hexc('cfd5de')
ARMOR_D = hexc('8c94a1')
STRIPE = hexc('9b4dff')
GRIP = hexc('1f2228')
COPPER = hexc('c07a34')
COPPER_D = hexc('7c4618')
PLASMA_HI = hexc('fbeaff')
PLASMA = hexc('c46bff')
PLASMA_LO = hexc('5b1fb0')
CYAN = hexc('7fe9ff')
CYAN_HI = hexc('e8fdff')
CELL = hexc('2d2440')

# ------------------------------------------------------------------ geometry
# (name, from, to, material (or per-face dict), emissive, rotation)
E = []


def box(name, f, t, mat, emissive=False, rot=None):
    E.append((name, f, t, mat, emissive, rot))


# receiver and stock
box('receiver', [5.5, 6.5, 2], [10.5, 11.5, 14], 'metal')
box('armor_top', [5, 11.5, 1.5], [11, 13, 12.5], {'up': 'armor_stripe', 'north': 'armor', 'south': 'armor', 'east': 'armor', 'west': 'armor', 'down': 'metal'})
box('armor_side_l', [4.75, 7.5, 6], [5.5, 11, 12], 'armor')
box('armor_side_r', [10.5, 7.5, 6], [11.25, 11, 12], 'armor')
box('stock', [6.25, 7, 14], [9.75, 11, 19], 'metal')
box('butt', [6, 6.25, 19], [10, 11.75, 20.5], 'grip')
box('cheek', [6.5, 11, 14.5], [9.5, 12, 18], 'armor')
# accelerator section: exposed plasma chamber inside three copper rings
box('chamber', [6.5, 7.5, -3.5], [9.5, 10.5, 2.5], 'plasma', True)
for i, z in enumerate((-3.0, -0.75, 1.5)):
    box('ring%d_t' % i, [5.25, 10.5, z], [10.75, 11.75, z + 1], 'copper')
    box('ring%d_b' % i, [5.25, 6.25, z], [10.75, 7.5, z + 1], 'copper')
    box('ring%d_l' % i, [5.25, 7.5, z], [6.5, 10.5, z + 1], 'copper')
    box('ring%d_r' % i, [9.5, 7.5, z], [10.75, 10.5, z + 1], 'copper')
box('collar', [5.25, 6.25, 2.25], [10.75, 11.75, 3], {'north': 'metal', 'south': 'metal', 'east': 'plasma', 'west': 'plasma', 'up': 'plasma', 'down': 'plasma'}, True)
box('spine', [7.25, 11.75, -4], [8.75, 12.5, 2], 'metal')
box('keel', [7.25, 5.5, -4], [8.75, 6.25, 2], 'metal')
# emitter and muzzle
box('emitter', [6.75, 7.75, -8], [9.25, 10.25, -3.5], 'vent')
box('prong_t', [7.4, 10.25, -11], [8.6, 11.25, -4], 'metal')
box('prong_b', [7.4, 6.75, -11], [8.6, 7.75, -4], 'metal')
box('tip_t', [7.5, 9.75, -12.5], [8.5, 10.75, -10.5], 'cyan', True)
box('tip_b', [7.5, 7.25, -12.5], [8.5, 8.25, -10.5], 'cyan', True)
box('focus', [7, 8, -9], [9, 10, -8], 'cyan', True)
box('arc', [7.75, 8.25, -11.75], [8.25, 9.75, -11.25], 'plasma', True)
# side conduits, cell, grip
box('conduit_l', [5.25, 8.75, 2], [5.5, 9.5, 12], 'cyan', True)
box('conduit_r', [10.5, 8.75, 2], [10.75, 9.5, 12], 'cyan', True)
box('cell', [6.5, 3.5, 6], [9.5, 6.5, 10], {'east': 'cell_lights', 'west': 'cell_lights', 'north': 'cell', 'south': 'cell', 'up': 'metal', 'down': 'metal'})
box('cell_lights', [6.4, 4.25, 6.75], [9.6, 5.75, 9.25], {'east': 'plasma_bar', 'west': 'plasma_bar'}, True)
box('grip', [7, 1, 10.5], [9, 6.5, 13], 'grip', False, {'angle': -22.5, 'axis': 'x', 'origin': [8, 6.5, 11.75]})
box('guard', [7.5, 4.25, 8.25], [8.5, 5, 10.5], 'metal')
box('trigger', [7.75, 5, 9.5], [8.25, 6.5, 10], 'armor')
# top: sight and heat sinks
box('sight', [7.25, 13, 3], [8.75, 14.5, 6], 'metal')
box('lens', [7.4, 13.15, 2.9], [8.6, 14.35, 3], {'north': 'lens'}, True)
for i, z in enumerate((7.5, 9, 10.5)):
    box('fin%d' % i, [6, 13, z], [10, 14, z + 0.5], 'metal')

FACE_DIRS = ('north', 'south', 'east', 'west', 'up', 'down')


def face_size(f, t, face):
    dx, dy, dz = (abs(t[i] - f[i]) for i in range(3))
    return {'north': (dx, dy), 'south': (dx, dy), 'east': (dz, dy), 'west': (dz, dy), 'up': (dx, dz), 'down': (dx, dz)}[face]


def face_material(mat, face):
    if isinstance(mat, dict):
        return mat.get(face)
    return mat


# ------------------------------------------------------------------ packing
class Packer:
    def __init__(self, size):
        self.size, self.x, self.y, self.row = size, 0, 0, 0

    def place(self, w, h):
        if self.x + w > self.size:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        if self.y + h > self.size:
            raise SystemExit('texture atlas full')
        at = (self.x, self.y)
        self.x += w
        self.row = max(self.row, h)
        return at


# ------------------------------------------------------------------ painting
def lengthwise(face):
    """True when the face's u axis runs along the gun (z): east/west faces."""
    return face in ('east', 'west')


def paint(c, x0, y0, w, h, material, face, frame):
    def put(x, y, col):
        if 0 <= x < w and 0 <= y < h:
            c.put(x0 + x, y0 + y, col)

    phase = frame / FRAMES
    if material in ('metal', 'armor', 'armor_stripe'):
        base = GUNMETAL if material == 'metal' else ARMOR
        dark = GUNMETAL_D if material == 'metal' else ARMOR_D
        lift = 0.10 if face == 'up' else -0.18 if face == 'down' else 0.0
        for y in range(h):
            for x in range(w):
                col = shade(base, lift + 0.08 * (1 - y / max(1, h - 1)) - 0.04)
                put(x, y, col)
        # bevel: light top-left, dark bottom-right
        for x in range(w):
            put(x, 0, shade(base, lift + 0.25))
            put(x, h - 1, dark)
        for y in range(h):
            put(0, y, shade(base, lift + 0.15))
            put(w - 1, y, dark)
        # panel seams across the long axis every ~7 px
        if lengthwise(face) or face in ('up', 'down'):
            along = w if lengthwise(face) else h
            for s in range(7, along - 3, 7):
                for k in range(1, (h if lengthwise(face) else w) - 1):
                    if lengthwise(face):
                        put(s, k, dark)
                    else:
                        put(k, s, dark)
        # rivets in the corners of larger panels
        if w >= 8 and h >= 6:
            for rx, ry in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
                put(rx, ry, shade(base, lift + 0.35))
        # side armour: cooling louvres and a status light
        if material == 'armor' and lengthwise(face) and w >= 10 and h >= 6:
            for k in range(3):
                yy = 2 + k * 2
                for xx in range(3, w - 5):
                    put(xx, yy, dark)
                    put(xx, yy + 1, shade(base, 0.2))
            lit = frame % 3 != 2
            put(w - 3, h - 3, PLASMA_HI if lit else PLASMA)
            put(w - 4, h - 3, PLASMA)
        # receiver sides: a violet trace line along the length
        if material == 'metal' and lengthwise(face) and w >= 16 and h >= 8:
            yy = h // 2
            for xx in range(2, w - 2):
                put(xx, yy, mix(PLASMA_LO, PLASMA, 0.5 + 0.5 * math.sin((xx / 5.0 - phase) * math.pi * 2)))
        if material == 'armor_stripe':
            mid = w // 2
            for y in range(1, h - 1):
                put(mid - 1, y, STRIPE)
                put(mid, y, shade(STRIPE, 0.25))
    elif material == 'grip':
        for y in range(h):
            for x in range(w):
                knurl = (x + y) % 3 == 0 or (x - y) % 3 == 0
                put(x, y, shade(GRIP, 0.12 if knurl else 0.0))
        for x in range(w):
            put(x, 0, shade(GRIP, 0.3))
    elif material == 'copper':
        for y in range(h):
            for x in range(w):
                band = (x if face in ('north', 'south', 'up', 'down') else y) % 3 == 0
                col = COPPER_D if band else COPPER
                put(x, y, shade(col, 0.15 * (1 - y / max(1, h - 1))))
        for x in range(w):
            put(x, 0, shade(COPPER, 0.35))
    elif material == 'vent':
        for y in range(h):
            for x in range(w):
                put(x, y, GUNMETAL_D)
        along_w = lengthwise(face) or face in ('north', 'south')
        for s in range(1, (w if lengthwise(face) else h) - 1, 2):
            glow = mix(PLASMA_LO, PLASMA, 0.5 + 0.5 * math.sin((s / 4.0 - phase) * math.pi * 2))
            for k in range(1, (h if lengthwise(face) else w) - 1):
                if lengthwise(face):
                    put(s, k, glow)
                else:
                    put(k, s, glow)
        for x in range(w):
            put(x, 0, GUNMETAL)
    elif material in ('plasma', 'plasma_bar'):
        for y in range(h):
            for x in range(w):
                # standing waves that run along the chamber
                along = (x if lengthwise(face) else y) / 6.0
                across = (y / max(1, h - 1)) if lengthwise(face) else (x / max(1, w - 1))
                core = 1 - abs(across - 0.5) * 2
                wave = 0.5 + 0.5 * math.sin((along - phase) * math.pi * 2)
                k = max(0.0, min(1.0, 0.25 + 0.55 * core + 0.35 * wave * core))
                col = mix(PLASMA_LO, PLASMA, min(1.0, k * 1.6)) if k < 0.62 else mix(PLASMA, PLASMA_HI, (k - 0.62) / 0.38)
                put(x, y, col)
        if material == 'plasma_bar':
            # charge gauge: segments with dark gaps, the lit count climbs with the frames
            lit = 2 + frame % (FRAMES - 1)
            seg = 0
            for x in range(w):
                gap = x % 2 == 1
                if not gap:
                    seg += 1
                for y in range(h):
                    if gap:
                        put(x, y, CELL)
                    elif seg > lit:
                        put(x, y, PLASMA_LO)
    elif material == 'cyan':
        for y in range(h):
            for x in range(w):
                k = 0.5 + 0.5 * math.sin((x + y) / 3.0 - phase * math.pi * 2)
                put(x, y, mix(CYAN, CYAN_HI, 0.35 * k))
    elif material == 'lens':
        for y in range(h):
            for x in range(w):
                d = math.hypot(x - (w - 1) / 2, y - (h - 1) / 2) / max(1, w / 2)
                put(x, y, mix(CYAN_HI, hexc('2a7fa8'), min(1.0, d)))
        put(w // 2, h // 2, (255, 120, 140, 255))
    elif material == 'cell':
        for y in range(h):
            for x in range(w):
                put(x, y, shade(CELL, 0.1 if (x + y) % 4 == 0 else 0))
    elif material == 'cell_lights':
        for y in range(h):
            for x in range(w):
                put(x, y, CELL)
    else:
        raise SystemExit('unknown material ' + material)


# ------------------------------------------------------------------ build
def build():
    packer = Packer(TEX)
    wanted = []
    for i, (name, f, t, mat, emissive, rot) in enumerate(E):
        for face in FACE_DIRS:
            m = face_material(mat, face)
            if m is None:
                continue
            fw, fh = face_size(f, t, face)
            wanted.append((i, face, max(1, round(fw * PX)), max(1, round(fh * PX)), m))
    # shelf packing, tallest first
    regions = []          # (element index, face, x, y, w, h, material)
    for i, face, w, h, m in sorted(wanted, key=lambda r: (-r[3], -r[2])):
        x, y = packer.place(w, h)
        regions.append((i, face, x, y, w, h, m))
    sheet = Canvas(TEX, TEX * FRAMES)
    for frame in range(FRAMES):
        frame_canvas = Canvas(TEX, TEX)
        for i, face, x, y, w, h, m in regions:
            paint(frame_canvas, x, y, w, h, m, face, frame)
        sheet.paste(frame_canvas, 0, frame * TEX)
    elements = []
    for i, (name, f, t, mat, emissive, rot) in enumerate(E):
        faces = {}
        for (j, face, x, y, w, h, m) in regions:
            if j != i:
                continue
            u0, v0 = x * 16 / TEX, y * 16 / TEX
            u1, v1 = (x + w) * 16 / TEX, (y + h) * 16 / TEX
            faces[face] = {'uv': [round(u0, 4), round(v0, 4), round(u1, 4), round(v1, 4)], 'texture': '#0'}
        e = {'name': name, 'from': f, 'to': t, 'faces': faces}
        if rot:
            e['rotation'] = rot
        if emissive:
            e['neoforge_data'] = {'block_light': 15, 'sky_light': 15}
        elements.append(e)
    model = {
        'credit': 'Singularity Iteration 0.1.10.4 - generated by tools/art/plasma_launcher_1104.py',
        'texture_size': [TEX, TEX],
        'textures': {'0': 'mio_icif:item/3d/item_tool_plasma_launcher', 'particle': 'mio_icif:item/3d/item_tool_plasma_launcher'},
        'elements': elements,
        'display': {
            'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 1.6, -2.2], 'scale': [0.58, 0.58, 0.58]},
            'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 1.6, -2.2], 'scale': [0.58, 0.58, 0.58]},
            'firstperson_righthand': {'rotation': [0, 6, 0], 'translation': [-0.9, 1.9, -1.2], 'scale': [0.36, 0.36, 0.36]},
            'firstperson_lefthand': {'rotation': [0, -6, 0], 'translation': [0.9, 1.9, -1.2], 'scale': [0.36, 0.36, 0.36]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.45, 0.45, 0.45]},
            'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [1, 1, 1]},
            'fixed': {'rotation': [0, 90, 0], 'translation': [0, 0, 0], 'scale': [0.55, 0.55, 0.55]},
            'gui': {'rotation': [20, 135, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
        },
    }
    used = sum(w * h for (_, _, _, _, w, h, _) in regions)
    return model, sheet, used, len(regions)


# ------------------------------------------------------------------ GUI icon
ICON = 32
OUTLINE = hexc('15171d')


def draw_icon(frame):
    """Hand-drawn 32x32 side view (stock left, muzzle right), the same parts and colours as the 3D
    model. The chamber, the collar, the arc, the cell gauge and the prong tips change per frame."""
    c = Canvas(ICON, ICON)
    phase = frame / FRAMES
    pulse = 0.5 + 0.5 * math.sin(phase * math.pi * 2)

    def rect(x0, y0, x1, y1, col):
        c.rect(x0, y0, x1, y1, col)

    # stock: tapered toward the butt pad, heat-sink fins on top
    for x in range(1, 6):
        top, bottom = 12, 17 - (5 - x) // 2
        rect(x, top, x, bottom, GUNMETAL)
        c.put(x, top, shade(GUNMETAL, 0.3))
        c.put(x, bottom, GUNMETAL_D)
    rect(0, 11, 0, 17, GRIP)
    for x in (2, 4):
        rect(x, 10, x, 11, GUNMETAL)
    # receiver
    rect(6, 11, 18, 18, GUNMETAL)
    rect(6, 18, 18, 18, GUNMETAL_D)
    # armour shroud: flat over the receiver, stepping down at the front
    rect(5, 9, 15, 10, ARMOR)
    rect(16, 10, 17, 10, ARMOR)
    rect(5, 9, 15, 9, shade(ARMOR, 0.25))
    rect(6, 10, 15, 10, STRIPE)
    rect(16, 11, 18, 11, ARMOR_D)
    # side armour plate: one louvre and a status light
    rect(8, 13, 15, 16, ARMOR)
    rect(8, 13, 15, 13, shade(ARMOR, 0.22))
    rect(8, 16, 15, 16, ARMOR_D)
    rect(9, 14, 13, 14, ARMOR_D)
    c.put(14, 15, PLASMA_HI if frame % 3 != 2 else PLASMA)
    # violet trace along the receiver
    rect(6, 17, 17, 17, mix(PLASMA_LO, PLASMA, pulse))
    # sight with a cyan lens
    rect(9, 7, 12, 8, GUNMETAL)
    rect(9, 7, 12, 7, shade(GUNMETAL, 0.3))
    c.put(13, 7, CYAN_HI)
    c.put(13, 8, CYAN)
    # energy cell, gauge climbs with the frames
    rect(7, 19, 11, 21, CELL)
    lit = 1 + frame % 5
    for k in range(5):
        c.put(7 + k, 20, PLASMA_HI if k < lit - 1 else PLASMA if k < lit else PLASMA_LO)
    # pistol grip, slanted back, and the trigger guard
    for i in range(7):
        x = 14 - (i + 1) // 3
        rect(x, 19 + i, x + 2, 19 + i, shade(GRIP, 0.14) if i % 2 else GRIP)
        c.put(x + 2, 19 + i, shade(GRIP, 0.3))
    rect(16, 19, 18, 19, GUNMETAL_D)
    c.put(18, 20, GUNMETAL_D)
    c.put(17, 20, ARMOR)
    # glowing collar
    rect(19, 10, 19, 19, PLASMA)
    rect(19, 12, 19, 17, mix(PLASMA, PLASMA_HI, pulse))
    # accelerator: plasma chamber with three copper rings
    for x in range(20, 27):
        wave = 0.5 + 0.5 * math.sin(((x - 20) / 3.0 - phase) * math.pi * 2)
        rect(x, 13, x, 17, PLASMA_LO)
        rect(x, 14, x, 16, mix(PLASMA, PLASMA_HI, wave * 0.8))
        c.put(x, 15, mix(PLASMA_HI, (255, 255, 255, 255), wave))
    rect(20, 12, 26, 12, GUNMETAL_D)
    rect(20, 18, 26, 18, GUNMETAL_D)
    for x in (21, 24):
        rect(x, 11, x, 19, COPPER)
        c.put(x, 11, shade(COPPER, 0.35))
        c.put(x, 19, COPPER_D)
    # emitter with vents and the focusing ring
    rect(27, 13, 28, 17, GUNMETAL_D)
    c.put(27, 14, PLASMA)
    c.put(27, 16, PLASMA)
    rect(29, 14, 29, 16, CYAN)
    # forked prongs with cyan tips and the arc between them
    rect(26, 11, 30, 11, GUNMETAL)
    rect(26, 19, 30, 19, GUNMETAL)
    c.put(26, 11, shade(GUNMETAL, 0.3))
    tip = CYAN_HI if frame % 2 == 0 else CYAN
    rect(31, 11, 31, 12, tip)
    rect(31, 18, 31, 19, tip)
    for y in range(13, 18):
        if (y + frame) % 3 != 0:
            c.put(31, y, PLASMA_HI if y == 15 else PLASMA)
    c.outline(OUTLINE)
    return c


def save_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def main():
    model, sheet, used, faces = build()
    save_json(os.path.join(A, 'models/item/3d/item_tool_plasma_launcher.json'), model)
    os.makedirs(os.path.join(A, 'textures/item/3d'), exist_ok=True)
    sheet.save(os.path.join(A, 'textures/item/3d/item_tool_plasma_launcher.png'))
    save_json(os.path.join(A, 'textures/item/3d/item_tool_plasma_launcher.png.mcmeta'),
              {'animation': {'frametime': 2, 'interpolate': True}})
    icon = Canvas(ICON, ICON * FRAMES)
    for frame in range(FRAMES):
        icon.paste(draw_icon(frame), 0, frame * ICON)
    icon.save(os.path.join(A, 'textures/item/item_tool_plasma_launcher.png'))
    save_json(os.path.join(A, 'textures/item/item_tool_plasma_launcher.png.mcmeta'),
              {'animation': {'frametime': 2, 'interpolate': False}})
    save_json(os.path.join(A, 'models/item/item_tool_plasma_launcher.json'), {
        'loader': 'neoforge:separate_transforms',
        'base': {'parent': 'mio_icif:item/3d/item_tool_plasma_launcher'},
        'perspectives': {
            'gui': {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mio_icif:item/item_tool_plasma_launcher'}},
            'fixed': {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mio_icif:item/item_tool_plasma_launcher'}},
        }})
    print('plasma launcher: %d elements, %d faces, atlas use %.0f %%' % (len(E), faces, 100.0 * used / (TEX * TEX)))


if __name__ == '__main__':
    main()
