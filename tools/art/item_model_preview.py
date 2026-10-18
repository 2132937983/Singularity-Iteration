#!/usr/bin/env python3
"""
0.1.10.4: offline preview of element item models (no dependencies).

Renders a Java block/item model (elements, element rotation, per-face UV and UV rotation, display
transforms) the way the game places it, so a 3D weapon can be checked without starting the game:

  gui        orthographic, the "gui" display transform (a model without one draws untransformed)
  first      first person, right hand: the vanilla arm offset (0.56, -0.52, -0.72) and the
             "firstperson_righthand" transform, perspective projection with a 70 degree FOV, 16:9
  ground     the "ground" transform, orthographic from the side and a little above
  fixed      the "fixed" transform (item frame), orthographic from the front
  side, top  the raw model from the side (+x) and from above, orthographic

Textures are sampled nearest; faces get a simple directional light (emissive elements:
"neoforge_data.block_light" > 0, are unlit).

Usage: item_model_preview.py <assets/mio_icif> <model ref, e.g. item/3d/weapon/nanosaber> <out.png> [views] [--cell=N]
  views: comma-separated list of the names above (default gui,first,ground,fixed,side,top)
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixart import read_png, write_png  # noqa: E402

FACES = {
    # vertex order of net.minecraft.client.renderer.FaceInfo; 0..2 = MIN/MAX of x, y, z
    'down': [(0, 0, 1), (0, 0, 0), (1, 0, 0), (1, 0, 1)],
    'up': [(0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)],
    'north': [(1, 1, 0), (1, 0, 0), (0, 0, 0), (0, 1, 0)],
    'south': [(0, 1, 1), (0, 0, 1), (1, 0, 1), (1, 1, 1)],
    'west': [(0, 1, 0), (0, 0, 0), (0, 0, 1), (0, 1, 1)],
    'east': [(1, 1, 1), (1, 0, 1), (1, 0, 0), (1, 1, 0)],
}
NORMALS = {'down': (0, -1, 0), 'up': (0, 1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0)}


def load(path):
    with open(path, encoding='utf-8-sig') as f:
        return json.load(f)


def resolve(assets, ref):
    """Elements, textures and display of a model with its mio_icif parents."""
    chain = []
    while ref:
        ns, path = ref.split(':', 1) if ':' in ref else ('mio_icif', ref)
        if ns != 'mio_icif':
            break
        file = os.path.join(assets, 'models', path + '.json')
        if not os.path.exists(file):
            break
        d = load(file)
        chain.append(d)
        ref = d.get('parent')
        if d.get('loader') == 'neoforge:separate_transforms':
            ref = d['base'].get('parent')
            chain[-1] = dict(d['base'], display={})
    tex, elements, display = {}, None, {}
    for d in reversed(chain):
        tex.update(d.get('textures', {}))
        display.update(d.get('display', {}))
    for d in chain:
        if 'elements' in d:
            elements = d['elements']
            break

    def res(t):
        for _ in range(8):
            if t.startswith('#'):
                t = tex.get(t[1:], t)
        return t
    return {k: res(v) for k, v in tex.items()}, elements or [], display


def rot(p, axis, deg, origin):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    x, y, z = p[0] - origin[0], p[1] - origin[1], p[2] - origin[2]
    if axis == 'x':
        y, z = y * c - z * s, y * s + z * c
    elif axis == 'y':
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return x + origin[0], y + origin[1], z + origin[2]


def quads(assets, ref):
    tex, elements, display = resolve(assets, ref)
    out = []
    for e in elements:
        f, t = e['from'], e['to']
        lo, hi = f, t
        r = e.get('rotation')
        light = (e.get('neoforge_data') or {}).get('block_light', 0)
        for name, face in e['faces'].items():
            corners = []
            for c in FACES[name]:
                p = tuple((hi if c[i] else lo)[i] for i in range(3))
                if r and r.get('angle'):
                    p = rot(p, r['axis'], r['angle'], r['origin'])
                corners.append(p)
            u0, v0, u1, v1 = face.get('uv', [0, 0, 16, 16])
            uv = [(u0, v0), (u0, v1), (u1, v1), (u1, v0)]
            k = (face.get('rotation', 0) // 90) % 4
            uv = uv[k:] + uv[:k]
            n = NORMALS[name]
            if r and r.get('angle'):
                n = rot(n, r['axis'], r['angle'], (0, 0, 0))
            ref_t = face['texture']
            out.append((corners, [(a / 16, b / 16) for a, b in uv], n, tex.get(ref_t.lstrip('#'), ref_t), light > 0))
    return out, display


def euler_xyz(v, deg):
    """Quaternionf.rotationXYZ: rotate about X, then Y, then Z (intrinsic) = apply Z, Y, X to a vector? No:
    rotationXYZ(a, b, c) builds q = qx * qy * qz, which rotates a vector by z first, then y, then x."""
    rx, ry, rz = (math.radians(a) for a in deg)
    x, y, z = v
    # z
    c, s = math.cos(rz), math.sin(rz)
    x, y = x * c - y * s, x * s + y * c
    # y
    c, s = math.cos(ry), math.sin(ry)
    x, z = x * c + z * s, -x * s + z * c
    # x
    c, s = math.cos(rx), math.sin(rx)
    y, z = y * c - z * s, y * s + z * c
    return x, y, z


def transform(p, tr):
    """Model space (0..16) -> item space of one display context (blocks), as ItemRenderer does."""
    x, y, z = (p[0] / 16 - 0.5, p[1] / 16 - 0.5, p[2] / 16 - 0.5)
    if tr:
        sc = tr.get('scale', [1, 1, 1])
        x, y, z = x * sc[0], y * sc[1], z * sc[2]
        x, y, z = euler_xyz((x, y, z), tr.get('rotation', [0, 0, 0]))
        t = tr.get('translation', [0, 0, 0])
        x, y, z = x + t[0] / 16, y + t[1] / 16, z + t[2] / 16
    return x, y, z


def raster(qs, textures, project, W, H, bg, normal_view):
    color = [[bg] * W for _ in range(H)]
    depth = [[-1e18] * W for _ in range(H)]
    light = (0.3, 0.8, 0.5)
    ln = math.sqrt(sum(c * c for c in light))
    light = tuple(c / ln for c in light)
    for pts, uvs, n, tex, emissive in qs:
        nv = normal_view(n)
        shade = 1.0 if emissive else 0.5 + 0.5 * max(0.0, nv[0] * light[0] + nv[1] * light[1] + nv[2] * light[2])
        tw, th, tpx = textures[tex]
        scr = [project(p) for p in pts]
        if any(s is None for s in scr):
            continue
        for tri in ((0, 1, 2), (0, 2, 3)):
            (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = (scr[i] for i in tri)
            uv = [uvs[i] for i in tri]
            area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
            if abs(area) < 1e-9:
                continue
            minx, maxx = max(0, int(min(x0, x1, x2))), min(W - 1, int(max(x0, x1, x2)) + 1)
            miny, maxy = max(0, int(min(y0, y1, y2))), min(H - 1, int(max(y0, y1, y2)) + 1)
            for yy in range(miny, maxy + 1):
                for xx in range(minx, maxx + 1):
                    px_, py_ = xx + 0.5, yy + 0.5
                    w0 = ((x1 - px_) * (y2 - py_) - (x2 - px_) * (y1 - py_)) / area
                    w1 = ((x2 - px_) * (y0 - py_) - (x0 - px_) * (y2 - py_)) / area
                    w2 = 1 - w0 - w1
                    if w0 < -1e-6 or w1 < -1e-6 or w2 < -1e-6:
                        continue
                    z = w0 * z0 + w1 * z1 + w2 * z2
                    if z <= depth[yy][xx]:
                        continue
                    u = w0 * uv[0][0] + w1 * uv[1][0] + w2 * uv[2][0]
                    v = w0 * uv[0][1] + w1 * uv[1][1] + w2 * uv[2][1]
                    tx = min(tw - 1, max(0, int(u * tw)))
                    ty = min(th - 1, max(0, int(v * th)))
                    r, g, b, a = tpx[ty * tw + tx]
                    if a < 20:
                        continue
                    depth[yy][xx] = z
                    color[yy][xx] = (int(r * shade), int(g * shade), int(b * shade), 255)
    return color


def texture_path(assets, ref):
    ns, path = ref.split(':', 1) if ':' in ref else ('minecraft', ref)
    return os.path.join(assets, 'textures', path + '.png')


def render_view(name, qs, display, textures, cell):
    W = H = cell
    bg = (139, 139, 139, 255) if name == 'gui' else (40, 46, 54, 255)
    if name == 'first':
        W, H = cell * 16 // 9, cell
        tr = display.get('firstperson_righthand')
        f = 1 / math.tan(math.radians(70) / 2)

        def place(p):
            x, y, z = transform(p, tr)
            return x + 0.56, y - 0.52, z - 0.72

        def project(p):
            x, y, z = place(p)
            if z > -0.05:
                return None
            return W / 2 + (x * f / -z) * H / 2, H / 2 - (y * f / -z) * H / 2, z
        normal_view = lambda n: euler_xyz(n, (tr or {}).get('rotation', [0, 0, 0]))
        return raster(qs, textures, project, W, H, bg, normal_view), W, H
    key = {'gui': 'gui', 'ground': 'ground', 'fixed': 'fixed'}.get(name)
    tr = display.get(key) if key else None
    yaw, pitch = {'ground': (90, 25), 'side': (90, 0), 'top': (0, 89)}.get(name, (0, 0))
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    scale = cell * (0.62 if name in ('gui', 'fixed') else 0.4)

    def view(v):
        x, y, z = v
        x1 = x * cy - z * sy
        d = x * sy + z * cy
        y1 = y * cp - d * sp
        d1 = d * cp + y * sp
        return x1, y1, d1

    def project(p):
        x, y, z = view(transform(p, tr) if key else (p[0] / 16 - 0.5, p[1] / 16 - 0.5, p[2] / 16 - 0.5))
        return W / 2 + x * scale, H / 2 - y * scale, z
    normal_view = lambda n: view(euler_xyz(n, (tr or {}).get('rotation', [0, 0, 0])) if key else n)
    return raster(qs, textures, project, W, H, bg, normal_view), W, H


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    opts = dict(a[2:].split('=', 1) for a in sys.argv[1:] if a.startswith('--'))
    assets, ref, out = args[0], args[1], args[2]
    views = (args[3] if len(args) > 3 else 'gui,first,ground,fixed,side,top').split(',')
    cell = int(opts.get('cell', 200))
    qs, display = quads(assets, ref)
    textures = {}
    frame = int(opts.get('frame', 0))
    for q in qs:
        if q[3] not in textures:
            w, h, px = read_png(texture_path(assets, q[3]))
            if h > w and h % w == 0:                        # animation strip: one frame
                k = frame % (h // w)
                px, h = px[k * w * w:(k + 1) * w * w], w
            textures[q[3]] = (w, h, px)
    images = [render_view(v, qs, display, textures, cell) for v in views]
    W = sum(w for _, w, _ in images) + 4 * (len(images) + 1)
    H = max(h for _, _, h in images) + 8
    sheet = [(12, 14, 18, 255)] * (W * H)
    ox = 4
    for img, w, h in images:
        for y in range(h):
            for x in range(w):
                sheet[(y + 4) * W + ox + x] = img[y][x]
        ox += w + 4
    write_png(out, W, H, sheet)
    print('wrote', out, len(qs), 'quads')


if __name__ == '__main__':
    main()
