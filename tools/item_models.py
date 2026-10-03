"""3D item models + textures + 32px icons: Advanced Tachyon Disruptor and Electric Light Generator.
Usage: item_models.py <assets/mio_icif>"""
import json, os, sys, random, subprocess
from PIL import Image, ImageFilter
A = sys.argv[1]
HERE = os.path.dirname(os.path.abspath(__file__))

def clamp(v): return max(0, min(255, int(v)))
def mix(a, b, t): return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))

class Atlas:
    """64x64 texture made of named rectangular regions; each element face maps a whole region."""
    def __init__(self, seed):
        self.im = Image.new('RGBA', (64, 64), (0, 0, 0, 0)); self.px = self.im.load()
        self.r = {}; self.rnd = random.Random(seed); self.cx = 0; self.cy = 0; self.rowh = 0
    def region(self, name, w, h, f):
        if self.cx + w > 64: self.cx = 0; self.cy += self.rowh; self.rowh = 0
        x0, y0 = self.cx, self.cy
        for y in range(h):
            for x in range(w): self.px[x0 + x, y0 + y] = f(x, y, w, h) + (255,)
        self.r[name] = [x0 / 4, y0 / 4, (x0 + w) / 4, (y0 + h) / 4]
        self.cx += w; self.rowh = max(self.rowh, h)
    def plate(self, base, light=(200, 206, 214), dark=(30, 33, 38), noise=4):
        def f(x, y, w, h):
            c = tuple(clamp(v + self.rnd.randint(-noise, noise)) for v in base)
            if y == 0 or x == 0: c = mix(c, light, 0.45)
            if y == h - 1 or x == w - 1: c = mix(c, dark, 0.5)
            return c
        return f

def box(at, name, f, t, side, top=None, bottom=None, front=None, glow=False, rot=None, faces=None):
    fs = {}
    for d in ('north', 'south', 'east', 'west', 'up', 'down'):
        if faces and d not in faces: continue
        reg = {'up': top or side, 'down': bottom or side, 'north': front or side}.get(d, side)
        fs[d] = {'uv': at.r[reg], 'texture': '#0'}
    e = {'name': name, 'from': f, 'to': t, 'faces': fs}
    if glow: e['neoforge_data'] = {'block_light': 15, 'sky_light': 15}; e['shade'] = False
    if rot: e['rotation'] = rot
    return e

def tachyon():
    at = Atlas(11)
    graphite, white, violet, cyan = (46, 48, 58), (214, 218, 226), (170, 90, 255), (90, 230, 255)
    at.region('body', 16, 16, at.plate(graphite))
    def panel(x, y, w, h):
        c = at.plate(white)(x, y, w, h)
        if y in (5, 10): c = mix(c, (120, 124, 134), 0.6)
        if 2 <= x <= 3 and 6 <= y <= 9: c = violet
        return c
    at.region('panel', 16, 16, panel)
    def core(x, y, w, h):
        t = abs(x - (w - 1) / 2) / ((w - 1) / 2 + 0.01)
        c = mix((240, 230, 255), violet, t)
        if y % 4 == 0: c = mix(c, cyan, 0.5)
        return c
    at.region('core', 8, 16, core)
    def ring(x, y, w, h):
        c = at.plate((70, 74, 88))(x, y, w, h)
        if y in (h // 2 - 1, h // 2): c = cyan
        return c
    at.region('ring', 8, 8, ring)
    at.region('glow', 4, 4, lambda x, y, w, h: mix(cyan, (255, 255, 255), 0.35 if (x + y) % 3 else 0.0))
    at.region('vglow', 4, 4, lambda x, y, w, h: mix(violet, (255, 240, 255), 0.3 if (x + y) % 2 else 0.0))
    at.region('grip', 8, 8, lambda x, y, w, h: (28, 30, 34) if y % 2 else (40, 42, 48))
    def fin(x, y, w, h):
        c = at.plate((96, 100, 112), noise=2)(x, y, w, h)
        return c if x % 2 else mix(c, (40, 42, 50), 0.5)
    at.region('fin', 8, 8, fin)
    at.region('hazard', 8, 4, lambda x, y, w, h: (236, 170, 40) if (x + y) % 4 < 2 else (30, 30, 34))
    E = [
        box(at, 'receiver', [5.5, 6.5, 2], [10.5, 11.5, 14], 'panel', 'body', 'body', 'body'),
        box(at, 'cheek_l', [5.25, 7.5, 4], [5.5, 10.5, 12], 'body', faces=('west',)),
        box(at, 'cheek_r', [10.5, 7.5, 4], [10.75, 10.5, 12], 'body', faces=('east',)),
        box(at, 'conduit_l', [5.15, 8.5, 5], [5.25, 9.25, 11], 'vglow', glow=True, faces=('west',)),
        box(at, 'conduit_r', [10.75, 8.5, 5], [10.85, 9.25, 11], 'vglow', glow=True, faces=('east',)),
        box(at, 'core_tube', [6.75, 7.75, -9], [9.25, 10.25, 1.5], 'core', glow=True),
        box(at, 'prong_top', [7.25, 10.75, -14], [8.75, 11.75, -3], 'body', 'panel'),
        box(at, 'prong_bot', [7.25, 6.25, -13], [8.75, 7.25, -3], 'body'),
        box(at, 'prong_tip_t', [7.0, 10.5, -15], [9.0, 12.0, -14], 'ring'),
        box(at, 'prong_tip_b', [7.0, 6.0, -14], [9.0, 7.5, -13], 'ring'),
        box(at, 'crystal', [7.25, 8.25, -11], [8.75, 9.75, -9], 'glow', glow=True),
        box(at, 'arc_emitter', [7.75, 7.25, -12.5], [8.25, 10.75, -12], 'vglow', glow=True),
        box(at, 'nose', [6.5, 7.5, -3], [9.5, 10.5, 2], 'body', 'fin'),
        box(at, 'ammo_cell', [6.5, 4.5, 3], [9.5, 6.5, 9], 'body', bottom='hazard'),
        box(at, 'cell_glow', [6.4, 5.0, 4], [9.6, 5.75, 8], 'glow', glow=True, faces=('east', 'west')),
        box(at, 'grip', [7.0, 1.5, 10], [9.0, 6.5, 12.5], 'grip'),
        box(at, 'guard', [7.5, 4.25, 8.5], [8.5, 5.25, 10], 'body'),
        box(at, 'stock', [7.0, 6.5, 14], [9.0, 10.5, 20], 'body', 'panel'),
        box(at, 'butt', [6.5, 6.0, 20], [9.5, 11.0, 21.25], 'panel'),
        box(at, 'scope', [7.25, 11.5, 6], [8.75, 13.25, 12], 'body'),
        box(at, 'scope_lens', [7.4, 11.65, 5.9], [8.6, 13.1, 6], 'glow', glow=True, faces=('north',)),
    ]
    for i, z in enumerate((-7.5, -5.5, -3.5)):
        E.append(box(at, f'coil_{i}', [6.0, 7.0, z], [10.0, 11.0, z + 1.0], 'ring'))
    for i, z in enumerate((2.75, 4.25, 12.5)):   # heat sinks around the scope
        E.append(box(at, f'sink_{i}', [6.25, 11.5, z], [9.75, 12.25, z + 0.75], 'fin'))
    disp = {"thirdperson_righthand": {"rotation": [80, 0, 0], "translation": [0, 2.5, -1.5], "scale": [0.55, 0.55, 0.55]},
            "thirdperson_lefthand": {"rotation": [80, 0, 0], "translation": [0, 2.5, -1.5], "scale": [0.55, 0.55, 0.55]},
            "firstperson_righthand": {"rotation": [0, 6, 0], "translation": [1.5, 0.5, -1.0], "scale": [0.58, 0.58, 0.58]},
            "firstperson_lefthand": {"rotation": [0, -6, 0], "translation": [-1.5, 0.5, -1.0], "scale": [0.58, 0.58, 0.58]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
            "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
            "gui": {"rotation": [20, 135, 0], "translation": [0, 0, 0], "scale": [0.55, 0.55, 0.55]}}
    return at, E, disp

def lighter():
    at = Atlas(5)
    steel, dark, orange = (150, 156, 166), (44, 47, 54), (236, 140, 30)
    at.region('body', 16, 16, at.plate(steel))
    def head(x, y, w, h):
        c = at.plate((120, 126, 136))(x, y, w, h)
        if x % 4 == 0: c = mix(c, dark, 0.4)
        return c
    at.region('head', 16, 8, head)
    def lens(x, y, w, h):
        d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 / 7.5
        c = mix((255, 255, 240), (255, 214, 120), d)
        if d > 0.92: c = (90, 94, 102)
        return c
    at.region('lens', 16, 16, lens)
    at.region('hazard', 16, 4, lambda x, y, w, h: orange if (x + y) % 4 < 2 else (34, 34, 38))
    at.region('grip', 8, 8, lambda x, y, w, h: (32, 34, 38) if y % 2 else (46, 48, 54))
    at.region('fin', 8, 8, lambda x, y, w, h: mix(steel, (255, 255, 255), 0.25) if y == 0 else mix(steel, dark, 0.2 + 0.3 * (x % 2)))
    at.region('led', 4, 4, lambda x, y, w, h: (80, 255, 120))
    at.region('cap', 8, 8, at.plate(dark, light=(110, 116, 126)))
    E = [
        box(at, 'body', [6, 6, 0], [10, 10, 12], 'body'),
        box(at, 'hazard_band', [5.9, 5.9, 1], [10.1, 10.1, 2], 'hazard', faces=('east', 'west', 'up', 'down')),
        box(at, 'head', [5, 5, -4], [11, 11, 0], 'head', front='lens'),
        box(at, 'lens', [5.5, 5.5, -4.15], [10.5, 10.5, -4], 'lens', glow=True, faces=('north',)),
        box(at, 'bezel', [4.75, 4.75, -3], [11.25, 11.25, -2], 'cap'),
        box(at, 'grip', [7, 1.5, 7.5], [9, 6, 10.5], 'grip'),
        box(at, 'trigger', [7.5, 5, 5.5], [8.5, 6, 7], 'cap'),
        box(at, 'cap', [6.5, 6.5, 12], [9.5, 9.5, 13.5], 'cap'),
        box(at, 'led', [10, 8.5, 6], [10.1, 9.5, 8], 'led', glow=True, faces=('east',)),
        box(at, 'switch', [7.5, 10, 6], [8.5, 10.6, 7.5], 'cap'),
    ]
    for i, z in enumerate((2.75, 4.25)):
        E.append(box(at, f'fin_{i}', [5.5, 6.5, z], [10.5, 9.5, z + 0.75], 'fin'))
    disp = {"thirdperson_righthand": {"rotation": [80, 0, 0], "translation": [0, 3.5, -2], "scale": [0.7, 0.7, 0.7]},
            "thirdperson_lefthand": {"rotation": [80, 0, 0], "translation": [0, 3.5, -2], "scale": [0.7, 0.7, 0.7]},
            "firstperson_righthand": {"rotation": [0, 6, 0], "translation": [1.5, 1.5, -1.0], "scale": [0.7, 0.7, 0.7]},
            "firstperson_lefthand": {"rotation": [0, -6, 0], "translation": [-1.5, 1.5, -1.0], "scale": [0.7, 0.7, 0.7]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
            "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
            "gui": {"rotation": [20, 135, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]}}
    return at, E, disp

for name, fn, yaw, pitch, tilt in (('item_tool_tachyon_disruptor', tachyon, 120, 18, 32), ('item_electric_lighter', lighter, 125, 22, 0)):
    at, E, disp = fn()
    at.im.save(os.path.join(A, 'textures/item/3d', name + '.png'))
    model = {'credit': 'Singularity Iteration (tools/item_models.py)', 'texture_size': [64, 64],
             'textures': {'0': f'mio_icif:item/3d/{name}', 'particle': f'mio_icif:item/3d/{name}'},
             'elements': E, 'display': disp}
    json.dump(model, open(os.path.join(A, 'models/item/3d', name + '.json'), 'w'), indent=1)
    # 32px inventory icon rendered from the model
    big = f'/tmp/{name}_big.png'
    subprocess.run([sys.executable, os.path.join(HERE, 'elemrender.py'), A, 'item/3d/' + name, big, '384', str(yaw), str(pitch)],
                   check=True, capture_output=True)
    im = Image.open(big).rotate(tilt, resample=Image.BICUBIC, expand=True); im = im.crop(im.getbbox()); s = max(im.size) + 8
    c = Image.new('RGBA', (s, s)); c.paste(im, ((s - im.width) // 2, (s - im.height) // 2))
    ic = c.resize((32, 32), Image.BOX)
    ic.putalpha(ic.split()[3].point(lambda v: 255 if v > 110 else 0))
    # brighten a touch and add a 1px dark outline so the silhouette reads in the inventory
    px = ic.load()
    for y in range(32):
        for x in range(32):
            r, g, b, a = px[x, y]
            if a: px[x, y] = (min(255, int(r * 1.15 + 8)), min(255, int(g * 1.15 + 8)), min(255, int(b * 1.15 + 8)), 255)
    out = ic.copy(); op = out.load()
    for y in range(32):
        for x in range(32):
            if px[x, y][3]: continue
            if any(0 <= x + dx < 32 and 0 <= y + dy < 32 and px[x + dx, y + dy][3] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                op[x, y] = (24, 26, 30, 255)
    ic = out
    ic.save(os.path.join(A, 'textures/item', name + '.png'))
    item = {'loader': 'neoforge:separate_transforms', 'base': {'parent': f'mio_icif:item/3d/{name}'},
            'perspectives': {p: {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mio_icif:item/{name}'}} for p in ('gui', 'fixed')}}
    json.dump(item, open(os.path.join(A, 'models/item', name + '.json'), 'w'), indent=2)
print('ok')
