#!/usr/bin/env python3
"""0.1.10.27 (X2): the Trade Terminal texture, made from the futures machine's six-face strip (96 x 16: down, up, east,
north, west, south; RGBA 8-bit PNG, pure Python). The coloured parts take a teal hue, and the screen of the front (north)
face shows a price line, so the two blocks differ at a glance. Also writes a 4x preview when asked.
Usage: tex_1127.py <tree> [preview.png]"""
import colorsys
import os
import struct
import sys
import zlib

T = sys.argv[1]
TEX = os.path.join(T, 'src/main/resources/assets/mio_icif/textures/block/producer')


def read_png(path):
    data = open(path, 'rb').read()
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    pos, idat, w, h = 8, b'', 0, 0
    while pos < len(data):
        n = struct.unpack('>I', data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + n]
        if kind == b'IHDR':
            w, h, depth, ctype, _, _, inter = struct.unpack('>IIBBBBB', body)
            assert depth == 8 and ctype == 6 and inter == 0, path
        elif kind == b'IDAT':
            idat += body
        pos += 12 + n
    raw = zlib.decompress(idat)
    stride = w * 4
    rows, prev = [], bytearray(stride)
    for y in range(h):
        f = raw[y * (stride + 1)]
        line = bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
        for x in range(stride):
            a = line[x - 4] if x >= 4 else 0
            b = prev[x]
            c = prev[x - 4] if x >= 4 else 0
            if f == 1: line[x] = (line[x] + a) & 255
            elif f == 2: line[x] = (line[x] + b) & 255
            elif f == 3: line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if pa <= pb and pa <= pc else b if pb <= pc else c
                line[x] = (line[x] + pr) & 255
        rows.append(line)
        prev = line
    return w, h, rows


def write_png(path, w, h, rows):
    raw = b''.join(b'\x00' + bytes(r) for r in rows)

    def chunk(kind, body):
        return struct.pack('>I', len(body)) + kind + body + struct.pack('>I', zlib.crc32(kind + body) & 0xffffffff)

    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) \
        + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'wb').write(png)


def recolour(rows, hue, sat_scale):
    out = []
    for r in rows:
        line = bytearray(r)
        for x in range(0, len(line), 4):
            if line[x + 3] == 0:
                continue
            hh, ll, ss = colorsys.rgb_to_hls(line[x] / 255, line[x + 1] / 255, line[x + 2] / 255)
            if ss > 0.15:   # coloured parts take the new hue; the grey casing stays grey
                hh = hue
                ss = min(1.0, ss * sat_scale)
            nr, ng, nb = colorsys.hls_to_rgb(hh, ll, ss)
            line[x], line[x + 1], line[x + 2] = int(nr * 255 + 0.5), int(ng * 255 + 0.5), int(nb * 255 + 0.5)
        out.append(line)
    return out


def put(rows, x, y, colour):
    rows[y][x * 4:x * 4 + 4] = bytes(colour)


src = os.path.join(TEX, 'block_future_elc.png')
w, h, rows = read_png(src)
assert (w, h) == (96, 16), (w, h)
rows = recolour(rows, 0.47, 1.1)
# the front face is the fourth sixth (north): x 48..63; its screen is x 51..60, y 3..6. A dark price line on the screen.
fx = 48
dark = (14, 52, 46, 255)
for x, y in ((4, 6), (5, 5), (6, 5), (7, 4), (8, 5), (9, 5), (10, 4), (11, 3)):
    put(rows, fx + x, y, dark)
write_png(os.path.join(TEX, 'block_trade_terminal.png'), w, h, rows)
print('block_trade_terminal.png written')

if len(sys.argv) > 2:
    scale = 4
    big = []
    for r in rows:
        line = bytearray()
        for x in range(w):
            line += bytes(r[x * 4:x * 4 + 4]) * scale
        for _ in range(scale):
            big.append(bytearray(line))
    write_png(sys.argv[2], w * scale, h * scale, big)
    print('preview', sys.argv[2])
