#!/usr/bin/env python3
"""Draw the repressurizer with SI's existing GUI theme and IC2's layout.

The 256x256 texture contains a 176x166 panel. Tank interiors match the screen's
38x47 fluid viewports exactly; item origins are one pixel inside the slot frames.
Run from any directory: python tools/gui_steam_repressurizer.py
"""
from pathlib import Path

from PIL import ImageDraw

import si_gui2 as theme


def draw():
    image = theme.new_canvas()
    theme.panel(image, 0, 0, 176, 166)
    theme.groove(image, 4, 15, 168)

    # One-pixel frame around the existing (15,19) and (123,19) fluid areas.
    for x in (14, 122):
        theme.well(image, x, 18, 40, 49, grid=False)

    # Static conversion indicator, using the theme's neutral inset colours.
    painter = ImageDraw.Draw(image)
    painter.polygon(
        [(68, 38), (94, 38), (94, 32), (107, 42),
         (94, 52), (94, 46), (68, 46)],
        fill=theme.SLOT_FILL + (255,),
        outline=theme.SLOT_LO + (255,),
    )
    painter.line([(69, 39), (95, 39), (95, 34)], fill=theme.SLOT_HI + (255,))

    theme.groove(image, 4, 78, 168)
    theme.inventory(image, 7, 83)
    return image


if __name__ == '__main__':
    target = (Path(__file__).resolve().parents[1]
              / 'src/main/resources/assets/mio_icif/textures/gui/gui_steam_repressurizer.png')
    draw().save(target)
    print(target)
