#!/usr/bin/env python3
"""0.1.10.42 textures of the Trade Boxes and their upgrade kit, drawn pixel by pixel (no external art).
- block_trade_box.png (96 x 16: down, up, east, north, west, south like the Trade Terminal): a plank crate with iron corner
  plates, a hopper opening on top and a coin slot with an emerald on the front.
- block_trade_box_advanced.png: the same crate in dark steel with a violet UU-matter strip, a small market screen and the
  emerald slot on the front.
- item_trade_box_upgrade.png (16 x 16): a steel plate with a violet UU-matter cell and an arrow up.
Deterministic: the same files from every run.
Usage: tex_1142.py <repo root> [--preview <dir>]
"""
import os
import random
import sys

from PIL import Image

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
TEX = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures')
PREVIEW = sys.argv[sys.argv.index('--preview') + 1] if '--preview' in sys.argv else None


def hexc(s, a=255):
    s = s.lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def shade(c, k):
    return (max(0, min(255, round(c[0] * k))), max(0, min(255, round(c[1] * k))), max(0, min(255, round(c[2] * k))), c[3])


class Face:
    def __init__(self):
        self.px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]

    def set(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[y][x] = c

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c)

    def frame(self, x0, y0, x1, y1, c):
        for x in range(x0, x1 + 1):
            self.set(x, y0, c)
            self.set(x, y1, c)
        for y in range(y0, y1 + 1):
            self.set(x0, y, c)
            self.set(x1, y, c)


def planks(seed, base, dark=1.0):
    """Horizontal planks of 4 rows with seams, grain and a little noise."""
    rnd = random.Random(seed)
    f = Face()
    tones = [shade(base, 1.0 * dark), shade(base, 0.9 * dark), shade(base, 1.08 * dark), shade(base, 0.95 * dark)]
    for y in range(16):
        row = y // 4
        for x in range(16):
            c = tones[row]
            if y % 4 == 3:
                c = shade(base, 0.55 * dark)
            elif rnd.random() < 0.18:
                c = shade(c, 0.9 + rnd.random() * 0.15)
            f.set(x, y, c)
        if y % 4 == 1:
            gx = rnd.randrange(2, 13)
            f.set(gx, y, shade(tones[row], 0.78))
            f.set(gx + 1, y, shade(tones[row], 0.78))
    # the board ends (a seam every row at another place)
    for row in range(4):
        sx = (5 + row * 7) % 16
        for y in range(row * 4, row * 4 + 3):
            f.set(sx, y, shade(base, 0.6 * dark))
    return f


def steel(seed, base):
    """Brushed steel panels with seams and a little noise."""
    rnd = random.Random(seed)
    f = Face()
    for y in range(16):
        for x in range(16):
            c = shade(base, 0.96 + rnd.random() * 0.08)
            if (x + y * 3) % 11 == 0:
                c = shade(base, 1.08)
            f.set(x, y, c)
    for x in range(16):
        f.set(x, 7, shade(base, 0.7))
        f.set(x, 8, shade(base, 1.15))
    return f


def crate(f, rim, plate, rivet):
    """The rim and the iron corner plates with a rivet."""
    f.frame(0, 0, 15, 15, rim)
    for cx, cy in ((1, 1), (12, 1), (1, 12), (12, 12)):
        f.rect(cx, cy, cx + 2, cy + 2, plate)
        f.set(cx + 1, cy + 1, rivet)
    return f


def opening(f, dark, bar):
    """The hopper opening of the top: a dark square with a grate."""
    f.rect(4, 4, 11, 11, dark)
    f.frame(4, 4, 11, 11, bar)
    for i in (6, 9):
        for k in range(5, 11):
            f.set(i, k, bar)
            f.set(k, i, bar)
    return f


def coin_slot(f, y, gold, slot, emerald, emerald_dark):
    """The coin slot (a dark slit in a gold rim) and the emerald under it."""
    f.rect(5, y, 10, y + 2, gold)
    f.rect(6, y + 1, 9, y + 1, slot)
    f.rect(7, y + 4, 8, y + 5, emerald)
    f.set(8, y + 5, emerald_dark)
    f.set(7, y + 4, shade(emerald, 1.3))
    return f


def strip(faces):
    im = Image.new('RGBA', (96, 16))
    for i, face in enumerate(faces):
        for y in range(16):
            for x in range(16):
                im.putpixel((i * 16 + x, y), face.px[y][x])
    return im


IRON, IRON_PLATE, RIVET = hexc('3a3d42'), hexc('8d939b'), hexc('d5dbe2')
GOLD, SLOT = hexc('e0b63a'), hexc('1a1410')
EMERALD, EMERALD_DARK = hexc('2fbf71'), hexc('17804a')
WOOD = hexc('8a5a2e')
STEEL, STEEL_RIM, STEEL_PLATE = hexc('5a6370'), hexc('1d232b'), hexc('a3abb6')
UU, UU_DARK = hexc('b05cf2'), hexc('6d2aa8')
SCREEN, CHART = hexc('0d1117'), hexc('2ec4a6')


def basic():
    side = lambda s: crate(planks(s, WOOD), IRON, IRON_PLATE, RIVET)
    down = crate(planks(11, WOOD, 0.8), IRON, IRON_PLATE, RIVET)
    up = opening(crate(planks(12, WOOD), IRON, IRON_PLATE, RIVET), hexc('161210'), IRON)
    north = coin_slot(side(13), 4, GOLD, SLOT, EMERALD, EMERALD_DARK)
    # a small label plate under the emerald
    north.rect(5, 12, 10, 13, hexc('c9b48a'))
    north.rect(6, 12, 9, 12, hexc('a8925f'))
    return strip([down, up, side(14), north, side(15), side(16)])


def advanced():
    def side(s):
        f = crate(steel(s, STEEL), STEEL_RIM, STEEL_PLATE, RIVET)
        # the violet UU-matter strip along the middle seam
        for x in range(3, 13):
            f.set(x, 7, UU_DARK)
            f.set(x, 8, UU if x % 3 else shade(UU, 1.25))
        return f
    down = crate(steel(21, shade(STEEL, 0.8)), STEEL_RIM, STEEL_PLATE, RIVET)
    up = opening(crate(steel(22, STEEL), STEEL_RIM, STEEL_PLATE, RIVET), hexc('0b0d10'), UU_DARK)
    north = crate(steel(23, STEEL), STEEL_RIM, STEEL_PLATE, RIVET)
    # the market screen and its chart
    north.rect(3, 2, 12, 7, SCREEN)
    north.frame(3, 2, 12, 7, hexc('2c3440'))
    for x, y in ((4, 6), (5, 5), (6, 5), (7, 4), (8, 5), (9, 4), (10, 3), (11, 3)):
        north.set(x, y, CHART)
    coin_slot(north, 9, GOLD, SLOT, EMERALD, EMERALD_DARK)
    north.set(3, 13, UU)
    north.set(12, 13, UU)
    return strip([down, up, side(24), north, side(25), side(26)])


def kit():
    f = Face()
    # a small steel crate with the violet UU-matter strip
    f.rect(1, 6, 11, 14, STEEL)
    f.frame(1, 6, 11, 14, STEEL_RIM)
    for x in range(2, 11):
        f.set(x, 9, UU_DARK)
        f.set(x, 10, UU if x % 3 else shade(UU, 1.25))
    for cx, cy in ((2, 7), (10, 7), (2, 13), (10, 13)):
        f.set(cx, cy, STEEL_PLATE)
    f.rect(5, 12, 7, 12, GOLD)
    # the UU-matter cell
    f.rect(3, 0, 6, 4, hexc('c8cdd4'))
    f.rect(4, 1, 5, 3, UU)
    f.set(4, 1, shade(UU, 1.3))
    # the arrow up (the upgrade)
    for y in range(3, 11):
        f.set(13, y, EMERALD)
        f.set(14, y, EMERALD_DARK)
    for d in (1, 2):
        f.set(13 - d, 2 + d, EMERALD)
        f.set(14 + d, 2 + d, EMERALD_DARK)
    f.set(13, 1, shade(EMERALD, 1.25))
    f.set(14, 1, EMERALD)
    f.set(13, 2, EMERALD)
    f.set(14, 2, EMERALD)
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), f.px[y][x])
    return im


def save(im, rel):
    path = os.path.join(TEX, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)
    print('wrote', rel)
    if PREVIEW:
        im.resize((im.width * 8, im.height * 8), Image.NEAREST).save(os.path.join(PREVIEW, os.path.basename(rel)))


save(basic(), 'block/producer/block_trade_box.png')
save(advanced(), 'block/producer/block_trade_box_advanced.png')
save(kit(), 'item/normal/item_trade_box_upgrade.png')
