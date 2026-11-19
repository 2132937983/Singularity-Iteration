#!/usr/bin/env python3
"""0.1.11.94: the two new faces of the toroidal field coil (it faces a direction now), drawn from the palette of its current
side texture (textures/block/fusion/toroidal_coil.png: copper turns between steel), which this script only reads:

  * toroidal_coil_edge.png: the front and back, the coil's narrow edge: the steel case with one copper conductor showing in
    the middle and a bolt every 8 pixels; open at the top and bottom, so a vertical leg reads as one edge;
  * toroidal_coil_end.png: the top and bottom, the cross-section of the winding pack: two conductor packs (front and back
    half, matching the two conductors the sides show) in the steel case.

Usage: fusion_coil_faces_11594.py <repo root>
"""
import os
import sys

from PIL import Image

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
TEX = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/block/fusion')


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def palette():
    """Steel and copper of the side texture, each from dark to light."""
    im = Image.open(os.path.join(TEX, 'toroidal_coil.png')).convert('RGBA')
    colours = {im.getpixel((x, y)) for x in range(16) for y in range(16)}
    lum = lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
    steel = sorted((c for c in colours if c[2] > c[0]), key=lum)     # blue-grey
    copper = sorted((c for c in colours if c[0] > c[2]), key=lum)    # orange
    return steel, copper


def edge(steel, copper):
    dark, mid_dark, mid, light = steel[0], steel[1], steel[-2], steel[-1]
    c_dark, c_mid, c_light = copper[0], copper[len(copper) // 2], copper[-1]
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    for y in range(16):
        alt = copper[1] if y % 3 == 0 else c_mid
        cols = [dark, light, mid, mid_dark, dark, c_dark, alt, c_light, copper[-2], c_mid, c_dark, dark, mid_dark, mid, mid_dark, dark]
        for x, c in enumerate(cols):
            px[x, y] = c
    for y in (3, 11):   # bolts in the steel case on both sides
        for x in (2, 13):
            px[x, y] = rgb('cdd1d7')
            px[x, y + 1] = dark
    return im


def end(steel, copper):
    dark, mid_dark, mid, light = steel[0], steel[1], steel[-2], steel[-1]
    c_dark, c_mid, c_light = copper[0], copper[len(copper) // 2], copper[-1]
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = mid
    for y in range(16):   # the case's sides, as on the edge face
        px[0, y], px[1, y], px[2, y] = dark, light, mid_dark
        px[13, y], px[14, y], px[15, y] = mid_dark, mid, dark
    for x in range(3, 13):   # the case between the packs and at the block's ends
        px[x, 0], px[x, 7], px[x, 8], px[x, 15] = mid_dark, dark, light, mid_dark
    for top in (1, 9):       # two packs of 6 rows
        rows = [c_dark, c_mid, c_light, copper[-2], c_mid, c_dark]
        for i, c in enumerate(rows):
            for x in range(3, 13):
                px[x, top + i] = c_dark if x in (3, 12) else c
        for x in (6, 9):     # the insulation between the turns
            for i in range(1, 5):
                px[x, top + i] = mid_dark
    return im


def main():
    steel, copper = palette()
    edge(steel, copper).save(os.path.join(TEX, 'toroidal_coil_edge.png'))
    end(steel, copper).save(os.path.join(TEX, 'toroidal_coil_end.png'))
    print('wrote toroidal_coil_edge.png and toroidal_coil_end.png | steel', len(steel), 'copper', len(copper))


if __name__ == '__main__':
    main()
