#!/usr/bin/env python3
"""0.1.8.3 Blockbench models of the upper-tier armor pieces.

Writes Blockbench projects (modded_entity, box UV, embedded 128x128 texture) to
tools/models/armor/<name>.bbmodel. They open and edit in Blockbench like the hand-made nano and
quantum suits; tools/gen_suit_armor.py then bakes them into armor_models/<name>.json and
textures/models/armor/<name>.png.

Pieces and design ideas:
  advanced_quantum_chestplate  quantum plating with gold trim, twin booster nozzles on the back,
                               a reactor core on the chest, vented shoulders
  heavy_quantum_chestplate     the gravity armor: heavy gunmetal plates, an anti-gravity core in
                               a diamond ring on the chest, a gyro ring on the back, graviton pads
  solar_helmet                 work helmet with one raised solar panel
  advanced_solar_helmet        sleek helmet, framed 2x2 panel on top and folded side panels
  hybrid_solar_helmet          carbon helmet, solar wings deployed to both sides, glowing visor
  ultimate_solar_helmet        crowned array: top panel, raised wings, a rear fan and a sun core

Coordinates are Blockbench units (y up, the player stands on y = 0, the front is -z). Every cube
sits at least 0.4 units outside the skin (and the 0.25 sleeve layer), so the armor never shares a
plane with the body. Group names follow gen_suit_armor.py: "胸甲主体" -> body, "护手" ->
gauntlets (arm or hand), anything else of a chestplate -> the arm on its side; helmet -> head.

Usage: suit_models_183.py <repo root> [preview dir]
"""
import base64
import json
import math
import os
import random
import struct
import sys
import uuid
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixart import Canvas, hexc, mix, shade, sheet  # noqa: E402

TEX = 128
NS = uuid.UUID('6b1f3c4e-0183-4a8f-9a51-5f0c2d8e7a10')


# ------------------------------------------------------------------ materials
def ramp(hex_base, lo=-0.5, hi=0.4, n=5):
    b = hexc(hex_base)
    return [shade(b, lo + (hi - lo) * i / (n - 1)) for i in range(n)]


class Mat:
    """A surface: kind (plate, trim, glow, solar, vent, nozzle, core, visor, dark) and a colour ramp."""

    def __init__(self, kind, base, accent=None, glow=None):
        self.kind = kind
        self.r = ramp(base)
        self.accent = hexc(accent) if accent else None
        self.glow = hexc(glow) if glow else None


FACE_LIGHT = {'top': 0.18, 'north': 0.0, 'south': -0.12, 'west': -0.06, 'east': -0.1, 'bottom': -0.32}


def paint_face(c, x0, y0, w, h, mat, face, rng):
    """Paints one box-UV face rectangle (integer pixels)."""
    if w <= 0 or h <= 0:
        return
    k = FACE_LIGHT[face]
    r = mat.r
    for y in range(h):
        for x in range(w):
            edge_tl = x == 0 or y == 0
            edge_br = x == w - 1 or y == h - 1
            col = r[2]
            if mat.kind in ('plate', 'trim', 'dark'):
                col = r[3] if edge_tl else r[1] if edge_br else r[2]
                if mat.kind == 'plate' and w >= 4 and h >= 4 and 0 < x < w - 1 and 0 < y < h - 1:
                    if (x + y) % 7 == 0 and rng.random() < 0.3:
                        col = r[3]
                if mat.kind == 'trim' and y == 1 and h > 2:
                    col = r[4]
                col = shade(col, k + (rng.random() - 0.5) * 0.04)
            elif mat.kind == 'glow':
                d = min(x, y, w - 1 - x, h - 1 - y)
                col = mat.glow if d >= 1 or min(w, h) <= 2 else shade(mat.glow, -0.25)
                if d >= 1 and (x + y) % 3 == 0:
                    col = shade(mat.glow, 0.4)
            elif mat.kind == 'solar':
                if face in ('top', 'north', 'south') and w >= 3 and h >= 3:
                    frame = edge_tl or edge_br
                    grid = (x % 4 == 0 or y % 4 == 0) and not frame
                    cell = mix(hexc('1a3f9e'), hexc('0b1a44'), (x + y) / (w + h))
                    if (x - y) % 11 in (0, 1):
                        cell = shade(cell, 0.3)                       # sky reflection
                    col = r[3] if frame else hexc('3a5a8c') if grid else cell
                else:
                    col = r[1] if not edge_tl else r[3]
                    col = shade(col, k)
            elif mat.kind == 'vent':
                col = r[0] if y % 2 == 0 else r[2]
                col = shade(col, k)
            elif mat.kind == 'nozzle':
                if face == 'bottom' and w >= 3 and h >= 3:
                    cx, cy = (w - 1) / 2, (h - 1) / 2
                    d = math.hypot((x - cx) / max(cx, 1), (y - cy) / max(cy, 1))
                    col = mat.glow if d < 0.55 else shade(mat.glow, -0.3) if d < 0.8 else r[0]
                    if d < 0.25:
                        col = shade(mat.glow, 0.55)
                else:
                    col = r[1] if y % 3 else r[0]
                    if y == 0 or x == 0:
                        col = r[2]
                    col = shade(col, k)
            elif mat.kind == 'core':
                cx, cy = (w - 1) / 2, (h - 1) / 2
                d = math.hypot(x - cx, y - cy) / max(1.0, min(w, h) / 2)
                col = shade(mat.glow, 0.6) if d < 0.35 else mat.glow if d < 0.75 else shade(mat.glow, -0.35)
            elif mat.kind == 'visor':
                t = y / max(1, h - 1)
                col = mix(mat.glow or hexc('3ac8ff'), hexc('061018'), 0.35 + 0.5 * t)
                if y == 1 and x % 4 != 3:
                    col = shade(col, 0.45)
            c.put(x0 + x, y0 + y, col)
    # accent pin-stripe on plates (light line one pixel from the bottom)
    if mat.kind == 'plate' and mat.accent is not None and face in ('north', 'south', 'west', 'east') and h >= 4 and w >= 3:
        for x in range(1, w - 1):
            c.put(x0 + x, y0 + h - 2, mat.accent)


# ------------------------------------------------------------------ model builder
class Model:
    def __init__(self, name):
        self.name = name
        self.cubes = []          # (name, frm, to, mat, group, rotation, origin)
        self.groups = []         # ordered group names
        self.rng = random.Random(name)

    def box(self, group, frm, to, mat, name='cube', rotation=None, origin=None):
        if group not in self.groups:
            self.groups.append(group)
        self.cubes.append((name, list(frm), list(to), mat, group, rotation, origin))

    def mirror(self, group, frm, to, mat, name='cube', rotation=None, origin=None):
        """Adds the cube and its copy mirrored across x = 0 (rotation about y and z negated)."""
        self.box(group, frm, to, mat, name, rotation, origin)
        mfrm = [-to[0], frm[1], frm[2]]
        mto = [-frm[0], to[1], to[2]]
        mrot = [rotation[0], -rotation[1], -rotation[2]] if rotation else None
        morg = [-origin[0], origin[1], origin[2]] if origin else None
        self.box(group, mfrm, mto, mat, name, mrot, morg)

    def uid(self, *parts):
        return str(uuid.uuid5(NS, self.name + '/' + '/'.join(str(p) for p in parts)))

    def build(self):
        """Packs the box UV islands, paints the texture and returns (bbmodel dict, Canvas)."""
        tex = Canvas(TEX, TEX)
        islands = []
        for i, (_, frm, to, mat, _, _, _) in enumerate(self.cubes):
            w, h, d = (int(math.ceil(abs(to[k] - frm[k]) - 1e-6)) for k in range(3))
            islands.append((i, max(1, w), max(1, h), max(1, d)))
        # shelf packing, tallest first
        order = sorted(islands, key=lambda t: -(t[3] + t[2]))
        x = y = shelf = 0
        uv = {}
        for i, w, h, d in order:
            iw, ih = 2 * (w + d), d + h
            if x + iw > TEX:
                x, y, shelf = 0, y + shelf, 0
            if y + ih > TEX:
                raise SystemExit('texture full for ' + self.name)
            uv[i] = (x, y)
            x += iw
            shelf = max(shelf, ih)
        for i, w, h, d in islands:
            u0, v0 = uv[i]
            mat = self.cubes[i][3]
            paint_face(tex, u0 + d, v0, w, d, mat, 'top', self.rng)
            paint_face(tex, u0 + d + w, v0, w, d, mat, 'bottom', self.rng)
            paint_face(tex, u0, v0 + d, d, h, mat, 'west', self.rng)
            paint_face(tex, u0 + d, v0 + d, w, h, mat, 'north', self.rng)
            paint_face(tex, u0 + d + w, v0 + d, d, h, mat, 'east', self.rng)
            paint_face(tex, u0 + d + w + d, v0 + d, w, h, mat, 'south', self.rng)
        elements, group_children = [], {g: [] for g in self.groups}
        for i, (name, frm, to, mat, group, rotation, origin) in enumerate(self.cubes):
            w, h, d = (max(1, int(math.ceil(abs(to[k] - frm[k]) - 1e-6))) for k in range(3))
            u0, v0 = uv[i]
            e_uuid = self.uid('e', i)
            org = origin or [(frm[k] + to[k]) / 2 for k in range(3)]
            el = {
                'name': name, 'box_uv': True, 'render_order': 'default', 'locked': False,
                'export': True, 'scope': 0, 'allow_mirror_modeling': True,
                'from': frm, 'to': to, 'autouv': 0, 'color': i % 8, 'origin': org,
                'uv_offset': [u0, v0],
                'faces': {
                    'north': {'uv': [u0 + d, v0 + d, u0 + d + w, v0 + d + h], 'texture': 0},
                    'east': {'uv': [u0, v0 + d, u0 + d, v0 + d + h], 'texture': 0},
                    'south': {'uv': [u0 + d + w + d, v0 + d, u0 + 2 * (d + w), v0 + d + h], 'texture': 0},
                    'west': {'uv': [u0 + d + w, v0 + d, u0 + d + w + d, v0 + d + h], 'texture': 0},
                    'up': {'uv': [u0 + d + w, v0 + d, u0 + d, v0], 'texture': 0},
                    'down': {'uv': [u0 + d + 2 * w, v0, u0 + d + w, v0 + d], 'texture': 0},
                },
                'type': 'cube', 'uuid': e_uuid,
            }
            if rotation:
                el['rotation'] = rotation
            elements.append(el)
            group_children[group].append(e_uuid)
        groups, outliner = [], []
        for g in self.groups:
            g_uuid = self.uid('g', g)
            groups.append({'name': g, 'uuid': g_uuid, 'export': True, 'locked': False, 'scope': 0,
                           'selected': False, 'visibility': True, 'origin': [0, 0, 0], 'rotation': [0, 0, 0],
                           'color': 0, 'children': [], 'reset': False, 'shade': True, 'mirror_uv': False,
                           'autouv': 0, 'isOpen': True, 'primary_selected': False})
            outliner.append({'uuid': g_uuid, 'isOpen': True, 'children': group_children[g]})
        png = png_bytes(tex)
        bb = {
            'meta': {'format_version': '5.0', 'model_format': 'modded_entity', 'box_uv': True},
            'name': self.name, 'model_identifier': '', 'modded_entity_entity_class': '',
            'modded_entity_version': '1.17', 'modded_entity_flip_y': True, 'visible_box': [1, 1, 0],
            'variable_placeholders': '', 'variable_placeholder_buttons': [], 'timeline_setups': [],
            'unhandled_root_fields': {}, 'resolution': {'width': TEX, 'height': TEX},
            'elements': elements, 'groups': groups, 'outliner': outliner,
            'textures': [{
                'name': self.name + '.png', 'path': '', 'folder': 'block', 'namespace': '', 'id': '0',
                'group': '', 'scope': 0, 'width': TEX, 'height': TEX, 'uv_width': TEX, 'uv_height': TEX,
                'particle': False, 'use_as_default': False, 'layers_enabled': False, 'sync_to_project': '',
                'file_format': 'png', 'render_mode': 'default', 'render_sides': 'auto', 'wrap_mode': 'limited',
                'pbr_channel': 'color', 'fps': 7, 'frame_time': 1, 'frame_order_type': 'loop',
                'frame_order': '', 'frame_interpolate': False, 'visible': True, 'internal': True,
                'saved': False, 'uuid': self.uid('t'),
                'source': 'data:image/png;base64,' + base64.b64encode(png).decode('ascii'),
            }],
        }
        return bb, tex


def png_bytes(c):
    raw = bytearray()
    for y in range(c.h):
        raw.append(0)
        for x in range(c.w):
            raw.extend(bytes(c.px[y * c.w + x]))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', c.w, c.h, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b''))


BODY, SHOULDER, GAUNTLET, HEAD = '胸甲主体', '肩甲', '护手', '头盔'


# ------------------------------------------------------------------ chestplates
def advanced_quantum_chestplate():
    m = Model('advanced_quantum_chestplate')
    white = Mat('plate', 'd6dde8', accent='3ab4ff')
    white_plain = Mat('plate', 'd6dde8')
    graphite = Mat('dark', '3a404c')
    gold = Mat('trim', 'e0b04a')
    cyan = Mat('glow', '3ab4ff', glow='4cc4ff')
    nozzle = Mat('nozzle', '2a2f38', glow='7fdcff')
    vent = Mat('vent', '4a5260')
    # torso shell and plates
    m.box(BODY, [-4.6, 11.6, -2.7], [4.6, 24.4, 2.7], graphite, 'undersuit')
    m.box(BODY, [-4.2, 17.0, -3.7], [4.2, 24.0, -2.7], white, 'chest_plate')
    m.mirror(BODY, [0.6, 12.6, -3.4], [3.8, 16.6, -2.7], white_plain, 'abdomen')
    m.box(BODY, [-0.6, 12.4, -3.6], [0.6, 16.8, -2.7], gold, 'abdomen_spine')
    # reactor core with a gold frame
    m.box(BODY, [-1.0, 19.0, -4.1], [1.0, 21.0, -3.7], cyan, 'core')
    m.box(BODY, [-2.0, 21.0, -4.3], [2.0, 22.0, -3.7], gold, 'core_frame_top')
    m.box(BODY, [-2.0, 18.0, -4.3], [2.0, 19.0, -3.7], gold, 'core_frame_bottom')
    m.mirror(BODY, [1.0, 19.0, -4.3], [2.0, 21.0, -3.7], gold, 'core_frame_side')
    m.mirror(BODY, [2.6, 17.6, -4.0], [3.6, 23.0, -3.7], cyan, 'chest_light')
    m.box(BODY, [-4.8, 24.0, -3.2], [4.8, 25.2, 3.2], white_plain, 'collar')
    # back: booster pack with twin nozzles and a gold spine fin
    m.box(BODY, [-3.6, 14.0, 2.7], [3.6, 23.0, 4.6], graphite, 'pack')
    m.mirror(BODY, [1.0, 13.0, 4.4], [4.2, 21.0, 7.4], white, 'booster')
    m.mirror(BODY, [1.3, 10.4, 4.7], [3.9, 13.0, 7.1], nozzle, 'nozzle')
    m.mirror(BODY, [1.4, 21.0, 4.8], [3.8, 22.0, 7.0], vent, 'intake')
    m.box(BODY, [-0.5, 15.0, 4.6], [0.5, 24.0, 8.0], gold, 'spine_fin')
    m.mirror(BODY, [4.2, 16.0, 4.8], [7.2, 17.0, 6.6], white_plain, 'stabilizer', rotation=[0, 0, 22.5], origin=[4.2, 16.5, 5.7])
    # shoulders (arms)
    m.mirror(SHOULDER, [3.6, 21.0, -3.0], [9.2, 25.0, 3.0], white, 'shoulder')
    m.mirror(SHOULDER, [9.2, 22.0, -2.0], [9.8, 24.4, 2.0], cyan, 'shoulder_vent_light')
    m.mirror(SHOULDER, [5.0, 25.0, -2.4], [8.6, 25.8, 2.4], vent, 'shoulder_vent')
    m.mirror(SHOULDER, [3.6, 16.6, -2.6], [8.6, 21.0, 2.6], graphite, 'upper_arm')
    # gauntlets: forearm (arm) and knuckle guard (hand cover)
    m.mirror(GAUNTLET, [3.4, 13.2, -2.8], [8.8, 17.4, 2.8], white, 'forearm')
    m.mirror(GAUNTLET, [8.8, 14.0, -1.6], [9.4, 16.8, 1.6], cyan, 'forearm_light')
    m.mirror(GAUNTLET, [3.6, 11.6, -2.6], [8.6, 13.2, 2.6], graphite, 'knuckles')
    return m


def heavy_quantum_chestplate():
    m = Model('heavy_quantum_chestplate')
    gun = Mat('plate', '626a78', accent='b06cff')
    gun_plain = Mat('plate', '626a78')
    dark = Mat('dark', '2c303a')
    silver = Mat('trim', 'b9c2cf')
    violet = Mat('glow', 'b06cff', glow='c08aff')
    core = Mat('core', '3a1a6a', glow='d4a8ff')
    vent = Mat('vent', '3a3f4a')
    # heavy torso
    m.box(BODY, [-4.8, 11.6, -2.8], [4.8, 24.4, 2.8], dark, 'undersuit')
    m.box(BODY, [-4.6, 15.0, -4.0], [4.6, 24.2, -2.8], gun, 'breastplate')
    m.box(BODY, [-4.2, 11.8, -3.6], [4.2, 15.0, -2.8], gun_plain, 'belt_plate')
    m.mirror(BODY, [3.0, 12.4, -3.9], [4.0, 14.4, -3.6], violet, 'belt_light')
    # anti-gravity core: a diamond ring (rotated 45 deg) around a glowing core
    ring = dict(rotation=[0, 0, 45], origin=[0, 20, -4.4])
    m.box(BODY, [-2.2, 21.4, -4.8], [2.2, 22.2, -4.0], silver, 'ring_top', **ring)
    m.box(BODY, [-2.2, 17.8, -4.8], [2.2, 18.6, -4.0], silver, 'ring_bottom', **ring)
    m.box(BODY, [-2.2, 18.6, -4.8], [-1.4, 21.4, -4.0], silver, 'ring_right', **ring)
    m.box(BODY, [1.4, 18.6, -4.8], [2.2, 21.4, -4.0], silver, 'ring_left', **ring)
    m.box(BODY, [-1.2, 18.8, -4.6], [1.2, 21.2, -4.0], core, 'gravity_core')
    m.mirror(BODY, [2.8, 16.0, -4.3], [4.2, 23.6, -4.0], vent, 'chest_vent')
    m.box(BODY, [-5.0, 24.0, -3.4], [5.0, 25.4, 3.4], gun_plain, 'gorget')
    # back: gyro ring standing on a heavy pack
    m.box(BODY, [-4.0, 13.0, 2.8], [4.0, 23.6, 5.0], gun_plain, 'pack')
    m.box(BODY, [-3.6, 22.6, 5.0], [3.6, 23.6, 6.0], silver, 'gyro_top')
    m.box(BODY, [-3.6, 13.6, 5.0], [3.6, 14.6, 6.0], silver, 'gyro_bottom')
    m.mirror(BODY, [2.6, 14.6, 5.0], [3.6, 22.6, 6.0], silver, 'gyro_side')
    m.box(BODY, [-1.6, 16.6, 5.0], [1.6, 20.6, 5.8], core, 'gyro_core')
    m.mirror(BODY, [0.4, 18.2, 5.0], [2.6, 19.0, 5.6], violet, 'gyro_spoke')
    # massive shoulders with graviton pads
    m.mirror(SHOULDER, [3.8, 20.4, -3.4], [9.8, 25.4, 3.4], gun, 'pauldron')
    m.mirror(SHOULDER, [5.0, 25.4, -2.6], [8.8, 26.0, 2.6], violet, 'graviton_pad')
    m.mirror(SHOULDER, [9.8, 21.4, -2.6], [10.4, 24.4, 2.6], silver, 'pauldron_rim')
    m.mirror(SHOULDER, [3.6, 16.4, -2.7], [8.8, 20.4, 2.7], dark, 'upper_arm')
    # gauntlets
    m.mirror(GAUNTLET, [3.4, 13.0, -3.0], [9.0, 17.2, 3.0], gun, 'vambrace')
    m.mirror(GAUNTLET, [9.0, 13.8, -1.4], [9.6, 16.4, 1.4], violet, 'vambrace_light')
    m.mirror(GAUNTLET, [3.6, 11.6, -2.7], [8.7, 13.0, 2.7], dark, 'knuckles')
    return m


# ------------------------------------------------------------------ helmets
def helmet_shell(m, plate, plate_plain, visor_mat, open_face=False):
    """Common helmet shell around the head box (x, z -4..4, y 24..32)."""
    m.box(HEAD, [-4.6, 31.4, -4.6], [4.6, 32.6, 4.6], plate, 'crown')
    m.box(HEAD, [-4.6, 24.6, 3.4], [4.6, 31.4, 4.6], plate_plain, 'back')
    m.mirror(HEAD, [3.4, 24.6, -4.6], [4.6, 31.4, 3.4], plate, 'side')
    m.box(HEAD, [-4.6, 29.8, -4.8], [4.6, 31.4, -4.4], plate_plain, 'brow')
    m.box(HEAD, [-3.4, 27.2, -4.9], [3.4, 29.8, -4.5], visor_mat, 'visor')
    if not open_face:
        m.mirror(HEAD, [2.2, 24.6, -4.8], [3.4, 27.2, -4.4], plate_plain, 'cheek')


def solar_helmet():
    m = Model('solar_helmet')
    iron = Mat('plate', '9aa3ad', accent='e0b04a')
    iron_plain = Mat('plate', '9aa3ad')
    visor = Mat('visor', '1a2a3a', glow='4a8ac0')
    solar = Mat('solar', '4a5260')
    dark = Mat('dark', '3a3f48')
    helmet_shell(m, iron, iron_plain, visor)
    m.box(HEAD, [-0.8, 32.6, -0.8], [0.8, 33.6, 0.8], dark, 'mount')
    m.box(HEAD, [-4.0, 33.6, -4.0], [4.0, 34.6, 4.0], solar, 'panel')
    m.mirror(HEAD, [4.6, 28.0, 0.0], [5.2, 29.0, 1.0], dark, 'cable_port')
    return m


def advanced_solar_helmet():
    m = Model('advanced_solar_helmet')
    white = Mat('plate', 'dfe5ee', accent='3a8aff')
    white_plain = Mat('plate', 'dfe5ee')
    visor = Mat('visor', '0a1830', glow='3a8aff')
    solar = Mat('solar', '5a6472')
    trim = Mat('trim', 'b9c2cf')
    helmet_shell(m, white, white_plain, visor)
    m.box(HEAD, [-1.0, 32.6, -1.0], [1.0, 33.4, 1.0], trim, 'mount')
    m.box(HEAD, [-4.6, 33.4, -4.6], [4.6, 34.4, 4.6], solar, 'top_panel')
    m.box(HEAD, [-4.8, 34.4, -4.8], [4.8, 34.8, -4.4], trim, 'panel_frame_front')
    m.box(HEAD, [-4.8, 34.4, 4.4], [4.8, 34.8, 4.8], trim, 'panel_frame_back')
    m.mirror(HEAD, [4.6, 26.6, -2.6], [5.4, 31.0, 3.4], solar, 'folded_panel')
    m.box(HEAD, [3.0, 32.6, 2.6], [3.6, 36.0, 3.2], trim, 'antenna')
    return m


def hybrid_solar_helmet():
    m = Model('hybrid_solar_helmet')
    carbon = Mat('plate', '3c424e', accent='4cffc0')
    carbon_plain = Mat('plate', '3c424e')
    visor = Mat('visor', '06201a', glow='4cffc0')
    solar = Mat('solar', '2c323c')
    glow = Mat('glow', '4cffc0', glow='5affc8')
    trim = Mat('trim', '9aa6b4')
    helmet_shell(m, carbon, carbon_plain, visor)
    m.box(HEAD, [-3.6, 32.6, -3.6], [3.6, 33.6, 3.6], solar, 'top_panel')
    m.box(HEAD, [-0.4, 32.6, -4.7], [0.4, 33.0, 4.7], glow, 'crest_light')
    # solar wings deployed to both sides on hinges at the crown edges
    m.mirror(HEAD, [4.6, 31.0, -3.6], [11.0, 32.0, 3.6], solar, 'wing',
             rotation=[0, 0, 22.5], origin=[4.6, 31.5, 0])
    m.mirror(HEAD, [4.4, 30.6, -1.0], [5.4, 32.4, 1.0], trim, 'hinge')
    m.mirror(HEAD, [4.6, 27.6, -4.0], [4.9, 28.4, 3.0], glow, 'side_vein')
    return m


def ultimate_solar_helmet():
    m = Model('ultimate_solar_helmet')
    white = Mat('plate', 'eef1f6', accent='ffb02e')
    white_plain = Mat('plate', 'eef1f6')
    gold = Mat('trim', 'e8b84a')
    visor = Mat('visor', '2a1a06', glow='ffc24a')
    solar = Mat('solar', '6a5a3a')
    sun = Mat('core', '6a3a06', glow='ffd36a')
    helmet_shell(m, white, white_plain, visor)
    m.box(HEAD, [-1.0, 30.0, -5.2], [1.0, 32.0, -4.6], sun, 'sun_core')
    m.mirror(HEAD, [1.0, 30.4, -5.0], [3.0, 31.6, -4.6], gold, 'brow_trim')
    # crown panel and raised side wings
    m.box(HEAD, [-4.2, 32.6, -4.2], [4.2, 33.6, 4.2], solar, 'top_panel')
    m.box(HEAD, [-4.6, 33.6, -4.6], [4.6, 34.0, -4.2], gold, 'crown_front')
    m.mirror(HEAD, [4.6, 31.6, -3.8], [12.0, 32.6, 3.8], solar, 'wing',
             rotation=[0, 0, 45], origin=[4.6, 32.1, 0])
    m.mirror(HEAD, [4.4, 31.2, -1.2], [5.6, 33.0, 1.2], gold, 'wing_hinge')
    # rear fan: three panels behind the head, spread upward
    m.box(HEAD, [-2.0, 30.0, 4.6], [2.0, 37.0, 5.6], solar, 'fan_centre',
          rotation=[-22.5, 0, 0], origin=[0, 30.0, 5.1])
    m.mirror(HEAD, [2.0, 29.0, 4.6], [6.0, 35.0, 5.6], solar, 'fan_side',
             rotation=[-22.5, 0, -22.5], origin=[2.0, 29.0, 5.1])
    m.box(HEAD, [-2.6, 28.6, 4.6], [2.6, 30.0, 5.8], gold, 'fan_hub')
    return m


MODELS = [advanced_quantum_chestplate, heavy_quantum_chestplate, solar_helmet,
          advanced_solar_helmet, hybrid_solar_helmet, ultimate_solar_helmet]


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else '.'
    preview = sys.argv[2] if len(sys.argv) > 2 else None
    out = os.path.join(root, 'tools/models/armor')
    os.makedirs(out, exist_ok=True)
    texes = []
    for fn in MODELS:
        m = fn()
        bb, tex = m.build()
        with open(os.path.join(out, m.name + '.bbmodel'), 'w', encoding='utf-8') as f:
            json.dump(bb, f, ensure_ascii=False, indent=1)
        texes.append(tex)
        print(m.name, len(m.cubes), 'cubes')
    if preview:
        os.makedirs(preview, exist_ok=True)
        sheet(texes, 3, 2).save(os.path.join(preview, 'suit_model_textures_183.png'))


if __name__ == '__main__':
    main()
