#!/usr/bin/env python3
"""0.1.11.167: the TESU relay as a pylon three blocks tall, this mod's own design: a full-block base (a plinth, a socket with a
cyan ring and four struts), a square shaft with a cyan light strip on its front and back between two dark rails, and a head with
cyan windows and an emitter on top. The relay block is the base; the two blocks above are the relay column (part=middle, top).
Each part has an unpowered model and a powered one (the cyan parts glow when the relay is linked to a TESU).

Writes the four textures, the six block models, the item model (the whole pylon, scaled down) and the two blockstates.
Running it again writes the same files. 0.1.11.169: a file that differs from what it makes (a hand edit, in Blockbench or a
paint program: the user redrew the frame and light textures) is kept; --force replaces it.
"""
import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
A = ROOT / 'src/main/resources/assets/mio_icif'
TEX = A / 'textures/block/wiring'
NS = 'mio_icif'
T = NS + ':block/wiring/'
FORCE = '--force' in sys.argv


def save_png(img, out):
    """0.1.11.169: writes {@code img} unless the file there differs from it (a hand edit, kept); --force replaces it."""
    out.parent.mkdir(parents=True, exist_ok=True)
    if out.exists() and not FORCE:
        old = Image.open(out).convert('RGBA')
        if old.size != img.size or old.tobytes() != img.convert('RGBA').tobytes():
            print('kept', out.relative_to(ROOT), '(differs from the generated image: hand-edited? --force replaces it)')
            return
    img.save(out)
    print('wrote', out.relative_to(ROOT))


# ---------------------------------------------------------------------------------------------------------------- textures

def frame():
    """Dark graphite metal with a light top-left edge, a dark bottom-right edge, a faint vertical grain and two rivets."""
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            v = 0x3a + ((x * 5 + y // 4) % 3 - 1) * 2
            r, g, b = v, v - 5, v + 14
            if x == 0 or y == 0:
                r, g, b = r + 22, g + 22, b + 24
            elif x == 15 or y == 15:
                r, g, b = r - 14, g - 12, b - 14
            px[x, y] = (r, g, b, 255)
    for x, y in ((3, 3), (12, 12)):
        px[x, y] = (0x74, 0x6c, 0x8a, 255)
        px[x + 1, y + 1] = (0x26, 0x22, 0x31, 255)
    return img


def panel():
    """Light brushed metal in the casing's silver-violet: vertical grain, a bright highlight stripe left of the middle and a
    darker one right of it (the shaft's faces use columns 5 to 11), seams at the edges."""
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            v = 0xc6 + ((x * 7) % 5 - 2) * 2 - (1 if (y + x) % 7 == 0 else 0) * 4
            if x in (6, 7):
                v += 22 if x == 6 else 12
            elif x in (10,):
                v -= 16
            elif x == 9:
                v -= 6
            if x in (0, 15) or y == 0:
                v -= 30
            r, g, b = v, v - 7, v + 12
            px[x, y] = (max(0, min(255, r)), max(0, min(255, g)), max(0, min(255, b)), 255)
    return img


def light(on):
    """The cyan light: brightest down columns 7 and 8 (the strip is two pixels wide), fading out to the sides; every fourth row a
    little dimmer, so the strip reads as segments."""
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    ramp_on = [(0x1e, 0x86, 0xa8), (0x2c, 0xa8, 0xcc), (0x3c, 0xc8, 0xe6), (0x8a, 0xe8, 0xf6), (0xc8, 0xfa, 0xff)]
    ramp_off = [(0x0c, 0x26, 0x30), (0x10, 0x30, 0x3c), (0x14, 0x3c, 0x4a), (0x1a, 0x4c, 0x5c), (0x22, 0x5c, 0x6e)]
    ramp = ramp_on if on else ramp_off
    for y in range(16):
        for x in range(16):
            d = min(abs(x - 7), abs(x - 8))      # 0 on the two middle columns
            i = max(0, 4 - d)
            if y % 4 == 3:
                i = max(0, i - 1)
            px[x, y] = ramp[i] + (255,)
    return img


TEXTURES = {'block_tesu_relay_frame': frame, 'block_tesu_relay_panel': panel,
            'block_tesu_relay_light': lambda: light(True), 'block_tesu_relay_light_off': lambda: light(False)}


# ---------------------------------------------------------------------------------------------------------------- elements

FACES = ('north', 'south', 'east', 'west', 'up', 'down')


def uv(face, frm, to):
    """Blockbench's automatic UV for a face of a box whose y is local to its block (0 to 16)."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    if face in ('north', 'south'):
        u0, u1 = (16 - x1, 16 - x0) if face == 'north' else (x0, x1)
        return [u0, 16 - y1, u1, 16 - y0]
    if face in ('east', 'west'):
        u0, u1 = (16 - z1, 16 - z0) if face == 'east' else (z0, z1)
        return [u0, 16 - y1, u1, 16 - y0]
    return [x0, z0, x1, z1]


def box(frm, to, tex, faces=FACES, glow=False, rot=None, cull=None):
    """A box element; {@code tex} a texture key, {@code glow} for the cyan parts (lit while the relay is powered)."""
    e = {'from': frm, 'to': to, 'faces': {}}
    for f in faces:
        face = {'uv': uv(f, frm, to), 'texture': '#' + tex}
        if cull and f in cull:
            face['cullface'] = f
        e['faces'][f] = face
    if rot:
        e['rotation'] = rot
    if glow:
        e['glow'] = True
    return e


SIDES = ('north', 'south', 'east', 'west')


def base():
    els = [
        box([0, 0, 0], [16, 2, 16], 'frame', cull=('down', 'north', 'south', 'east', 'west')),          # plinth
        box([1, 2, 1], [15, 4, 15], 'casing', faces=SIDES + ('up',)),                                     # plate
        box([5, 4, 5], [11, 16, 11], 'frame', faces=SIDES + ('up',)),                                     # socket
        box([4.5, 9, 4.5], [11.5, 10, 11.5], 'light', faces=SIDES + ('up', 'down'), glow=True),          # ring
        box([4.5, 13, 4.5], [11.5, 14, 11.5], 'frame', faces=SIDES + ('up', 'down')),                     # collar
    ]
    for x, z in ((1.5, 1.5), (12.5, 1.5), (1.5, 12.5), (12.5, 12.5)):                                    # bolts
        els.append(box([x, 4, z], [x + 2, 5, z + 2], 'frame', faces=SIDES + ('up',)))
    # four struts leaning in from the plate to the socket
    els.append(box([7, 4, 2], [9, 13, 4], 'panel', faces=SIDES, rot={'angle': 22.5, 'axis': 'x', 'origin': [8, 4, 3]}))
    els.append(box([7, 4, 12], [9, 13, 14], 'panel', faces=SIDES, rot={'angle': -22.5, 'axis': 'x', 'origin': [8, 4, 13]}))
    els.append(box([2, 4, 7], [4, 13, 9], 'panel', faces=SIDES, rot={'angle': -22.5, 'axis': 'z', 'origin': [3, 4, 8]}))
    els.append(box([12, 4, 7], [14, 13, 9], 'panel', faces=SIDES, rot={'angle': 22.5, 'axis': 'z', 'origin': [13, 4, 8]}))
    return els


def shaft(y0, y1, strip0, strip1):
    """The shaft between y0 and y1 with its rails, and the light strips between strip0 and strip1 on its front and back."""
    return [
        box([5.5, y0, 5.5], [10.5, y1, 10.5], 'panel', faces=SIDES),
        box([7, strip0, 5], [9, strip1, 5.5], 'light', faces=('north', 'east', 'west', 'up', 'down'), glow=True),
        box([7, strip0, 10.5], [9, strip1, 11], 'light', faces=('south', 'east', 'west', 'up', 'down'), glow=True),
        box([4.5, y0, 6.5], [5.5, y1, 9.5], 'frame', faces=('north', 'south', 'west')),
        box([10.5, y0, 6.5], [11.5, y1, 9.5], 'frame', faces=('north', 'south', 'east')),
    ]


def middle():
    return [box([5, 0, 5], [11, 1, 11], 'frame', faces=SIDES + ('down',))] + shaft(0, 16, 1.5, 16)


def top():
    els = shaft(0, 9, 0, 8)
    els += [
        box([4.5, 9, 4.5], [11.5, 10.5, 11.5], 'frame', faces=FACES),                                    # collar
        box([5, 10.5, 5], [11, 13.5, 11], 'panel', faces=SIDES),                                          # head
        box([6.5, 11, 4.8], [9.5, 13, 5], 'light', faces=('north',), glow=True),                          # windows
        box([6.5, 11, 11], [9.5, 13, 11.2], 'light', faces=('south',), glow=True),
        box([11, 11, 6.5], [11.2, 13, 9.5], 'light', faces=('east',), glow=True),
        box([4.8, 11, 6.5], [5, 13, 9.5], 'light', faces=('west',), glow=True),
        box([5.5, 13.5, 5.5], [10.5, 14.5, 10.5], 'frame', faces=FACES),                                 # cap
        box([7, 14.5, 7], [9, 16, 9], 'light', faces=SIDES + ('up',), glow=True),                         # emitter
    ]
    for x, z in ((5.5, 5.5), (9.5, 5.5), (5.5, 9.5), (9.5, 9.5)):                                        # prongs
        els.append(box([x, 14.5, z], [x + 1, 15.5, z + 1], 'frame', faces=SIDES + ('up',)))
    return els


PARTS = {'base': base, 'middle': middle, 'top': top}
# the emitter's middle, in blocks above the relay (the particle stream starts and ends there): 2 + (14.5 + 16) / 2 / 16
EMITTER_Y = 2 + (14.5 + 16) / 2 / 16


def finish(els, on):
    """Turns the glow marks into the light texture (lit or not) and NeoForge's full-bright face data when lit."""
    out = []
    for e in els:
        e = json.loads(json.dumps(e))
        glow = e.pop('glow', False)
        if glow:
            for face in e['faces'].values():
                face['texture'] = '#light'
            if on:
                e['neoforge_data'] = {'block_light': 15, 'sky_light': 15, 'ambient_occlusion': False}
        out.append(e)
    return out


def textures(on):
    return {'frame': T + 'block_tesu_relay_frame', 'panel': T + 'block_tesu_relay_panel', 'casing': T + 'block_tesu_casing',
            'light': T + ('block_tesu_relay_light' if on else 'block_tesu_relay_light_off'), 'particle': T + 'block_tesu_relay_panel'}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists() and not FORCE and json.loads(path.read_text(encoding='utf-8-sig')) != json.loads(json.dumps(data)):
        print('kept', path.relative_to(ROOT), '(differs from the generated model: hand-edited? --force replaces it)')
        return
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
    print('wrote', path.relative_to(ROOT))


def model_name(part, on):
    return 'block_tesu_relay' + ('' if part == 'base' else '_' + part) + ('_on' if on else '')


def main():
    for name, make in TEXTURES.items():
        save_png(make(), TEX / (name + '.png'))
    for part, make in PARTS.items():
        for on in (False, True):
            write(A / 'models/block/wiring' / (model_name(part, on) + '.json'),
                  {'parent': 'minecraft:block/block', 'textures': textures(on), 'elements': finish(make(), on)})
    # the item: the whole pylon (powered), its base at y = -16 so the 48 pixels fit the model's -16 to 32
    whole = []
    for i, part in enumerate(('base', 'middle', 'top')):
        for e in finish(PARTS[part](), True):
            e['from'][1] += i * 16 - 16
            e['to'][1] += i * 16 - 16
            if 'rotation' in e:
                e['rotation']['origin'][1] += i * 16 - 16
            for face in e['faces'].values():
                face.pop('cullface', None)
            whole.append(e)
    display = {
        'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.3, 0.3, 0.3]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.2, 0.2, 0.2]},
        'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.3, 0.3, 0.3]},
        'head': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.25, 0.25, 0.25]},
        'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.25, 0.25, 0.25]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.25, 0.25, 0.25]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.25, 0.25, 0.25]},
    }
    write(A / 'models/item/wiring/block_tesu_relay.json', {'parent': 'minecraft:block/block', 'textures': textures(True),
                                                           'elements': whole, 'display': display})
    write(A / 'blockstates/wiring/block_tesu_relay.json', {'variants': {
        'powered=false': {'model': NS + ':block/wiring/' + model_name('base', False)},
        'powered=true': {'model': NS + ':block/wiring/' + model_name('base', True)}}})
    write(A / 'blockstates/wiring/block_tesu_relay_column.json', {'variants': {
        'part=%s,powered=%s' % (part, str(on).lower()): {'model': NS + ':block/wiring/' + model_name(part, on)}
        for part in ('middle', 'top') for on in (False, True)}})
    print('emitter at', EMITTER_Y, 'blocks above the relay')


if __name__ == '__main__':
    main()
