#!/usr/bin/env python3
"""0.1.11.76: the reactor config card item textures (16 x 16), drawn here pixel by pixel (original art).
A slate-green card with rounded corners, gold contacts on the left and a 3 x 3 layout grid on a dark plate on the right:
dim on the blank card, lit (uranium green, brightest in the middle) on a written card.
Usage: tex_11576.py <repo root>
"""
import os
import sys

from PIL import Image

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
OUT = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/item/reactor')

OUTLINE = (20, 32, 28, 255)
BODY = (52, 82, 74, 255)
BODY_HI = (84, 122, 110, 255)
BODY_LO = (34, 54, 48, 255)
GOLD = (226, 186, 74, 255)
GOLD_LO = (164, 124, 40, 255)
GRID_OFF = (78, 108, 98, 255)
GRID_ON = (150, 226, 96, 255)
GRID_CORE = (222, 255, 160, 255)


def card(written):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()
    x0, y0, x1, y1 = 1, 3, 14, 12          # body, inclusive
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or y in (y0, y1)
            px[x, y] = OUTLINE if edge else BODY
    # rounded corners
    for x, y in ((x0, y0), (x1, y0), (x0, y1), (x1, y1)):
        px[x, y] = (0, 0, 0, 0)
    # bevel: light top row and left column, dark bottom row and right column
    for x in range(x0 + 1, x1):
        px[x, y0 + 1] = BODY_HI
    for y in range(y0 + 1, y1):
        px[x0 + 1, y] = BODY_HI
    for x in range(x0 + 2, x1):
        px[x, y1 - 1] = BODY_LO
    for y in range(y0 + 2, y1):
        px[x1 - 1, y] = BODY_LO
    # gold contacts: a 3 x 5 pad beside the grid, two dark seams across it and a dark right edge
    for y in range(5, 10):
        for x in range(3, 6):
            px[x, y] = GOLD_LO if y in (6, 8) or x == 5 else GOLD
    # layout grid: 3 x 3 cells, one pixel each, one pixel apart, on a dark plate
    for y in range(4, 11):
        for x in range(7, 14):
            if x < x1 - 1:
                px[x, y] = BODY_LO
    lit = {(0, 0), (2, 0), (1, 1), (0, 2), (2, 2)} if written else set()
    for gy in range(3):
        for gx in range(3):
            x, y = 8 + gx * 2, 5 + gy * 2
            on = (gx, gy) in lit
            px[x, y] = (GRID_CORE if (gx, gy) == (1, 1) else GRID_ON) if on else GRID_OFF
    return im


def main():
    os.makedirs(OUT, exist_ok=True)
    card(False).save(os.path.join(OUT, 'item_reactor_config_card.png'))
    card(True).save(os.path.join(OUT, 'item_reactor_config_card_written.png'))
    print('written to', OUT)


if __name__ == '__main__':
    main()
