#!/usr/bin/env python3
"""
0.1.10.30: luminance copies of the frame core attachment textures (<texture>_lum.png next to each texture).

With a suit palette, the glowing segments of an attachment (graviton lights, propulsion nozzles, sun cores) shine in the palette's
light colour: the attachment layer draws them from this copy (white times the stretched luminance of each pixel, alpha kept)
tinted with the accent colour, instead of the coloured original. Without a palette nothing changes.

Usage: attachment_lum_1130.py <repo root> [--check]
"""
import json
import os
import sys

from PIL import Image

LOW = 0.45   # darkest grey of the copy (0..1): the darkest pixel still carries some of the light colour


def lum_copy(src):
    img = Image.open(src).convert('RGBA')
    raw = img.tobytes()
    px = [tuple(raw[i:i + 4]) for i in range(0, len(raw), 4)]
    lums = [0.2126 * r + 0.7152 * g + 0.0722 * b for r, g, b, a in px if a > 0]
    lo, hi = (min(lums), max(lums)) if lums else (0, 255)
    span = max(1e-6, hi - lo)
    out = []
    for r, g, b, a in px:
        if a == 0:
            out.append((0, 0, 0, 0))
            continue
        k = LOW + (1 - LOW) * ((0.2126 * r + 0.7152 * g + 0.0722 * b) - lo) / span
        v = int(round(255 * min(1.0, max(0.0, k))))
        out.append((v, v, v, a))
    copy = Image.new('RGBA', img.size)
    copy.putdata(out)
    return copy


def main():
    root = sys.argv[1]
    check = '--check' in sys.argv[2:]
    a = os.path.join(root, 'src/main/resources/assets/mio_icif')
    textures = set()
    d = os.path.join(a, 'armor_models/attachments')
    for name in sorted(os.listdir(d)):
        m = json.load(open(os.path.join(d, name), encoding='utf-8'))
        if any(s.get('emissive') or s.get('glow', 'none') != 'none' for s in m['segments']):
            textures.add(m['texture'].split(':', 1)[1])
    bad = 0
    for path in sorted(textures):
        src = os.path.join(a, path)
        dst = src[:-4] + '_lum.png'
        copy = lum_copy(src)
        if check:
            old = Image.open(dst).convert('RGBA') if os.path.exists(dst) else None
            same = old is not None and old.size == copy.size and old.tobytes() == copy.tobytes()
            print(path, 'same' if same else 'DIFFERENT')
            bad += 0 if same else 1
        else:
            copy.save(dst, optimize=True)
            print('wrote', dst)
    if check and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
