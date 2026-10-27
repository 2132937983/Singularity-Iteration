#!/usr/bin/env python3
"""
0.1.10.31: offline preview of the normal-mode thrusters of the quantum suit (no game needed).

Renders the worn quantum suit at night (the renderer of glow_preview_1130.py, glow on) from the side, three-quarter back and back,
and adds the jets of SuitThrustLayer: the two back pods (body pixels x = +-2.92, y = 12, z = 5.4, tilted 12 degrees back) and
the soles, with the generated flame texture (thruster_flame.png, frame 0) in the colour of FlameRules.colour, additive. Rows:
thrust 0.35 (white), 0.6 (blue), 1.0 (violet). Flames are drawn over the suit (no depth test): a jet behind the body shows through.

Usage: thrust_preview_1131.py <repo root> <out.png>
"""
import importlib.util
import math
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location('glow_preview_1130', os.path.join(HERE, 'glow_preview_1130.py'))
gp = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gp)

WHITE, BLUE, VIOLET = 0xFFF2E0, 0x5AA8FF, 0xB878FF
VIEWS = [('side', 90, 0), ('3q back', 150, 10), ('back', 180, 0)]
THRUSTS = [0.35, 0.6, 1.0]


def mix(a, b, t):
    return tuple(((a >> s) & 255) + (((b >> s) & 255) - ((a >> s) & 255)) * t for s in (16, 8, 0))


def colour(t):
    # FlameRules.colour
    t = max(0.0, min(1.0, t))
    if t <= 0.3:
        return tuple((WHITE >> s) & 255 for s in (16, 8, 0))
    if t <= 0.65:
        return mix(WHITE, BLUE, (t - 0.3) / 0.35)
    return mix(BLUE, VIOLET, (t - 0.65) / 0.35)


def project(p, yaw, pitch):
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    x, y, z = p
    x1 = x * cy + z * sy
    d = -z * cy + x * sy
    return gp.CELL_W / 2 + x1 * gp.SCALE, 40 + (y * cp + d * sp + 8) * gp.SCALE


def flame(img, flame_tex, a, b, half, rgb, power):
    """Additive strip from screen point a to b, half width in screen pixels, texture frame 0 along it."""
    ax, ay = a
    bx, by = b
    dx, dy = bx - ax, by - ay
    length = math.hypot(dx, dy)
    if length < 1e-6:
        return
    ux, uy = dx / length, dy / length
    nx, ny = -uy, ux
    h, w = len(img), len(img[0])
    reach = int(max(half, 4) + 2)
    for yy in range(max(0, int(min(ay, by)) - reach), min(h, int(max(ay, by)) + reach + 1)):
        for xx in range(max(0, int(min(ax, bx)) - reach), min(w, int(max(ax, bx)) + reach + 1)):
            px, py = xx + 0.5 - ax, yy + 0.5 - ay
            along = (px * ux + py * uy) / length
            across = (px * nx + py * ny) / max(half, 1e-6)
            k = 0.0
            if 0 <= along <= 1 and abs(across) <= 1:
                tx = min(15, int((across * 0.5 + 0.5) * 16))
                ty = min(63, int(along * 64))
                k += flame_tex.getpixel((tx, ty))[0] / 255 * 0.9 * power
            # nozzle disc
            dd = math.hypot(px, py) / max(half * 0.9, 1)
            if dd < 1:
                k += (1 - dd) * 0.85 * power
            if k <= 0:
                continue
            r, g, bl = img[yy][xx]
            img[yy][xx] = (min(255, int(r + rgb[0] * k)), min(255, int(g + rgb[1] * k)), min(255, int(bl + rgb[2] * k)))


def main():
    root, out = sys.argv[1], sys.argv[2]
    gp.CELL_H = 400
    quads = gp.load(root, 'glow', None)
    flame_tex = Image.open(os.path.join(root, 'src/main/resources/assets/mio_icif/textures/effect/thruster_flame.png')).convert('RGB')
    tilt = math.radians(12)
    sheet = Image.new('RGB', (gp.CELL_W * len(VIEWS), gp.CELL_H * len(THRUSTS)), (8, 9, 12))
    for ri, jet in enumerate(THRUSTS):
        rgb = colour(jet)
        for vi, (_, yaw, pitch) in enumerate(VIEWS):
            img = gp.render(quads, yaw, pitch, 'glow', (0.24, 0.71, 1.0), None, None)
            # back pods: length (0.18 + 0.5 jet) blocks x 1.15, half width 0.085 x 1.15 blocks (16 pixels a block)
            length = (0.18 + 0.5 * jet) * 1.15 * 16
            for side in (-1, 1):
                a = (side * 2.92, 12.0, 5.4)
                b = (a[0], a[1] + length * math.cos(tilt), a[2] + length * math.sin(tilt))
                flame(img, flame_tex, project(a, yaw, pitch), project(b, yaw, pitch), 0.085 * 1.15 * 16 * gp.SCALE, rgb, jet)
            # soles (the boots): 0.75 of the thrust
            sole = 0.75 * jet
            length = (0.14 + 0.4 * sole) * 1.15 * 16
            for side in (-1, 1):
                a = (side * 1.9, 24.2, 0.0)
                b = (a[0], a[1] + length, 0.0)
                flame(img, flame_tex, project(a, yaw, pitch), project(b, yaw, pitch), 0.07 * 1.15 * 16 * gp.SCALE, rgb, sole)
            im = Image.new('RGB', (gp.CELL_W, gp.CELL_H))
            im.putdata([p for row in img for p in row])
            sheet.paste(im, (vi * gp.CELL_W, ri * gp.CELL_H))
    sheet.save(out)
    print('wrote', out)


if __name__ == '__main__':
    main()
