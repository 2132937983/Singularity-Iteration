#!/usr/bin/env python3
"""
0.1.8.0: makes the two colour layers of the suit palette from a suit texture (no dependencies).

For each input texture <name>.png it writes:
  <name>_plating.png  every pixel as a light grey value (the luminance, lifted so a tint shows on
                      dark plates). The armor layer multiplies it by the primary colour.
  <name>_accent.png   only the accent pixels (saturated and bright: the light strips, the visor),
                      as white times their luminance; every other pixel is transparent. The armor
                      layer multiplies it by the accent colour and draws it over the plating.

An accent pixel has a saturation of at least ACCENT_SAT and a value of at least ACCENT_VAL.

Usage: gen_suit_palette.py <texture dir> <name> [<name> ...]
"""
import os
import struct
import sys
import zlib

ACCENT_SAT = 0.45
ACCENT_VAL = 0.30
PLATING_FLOOR = 0.38   # lowest grey of the plating layer (0..1)


def read_png(path):
    with open(path, 'rb') as f:
        data = f.read()
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError('not a PNG: ' + path)
    pos, idat, palette, trns = 8, b'', None, None
    width = height = depth = ctype = interlace = None
    while pos < len(data):
        length, kind = struct.unpack('>I4s', data[pos:pos + 8])
        chunk = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b'IHDR':
            width, height, depth, ctype, _, _, interlace = struct.unpack('>IIBBBBB', chunk)
        elif kind == b'PLTE':
            palette = [tuple(chunk[i:i + 3]) for i in range(0, len(chunk), 3)]
        elif kind == b'tRNS':
            trns = chunk
        elif kind == b'IDAT':
            idat += chunk
        elif kind == b'IEND':
            break
    if depth != 8 or interlace:
        raise ValueError('only 8-bit non-interlaced PNG files are supported: ' + path)
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(idat)
    stride = width * channels
    rows, prev, at = [], bytearray(stride), 0
    for _ in range(height):
        kind = raw[at]
        line = bytearray(raw[at + 1:at + 1 + stride])
        at += 1 + stride
        for i in range(stride):
            a = line[i - channels] if i >= channels else 0
            b = prev[i]
            c = prev[i - channels] if i >= channels else 0
            if kind == 1:
                line[i] = (line[i] + a) & 255
            elif kind == 2:
                line[i] = (line[i] + b) & 255
            elif kind == 3:
                line[i] = (line[i] + (a + b) // 2) & 255
            elif kind == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line)
        prev = line
    pixels = []
    for line in rows:
        out = []
        for x in range(width):
            px = line[x * channels:(x + 1) * channels]
            if ctype == 6:
                out.append(tuple(px))
            elif ctype == 2:
                out.append((px[0], px[1], px[2], 255))
            elif ctype == 3:
                r, g, b = palette[px[0]]
                alpha = trns[px[0]] if trns and px[0] < len(trns) else 255
                out.append((r, g, b, alpha))
            elif ctype == 0:
                out.append((px[0], px[0], px[0], 255))
            else:
                out.append((px[0], px[0], px[0], px[1]))
        pixels.append(out)
    return width, height, pixels


def write_png(path, width, height, pixels):
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for r, g, b, a in row:
            raw += bytes((r, g, b, a))

    def chunk(kind, body):
        return struct.pack('>I', len(body)) + kind + body + struct.pack('>I', zlib.crc32(kind + body) & 0xffffffff)
    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n')
        f.write(chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)))
        f.write(chunk(b'IDAT', zlib.compress(bytes(raw), 9)))
        f.write(chunk(b'IEND', b''))


def is_accent(r, g, b, a):
    mx, mn = max(r, g, b) / 255.0, min(r, g, b) / 255.0
    sat = 0.0 if mx == 0 else (mx - mn) / mx
    return a > 0 and sat >= ACCENT_SAT and mx >= ACCENT_VAL


def luminance(r, g, b):
    return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0


def split(pixels):
    # Stretch the plating luminance of this texture to [PLATING_FLOOR, 1]: a dark texture keeps
    # its panel lines and shading after the tint.
    plates = [luminance(r, g, b) for row in pixels for (r, g, b, a) in row if a > 0 and not is_accent(r, g, b, a)]
    lo, hi = (min(plates), max(plates)) if plates else (0.0, 1.0)
    span = max(1e-6, hi - lo)
    plating, accent = [], []
    accents = 0
    for row in pixels:
        prow, arow = [], []
        for r, g, b, a in row:
            mx = max(r, g, b) / 255.0
            norm = min(1.0, max(0.0, (luminance(r, g, b) - lo) / span))
            grey = int(round(255 * (PLATING_FLOOR + (1 - PLATING_FLOOR) * norm)))
            if is_accent(r, g, b, a):
                accents += 1
                light = int(round(255 * min(1.0, 0.55 + 0.45 * mx)))
                arow.append((light, light, light, a))
                prow.append((int(grey * 0.6), int(grey * 0.6), int(grey * 0.6), a))
            else:
                arow.append((0, 0, 0, 0))
                prow.append((grey, grey, grey, a))
        plating.append(prow)
        accent.append(arow)
    return plating, accent, accents


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(2)
    folder = sys.argv[1]
    for name in sys.argv[2:]:
        width, height, pixels = read_png(os.path.join(folder, name + '.png'))
        plating, accent, count = split(pixels)
        write_png(os.path.join(folder, name + '_plating.png'), width, height, plating)
        write_png(os.path.join(folder, name + '_accent.png'), width, height, accent)
        print(name, width, 'x', height, 'accent pixels:', count)


if __name__ == '__main__':
    main()
