#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""0.1.11.140: the connected-texture tiles of the fusion reactor's components (ConnectedModel, loader mio_icif:connected).

Each base texture (16 x 16, a 1-pixel border) gives four tiles beside it in component/ctm/; the base itself is the "full" tile:
  <name>_h      the left and right borders gone (joined left and right), the top and bottom borders kept
  <name>_v      the top and bottom borders gone, the left and right kept
  <name>_none   no border (joined on all four sides)
  <name>_inner  no border but the four corner pixels (two sides joined, the block across the corner not)
A border pixel is replaced by its inner neighbour (column 1 / 14, row 1 / 14), so the colours are the base texture's own.

Run from the repository root:  python3 tools/fusion_ctm_tiles.py
"""
import os
from PIL import Image

ROOT = "src/main/resources/assets/mio_icif/textures/block/fusion/component"
NAMES = ["casing", "dark_metal", "coil"]


def strip_sides(src):
    out = src.copy()
    for y in range(16):
        out.putpixel((0, y), src.getpixel((1, y)))
        out.putpixel((15, y), src.getpixel((14, y)))
    return out


def strip_ends(src):
    out = src.copy()
    for x in range(16):
        out.putpixel((x, 0), src.getpixel((x, 1)))
        out.putpixel((x, 15), src.getpixel((x, 14)))
    return out


def main():
    os.makedirs(os.path.join(ROOT, "ctm"), exist_ok=True)
    for name in NAMES:
        base = Image.open(os.path.join(ROOT, name + ".png")).convert("RGBA")
        if base.size != (16, 16):
            raise SystemExit(name + ": not 16 x 16")
        h = strip_sides(base)
        v = strip_ends(base)
        none = strip_ends(h)
        inner = none.copy()
        for corner in ((0, 0), (15, 0), (0, 15), (15, 15)):
            inner.putpixel(corner, base.getpixel(corner))
        for suffix, image in (("h", h), ("v", v), ("none", none), ("inner", inner)):
            image.save(os.path.join(ROOT, "ctm", "%s_%s.png" % (name, suffix)))
        print(name, "-> h v none inner")


if __name__ == "__main__":
    main()
