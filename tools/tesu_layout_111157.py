#!/usr/bin/env python3
"""0.1.11.157: the TESU (太级储电罐) multiblock layout, written to src/main/resources/data/mio_icif/multiblock/tesu.json.

0.1.11.168: a box, 7 x 7 x 13 (it was a cylinder: the 3-5-7-7-7-5-3 disc). Every layer is the full 7 x 7; the cells keep the
old inside (the 3-5-5-5-3 disc, 21 a layer, 189 in all; the user's choice, 2026-11-22), so every position of the cylinder keeps its
role and a TESU built before forms again once the new casing is added. Local frame: x 0..6 (west to east), y 0..12, z 0..6 with
the front at z = 0 (north): the controller looks out of the front, the structure turns with it.

  y0      base, solid casing
  y1      the controller in the middle of the front, 8 port slots (2 on each side), the rest casing
  y2-y10  the shell: a reinforced-glass window three columns wide in the middle of each side (0.1.11.169); the 3-5-5-5-3 disc inside full of energy cells
          (0.1.11.158), the rest casing (the walls and the four inner corners)
  y11     lid, solid casing
  y12     top, solid casing (0.1.11.166: no top electrode)

Roles: casing, glass, cell, controller, port; "air" lists the cells that must be empty. Running it again writes the
same file.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/data/mio_icif/multiblock/tesu.json'

ROWS = [(2, 4), (1, 5), (0, 6), (0, 6), (0, 6), (1, 5), (2, 4)]   # x range of each z row: widths 3-5-7-7-7-5-3


def disc():
    return [(x, z) for z, (a, b) in enumerate(ROWS) for x in range(a, b + 1)]


DISC = disc()
DISC_SET = set(DISC)


def ring(x, z):
    """On the shell: a face neighbour lies outside the disc."""
    return any((x + dx, z + dz) not in DISC_SET for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))


RING = [p for p in DISC if ring(*p)]
INSIDE = [p for p in DISC if not ring(*p)]
COLUMN = {(x, z) for x in range(2, 5) for z in range(2, 5)}
# 0.1.11.169: the middle three columns of each side (it was the middle one)
WINDOWS = {(x, z) for x in (2, 3, 4) for z in (0, 6)} | {(x, z) for x in (0, 6) for z in (2, 3, 4)}
PORTS = {(2, 0), (4, 0), (2, 6), (4, 6), (0, 2), (0, 4), (6, 2), (6, 4)}
CONTROLLER = (3, 0)


SQUARE = [(x, z) for z in range(7) for x in range(7)]      # 0.1.11.168: every layer
INSIDE_SET = set(INSIDE)


def build():
    blocks, air = [], []
    for x, z in SQUARE:                                       # y0: base
        blocks.append([x, 0, z, 'casing', ''])
    for x, z in SQUARE:                                       # y1: controller and port slots
        if (x, z) == CONTROLLER:
            blocks.append([x, 1, z, 'controller', 'north'])
        elif (x, z) in PORTS:
            blocks.append([x, 1, z, 'port', ''])
        else:
            blocks.append([x, 1, z, 'casing', ''])
    for y in range(2, 11):                                    # y2-y10: shell, windows, cells
        for x, z in SQUARE:
            role = 'glass' if (x, z) in WINDOWS else 'cell' if (x, z) in INSIDE_SET else 'casing'
            blocks.append([x, y, z, role, ''])
    for y in (11, 12):                                        # y11: lid, y12: top
        for x, z in SQUARE:
            blocks.append([x, y, z, 'casing', ''])
    return blocks, air


def main():
    assert len(DISC) == 37 and len(RING) == 16 and len(INSIDE) == 21, (len(DISC), len(RING), len(INSIDE))
    assert WINDOWS <= set(RING) and PORTS <= set(RING) and COLUMN <= set(INSIDE)
    blocks, air = build()
    counts = {}
    for b in blocks:
        counts[b[3]] = counts.get(b[3], 0) + 1
    layout = {
        'comment': 'TESU (太级储电罐) multiblock, written by tools/tesu_layout_111157.py; local frame, front at z = 0 (north)',
        'size': [7, 13, 7],
        'front': 'north',
        'counts': dict(sorted(counts.items())),
        'blocks': blocks,
        'air': air,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(layout, ensure_ascii=False, separators=(',', ':'))
    # one block per line keeps the diff readable
    text = text.replace('],[', '],\n[').replace('"blocks":[[', '"blocks":[\n[').replace('"air":[[', '"air":[\n[')
    OUT.write_text(text + '\n', encoding='utf-8', newline='\n')
    print('wrote', OUT.relative_to(ROOT), counts, 'air', len(air))


if __name__ == '__main__':
    main()
