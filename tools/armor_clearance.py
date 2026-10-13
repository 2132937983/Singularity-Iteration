#!/usr/bin/env python3
"""0.1.8.3: skin clearance of baked arm armor (fixes the first-person hand flicker).

The first-person arm draws the skin arm (box x -3..1 / -1..3, y -2..10, z -2..2 in arm space)
and the sleeve layer 0.25 outside it. Gauntlet faces that lie on those planes z-fight with them:
the quantum and nano gauntlets had their inner faces exactly at x = +-1.0 and the hand end at
y = 10, so the hand flickered. This pass moves every arm and hand vertex that lies on a skin
plane, or between the skin and the sleeve (up to 0.3 outside), to 0.32 outside the skin.
Neighbouring quads share those vertices, so they stretch with them and no crack opens.
Running it again changes nothing.

Usage: armor_clearance.py <armor_models/*.json> ...   (rewrites the files in place)
gen_suit_armor.py applies the same pass to every chestplate it bakes.
"""
import json
import sys

CLEAR = 0.32
LOW, HIGH = -0.02, 0.30
# outward planes of the skin box per arm part: (axis, plane, outward sign)
BOX = {
    'right': [(0, -3.0, -1), (0, 1.0, 1), (1, 10.0, 1), (2, -2.0, -1), (2, 2.0, 1)],
    'left': [(0, -1.0, -1), (0, 3.0, 1), (1, 10.0, 1), (2, -2.0, -1), (2, 2.0, 1)],
}


def clear(parts):
    """Applies the clearance to the arm and hand parts of a baked model (dict part -> quads). Returns moved vertices."""
    moved = 0
    for part, quads in parts.items():
        side = 'right' if part.startswith('right_') else 'left' if part.startswith('left_') else None
        if side is None or not (part.endswith('_arm') or part.endswith('_hand')):
            continue
        for q in quads:
            v = q['v']
            for i in range(4):
                for axis, plane, sign in BOX[side]:
                    k = i * 5 + axis
                    off = (v[k] - plane) * sign
                    if LOW <= off < HIGH:
                        v[k] = round(plane + sign * CLEAR, 4)
                        moved += 1
    return moved


def main():
    for path in sys.argv[1:]:
        with open(path, encoding='utf-8') as f:
            model = json.load(f)
        moved = clear(model['parts'])
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            json.dump(model, f, separators=(',', ':'))
        print(path, 'moved', moved)


if __name__ == '__main__':
    main()
