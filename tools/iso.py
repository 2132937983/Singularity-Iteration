"""Tiny isometric preview of full-cube block models (north face, east face, top)."""
import json, os, sys, math
from PIL import Image
A = sys.argv[1]; OUT = sys.argv[2]; names = sys.argv[3:]
def load(p): return json.load(open(p, encoding='utf-8-sig'))
def mp(ref):
    ns, p = ref.split(':') if ':' in ref else ('minecraft', ref)
    return os.path.join(A, 'models', p + '.json') if ns == 'mio_icif' else None
def resolve(path):
    chain = []; d = load(path)
    while True:
        chain.append(d); par = d.get('parent'); q = mp(par) if par else None
        if not q or not os.path.exists(q): break
        d = load(q)
    tex = {}; els = None
    for d in reversed(chain): tex.update(d.get('textures', {}))
    for d in chain:
        if 'elements' in d: els = d['elements']; break
    def r(t):
        for _ in range(8):
            if t.startswith('#'): t = tex.get(t[1:], t)
        return t
    return {k: r(v) for k, v in tex.items()}, els
def face_img(tex, fd):
    t = fd['texture'].lstrip('#'); ref = tex.get(t, '')
    ns, p = ref.split(':') if ':' in ref else ('minecraft', ref)
    im = Image.open(os.path.join(A, 'textures', p + '.png')).convert('RGBA')
    u0, v0, u1, v1 = fd.get('uv', [0, 0, 16, 16])
    sx, sy = im.width / 16, im.height / 16
    if im.height > im.width * 1.5: sy = im.width / 16  # animated strip: first frame
    box = (int(round(min(u0, u1) * sx)), int(round(min(v0, v1) * sy)), int(round(max(u0, u1) * sx)), int(round(max(v0, v1) * sy)))
    c = im.crop(box).resize((16, 16), Image.NEAREST)
    if u0 > u1: c = c.transpose(Image.FLIP_LEFT_RIGHT)
    if v0 > v1: c = c.transpose(Image.FLIP_TOP_BOTTOM)
    rot = fd.get('rotation', 0)
    if rot: c = c.rotate(-rot)
    return c
S = 6
def iso(path):
    tex, els = resolve(path)
    e = els[0]; f = e['faces']
    W = 32 * S; out = Image.new('RGBA', (W, W), (0, 0, 0, 0))
    def shade(img, k):
        px = img.load()
        for y in range(16):
            for x in range(16):
                r, g, b, a = px[x, y]; px[x, y] = (int(r * k), int(g * k), int(b * k), a)
        return img
    top = shade(face_img(tex, f['up']), 1.0); north = shade(face_img(tex, f['north']), 0.85); east = shade(face_img(tex, f['east']), 0.7)
    o = out.load()
    for y in range(16):
        for x in range(16):
            # top: diamond
            c = top.getpixel((x, y))
            cx = (x - y) * S + 16 * S; cy = (x + y) * S // 2
            for dx in range(S * 2):
                for dy in range(S):
                    if 0 <= cx + dx - S < W and 0 <= cy + dy < W: o[cx + dx - S, cy + dy] = c
            # north (left, facing viewer-left)
            c = north.getpixel((x, y)); px_ = x * S; py_ = 8 * S + x * S // 2 + y * S
            for dx in range(S):
                for dy in range(S + 1):
                    if 0 <= px_ + dx < W and 0 <= py_ + dy < W: o[px_ + dx, py_ + dy] = c
            c = east.getpixel((x, y)); px_ = 16 * S + x * S; py_ = 16 * S - x * S // 2 + y * S
            for dx in range(S):
                for dy in range(S + 1):
                    if 0 <= px_ + dx < W and 0 <= py_ + dy < W: o[px_ + dx, py_ + dy] = c
    return out
tiles = [iso(os.path.join(A, 'models/block', n + '.json')) for n in names]
cols = 6; rows = (len(tiles) + cols - 1) // cols
sheet = Image.new('RGBA', (cols * 32 * S, rows * 32 * S), (120, 150, 110, 255))
for i, t in enumerate(tiles): sheet.alpha_composite(t, ((i % cols) * 32 * S, (i // cols) * 32 * S))
sheet.save(OUT)
