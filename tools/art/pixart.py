#!/usr/bin/env python3
"""Small pure-Python pixel-art kit (no PIL): RGBA canvas, primitives, PNG read/write, preview sheets.

Used by the 0.1.8.2 texture generators (crops_182.py, tools_182.py). Colours are (r, g, b, a) tuples.
"""
import struct
import zlib

TRANSPARENT = (0, 0, 0, 0)


def hexc(value, alpha=255):
    value = value.lstrip('#')
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


def mix(a, b, t):
    """Linear mix of two colours (t = 0 gives a)."""
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def shade(c, f):
    """f > 0 brightens toward white, f < 0 darkens toward black."""
    if f >= 0:
        return mix(c, (255, 255, 255, c[3]), f)
    return mix(c, (0, 0, 0, c[3]), -f)


def ramp(base, n=4, dark=-0.45, light=0.35):
    """n colours from dark to light around base."""
    out = []
    for i in range(n):
        t = i / (n - 1) if n > 1 else 0.5
        f = dark + (light - dark) * t
        out.append(shade(base, f))
    return out


class Canvas:
    def __init__(self, w, h, fill=TRANSPARENT):
        self.w, self.h = w, h
        self.px = [fill] * (w * h)

    def inside(self, x, y):
        return 0 <= x < self.w and 0 <= y < self.h

    def get(self, x, y):
        return self.px[y * self.w + x] if self.inside(x, y) else TRANSPARENT

    def set(self, x, y, c):
        x, y = int(x), int(y)
        if not self.inside(x, y) or c is None:
            return
        if c[3] >= 255 or self.px[y * self.w + x][3] == 0:
            self.px[y * self.w + x] = c if c[3] > 0 else self.px[y * self.w + x]
        else:
            self.px[y * self.w + x] = blend(self.px[y * self.w + x], c)

    def put(self, x, y, c):
        """Set without blending (also allows clearing)."""
        x, y = int(x), int(y)
        if self.inside(x, y):
            self.px[y * self.w + x] = c

    def line(self, x0, y0, x1, y1, c):
        x0, y0, x1, y1 = int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            self.set(x0, y0, c)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def rect(self, x0, y0, x1, y1, c):
        for y in range(int(y0), int(y1) + 1):
            for x in range(int(x0), int(x1) + 1):
                self.set(x, y, c)

    def ellipse(self, cx, cy, rx, ry, c):
        for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
            for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
                if rx <= 0 or ry <= 0:
                    continue
                if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1.0:
                    self.set(x, y, c)

    def outline(self, c, where=None):
        """Darken transparent pixels next to opaque ones (a 1 px outline)."""
        src = list(self.px)
        for y in range(self.h):
            for x in range(self.w):
                if src[y * self.w + x][3] != 0:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < self.w and 0 <= ny < self.h and src[ny * self.w + nx][3] > 0:
                        if where is None or where(nx, ny, src[ny * self.w + nx]):
                            self.px[y * self.w + x] = c
                            break

    def copy(self):
        o = Canvas(self.w, self.h)
        o.px = list(self.px)
        return o

    def paste(self, other, ox, oy):
        for y in range(other.h):
            for x in range(other.w):
                c = other.px[y * other.w + x]
                if c[3]:
                    self.set(ox + x, oy + y, c)

    def scaled(self, k):
        o = Canvas(self.w * k, self.h * k)
        for y in range(o.h):
            for x in range(o.w):
                o.px[y * o.w + x] = self.px[(y // k) * self.w + (x // k)]
        return o

    def opaque_count(self):
        return sum(1 for p in self.px if p[3] > 0)

    def save(self, path):
        write_png(path, self.w, self.h, self.px)


def blend(dst, src):
    a = src[3] / 255.0
    b = dst[3] / 255.0
    out_a = a + b * (1 - a)
    if out_a <= 0:
        return TRANSPARENT
    rgb = [int(round((src[i] * a + dst[i] * b * (1 - a)) / out_a)) for i in range(3)]
    return (rgb[0], rgb[1], rgb[2], int(round(out_a * 255)))


# ------------------------------------------------------------------ PNG
def write_png(path, w, h, px):
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w):
            raw.extend(bytes(px[y * w + x]))
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
    data += chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b'')
    with open(path, 'wb') as f:
        f.write(data)


def read_png(path):
    """Returns (w, h, px) for 8-bit and low-bit PNGs (colour types 0, 2, 3, 4, 6; no interlace)."""
    with open(path, 'rb') as f:
        data = f.read()
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError('not a PNG: ' + path)
    pos, idat, plte, trns = 8, bytearray(), None, None
    w = h = depth = ctype = interlace = 0
    while pos < len(data):
        length = struct.unpack('>I', data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b'IHDR':
            w, h, depth, ctype, _, _, interlace = struct.unpack('>IIBBBBB', body)
        elif kind == b'PLTE':
            plte = [tuple(body[i:i + 3]) for i in range(0, len(body), 3)]
        elif kind == b'tRNS':
            trns = body
        elif kind == b'IDAT':
            idat.extend(body)
    if interlace:
        raise ValueError('interlaced PNG not supported: ' + path)
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    bpp = max(1, channels * depth // 8)
    stride = (w * channels * depth + 7) // 8
    raw = zlib.decompress(bytes(idat))
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(h):
        ft = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if ft == 1:
                line[x] = (line[x] + a) & 255
            elif ft == 2:
                line[x] = (line[x] + b) & 255
            elif ft == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif ft == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line)
        prev = line
    px = []
    for line in rows:
        if depth == 8:
            vals = list(line)
        elif depth == 16:
            vals = [line[k] for k in range(0, len(line), 2)]
        else:
            vals = []
            for byte in line:
                for s in range(8 // depth):
                    vals.append((byte >> (8 - depth * (s + 1))) & ((1 << depth) - 1))
        for x in range(w):
            if ctype == 6:
                px.append(tuple(vals[x * 4:x * 4 + 4]))
            elif ctype == 2:
                r, g, b = vals[x * 3:x * 3 + 3]
                px.append((r, g, b, 255))
            elif ctype == 3:
                idx = vals[x]
                r, g, b = plte[idx]
                a = trns[idx] if trns is not None and idx < len(trns) else 255
                px.append((r, g, b, a))
            elif ctype == 0:
                v = vals[x] * (255 // ((1 << depth) - 1)) if depth < 8 else vals[x]
                px.append((v, v, v, 255))
            elif ctype == 4:
                v, a = vals[x * 2:x * 2 + 2]
                px.append((v, v, v, a))
    return w, h, px


def load(path):
    w, h, px = read_png(path)
    c = Canvas(w, h)
    c.px = px
    return c


# ------------------------------------------------------------------ preview sheets
def sheet(images, cols, scale, pad=2, bg=((58, 62, 68, 255), (70, 75, 82, 255))):
    """Grid of images on a checkerboard; images may be None (gap). Each cell is the size of the largest image."""
    cw = max((im.w for im in images if im), default=16) * scale
    ch = max((im.h for im in images if im), default=16) * scale
    rows = (len(images) + cols - 1) // cols
    out = Canvas(cols * (cw + pad) + pad, rows * (ch + pad) + pad)
    for y in range(out.h):
        for x in range(out.w):
            out.px[y * out.w + x] = bg[((x // 8) + (y // 8)) & 1]
    for i, im in enumerate(images):
        if im is None:
            continue
        ox = pad + (i % cols) * (cw + pad)
        oy = pad + (i // cols) * (ch + pad)
        big = im.scaled(scale)
        out.paste(big, ox + (cw - big.w) // 2, oy + (ch - big.h))
    return out
