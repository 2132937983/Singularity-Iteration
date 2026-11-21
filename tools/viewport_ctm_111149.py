#!/usr/bin/env python3
"""0.1.11.149: the connected-texture tiles of the fusion reactor's diagnostic viewport (textures/block/fusion/viewport.png),
for the mio_icif:connected loader (common/client/render/ConnectedModel): <name>_h, _v, _none and _inner beside the texture.

The viewport is a two-pixel metal frame (a light outer edge, grey inside), a dark gasket ring and translucent glass with a few
glints. A face is cut into quarters, and each quarter shows its corner of:
  the texture itself  when neither neighbour beside that quarter joins;
  _h                  when the neighbour to the left or right joins: the side frames give way to a clean middle column, so the
                      top and bottom frames run on into the next window;
  _v                  the same upward or downward (the top and bottom frames give way to a clean middle row);
  _none               when both join and so does the block across the corner: glass to the edges (the glints stay);
  _inner              when both join but not the block across the corner: _none with the texture's 3 x 3 corner, which is just
                      where the two frames of the neighbours meet.
Every tile is derived from viewport.png, so it can be redrawn and this script rerun. Idempotent.

Usage: viewport_ctm_111149.py <repo root> [--preview <png>]
"""
import os
import sys

from PIL import Image

ROOT = sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith('--') else '.'
DIR = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/block/fusion')
NAME = 'viewport'
BORDER = 3          # the frame (2) and the gasket (1)
CLEAN = 8           # a column and a row of the texture without glints


def tiles(base):
    w, h = base.size
    px = base.load()
    out = {}
    hor = base.copy()   # _h: the side borders replaced by the clean column
    p = hor.load()
    for y in range(h):
        for x in list(range(BORDER)) + list(range(w - BORDER, w)):
            p[x, y] = px[CLEAN, y]
    out['_h'] = hor
    ver = base.copy()   # _v: the top and bottom borders replaced by the clean row
    p = ver.load()
    for x in range(w):
        for y in list(range(BORDER)) + list(range(h - BORDER, h)):
            p[x, y] = px[x, CLEAN]
    out['_v'] = ver
    none = base.copy()  # _none: glass (the clean glass pixel) wherever the borders were
    p = none.load()
    glass = px[CLEAN, CLEAN]
    for y in range(h):
        for x in range(w):
            if x < BORDER or x >= w - BORDER or y < BORDER or y >= h - BORDER:
                p[x, y] = glass
    out['_none'] = none
    inner = none.copy()  # _inner: _none with the texture's four corners
    p = inner.load()
    for y in range(h):
        for x in range(w):
            if (x < BORDER or x >= w - BORDER) and (y < BORDER or y >= h - BORDER):
                p[x, y] = px[x, y]
    out['_inner'] = inner
    return out


def quarter_tile(h, v, corner):
    if not h and not v:
        return ''
    if h and not v:
        return '_h'
    if not h:
        return '_v'
    return '_none' if corner else '_inner'


def preview(images, out):
    """A wall of windows as the loader puts it together: an L of five beside a lone one, on a green backdrop, 8 x."""
    wall = {(0, 0), (1, 0), (2, 0), (0, 1), (0, 2), (2, 2), (1, 2), (2, 1), (4, 1)}   # a ring of 8 (the middle empty) and one alone
    s = 16
    img = Image.new('RGBA', (6 * s, 4 * s), (110, 150, 90, 255))
    for (bx, by) in wall:
        for qx in (0, 1):
            for qy in (0, 1):
                dx, dy = (-1 if qx == 0 else 1), (-1 if qy == 0 else 1)
                h = (bx + dx, by) in wall
                v = (bx, by + dy) in wall
                c = (bx + dx, by + dy) in wall
                tile = images[quarter_tile(h, v, c)]
                box = (qx * 8, qy * 8, qx * 8 + 8, qy * 8 + 8)
                img.alpha_composite(tile.crop(box), (bx * s + qx * 8 + 8, by * s + qy * 8 + 8))
    img.resize((img.width * 6, img.height * 6), Image.NEAREST).save(out)
    print('preview', out)


def main():
    base = Image.open(os.path.join(DIR, NAME + '.png')).convert('RGBA')
    made = tiles(base)
    for suffix, im in made.items():
        im.save(os.path.join(DIR, NAME + suffix + '.png'))
    print('tiles written:', ', '.join(NAME + s for s in made))
    if '--preview' in sys.argv:
        made[''] = base
        preview(made, sys.argv[sys.argv.index('--preview') + 1])


if __name__ == '__main__':
    main()
