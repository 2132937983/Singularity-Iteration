#!/usr/bin/env python3
"""
SI "refined classic" machine models.

Takes the MioPha / IC2-style full-cube machine models and produces, per model:
  * a 128x64 texture (6 faces at 32x32 + an edge cell) where the brick casing is
    replaced by a clean, finely shaded industrial plate and every functional pixel of
    the original face (screens, vents, ports, lamps...) is kept at its position,
    upscaled 2x and given a 1-px bevel so it reads as engraved / inset;
  * a 4-element model: the body cube plus three cross slabs, so every one of the six
    faces carries a panel standing 0.5 px proud inside a 1.5 px frame band - a crisp
    raised-panel look that catches light on its edges at almost no vertex cost.

Texture continuity: every element face takes the UV slice of its face cell that the
default Minecraft UV mapping would give it, so outer faces line up seamlessly.

Usage: refine_models.py <assets/mio_icif> [--preview out.png ids...] [--write]
"""
import json, os, sys, glob, math
from PIL import Image

FACES = ('north', 'east', 'south', 'west', 'up', 'down')
CELL = {'north': 0, 'east': 1, 'south': 2, 'west': 3, 'up': 4, 'down': 5}
EDGE_CELL = 6
FRAME = 1.5          # width of the frame band left visible around each raised panel (model px)
RAISE = 0.5          # how far each face panel stands proud of the block (model px = 1 texel at 32 px)
TW, TH = 128, 64     # texture size: 4 x 2 cells of 32 px


def load(p):
    return json.load(open(p, encoding='utf-8-sig'))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def sat(c):
    return max(c[:3]) - min(c[:3])


def clamp(v):
    return max(0, min(255, int(round(v))))


def scale(c, f):
    return (clamp(c[0] * f), clamp(c[1] * f), clamp(c[2] * f), c[3] if len(c) > 3 else 255)


def h32(n):
    n &= 0xFFFFFFFF
    n = ((n >> 16) ^ n) * 0x45d9f3b & 0xFFFFFFFF
    n = ((n >> 16) ^ n) * 0x45d9f3b & 0xFFFFFFFF
    return (n >> 16) ^ n


# ------------------------------------------------------------------ face extraction
def crop_face(assets, tex_ref, uv, rotation=0):
    ns, path = tex_ref.split(':') if ':' in tex_ref else ('minecraft', tex_ref)
    img = Image.open(os.path.join(assets, 'textures', path + '.png')).convert('RGBA')
    w, h = img.size
    u0, v0, u1, v1 = uv
    box = (round(min(u0, u1) * w / 16), round(min(v0, v1) * h / 16), round(max(u0, u1) * w / 16), round(max(v0, v1) * h / 16))
    face = img.crop(box).resize((16, 16), Image.NEAREST)
    if u0 > u1: face = face.transpose(Image.FLIP_LEFT_RIGHT)
    if v0 > v1: face = face.transpose(Image.FLIP_TOP_BOTTOM)
    if rotation: face = face.rotate(-rotation)
    return face


def feature_mask(face, refs):
    """True where the face carries machine-specific detail rather than casing."""
    px = face.load()
    best = None
    for ref in refs:
        rp = ref.load()
        diff = [[sum(abs(px[x, y][i] - rp[x, y][i]) for i in range(3)) > 24 for x in range(16)] for y in range(16)]
        n = sum(map(sum, diff))
        if best is None or n < best[0]: best = (n, diff)
    if best is not None and best[0] < 16 * 16 * 0.6:
        return best[1]
    # fallback: neutral light pixels are casing
    return [[not (sat(px[x, y]) < 22 and lum(px[x, y]) >= 110) for x in range(16)] for y in range(16)]


# ------------------------------------------------------------------ painting
def plate(base_l, role, size=32):
    """Face cell: a 3-texel frame band around a raised panel (texels 3..28).

    The body face shows only the band; the raised panel element shows the middle.
    Lighting cue: panel edges lit top/left and shaded bottom/right, the band gets a
    soft contact shadow where the panel stands on it."""
    img = Image.new('RGBA', (size, size))
    p = img.load()
    for y in range(size):
        for x in range(size):
            t = y / (size - 1)
            l = base_l * (1.04 - 0.08 * t) + ((h32(x * 7919 + y * 104729 + int(base_l)) % 5) - 2) * 0.6
            band = x < 3 or y < 3 or x > size - 4 or y > size - 4
            if band: l *= 0.93                     # frame band: slightly darker machined metal
            p[x, y] = (clamp(l - 2), clamp(l), clamp(l + 3), 255)
    ref = p[size // 2, size // 2]
    rim = scale(p[0, 0], 0.74)
    for i in range(size):                         # outer rim line + inner highlight of the frame
        p[i, 0] = rim; p[i, size - 1] = rim; p[0, i] = rim; p[size - 1, i] = rim
    for i in range(1, size - 1):
        p[i, 1] = scale(p[i, 1], 1.07); p[1, i] = scale(p[1, i], 1.07)
    for i in range(3, size - 3):                  # raised panel bevel
        p[i, 3] = scale(ref, 1.10); p[3, i] = scale(ref, 1.10)
        p[i, size - 4] = scale(ref, 0.84); p[size - 4, i] = scale(ref, 0.84)
    for i in range(2, size - 2):                  # contact shadow on the band below/right of the panel
        p[i, size - 3] = scale(p[i, size - 3], 0.86); p[size - 3, i] = scale(p[size - 3, i], 0.86)
    if role in ('side', 'up', 'down'):
        seam = size // 2
        for i in range(5, size - 5):
            p[i, seam - 1] = scale(p[i, seam - 1], 0.88)
            p[i, seam] = scale(p[i, seam], 1.06)
    return img


def screws(img, mask16, base_l):
    p = img.load()
    for (sx, sy) in ((6, 6), (24, 6), (6, 24), (24, 24)):
        if any(mask16[min(15, (sy + dy) // 2)][min(15, (sx + dx) // 2)] for dx in (0, 1) for dy in (0, 1)):
            continue
        p[sx, sy] = scale(p[sx, sy], 0.72); p[sx + 1, sy] = scale(p[sx + 1, sy], 0.84)
        p[sx, sy + 1] = scale(p[sx, sy + 1], 0.84); p[sx + 1, sy + 1] = scale(p[sx + 1, sy + 1], 1.08)


def refine_face(face, mask16, role, base_l):
    out = plate(base_l, role)
    if role == 'side':
        screws(out, mask16, base_l)
    op = out.load()
    src = face.load()
    feat = [[mask16[y // 2][x // 2] for x in range(32)] for y in range(32)]
    for y in range(32):
        for x in range(32):
            if feat[y][x]:
                c = src[x // 2, y // 2]
                if c[3] < 8: continue
                if sat(c) < 22 and lum(c) > 170:
                    # light neutral trim (old casing highlights): keep the shape, tone it into the plate
                    op[x, y] = scale(op[x, y], 1.0 + min(0.07, (lum(c) - 170) / 900))
                    feat[y][x] = False
                    continue
                op[x, y] = (c[0], c[1], c[2], 255)
    # bevel: features read as set into the plate (lit from the top-left)
    snap = out.copy().load()
    for y in range(32):
        for x in range(32):
            f = feat[y][x]
            up = feat[y - 1][x] if y > 0 else f
            left = feat[y][x - 1] if x > 0 else f
            down = feat[y + 1][x] if y < 31 else f
            right = feat[y][x + 1] if x < 31 else f
            if f:
                if not up or not left: op[x, y] = scale(snap[x, y], 0.80)      # inner shadow
                elif not down or not right: op[x, y] = scale(snap[x, y], 1.10)  # lit lower lip
            else:
                if down or right:
                    if 1 < x < 30 and 1 < y < 30: op[x, y] = scale(snap[x, y], 0.90)   # rim above/left
                elif up or left:
                    if 1 < x < 30 and 1 < y < 30: op[x, y] = scale(snap[x, y], 1.05)   # catch light below/right
    return out


def edge_cell(base_l):
    """Thin side walls (0.5 px) of the raised panels."""
    img = Image.new('RGBA', (32, 32))
    p = img.load()
    for y in range(32):
        for x in range(32):
            l = base_l * (0.86 - 0.06 * (y / 31))
            p[x, y] = (clamp(l - 2), clamp(l), clamp(l + 3), 255)
    return img


# ------------------------------------------------------------------ geometry
def default_uv(face, f, t):
    """Minecraft's automatic UV rectangle for a face of the box f..t (model px)."""
    x0, y0, z0 = f; x1, y1, z1 = t
    return {
        'north': (16 - x1, 16 - y1, 16 - x0, 16 - y0),
        'south': (x0, 16 - y1, x1, 16 - y0),
        'east': (16 - z1, 16 - y1, 16 - z0, 16 - y0),
        'west': (z0, 16 - y1, z1, 16 - y0),
        'up': (x0, z0, x1, z1),
        'down': (x0, 16 - z1, x1, 16 - z0),
    }[face]


def cell_uv(cell, rect):
    cu, cv = (cell % 4) * 4.0, (cell // 4) * 8.0
    u0, v0, u1, v1 = rect
    return [round(cu + u0 / 16 * 4, 5), round(cv + v0 / 16 * 8, 5), round(cu + u1 / 16 * 4, 5), round(cv + v1 / 16 * 8, 5)]


def element(name, f, t, faces):
    return {'name': name, 'from': list(f), 'to': list(t), 'faces': faces}


def face_entry(face, f, t, cell=None, cull=None):
    rect = default_uv(face, f, t)
    e = {'uv': cell_uv(CELL[face] if cell is None else cell, rect), 'texture': '#r'}
    if cull: e['cullface'] = cull
    return e


def build_elements():
    """Body cube + three cross slabs. Each slab pokes out RAISE px through a pair of
    opposite faces, giving every face a raised panel with a FRAME-wide band around it
    (4 elements in total). Panel faces cull with the face they sit on, so buried sides
    between adjacent machines cost nothing."""
    B, R = FRAME, RAISE
    lo, hi = B, 16 - B
    els = []
    body_f, body_t = (0, 0, 0), (16, 16, 16)
    els.append(element('body', body_f, body_t, {fn: face_entry(fn, body_f, body_t, cull=fn) for fn in FACES}))
    slabs = {
        'panels_ew': ((-R, lo, lo), (16 + R, hi, hi), ('east', 'west')),
        'panels_ud': ((lo, -R, lo), (hi, 16 + R, hi), ('up', 'down')),
        'panels_ns': ((lo, lo, -R), (hi, hi, 16 + R), ('north', 'south')),
    }
    for name, (f, t, outer) in slabs.items():
        faces = {}
        for fn in FACES:
            if fn in outer:
                faces[fn] = face_entry(fn, f, t, cull=fn)
            else:
                faces[fn] = {'uv': cell_uv(EDGE_CELL, (0, 0, 16, 16)), 'texture': '#r'}
        els.append(element(name, f, t, faces))
    return els


COPPER_CELL = 7


def copper_cell():
    """Top/bottom ledges of the coil windings."""
    img = Image.new('RGBA', (32, 32))
    p = img.load()
    for y in range(32):
        for x in range(32):
            k = 1.0 - 0.18 * (y / 31) + ((h32(x * 31 + y * 17) % 3) - 1) * 0.02
            p[x, y] = (clamp(236 * k), clamp(128 * k), clamp(40 * k), 255)
    return img


def tesla_elements():
    """Tesla coil: the grey/dark housing sits 1 px in, four copper winding packs stand
    proud to the full block edge (5 elements). No two faces share a plane and direction."""
    els = []
    core_f, core_t = (1, 0, 1), (15, 16, 15)
    els.append(element('housing', core_f, core_t, {
        'north': face_entry('north', core_f, core_t), 'south': face_entry('south', core_f, core_t),
        'east': face_entry('east', core_f, core_t), 'west': face_entry('west', core_f, core_t),
        'up': face_entry('up', core_f, core_t, cull='up')}))       # no down face: the lowest pack closes the bottom
    for i, (y0, y1) in enumerate(((12, 15), (8, 11), (4, 7), (0, 3))):
        f, t = (0, y0, 0), (16, y1, 16)
        faces = {fn: face_entry(fn, f, t, cull=fn) for fn in ('north', 'south', 'east', 'west')}
        faces['up'] = {'uv': cell_uv(COPPER_CELL, (0, 0, 16, 16)), 'texture': '#r'}
        if y0 == 0:
            faces['down'] = face_entry('down', f, t, cull='down')
        else:
            faces['down'] = {'uv': cell_uv(COPPER_CELL, (0, 0, 16, 16)), 'texture': '#r'}
        els.append(element(f'winding_{i}', f, t, faces))
    return els


SPECIAL_GEOMETRY = {'producer/block_tesla': tesla_elements}


# ------------------------------------------------------------------ per-model
def machine_models(assets):
    """Full-cube machine models, including texture-only children of full-cube parents
    (resolved against the original parent so they keep their own look)."""
    pref = ["producer/", "generator/", "kugenerator/", "hugenerator/", "energy_converter/", "oilrig/", "checker/",
            "wiring/transformer", "wiring/block_", "reactor/"]
    root = os.path.join(assets, 'models/block')
    originals = {}
    for f in glob.glob(os.path.join(root, '**/*.json'), recursive=True):
        rel = os.path.relpath(f, root)[:-5]
        try: originals[rel] = (f, load(f))
        except Exception: pass
    def resolve(rel, depth=0):
        f, d = originals[rel]
        if 'elements' in d or depth > 6: return dict(d)
        par = d.get('parent', '')
        prel = par.split(':', 1)[1][len('block/'):] if par.startswith('mio_icif:block/') else None
        if not prel or prel not in originals: return dict(d)
        base = resolve(prel, depth + 1)
        merged = dict(base)
        tex = dict(base.get('textures', {})); tex.update(d.get('textures', {}))
        merged['textures'] = tex
        return merged
    out = []
    for rel in sorted(originals):
        if not any(rel.startswith(p) for p in pref): continue
        f, raw = originals[rel]
        if 'refined' in raw.get('credit', ''): continue
        d = resolve(rel)
        els = d.get('elements')
        if not els or len(els) != 1 or els[0]['from'] != [0, 0, 0] or els[0]['to'] != [16, 16, 16]: continue
        out.append((rel, f, d))
    return out


def refine_model(assets, rel, d, casing_refs):
    el = d['elements'][0]
    tex = d.get('textures', {})
    def res(t):
        for _ in range(8):
            if t.startswith('#'): t = tex.get(t[1:], t)
        return t
    faces16 = {}
    for fn in FACES:
        fd = el['faces'].get(fn)
        if fd is None: return None
        faces16[fn] = crop_face(assets, res(fd['texture']), fd.get('uv', [0, 0, 16, 16]), fd.get('rotation', 0))
    refs = list(casing_refs)
    strip = Image.new('RGBA', (TW, TH), (0, 0, 0, 0))
    base_ls = []
    for fn in FACES:
        mask = feature_mask(faces16[fn], refs)
        casing = [faces16[fn].getpixel((x, y)) for y in range(16) for x in range(16) if not mask[y][x]]
        mean = sum(lum(c) for c in casing) / len(casing) if casing else 200
        base_l = max(60, min(214, mean * 0.92))
        base_ls.append(base_l)
        role = 'front' if fn == 'north' else ('side' if fn in ('east', 'west', 'south') else fn)
        cell = CELL[fn]
        strip.paste(refine_face(faces16[fn], mask, role, base_l), ((cell % 4) * 32, (cell // 4) * 32))
    strip.paste(edge_cell(base_ls[0]), ((EDGE_CELL % 4) * 32, (EDGE_CELL // 4) * 32))
    strip.paste(copper_cell(), ((COPPER_CELL % 4) * 32, (COPPER_CELL // 4) * 32))
    model = {
        'credit': 'Singularity Iteration refined classic machine (refine_models.py)',
        'parent': 'block/block',
        'textures': {'particle': tex.get('particle', 'mio_icif:block/producer/block_machine'),
                     'r': 'mio_icif:block/refined/' + rel},
        'elements': SPECIAL_GEOMETRY.get(rel.replace('_on', ''), build_elements)(),
    }
    if 'display' in d: model['display'] = d['display']
    return strip, model


def common_faces(assets, models, min_models=6):
    """Casing patterns = face images shared by many machines (plain sides, tops, bottoms)."""
    from collections import Counter
    count = Counter(); imgs = {}; owners = {}
    for rel, path, d in models:
        el = d['elements'][0]; tex = d.get('textures', {})
        for fn in FACES:
            fd = el['faces'].get(fn)
            if not fd: continue
            t = fd['texture']
            for _ in range(8):
                if t.startswith('#'): t = tex.get(t[1:], t)
            try:
                img = crop_face(assets, t, fd.get('uv', [0, 0, 16, 16]), fd.get('rotation', 0))
            except Exception:
                continue
            key = img.tobytes()
            count[key] += 1; imgs[key] = img
            owners.setdefault(key, set()).add(rel.replace('_on', ''))
    def plain(img):
        px = [img.getpixel((x, y)) for y in range(16) for x in range(16)]
        neutral = sum(1 for c in px if sat(c) < 22) / 256
        dark = sum(1 for c in px if lum(c) < 110) / 256
        return neutral >= 0.92 and dark <= 0.03
    refs = [imgs[k] for k, n in count.most_common() if len(owners[k]) >= min_models and plain(imgs[k])]
    print('casing patterns:', len(refs))
    return refs


# ------------------------------------------------------------------ preview renderer (z-buffered splats)
def render(model, strip, scale=7):
    W = int(30 * scale); H = int(34 * scale)
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    zbuf = [[1e9] * W for _ in range(H)]
    px = img.load(); sp = strip.load()
    shade = {'north': 0.86, 'east': 0.72, 'up': 1.0, 'south': 0.8, 'west': 0.8, 'down': 0.6}
    normal = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}
    dv = (-1, -1, 1)
    for el in model['elements']:
        f, t = el['from'], el['to']
        for fn, fd in el['faces'].items():
            n = normal[fn]
            if n[0] * dv[0] + n[1] * dv[1] + n[2] * dv[2] >= 0: continue
            rect = default_uv(fn, f, t)
            u0, v0, u1, v1 = fd['uv']
            steps = max(64, int(scale * 10 * max(t[0] - f[0], t[1] - f[1], t[2] - f[2]) / 16))
            for a in range(steps):
                for b in range(steps):
                    s = (a + 0.5) / steps; r = (b + 0.5) / steps
                    if fn in ('north', 'south'):
                        x = f[0] + (t[0] - f[0]) * s; y = f[1] + (t[1] - f[1]) * r; z = f[2] if fn == 'north' else t[2]
                    elif fn in ('east', 'west'):
                        z = f[2] + (t[2] - f[2]) * s; y = f[1] + (t[1] - f[1]) * r; x = t[0] if fn == 'east' else f[0]
                    else:
                        x = f[0] + (t[0] - f[0]) * s; z = f[2] + (t[2] - f[2]) * r; y = t[1] if fn == 'up' else f[1]
                    du, dvv = {
                        'north': (16 - x, 16 - y), 'south': (x, 16 - y), 'east': (16 - z, 16 - y),
                        'west': (z, 16 - y), 'up': (x, z), 'down': (x, 16 - z)}[fn]
                    fu = (du - rect[0]) / max(1e-6, rect[2] - rect[0]); fv = (dvv - rect[1]) / max(1e-6, rect[3] - rect[1])
                    tu = (u0 + (u1 - u0) * fu) / 16 * TW; tv = (v0 + (v1 - v0) * fv) / 16 * TH
                    c = sp[min(TW - 1, max(0, int(tu))), min(TH - 1, max(0, int(tv)))]
                    sx = (x + z) * 0.866 * scale + 1.5 * scale
                    sy = ((x - z) * 0.5 - y) * scale + 25 * scale
                    depth = -x - y + z
                    k = shade[fn]
                    col = (clamp(c[0] * k), clamp(c[1] * k), clamp(c[2] * k), 255)
                    for oy in range(2):
                        for ox in range(2):
                            ix, iy = int(sx) + ox, int(sy) + oy
                            if 0 <= ix < W and 0 <= iy < H and depth < zbuf[iy][ix]:
                                zbuf[iy][ix] = depth; px[ix, iy] = col
    return img


def main():
    assets = sys.argv[1]
    write = '--write' in sys.argv
    preview = None
    ids = []
    if '--preview' in sys.argv:
        i = sys.argv.index('--preview'); preview = sys.argv[i + 1]
        ids = [a for a in sys.argv[i + 2:] if not a.startswith('--')]
    models = machine_models(assets)
    casing_refs = common_faces(assets, models)
    done = 0; tiles = []
    for rel, path, d in models:
        if ids and rel not in ids: continue
        r = refine_model(assets, rel, d, casing_refs)
        if r is None: continue
        strip, model = r
        if write:
            tp = os.path.join(assets, 'textures/block/refined', rel + '.png')
            os.makedirs(os.path.dirname(tp), exist_ok=True)
            strip.save(tp)
            json.dump(model, open(path, 'w'), indent=1)
        if preview: tiles.append((rel, render(model, strip)))
        done += 1
    print('refined', done, 'of', len(models))
    if preview and tiles:
        tw, th = tiles[0][1].size
        cols = min(6, len(tiles)); rows = (len(tiles) + cols - 1) // cols
        sheet = Image.new('RGBA', (cols * tw, rows * th), (118, 140, 108, 255))
        for i, (_, t) in enumerate(tiles):
            sheet.alpha_composite(t, ((i % cols) * tw, (i // cols) * th))
        sheet.save(preview)


if __name__ == '__main__':
    main()
