"""Orthographic renderer for element-based block models (supports y/x/z element rotation).
Usage: elemrender.py <assets/mio_icif> <model ref e.g. block/producer/x> <out.png> [size] [yaw] [pitch]"""
import json, os, sys, math
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from bbexport import resolved
A, ref, out = sys.argv[1], sys.argv[2], sys.argv[3]
S = int(sys.argv[4]) if len(sys.argv) > 4 else 256
YAW = math.radians(float(sys.argv[5]) if len(sys.argv) > 5 else 225)
PIT = math.radians(float(sys.argv[6]) if len(sys.argv) > 6 else 30)
tex, els = resolved(os.path.join(A, 'models', ref + '.json'))
imgs = {}
def timg(key):
    t = tex.get(key.lstrip('#'), key)
    if t not in imgs:
        ns, p = t.split(':'); imgs[t] = np.asarray(Image.open(os.path.join(A, 'textures', p + '.png')).convert('RGBA')).astype(float)
    return imgs[t]
def rot(v, r):
    if not r: return v
    a = math.radians(r['angle']); o = np.array(r['origin'], float); p = v - o
    c, s = math.cos(a), math.sin(a)
    x, y, z = p
    if r['axis'] == 'y': p = np.array([x * c + z * s, y, -x * s + z * c])
    elif r['axis'] == 'x': p = np.array([x, y * c - z * s, y * s + z * c])
    else: p = np.array([x * c - y * s, x * s + y * c, z])
    return p + o
# face corners: (u0,v0) top-left ... in MC face orientation
def corners(f, t, d):
    x0, y0, z0 = f; x1, y1, z1 = t
    return {'north': [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
            'south': [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
            'east': [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
            'west': [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
            'up': [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
            'down': [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)]}[d]
SHADE = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'east': 0.6, 'west': 0.6}
cy, sy, cp, sp = math.cos(YAW), math.sin(YAW), math.cos(PIT), math.sin(PIT)
def proj(p):
    x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
    rx = x * cy - z * sy; rz = x * sy + z * cy
    ry = y * cp - rz * sp; d = y * sp + rz * cp
    k = S / 30.0
    return np.array([S / 2 + rx * k, S / 2 - ry * k]), d
img = np.zeros((S, S, 4)); zbuf = np.full((S, S), -1e9)
for e in els:
    for d, fd in e.get('faces', {}).items():
        P = [rot(np.array(c, float), e.get('rotation')) for c in corners(e['from'], e['to'], d)]
        q = [proj(p) for p in P]
        a, b, c4 = q[0][0], q[1][0], q[3][0]
        U, V = b - a, c4 - a
        M = np.array([[U[0], V[0]], [U[1], V[1]]])
        if abs(np.linalg.det(M)) < 1e-6: continue
        Minv = np.linalg.inv(M)
        da, db, dc = q[0][1], q[1][1], q[3][1]
        xs = [p[0][0] for p in q]; ys = [p[0][1] for p in q]
        T = timg(fd['texture']); th, tw = T.shape[:2]
        u0, v0, u1, v1 = fd.get('uv', [0, 0, 16, 16])
        sh = SHADE[d] if e.get('shade', True) else 1.0
        for py in range(max(0, int(min(ys))), min(S, int(max(ys)) + 1)):
            for px_ in range(max(0, int(min(xs))), min(S, int(max(xs)) + 1)):
                s, t = Minv @ (np.array([px_ + .5, py + .5]) - a)
                if not (0 <= s <= 1 and 0 <= t <= 1): continue
                depth = da + (db - da) * s + (dc - da) * t
                if depth <= zbuf[py, px_]: continue
                tu = (u0 + (u1 - u0) * s) / 16 * tw; tv = (v0 + (v1 - v0) * t) / 16 * th
                col = T[min(th - 1, max(0, int(tv))), min(tw - 1, max(0, int(tu)))]
                if col[3] < 128: continue
                zbuf[py, px_] = depth
                img[py, px_] = [col[0] * sh, col[1] * sh, col[2] * sh, 255]
Image.fromarray(img.clip(0, 255).astype('uint8')).save(out)
