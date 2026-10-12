#!/usr/bin/env python3
"""
0.1.8.0: textures of the Blueprint Builder system (no dependencies).

  textures/block/refined/producer/block_blueprint_builder.png   refined machine atlas (size of the Armory atlas)
  textures/block/refined/producer/block_material_interface.png  refined machine atlas (size of the Armory atlas)
  textures/entity/builder_drone.png                             32x32 drone skin
  textures/entity/builder_drone_glow.png                        32x32 emissive layer (lights only)

The machine atlases start from the Armory atlas (same frame, vents and bevel style); this
script repaints the faces. Atlas layout (16x16 per face): row 0 north, east, south, west;
row 1 up, down, panel inset, spare.

Usage: tex_builder_180.py <repo root>
"""
import os
import struct
import sys
import zlib

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
TEX = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures')


def read_png(path):
    with open(path, 'rb') as f:
        data = f.read()
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    pos, idat, w, h, ctype, palette, trns = 8, b'', 0, 0, 0, None, None
    while pos < len(data):
        n = struct.unpack('>I', data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + n]
        if kind == b'IHDR':
            w, h, depth, ctype = struct.unpack('>IIBB', body[:10])
            assert depth == 8, 'only 8-bit PNG'
        elif kind == b'PLTE':
            palette = [tuple(body[i:i + 3]) for i in range(0, len(body), 3)]
        elif kind == b'tRNS':
            trns = body
        elif kind == b'IDAT':
            idat += body
        pos += 12 + n
    channels = {6: 4, 2: 3, 3: 1, 0: 1, 4: 2}[ctype]
    raw = zlib.decompress(idat)
    stride = w * channels
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(h):
        f = raw[i]; i += 1
        line = bytearray(raw[i:i + stride]); i += stride
        for x in range(stride):
            a = line[x - channels] if x >= channels else 0
            b = prev[x]
            c = prev[x - channels] if x >= channels else 0
            if f == 1: line[x] = (line[x] + a) & 255
            elif f == 2: line[x] = (line[x] + b) & 255
            elif f == 3: line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line)
        prev = line
    px = [[(0, 0, 0, 0)] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            o = x * channels
            v = rows[y][o:o + channels]
            if ctype == 6: px[y][x] = tuple(v)
            elif ctype == 2: px[y][x] = (v[0], v[1], v[2], 255)
            elif ctype == 3:
                r, g, b = palette[v[0]]
                a = trns[v[0]] if trns and v[0] < len(trns) else 255
                px[y][x] = (r, g, b, a)
            elif ctype == 0: px[y][x] = (v[0], v[0], v[0], 255)
            else: px[y][x] = (v[0], v[0], v[0], v[1])
    return w, h, px


def write_png(path, px):
    h, w = len(px), len(px[0])
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w):
            raw += bytes(max(0, min(255, int(c))) for c in px[y][x])

    def chunk(kind, body):
        return struct.pack('>I', len(body)) + kind + body + struct.pack('>I', zlib.crc32(kind + body) & 0xffffffff)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n')
        f.write(chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)))
        f.write(chunk(b'IDAT', zlib.compress(bytes(raw), 9)))
        f.write(chunk(b'IEND', b''))


CYAN = (80, 220, 255, 255)
CYAN_DIM = (40, 120, 150, 255)
SCREEN = (22, 34, 48, 255)
DARK = (58, 62, 68, 255)
WHITE = (236, 240, 242, 255)


def face(px, col, row, fn):
    """Repaints face (col,row) of the atlas; fn(x, y, old) -> new or None (keep).
    x and y are on the 16 unit design grid; a 128x64 atlas has 2x2 pixels per unit."""
    s = len(px[0]) // 64
    for y in range(16 * s):
        for x in range(16 * s):
            ox, oy = col * 16 * s + x, row * 16 * s + y
            new = fn(x // s, y // s, px[oy][ox])
            if new is not None:
                px[oy][ox] = new


def inner(x, y, m=3):
    return m <= x <= 15 - m and m <= y <= 15 - m


# ---------------------------------------------------------------- Blueprint Builder
def builder_front(x, y, old):
    if not inner(x, y, 2):
        return None
    if x in (2, 13) or y in (2, 13):
        return DARK
    # blueprint screen: dark blue with a cyan grid and a small drone mark
    if (x - 3) % 3 == 0 or (y - 3) % 3 == 0:
        return CYAN_DIM
    if (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        return CYAN
    if (x, y) in ((5, 5), (10, 5), (5, 10), (10, 10)):
        return WHITE
    return SCREEN


def builder_side(x, y, old):
    return CYAN if y == 13 and 2 <= x <= 13 else None


def builder_top(x, y, old):
    if not inner(x, y, 1):
        return None
    if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        return CYAN                                        # dock lights
    if (x in (5, 10) and 5 <= y <= 10) or (y == 7 and 5 <= x <= 10) or (y == 8 and 5 <= x <= 10):
        return WHITE                                       # landing mark "H"
    if x in (1, 14) or y in (1, 14):
        return DARK
    return (64, 70, 78, 255) if (x + y) % 2 == 0 else (60, 66, 74, 255)


# ---------------------------------------------------------------- Material Interface
def port_face(x, y, old):
    if not inner(x, y, 3):
        return None
    if x in (3, 12) or y in (3, 12):
        return CYAN
    # arrows into the port
    if (y == 7 or y == 8) and 5 <= x <= 10:
        return CYAN_DIM
    if (x, y) in ((6, 6), (6, 9), (9, 6), (9, 9)):
        return CYAN_DIM
    return SCREEN


def interface_top(x, y, old):
    if not inner(x, y, 2):
        return None
    if x in (2, 13) or y in (2, 13):
        return DARK
    if x == y or x == 15 - y:
        return CYAN_DIM
    return (70, 76, 84, 255)


# ---------------------------------------------------------------- drone skin (32x32)
def drone():
    skin = [[(0, 0, 0, 0)] * 32 for _ in range(32)]
    glow = [[(0, 0, 0, 0)] * 32 for _ in range(32)]

    def rect(img, x0, y0, x1, y1, c):
        for y in range(y0, y1):
            for x in range(x0, x1):
                img[y][x] = c
    graphite, plate, edge = (52, 57, 64, 255), (176, 182, 188, 255), (34, 38, 44, 255)
    # body 6x3x6 at (0,0): top 6..12 x 0..6, bottom 12..18 x 0..6, sides row 6..9
    rect(skin, 0, 0, 24, 9, graphite)
    rect(skin, 6, 0, 12, 6, plate)
    rect(skin, 7, 1, 11, 5, (150, 158, 166, 255))
    rect(skin, 12, 0, 18, 6, edge)
    rect(skin, 0, 7, 24, 8, CYAN_DIM)
    rect(glow, 0, 7, 24, 8, CYAN)                      # side light strip
    # canopy 4x1x4 at (0,9)
    rect(skin, 0, 9, 16, 14, (30, 70, 86, 255))
    rect(skin, 4, 9, 8, 13, (60, 150, 180, 255))
    rect(glow, 4, 10, 8, 12, (70, 190, 230, 200))
    # eye 2x1x1 at (16,9)
    rect(skin, 16, 9, 22, 11, CYAN)
    rect(glow, 16, 9, 22, 11, (140, 240, 255, 255))
    # arms 12x1x1 at (0,14)
    rect(skin, 0, 14, 26, 16, (70, 75, 82, 255))
    # rotor disc 5x0.3x5 at (0,16): blades as a cross, rim lights
    rect(skin, 0, 16, 20, 22, (90, 96, 104, 120))
    for i in range(5):
        skin[16 + i][5 + 2] = plate
        skin[16 + 2][5 + i] = plate
    for x in range(5, 10):
        glow[16][x] = (80, 220, 255, 140)
        glow[20][x] = (80, 220, 255, 140)
    # hub 1x1x1 at (20,16)
    rect(skin, 20, 16, 24, 18, plate)
    return skin, glow


def main():
    base_path = os.path.join(TEX, 'block/refined/producer/block_armory.png')
    w, h, base = read_png(base_path)
    assert w == 2 * h and w % 64 == 0, (w, h)

    b = [row[:] for row in base]
    face(b, 0, 0, builder_front)
    for col in (1, 2, 3):
        face(b, col, 0, builder_side)
    face(b, 0, 1, builder_top)
    write_png(os.path.join(TEX, 'block/refined/producer/block_blueprint_builder.png'), b)

    m = [row[:] for row in base]
    for col in (0, 1, 2, 3):
        face(m, col, 0, port_face)
    face(m, 0, 1, interface_top)
    write_png(os.path.join(TEX, 'block/refined/producer/block_material_interface.png'), m)

    skin, glow = drone()
    write_png(os.path.join(TEX, 'entity/builder_drone.png'), skin)
    write_png(os.path.join(TEX, 'entity/builder_drone_glow.png'), glow)
    print('wrote builder textures')


if __name__ == '__main__':
    main()
