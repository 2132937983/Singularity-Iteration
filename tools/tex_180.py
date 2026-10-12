#!/usr/bin/env python3
"""
0.1.8.0: procedural block textures (no dependencies).

Holo display projector (replaces the Armor Showcase pedestal look):
  holo_projector_top.png     graphite plate with a cyan emitter ring and corner lenses
  holo_projector_side.png    graphite band with a cyan light strip and vents
  holo_projector_bottom.png  plain graphite with bolts
  holo_projector_lens.png    cyan lens with a bright core

Usage: tex_180.py <repo root>
"""
import math
import os
import struct
import sys
import zlib

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
OUT = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/block/producer')


def write_png(path, size, pixel):
    raw = bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):
            r, g, b, a = pixel(x, y)
            raw += bytes((max(0, min(255, int(r))), max(0, min(255, int(g))), max(0, min(255, int(b))), max(0, min(255, int(a)))))

    def chunk(kind, body):
        return struct.pack('>I', len(body)) + kind + body + struct.pack('>I', zlib.crc32(kind + body) & 0xffffffff)
    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n')
        f.write(chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0)))
        f.write(chunk(b'IDAT', zlib.compress(bytes(raw), 9)))
        f.write(chunk(b'IEND', b''))


def graphite(x, y, seed=0):
    n = ((x * 73856093) ^ (y * 19349663) ^ seed) & 15
    v = 46 + n
    return (v, v + 3, v + 8, 255)


def top(x, y):
    c = graphite(x, y, 7)
    d = math.hypot(x - 7.5, y - 7.5)
    if 4.4 <= d <= 5.6:                       # emitter ring
        k = 1 - abs(d - 5.0) / 0.6
        return (60 + 80 * k, 200 + 55 * k, 255, 255)
    if d < 2.2:                               # central lens seat
        return (24, 30, 38, 255)
    if (x in (0, 15) or y in (0, 15)):        # rim
        return (c[0] + 22, c[1] + 22, c[2] + 24, 255)
    if (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        return (90, 220, 255, 255)            # corner lenses
    return c


def side(x, y):
    c = graphite(x, y, 11)
    if y == 0 or y == 15:
        return (c[0] + 20, c[1] + 20, c[2] + 22, 255)
    if 11 <= y <= 12:                          # light strip (the 4 px tall plate shows rows 12..15)
        k = 0.75 + 0.25 * math.sin(x * 0.9)
        return (60 * k, 210 * k, 255 * k, 255)
    if y == 14 and x % 3 == 1:                 # vents
        return (20, 24, 30, 255)
    return c


def bottom(x, y):
    c = graphite(x, y, 3)
    if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        return (110, 116, 124, 255)
    return c


def lens(x, y):
    d = math.hypot(x - 7.5, y - 7.5)
    if d > 7.6:
        return (0, 0, 0, 0)
    k = max(0.0, 1 - d / 7.5)
    return (60 + 190 * k * k, 190 + 65 * k, 255, 255)


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, fn in (('holo_projector_top', top), ('holo_projector_side', side),
                     ('holo_projector_bottom', bottom), ('holo_projector_lens', lens)):
        write_png(os.path.join(OUT, name + '.png'), 16, fn)
        print('wrote', name)


if __name__ == '__main__':
    main()
