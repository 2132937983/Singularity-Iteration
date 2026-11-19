#!/usr/bin/env python3
"""0.1.11.89: textures of the fusion reactor (tokamak) blocks, 16x16, in textures/block/fusion/.

Drawn after the mod's own textures:
  * the frame (dark outline, light top-left bevel, dark bottom-right bevel) and the port flange of the Steam Tunnel
    casing and ports (generator/block_steam_tunnel_*), in a steel-violet tone;
  * the vacuum vessel: the grid of the reactor pressure vessel (reactor/block_reactor_vessel) in stainless steel greys;
  * the coil windings: the purple and silver of the superconducting cable (wiring/cable/block_superconducting_cable_*);
  * the power output port: the bolt of the Steam Tunnel power port.

Usage: fusion_textures_11589.py <repo root> [--preview <png>]
"""
import math
import os
import random
import sys

from PIL import Image, ImageDraw

ROOT = sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith('--') else '.'
TEX = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/block')
OUT = os.path.join(TEX, 'fusion')


def rgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


# the fusion frame (Steam Tunnel layout, steel-violet)
OUTLINE, BEVEL_LIGHT, BEVEL_DARK = rgb('#2b2f3d'), rgb('#8a90a8'), rgb('#575c73')
PANEL = [rgb('#c3c8d8'), rgb('#b8bdcf'), rgb('#ccd1e0'), rgb('#d6dae7')]
RIVET = rgb('#5fd8f5')


def new():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def frame(im, sides=(True, True, True, True)):
    """Outline and bevel. sides: top, right, bottom, left (False leaves that edge open, for a texture that tiles)."""
    top, right, bottom, left = sides
    px = im.load()
    for i in range(16):
        if top:
            px[i, 0] = OUTLINE
        if bottom:
            px[i, 15] = OUTLINE
        if left:
            px[0, i] = OUTLINE
        if right:
            px[15, i] = OUTLINE
    for i in range(1, 15):
        if top:
            px[i, 1] = BEVEL_LIGHT
        if bottom:
            px[i, 14] = BEVEL_DARK
    # a side bevel runs through an open edge, so a column of the texture tiles without gaps
    for i in range(0 if not top else 1, 16 if not bottom else 15):
        if left:
            px[1, i] = BEVEL_LIGHT
        if right:
            px[14, i] = BEVEL_DARK
    if top and right:
        px[14, 1] = BEVEL_DARK
    if bottom and left:
        px[1, 14] = BEVEL_LIGHT


def noise_fill(im, box, colours, seed, weights=None):
    rnd = random.Random(seed)
    px = im.load()
    x0, y0, x1, y1 = box
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = rnd.choices(colours, weights=weights)[0]


def casing(seed=1, rivets=True):
    im = new()
    noise_fill(im, (2, 2, 13, 13), PANEL, seed, [5, 3, 3, 1])
    # the Steam Tunnel panel's darker lower lip and lighter upper lip
    px = im.load()
    for x in range(2, 14):
        px[x, 13] = PANEL[1]
        px[x, 2] = PANEL[3]
    frame(im)
    if rivets:
        for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
            px[x, y] = RIVET
    return im


def flange(im, aperture):
    """The round port flange of the Steam Tunnel ports: grey rings around an aperture of colour {@code aperture}."""
    px = im.load()
    cx = cy = 7.5
    for y in range(2, 14):
        for x in range(2, 14):
            d = math.hypot(x - cx, y - cy)
            if d <= 3.2:
                px[x, y] = aperture
            elif d <= 4.2:
                px[x, y] = rgb('#3c4152')
            elif d <= 5.3:
                # lit from the top left
                lit = (x - cx) + (y - cy) < 0
                px[x, y] = rgb('#9aa0b6') if lit else rgb('#6b7086')
            elif d <= 5.9:
                px[x, y] = rgb('#555a6e')


def put(im, art, palette, ox=0, oy=0):
    px = im.load()
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch != '.' and ch in palette:
                px[ox + x, oy + y] = palette[ch]


# ------------------------------------------------------------------ blocks

def cryostat():
    return casing(seed=11)


def vacuum_vessel():
    """Seamless stainless-steel panels (the torus shell is many blocks wide, so no per-block outline): the reactor pressure
    vessel's light-steel tones, a soft seam on two edges with a lit lip beside it, and a bolt at the seam crossing."""
    im = new()
    px = im.load()
    rnd = random.Random(31)
    tones = [rgb('#bcc2ce'), rgb('#b4bac7'), rgb('#c3c8d3'), rgb('#b0b6c3')]
    for y in range(16):
        for x in range(16):
            px[x, y] = rnd.choices(tones, weights=[6, 3, 2, 1])[0]
    # a gentle brushed sheen across the panel
    for x in range(3, 12):
        px[x, 4 + (x % 2)] = rgb('#c9ced8')
    seam, lip, shade = rgb('#878e9d'), rgb('#d2d7e0'), rgb('#a4aab8')
    for i in range(16):
        px[i, 15] = seam       # the seam runs along the bottom and the right edge; tiled, every panel has one on each side
        px[15, i] = seam
        px[i, 0] = lip         # the next panel's lit lip
        px[0, i] = lip
        px[i, 14] = shade
        px[14, i] = shade
    for x, y in ((15, 15), (2, 2), (12, 2), (2, 12)):
        px[x, y] = rgb('#6e7585')   # bolts
    px[15, 15] = rgb('#5d6474')
    return im


def blanket():
    """First-wall tiles (2 x 2) on a blanket module, with lithium-teal coolant joints."""
    im = new()
    px = im.load()
    gap, glow = rgb('#353a48'), rgb('#3fd0b0')
    for y in range(2, 14):
        for x in range(2, 14):
            px[x, y] = gap
    for tx, ty in ((2, 2), (8, 2), (2, 8), (8, 8)):
        for y in range(ty, ty + 6):
            for x in range(tx, tx + 6):
                if y == ty or x == tx:
                    c = rgb('#e1e3df')
                elif y == ty + 5 or x == tx + 5:
                    c = rgb('#a3a7a0')
                else:
                    c = rgb('#c9ccc5') if (x + y) % 5 else rgb('#d3d6cf')
                px[x, y] = c
        px[tx + 2, ty + 2] = rgb('#8f948c')   # tile bolt
    for p in ((7, 7), (8, 7), (7, 8), (8, 8), (7, 4), (4, 7), (11, 8), (8, 11)):
        px[p] = glow
    frame(im)
    return im


def divertor():
    """Tungsten monoblocks in a cassette; the strike points at the bottom glow."""
    im = new()
    px = im.load()
    for y in range(2, 14):
        for x in range(2, 14):
            px[x, y] = rgb('#23262e')
    for cx in (2, 6, 10):
        for by in (2, 5, 8, 11):
            for y in range(by, by + 3):
                for x in range(cx, cx + 3):
                    if y >= 14:
                        continue
                    c = rgb('#727986') if y == by else (rgb('#4a4f59') if x == cx + 2 else rgb('#5d636e'))
                    px[x, y] = c
    heat = {13: rgb('#ff8c2e'), 12: rgb('#d9662a'), 11: rgb('#9a5634')}
    for y, c in heat.items():
        for x in range(2, 14):
            if px[x, y] != rgb('#23262e'):
                base = px[x, y]
                a = {13: 0.85, 12: 0.6, 11: 0.35}[y]
                px[x, y] = tuple(int(base[i] * (1 - a) + c[i] * a) for i in range(3)) + (255,)
    frame(im)
    return im


def winding_grid(insul, hi, body, low):
    """A seamless winding pack: 4 x 4 conductors of 3 x 3 pixels in 1-pixel insulation (period 4, so blocks join up)."""
    im = new()
    px = im.load()
    for y in range(16):
        for x in range(16):
            gx, gy = x % 4, y % 4
            if gx == 3 or gy == 3:
                px[x, y] = insul
            elif gx == 0 and gy == 0:
                px[x, y] = hi
            elif gx == 2 or gy == 2:
                px[x, y] = low
            else:
                px[x, y] = body
    return im


def winding_bands(flange, hi, body, low, bolt):
    """Seamless horizontal windings (a coil around a vertical axis): 3 rows of conductor and a flange row, period 4."""
    im = new()
    px = im.load()
    for y in range(16):
        for x in range(16):
            row = y % 4
            if row == 0:
                px[x, y] = bolt if x % 8 == 3 else flange
            else:
                px[x, y] = (hi, body, low)[row - 1]
    return im


def toroidal_coil():
    """The toroidal field coil: purple conductors (the superconducting cable's colours) in steel insulation, seamless, so a
    coil loop reads as one winding pack."""
    return winding_grid(rgb('#7e8da8'), rgb('#c890ff'), rgb('#8a2cff'), rgb('#6a14d8'))


def central_solenoid_side():
    """The central solenoid: blue-violet windings between silver flanges, seamless."""
    return winding_bands(rgb('#a3b6d9'), rgb('#7f73ff'), rgb('#5a4bf0'), rgb('#4436c8'), rgb('#687282'))


def poloidal_coil_side():
    """An outer poloidal field coil (a ring around the torus): steel-blue windings, seamless."""
    return winding_bands(rgb('#b9c6d8'), rgb('#6cc8ff'), rgb('#3aa0e0'), rgb('#2a78b8'), rgb('#6b7a8e'))


def poloidal_coil_end():
    return winding_grid(rgb('#8d9bb0'), rgb('#9fdcff'), rgb('#3aa0e0'), rgb('#2a78b8'))


def central_solenoid_end():
    """The end of the solenoid (it is several blocks wide): its winding pack, seamless."""
    return winding_grid(rgb('#8d97b8'), rgb('#9a90ff'), rgb('#5a4bf0'), rgb('#4436c8'))


def controller_front():
    """The control room: a screen with the plasma ring (magenta) around the central solenoid, and status lights."""
    im = casing(seed=5, rivets=False)
    px = im.load()
    for y in range(3, 12):
        for x in range(3, 13):
            px[x, y] = rgb('#3a4050') if y in (3, 11) or x in (3, 12) else rgb('#11141d')
    cx, cy = 7.5, 7.0
    for y in range(4, 11):
        for x in range(4, 12):
            e = ((x - cx) / 3.6) ** 2 + ((y - cy) / 2.6) ** 2
            if 0.55 <= e <= 1.05:
                px[x, y] = rgb('#ff5ad9') if y <= cy else rgb('#c43cff')
    px[7, 7] = rgb('#8a90a8')
    px[8, 7] = rgb('#8a90a8')
    for x, c in ((4, '#4dff7a'), (6, '#ffc23d'), (8, '#ff4848')):
        px[x, 13] = rgb(c)
    for x in range(10, 13):
        px[x, 13] = rgb('#2b2f3d')
    return im


def controller_front_active():
    """0.1.11.92: the control room while there is plasma: the ring lit up, a glow around it, the lights on."""
    im = controller_front()
    px = im.load()
    cx, cy = 7.5, 7.0
    for y in range(4, 11):
        for x in range(4, 12):
            e = ((x - cx) / 3.6) ** 2 + ((y - cy) / 2.6) ** 2
            if 0.55 <= e <= 1.05:
                px[x, y] = rgb('#ffd0f6') if y <= cy else rgb('#ff6ae0')
            elif 0.3 <= e < 0.55 or 1.05 < e <= 1.4:
                px[x, y] = rgb('#4a1d5c')
    for x in (4, 6, 8):
        px[x, 13] = rgb('#7dff9a')
    return im


def item_deuterium_cell():
    """0.1.11.92: the deuterium cell: the tritium cell with its fuel blue instead of yellow."""
    import colorsys
    src = Image.open(os.path.join(TEX, '..', 'item', 'normal', 'item_tritium_cell.png')).convert('RGBA')
    im = src.copy()
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            if s > 0.2:
                nr, ng, nb = colorsys.hls_to_rgb(0.58, l, min(1.0, s))
                px[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return im


ITEM_TEXTURES = {'normal/item_deuterium_cell': item_deuterium_cell}


BOLT = ['....M..',
        '...OP..',
        '..RSTX.',
        '.VRWX..',
        '..YZY..',
        '...b...']


def port(kind):
    apertures = {'nbi': '#0d1a2a', 'ecrh': '#1c1410', 'fuel': '#0f1a12', 'exhaust': '#16181e', 'cryo': '#0f1c26',
                 'power': '#4d5358', 'mitigation': '#101820'}
    im = casing(seed={'nbi': 21, 'ecrh': 22, 'fuel': 23, 'exhaust': 24, 'cryo': 25, 'power': 26, 'mitigation': 27}[kind], rivets=False)
    flange(im, rgb(apertures[kind]))
    px = im.load()
    if kind == 'nbi':
        # the beam leaving the accelerator grid
        put(im, ['.cc.', 'cCCc', 'cCCc', '.cc.'], {'c': rgb('#33c8e8'), 'C': rgb('#b8ffff')}, 6, 6)
        for p in ((5, 5), (10, 5), (5, 10), (10, 10)):
            px[p] = rgb('#33c8e8')
    elif kind == 'ecrh':
        # microwaves leaving the launcher mirror (left) as two wave fronts
        e, w1, w2 = rgb('#ffe2b0'), rgb('#ff9a3c'), rgb('#d9662a')
        for p in ((5, 7), (5, 8)):
            px[p] = e
        for p in ((6, 6), (7, 7), (7, 8), (6, 9)):
            px[p] = w1
        for p in ((8, 5), (9, 6), (9, 7), (9, 8), (9, 9), (8, 10)):
            px[p] = w2
    elif kind == 'fuel':
        # a deuterium (blue) and a tritium (green) pellet
        put(im, ['dD....', 'DD.tT.', '...TT.'], {'d': rgb('#bfe0ff'), 'D': rgb('#4aa3ff'), 't': rgb('#d6ffd9'), 'T': rgb('#5be37a')}, 5, 6)
    elif kind == 'exhaust':
        for y in (5, 7, 9):
            for x in range(6, 10):
                px[x, y] = rgb('#5a6070')
            px[6, y + 1] = rgb('#2b2f3d')
        px[7, 6] = rgb('#ffe28a')   # helium ash
        px[9, 8] = rgb('#ffe28a')
    elif kind == 'cryo':
        put(im, ['.s.s.', '..S..', 'sSWSs', '..S..', '.s.s.'], {'s': rgb('#58d7ff'), 'S': rgb('#a8f0ff'), 'W': rgb('#ffffff')}, 5, 5)
    elif kind == 'power':
        reds = {k: rgb(v) for k, v in {'M': '#ee180d', 'O': '#da1d13', 'P': '#c2332c', 'R': '#ff1408', 'S': '#d51e14', 'T': '#d21f16',
                                       'V': '#c53e37', 'W': '#c83b34', 'X': '#ce1f16', 'Y': '#d4241a', 'Z': '#f6160b', 'b': '#f2271d'}.items()}
        put(im, BOLT, reds, 4, 5)
    elif kind == 'mitigation':
        # shattered pellets of frozen gas
        put(im, ['i...', '.I.i', '..I.', 'iI..'], {'i': rgb('#9fe4ff'), 'I': rgb('#effcff')}, 6, 6)
        # hazard corners
        for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
            px[x, y] = rgb('#ffd23d')
            px[x + 1, y] = rgb('#2b2f3d')
            px[x, y + 1] = rgb('#2b2f3d')
            px[x + 1, y + 1] = rgb('#ffd23d')
    return im


TEXTURES = {
    'cryostat': cryostat,
    'vacuum_vessel': vacuum_vessel,
    'blanket': blanket,
    'divertor': divertor,
    'toroidal_coil': toroidal_coil,
    'central_solenoid_side': central_solenoid_side,
    'central_solenoid_end': central_solenoid_end,
    'poloidal_coil_side': poloidal_coil_side,
    'poloidal_coil_end': poloidal_coil_end,
    'controller_front': controller_front,
    'controller_front_active': controller_front_active,
    'nbi_port': lambda: port('nbi'),
    'ecrh_port': lambda: port('ecrh'),
    'fuel_port': lambda: port('fuel'),
    'exhaust_port': lambda: port('exhaust'),
    'cryo_port': lambda: port('cryo'),
    'power_port': lambda: port('power'),
    'mitigation_port': lambda: port('mitigation'),
}


def main():
    os.makedirs(OUT, exist_ok=True)
    made = {}
    for name, draw in TEXTURES.items():
        im = draw()
        im.save(os.path.join(OUT, name + '.png'))
        made[name] = im
    for path, draw in ITEM_TEXTURES.items():
        out = os.path.join(TEX, '..', 'item', path + '.png')
        draw().save(out)
        made[path.split('/')[-1]] = Image.open(out).convert('RGBA')
    print('wrote', len(made), 'textures to', OUT, 'and textures/item')
    if '--preview' in sys.argv:
        out = sys.argv[sys.argv.index('--preview') + 1]
        cell, cols = 128, 5
        rows = (len(made) + cols - 1) // cols
        sheet = Image.new('RGBA', (cols * (cell + 10), rows * (cell + 24)), (48, 48, 52, 255))
        d = ImageDraw.Draw(sheet)
        for i, (name, im) in enumerate(made.items()):
            x, y = (i % cols) * (cell + 10), (i // cols) * (cell + 24)
            sheet.paste(im.resize((cell, cell), Image.NEAREST), (x, y))
            d.text((x, y + cell + 4), name, fill=(235, 235, 235))
        sheet.save(out)


if __name__ == '__main__':
    main()
