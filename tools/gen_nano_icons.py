#!/usr/bin/env python3
"""
0.1.7.34: draws the 16x16 inventory icons of the nano suit from its baked armor models.

Each piece is rendered in software (orthographic 3/4 view, z-buffer, nearest texture lookup,
simple directional light) at 8x supersampling, reduced to 16x16 by area average, then cleaned up
as pixel art: alpha snap, a small palette taken from the piece, and a dark outline.
A 128x128 preview sheet (nearest-neighbour) is written next to the icons for review.

Usage: gen_nano_icons.py <repo root> [preview png]
"""
import json, math, os, sys
import numpy as np
from PIL import Image

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/mio_icif')
PIVOT = {'head': (0, 0, 0), 'body': (0, 0, 0), 'right_arm': (-5, 2, 0), 'left_arm': (5, 2, 0),
         'right_leg': (-1.9, 12, 0), 'left_leg': (1.9, 12, 0)}

# piece, yaw (deg, + turns the left side towards the viewer), pitch (deg, + looks down), icon name
# spread: extra sideways gap of the limbs (model units) so the silhouette reads at 16 px
VIEWS = [
    ('nano_helmet', -30, 15, 'item_armor_nano_helmet', 0),
    ('nano_chestplate', -8, 4, 'item_armor_nano_chestplate', 1.5),
    ('nano_leggings', -6, 2, 'item_armor_nano_leggings', 1.6),
    ('nano_boots', -38, 24, 'item_armor_nano_boots', 2.0),
]
SS = 8          # supersampling
SIZE = 16


def load(name, spread=0.0):
    model = json.load(open(os.path.join(ASSETS, 'armor_models', name + '.json')))
    tex = np.asarray(Image.open(os.path.join(ASSETS, 'textures/models/armor', name + '.png')).convert('RGBA')).astype(np.float32) / 255
    quads = []
    for part, qs in model['parts'].items():
        px, py, pz = PIVOT[part]
        if part.startswith('right_'):
            px -= spread
        elif part.startswith('left_'):
            px += spread
        for q in qs:
            v = q['v']
            pts = [(v[i * 5] + px, v[i * 5 + 1] + py, v[i * 5 + 2] + pz, v[i * 5 + 3], v[i * 5 + 4]) for i in range(4)]
            quads.append(pts)
    return quads, tex


def render(quads, tex, yaw, pitch):
    """Returns an RGBA float image SIZE*SS square."""
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

    def view(x, y, z):
        # model space: y down, front = -z. Screen: x right, y down, depth +towards the back.
        x1 = x * cy - z * sy
        z1 = x * sy + z * cy
        y2 = y * cp - z1 * sp
        z2 = y * sp + z1 * cp
        return x1, y2, z2

    pts = [[view(p[0], p[1], p[2]) + (p[3], p[4]) for p in q] for q in quads]
    allp = np.array([p[:3] for q in pts for p in q])
    # robust bounds: thin spikes (antennas) must not shrink the whole icon
    lo, hi = np.percentile(allp, 1.5, axis=0), np.percentile(allp, 98.5, axis=0)
    span = max(hi[0] - lo[0], hi[1] - lo[1]) * 1.04
    n = SIZE * SS
    scale = (n - 2 * SS * 0.5) / span
    ox = (n - (hi[0] - lo[0]) * scale) / 2 - lo[0] * scale
    oy = (n - (hi[1] - lo[1]) * scale) / 2 - lo[1] * scale
    img = np.zeros((n, n, 4), np.float32)
    zbuf = np.full((n, n), np.inf, np.float32)
    th, tw = tex.shape[0], tex.shape[1]
    light = np.array([-0.45, -0.75, -0.5])
    light /= np.linalg.norm(light)
    for q in pts:
        P = np.array([[p[0] * scale + ox, p[1] * scale + oy, p[2]] for p in q])
        UV = np.array([[p[3], p[4]] for p in q])
        n3 = np.cross(np.array(q[1][:3]) - np.array(q[0][:3]), np.array(q[2][:3]) - np.array(q[0][:3]))
        if np.linalg.norm(n3) < 1e-9:
            n3 = np.cross(np.array(q[2][:3]) - np.array(q[0][:3]), np.array(q[3][:3]) - np.array(q[0][:3]))
        ln = np.linalg.norm(n3)
        shade = 1.0
        if ln > 1e-9:
            n3 = n3 / ln
            shade = 0.6 + 0.4 * abs(float(np.dot(n3, light)))
        for tri in ((0, 1, 2), (0, 2, 3)):
            raster(img, zbuf, P[list(tri)], UV[list(tri)], tex, tw, th, shade)
    return img


def raster(img, zbuf, P, UV, tex, tw, th, shade):
    n = img.shape[0]
    minx, maxx = max(0, int(math.floor(P[:, 0].min()))), min(n - 1, int(math.ceil(P[:, 0].max())))
    miny, maxy = max(0, int(math.floor(P[:, 1].min()))), min(n - 1, int(math.ceil(P[:, 1].max())))
    if minx > maxx or miny > maxy:
        return
    (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = P
    den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
    if abs(den) < 1e-9:
        return
    xs, ys = np.meshgrid(np.arange(minx, maxx + 1) + 0.5, np.arange(miny, maxy + 1) + 0.5)
    w0 = ((y1 - y2) * (xs - x2) + (x2 - x1) * (ys - y2)) / den
    w1 = ((y2 - y0) * (xs - x2) + (x0 - x2) * (ys - y2)) / den
    w2 = 1 - w0 - w1
    inside = (w0 >= -1e-4) & (w1 >= -1e-4) & (w2 >= -1e-4)
    if not inside.any():
        return
    z = w0 * z0 + w1 * z1 + w2 * z2
    u = w0 * UV[0, 0] + w1 * UV[1, 0] + w2 * UV[2, 0]
    v = w0 * UV[0, 1] + w1 * UV[1, 1] + w2 * UV[2, 1]
    tu = np.clip((u * tw).astype(int), 0, tw - 1)
    tv = np.clip((v * th).astype(int), 0, th - 1)
    texel = tex[tv, tu]
    sub = zbuf[miny:maxy + 1, minx:maxx + 1]
    ok = inside & (z < sub) & (texel[..., 3] > 0.5)
    sub[ok] = z[ok]
    region = img[miny:maxy + 1, minx:maxx + 1]
    col = texel.copy()
    col[..., :3] *= shade
    region[ok] = col[ok]


def pixelize(img, icon_name):
    """16x16 pixel art from the supersampled render: accent colours win their cell, the dark
    armour body is lifted into readable greys, a rim light runs along the top-left edges and a
    dark outline closes the shape."""
    n = SIZE
    a = img.reshape(n, SS, n, SS, 4).transpose(0, 2, 1, 3, 4).reshape(n, n, SS * SS, 4)
    alpha = a[..., 3].mean(axis=2)
    rgb = a[..., :3]
    mx, mn = rgb.max(-1), rgb.min(-1)
    accent = (mx - mn > 0.22) & (mx > 0.30) & (a[..., 3] > 0.5)
    mask = alpha > 0.36
    out = np.zeros((n, n, 4), np.float32)
    for y in range(n):
        for x in range(n):
            if not mask[y, x]:
                continue
            solid = a[y, x, :, 3] > 0.5
            acc = accent[y, x]
            if acc.mean() > 0.10:
                c = rgb[y, x][acc].mean(0)
                c = np.clip(c / max(1e-3, c.max()) * 0.95, 0, 1)   # full-brightness accent
            else:
                c = rgb[y, x][solid].mean(0) if solid.any() else np.zeros(3)
                lum = float(c.mean())
                level = min(3, int(lum * 9))          # 4 body tones
                tones = [0.17, 0.25, 0.34, 0.45]
                tint = c - lum
                c = np.clip(tones[level] + tint * 0.8 + np.array([-0.01, 0.0, 0.02]), 0, 1)
            out[y, x, :3] = c
            out[y, x, 3] = 1
    # rim light on top/left edges, shade on bottom/right edges
    for y in range(n):
        for x in range(n):
            if not mask[y, x]:
                continue
            up = y == 0 or not mask[y - 1, x]
            left = x == 0 or not mask[y, x - 1]
            down = y == n - 1 or not mask[y + 1, x]
            right = x == n - 1 or not mask[y, x + 1]
            if up or left:
                out[y, x, :3] = np.clip(out[y, x, :3] + 0.12, 0, 1)
            elif down or right:
                out[y, x, :3] = np.clip(out[y, x, :3] - 0.05, 0, 1)
    rim = np.zeros((n, n), bool)
    for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0)):
        rim |= np.roll(np.roll(mask, dy, 0), dx, 1)
    rim &= ~mask
    out[rim] = [0.05, 0.06, 0.07, 1.0]
    return out


def main():
    sheet = []
    for piece, yaw, pitch, icon, spread in VIEWS:
        quads, tex = load(piece, spread)
        img = render(quads, tex, yaw, pitch)
        px = pixelize(img, icon)
        im = Image.fromarray((px * 255).astype(np.uint8), 'RGBA')
        path = os.path.join(ASSETS, 'textures/item/armor', icon + '.png')
        im.save(path)
        print('wrote', path)
        big = Image.fromarray((img * 255).astype(np.uint8), 'RGBA')
        sheet.append((im, big))
    if PREVIEW:
        w = 128
        canvas = Image.new('RGBA', (w * len(sheet), w * 2), (40, 44, 52, 255))
        for i, (im, big) in enumerate(sheet):
            canvas.alpha_composite(im.resize((w, w), Image.NEAREST), (i * w, 0))
            canvas.alpha_composite(big.resize((w, w), Image.LANCZOS), (i * w, w))
        canvas.save(PREVIEW)
        print('preview', PREVIEW)


if __name__ == '__main__':
    main()
