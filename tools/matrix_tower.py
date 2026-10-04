"""Matrix Core Lightning Tower: textures + static block models for the ground and sky-patrol towers.
Usage: matrix_tower.py <assets/mio_icif>"""
import json, os, sys, random
from PIL import Image
A = sys.argv[1]
TEX = os.path.join(A, 'textures/block/producer')
MOD = os.path.join(A, 'models/block/producer')

def clamp(v): return max(0, min(255, int(v)))
def mix(a, b, t): return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))

def texture(accent, on):
    """64x64 atlas; regions documented in REG (pixels)."""
    rnd = random.Random(7)
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0)); px = im.load()
    glow = accent if on else mix(accent, (34, 40, 48), 0.78)
    hot = mix(accent, (255, 255, 255), 0.55) if on else mix(accent, (40, 46, 54), 0.7)
    metal, dark, light = (66, 72, 82), (34, 38, 45), (122, 130, 142)
    def rect(x0, y0, w, h, f):
        for y in range(h):
            for x in range(w): px[x0 + x, y0 + y] = f(x, y, w, h) + (255,)
    def brushed(x, y, w, h, base=metal):
        n = rnd.randint(-5, 5)
        c = tuple(clamp(v + n) for v in base)
        if y == 0 or x == 0: c = mix(c, light, 0.55)
        if y == h - 1 or x == w - 1: c = mix(c, dark, 0.6)
        return c
    # plinth top (0,0) 16x16: dark panel, glowing circuit traces, centre socket
    def panel(x, y, w, h):
        c = brushed(x, y, w, h, (48, 53, 61))
        if x in (1, 14) or y in (1, 14): c = mix(c, dark, 0.5)
        trace = (y == 4 and 2 <= x <= 6) or (x == 4 and 4 <= y <= 11) or (y == 11 and 4 <= x <= 11) \
            or (x == 11 and 4 <= y <= 11) or (y == 4 and 9 <= x <= 13) or (x == 13 and 4 <= y <= 8)
        if trace: c = glow
        if (x, y) in ((2, 4), (13, 8), (4, 11), (11, 4)): c = hot
        if 6 <= x <= 9 and 6 <= y <= 9: c = mix(dark, glow, 0.25 if (x in (6, 9) or y in (6, 9)) else 0.0)
        return c
    rect(0, 0, 16, 16, panel)
    # base side (16,0) 16x8: metal with accent stripe and hazard ticks
    def side(x, y, w, h):
        c = brushed(x, y, w, h)
        if y == 3 or y == 4: c = glow if (x // 2) % 2 == 0 else mix(glow, dark, 0.6)
        return c
    rect(16, 0, 16, 8, side)
    # plinth side (16,8) 16x8
    def pside(x, y, w, h):
        c = brushed(x, y, w, h, (56, 61, 70))
        if 2 <= y <= 5 and x % 4 == 1: c = mix(c, dark, 0.7)   # vents
        return c
    rect(16, 8, 16, 8, pside)
    # pylon (32,0) 8x32: dark composite with a glowing vertical channel
    def pylon(x, y, w, h):
        c = brushed(x, y, w, h, (44, 49, 57))
        if x in (3, 4): c = mix(glow, dark, 0.35 + 0.25 * ((y // 3) % 2))
        if y % 8 == 0: c = mix(c, light, 0.35)
        return c
    rect(32, 0, 8, 32, pylon)
    # coil (40,0) 8x8: copper windings, glowing seam in the middle
    def coil(x, y, w, h):
        c = (176, 104, 54) if y % 2 == 0 else (124, 70, 36)
        if y in (3, 4): c = glow if on else mix(c, glow, 0.4)
        if x == 0: c = mix(c, (255, 220, 180), 0.25)
        return c
    rect(40, 0, 8, 8, coil)
    # cap (40,8) 8x8
    rect(40, 8, 8, 8, lambda x, y, w, h: brushed(x, y, w, h, (86, 93, 104)))
    # core crystal (48,0) 16x16: bright lattice
    def core(x, y, w, h):
        d = max(abs(x - 7.5), abs(y - 7.5)) / 7.5
        c = mix(hot, glow, d)
        if x % 5 == 0 or y % 5 == 0: c = mix(c, (255, 255, 255), 0.35 if on else 0.05)
        return c
    rect(48, 0, 16, 16, core)
    # frame bar (0,16) 16x4
    def bar(x, y, w, h):
        c = brushed(x, y, w, h, (78, 85, 96))
        if y in (1, 2) and x % 3 == 0: c = glow
        return c
    rect(0, 16, 16, 4, bar)
    # electrode / spike (16,16) 4x16
    def spike(x, y, w, h):
        c = mix((190, 198, 210), (90, 96, 108), x / 3)
        if y % 4 == 1: c = mix(c, glow, 0.7)
        return c
    rect(16, 16, 4, 16, spike)
    # conductor arm (24,16) 16x4: glowing conduit
    rect(24, 16, 16, 4, lambda x, y, w, h: mix(glow, dark, 0.15 if y in (1, 2) else 0.55))
    # pedestal top (0,20) 8x8 emitter ring
    def ring(x, y, w, h):
        r = ((x - 3.5) ** 2 + (y - 3.5) ** 2) ** 0.5
        return glow if 1.8 < r < 3.2 else (40, 45, 52)
    rect(0, 20, 8, 8, ring)
    return im

# uv helpers: atlas pixels -> model uv (64px atlas = 16 uv units)
def uv(x, y, w, h): return [x / 4, y / 4, (x + w) / 4, (y + h) / 4]
R = {'panel': uv(0, 0, 16, 16), 'side': uv(16, 0, 16, 8), 'pside': uv(16, 8, 16, 8), 'pylon': uv(32, 0, 8, 32),
     'coil': uv(40, 0, 8, 8), 'cap': uv(40, 8, 8, 8), 'core': uv(48, 0, 16, 16), 'bar': uv(0, 16, 16, 4),
     'spike': uv(16, 16, 4, 16), 'arm': uv(24, 16, 16, 4), 'ring': uv(0, 20, 8, 8), 'dark': uv(1, 7, 1, 1)}

def box(name, f, t, sides, top=None, bottom=None, glow=False, rot=None, skip=()):
    faces = {}
    for d in ('north', 'south', 'east', 'west'):
        if d not in skip: faces[d] = {'uv': R[sides], 'texture': '#0'}
    if 'up' not in skip: faces['up'] = {'uv': R[top or sides], 'texture': '#0'}
    if 'down' not in skip: faces['down'] = {'uv': R[bottom or 'dark'], 'texture': '#0'}
    e = {'name': name, 'from': f, 'to': t, 'faces': faces}
    if glow: e['neoforge_data'] = {'block_light': 15, 'sky_light': 15}; e['shade'] = False
    if rot: e['rotation'] = rot
    return e

def elements(sky, on):
    els = [box('base', [0, 0, 0], [16, 1.5, 16], 'side', 'panel'),
           box('plinth', [1.5, 1.5, 1.5], [14.5, 3, 14.5], 'pside', 'panel', skip=('down',))]
    corners = [(1.5, 1.5), (12, 1.5), (1.5, 12), (12, 12)]
    for i, (x, z) in enumerate(corners):
        els.append(box(f'pylon_{i}', [x, 3, z], [x + 2.5, 14, z + 2.5], 'pylon', 'cap', skip=('down', 'up')))
        for j, y in enumerate((5, 8.5)):
            els.append(box(f'coil_{i}_{j}', [x - 0.5, y, z - 0.5], [x + 3, y + 1.5, z + 3], 'coil', 'cap', 'cap', glow=on))
        els.append(box(f'cap_{i}', [x - 0.5, 14, z - 0.5], [x + 3, 15, z + 3], 'cap'))
    # matrix frame between the pylon caps
    els += [box('frame_n', [4.5, 14.2, 2.25], [11.5, 14.8, 3.25], 'bar', skip=('east', 'west')),
            box('frame_s', [4.5, 14.2, 12.75], [11.5, 14.8, 13.75], 'bar', skip=('east', 'west')),
            box('frame_w', [2.25, 14.2, 4.5], [3.25, 14.8, 11.5], 'bar', skip=('north', 'south')),
            box('frame_e', [12.75, 14.2, 4.5], [13.75, 14.8, 11.5], 'bar', skip=('north', 'south'))]
    # conductor arms from each pylon toward the core (two diagonals, rotated 45 degrees)
    for k, a in enumerate((45, -45)):
        rot = {'angle': a, 'axis': 'y', 'origin': [8, 8, 8]}
        els.append(box(f'arm_{k}a', [7.6, 7.6, 2.4], [8.4, 8.4, 6.2], 'arm', glow=on, rot=rot))
        els.append(box(f'arm_{k}b', [7.6, 7.6, 9.8], [8.4, 8.4, 13.6], 'arm', glow=on, rot=rot))
    # core: pedestal, emitter ring, floating crystal, upper electrode
    els += [box('pedestal', [5.5, 3, 5.5], [10.5, 4.5, 10.5], 'pside', 'ring', skip=('down',)),
            box('emitter', [6.5, 4.5, 6.5], [9.5, 5.25, 9.5], 'bar', 'ring', skip=('down',)),
            box('core', [6.25, 6, 6.25], [9.75, 9.5, 9.75], 'core', glow=on),
            box('electrode', [6.5, 10.5, 6.5], [9.5, 11.25, 9.5], 'bar', 'cap', 'ring'),
            box('spike', [7.5, 11.25, 7.5], [8.5, 16 if sky else 15.5, 8.5], 'spike', 'cap', skip=('down',))]
    if sky:  # sky patrol: upward antenna fins around the spike
        for i, (x, z) in enumerate(((5.5, 7.75), (10, 7.75), (7.75, 5.5), (7.75, 10))):
            els.append(box(f'fin_{i}', [x, 11.25, z], [x + 0.5, 13.5, z + 0.5], 'spike', 'cap', skip=('down',)))
    return els

for name, accent, sky in (('block_laser_defense_tower', (70, 214, 255), False),
                          ('block_sky_patrol_laser_tower', (196, 120, 255), True)):
    for on in (True, False):
        suffix = '' if on else '_off'
        texture(accent, on).save(os.path.join(TEX, name + suffix + '.png'))
        model = {'credit': 'Singularity Iteration - Matrix Core Lightning Tower (tools/matrix_tower.py)',
                 'parent': 'block/block', 'texture_size': [64, 64],
                 'textures': {'0': f'mio_icif:block/producer/{name}{suffix}', 'particle': f'mio_icif:block/producer/{name}{suffix}'},
                 'elements': elements(sky, on)}
        json.dump(model, open(os.path.join(MOD, name + suffix + '.json'), 'w'), indent=1)
    for part in ('_base', '_base_off', '_pitch', '_pitch_off', '_yaw', '_yaw_off'):
        p = os.path.join(MOD, name + part + '.json')
        if os.path.exists(p): os.remove(p)
    bs = {'variants': {}}
    for on in (True, False):
        for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            v = {'model': f'mio_icif:block/producer/{name}{"" if on else "_off"}'}
            if y: v['y'] = y
            bs['variants'][f'active={str(on).lower()},facing={f}'] = v
    json.dump(bs, open(os.path.join(A, 'blockstates/producer', name + '.json'), 'w'), indent=2)
print('ok')
