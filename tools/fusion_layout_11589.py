#!/usr/bin/env python3
"""0.1.11.89: the fusion reactor (tokamak) layout as a torus, written to the mod resource data/mio_icif/multiblock/fusion_tokamak.json
(0.1.11.92: the controller checks its structure against this file; the sample world builder and the preview read it too).

The shape follows a real tokamak (a doughnut, not a drum):
  * the torus: t = distance from the plasma's centre ring, measured in the poloidal plane, with the height divided by the
    elongation (a slightly tall, D-like cross-section). t <= PLASMA: the plasma (air); then the blanket (the divertor at the
    bottom of that shell); then the vacuum vessel. Each shell is at least one block thick, and t changes by at most one per
    block step, so the plasma is closed in by the blanket and the blanket by the vessel (no face-to-face leak);
  * eight D-shaped toroidal field coils loop around the vessel (just outside it) along the axes and the diagonals; their inner
    legs pass through the hole beside the central solenoid;
  * the central solenoid (the inner poloidal field coil) fills the hole;
  * two outer poloidal field coils ring the torus above and below the midplane, outside the toroidal coils;
  * the ports end short ducts (vacuum vessel blocks) that leave the vessel between the coils: equatorial (controller, heating,
    fuel, power), and on the east side a column of three: upper (disruption mitigation), middle (cryogenic, 0.1.11.94; it
    was on a toroidal coil's outer leg) and lower (exhaust);
  * 0.1.11.94: each toroidal coil block faces outward along its coil's plane (COIL_FACING).

Positions are from the centre of the torus (y = 0 is the midplane). Usage: fusion_layout_11589.py <repo root>
"""
import json
import math
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
OUT = os.path.join(ROOT, 'src/main/resources/data/mio_icif/multiblock/fusion_tokamak.json')

MAJOR = 7.0          # the plasma's centre ring, from the axis
ELONGATION = 1.25    # the cross-section is this much taller than wide
PLASMA, BLANKET, VESSEL, COIL = 1.5, 2.5, 3.5, 4.5   # outer edge of each shell (t)
SOLENOID = 2.5       # the central solenoid: columns closer than this to the axis
SOLENOID_HALF = 5    # it spans y = -5 .. 5
PF = [(3, 10.5, 11.5), (-3, 10.5, 11.5)]   # outer poloidal field coils: (y, inner radius, outer radius)
COILS = 8            # toroidal field coils, along the axes and the diagonals
SPAN = 13            # half-width of the box searched


def t_of(x, y, z):
    rho = math.hypot(x, z)
    return math.hypot(rho - MAJOR, y / ELONGATION), rho


# 0.1.11.94: the facing of each toroidal field coil's blocks: outward along the coil's plane (front and back show the coil's
# narrow steel edge, the two sides its copper turns). A diagonal coil takes the next direction clockwise (seen from above).
COIL_FACING = ['east', 'south', 'south', 'west', 'west', 'north', 'north', 'east']


def coil_of(x, z):
    """The toroidal field coil (0-7: the axes and the diagonals, from +x towards +z) whose plane holds (x, z) on its side of
    the axis, or None."""
    for k in range(COILS):
        a = 2 * math.pi * k / COILS
        c, s = math.cos(a), math.sin(a)
        if x * c + z * s > 0 and abs(-x * s + z * c) <= 0.5 + 1e-9:
            return k
    return None


def on_coil(x, z):
    return coil_of(x, z) is not None


def plasma(rho, y):
    """The plasma channel: as wide at every height as in its top and bottom layers (the narrowest), so its side walls are flat.
    Measured by t alone, the midplane layer reached one cell further out in some columns and left pockets in the walls. The
    cells left out are inside the blanket shell and become blanket."""
    half_height = int(PLASMA * ELONGATION)                        # layers -1 .. 1
    half_width = math.sqrt(PLASMA ** 2 - (half_height / ELONGATION) ** 2)
    return abs(y) <= half_height and abs(rho - MAJOR) <= half_width


def base(x, y, z):
    t, rho = t_of(x, y, z)
    if rho < SOLENOID and abs(y) <= SOLENOID_HALF:
        return 'central_solenoid'
    if plasma(rho, y):
        return 'air'
    if t <= BLANKET:
        # the divertor: the bottom of the blanket shell, under the plasma
        return 'divertor' if y < 0 and abs(rho - MAJOR) <= 1.5 and y <= -PLASMA * ELONGATION + 0.5 else 'blanket'
    if t <= VESSEL:
        return 'vacuum_vessel'
    if t <= COIL and on_coil(x, z):
        return 'toroidal_coil'
    for py, r0, r1 in PF:
        if y == py and r0 <= rho < r1:
            return 'poloidal_coil'
    return None


# ports: (name, facing, the duct's row (fixed coordinate), axis it runs along, direction, y)
#   a duct runs outward along +x / -x / +z / -z in the gap between two coils, from the vessel until past the coil ring
PORTS = [
    ('controller', 'east', 'z', 4, 'x', +1, 0),
    ('nbi_port', 'south', 'x', 4, 'z', +1, 0),
    ('nbi_port', 'south', 'x', -4, 'z', +1, 0),
    ('ecrh_port', 'west', 'z', 4, 'x', -1, 0),
    ('ecrh_port', 'west', 'z', -4, 'x', -1, 0),
    ('fuel_port', 'north', 'x', -4, 'z', -1, 0),
    ('power_port', 'north', 'x', 4, 'z', -1, 0),
    ('mitigation_port', 'east', 'z', -4, 'x', +1, 2),
    ('exhaust_port', 'east', 'z', -4, 'x', +1, -2),
    # 0.1.11.94: the cryogenic port ends the middle duct between those two (it was on the +x coil's outer leg)
    ('cryo_port', 'east', 'z', -4, 'x', +1, 0),
]


def build():
    cells = {}
    for x in range(-SPAN, SPAN + 1):
        for z in range(-SPAN, SPAN + 1):
            for y in range(-SPAN, SPAN + 1):
                b = base(x, y, z)
                if b is not None:
                    cells[(x, y, z)] = (b, '')
    # 0.1.11.94: the outer legs of the diagonal coils had a gap at y = +-2 (the shell skipped radius 8 there): close it, so
    # each corner leg runs from the lower to the upper poloidal coil
    for sx in (1, -1):
        for sz in (1, -1):
            for y in (2, -2):
                at = (8 * sx, y, 8 * sz)
                if at in cells:
                    raise SystemExit('the corner coil block %s is taken by %s' % (at, cells[at][0]))
                cells[at] = ('toroidal_coil', '')
    for (x, y, z), (b, f) in list(cells.items()):
        if b == 'toroidal_coil':
            cells[(x, y, z)] = (b, COIL_FACING[coil_of(x, z)])
    for name, facing, fixed_axis, fixed, axis, sign, y in PORTS:
        pos = lambda v: (v, y, fixed) if axis == 'x' else (fixed, y, v)
        # start at the plasma's centre ring (the outer half of the torus), walk out through the torus, then lay the duct
        v = 0
        while math.hypot(pos(v)[0], pos(v)[2]) < MAJOR:
            v += sign
        while pos(v) in cells and cells[pos(v)][0] in ('air', 'blanket', 'divertor', 'vacuum_vessel'):
            v += sign
        while True:
            x0, _, z0 = pos(v)
            if math.hypot(x0, z0) > MAJOR + COIL + 0.5:
                break
            if pos(v) in cells:
                raise SystemExit('the duct of %s runs into %s at %s' % (name, cells[pos(v)][0], pos(v)))
            cells[pos(v)] = ('vacuum_vessel', '')
            v += sign
        cells[pos(v)] = (name, facing)
    return cells


def main():
    cells = build()
    blocks = sorted([x, y, z, b, f] for (x, y, z), (b, f) in cells.items() if b != 'air')
    plasma = sorted([x, y, z] for (x, y, z), (b, f) in cells.items() if b == 'air')
    counts = {}
    for b in blocks:
        counts[b[3]] = counts.get(b[3], 0) + 1
    xs, ys, zs = [b[0] for b in blocks], [b[1] for b in blocks], [b[2] for b in blocks]
    size = (max(xs) - min(xs) + 1, max(ys) - min(ys) + 1, max(zs) - min(zs) + 1)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w', encoding='utf-8', newline='\n') as f:
        json.dump({'note': 'generated by tools/fusion_layout_11589.py; positions from the torus centre, y = 0 the midplane',
                   'size': size, 'counts': dict(sorted(counts.items())), 'blocks': blocks, 'plasma': plasma}, f, separators=(',', ':'))
    print('size %dx%dx%d' % size, '| blocks', len(blocks), '| plasma', len(plasma), '|', dict(sorted(counts.items())))


if __name__ == '__main__':
    main()
