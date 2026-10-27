#!/usr/bin/env python3
"""
0.1.10.32: generates the texture of the Armory iris hatch (no hand-drawn art).

  textures/block/armory_iris.png  32 x 32: dark gunmetal plating seen from above (drawn planar over the hatch, centre at the
                                  middle): six radial seams, a ring of rivets near the rim, a brushed grain and a faint
                                  bevel towards the centre.

Deterministic. With --check it compares with the file on disk and writes nothing. Usage: armory_textures_1132.py <repo root> [--check]
"""
import math
import os
import random
import sys

from PIL import Image


def iris():
    n = 32
    rnd = random.Random(1132)
    img = Image.new('RGBA', (n, n), (0, 0, 0, 255))
    px = img.load()
    for y in range(n):
        for x in range(n):
            u, v = (x + 0.5) / n * 2 - 1, (y + 0.5) / n * 2 - 1
            r = math.hypot(u, v)
            a = math.atan2(v, u)
            base = 58 + 14 * (1 - r) + rnd.uniform(-4, 4) + 3 * math.sin((u + v) * 40)      # brushed grain
            seam = min(abs(((a / (math.pi / 3)) % 1) - 0.5) for _ in (0,))
            if abs(seam - 0.5) < 0.06 and r > 0.12:                                           # radial seams between blades
                base -= 22
            ring = abs(r - 0.82)
            if ring < 0.05:
                base -= 12
            k = (a / (math.pi / 6)) % 1
            if abs(r - 0.72) < 0.06 and abs(k - 0.5) < 0.12:                                  # rivets
                base += 34
            g = max(0, min(255, int(round(base))))
            px[x, y] = (g, min(255, g + 3), min(255, g + 8), 255)
    return img


def main():
    root = sys.argv[1]
    check = '--check' in sys.argv[2:]
    path = os.path.join(root, 'src/main/resources/assets/mio_icif/textures/block/armory_iris.png')
    img = iris()
    if check:
        old = Image.open(path).convert('RGBA') if os.path.exists(path) else None
        same = old is not None and old.size == img.size and old.tobytes() == img.tobytes()
        print('block/armory_iris.png', 'same' if same else 'DIFFERENT')
        sys.exit(0 if same else 1)
    img.save(path, optimize=True)
    print('wrote block/armory_iris.png')


if __name__ == '__main__':
    main()
