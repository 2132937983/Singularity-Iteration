#!/usr/bin/env python3
"""
Z-fighting lint for Minecraft block/item models.

Reports every pair of faces inside one model that lie in the same plane, face the same
way and overlap with positive area - the geometry that flickers. Overlaps buried
strictly inside a third element (never visible) are ignored. Elements with a rotation
are checked only against faces with the same rotation.

Usage: zfight_lint.py <assets/<ns>> [path-prefix ...]   (exit code 1 when issues found)
"""
import json, os, sys, glob

AXIS = {'east': (0, 1), 'west': (0, -1), 'up': (1, 1), 'down': (1, -1), 'south': (2, 1), 'north': (2, -1)}
EPS = 1e-6


def load(p):
    return json.load(open(p, encoding='utf-8-sig'))


def faces(el):
    f, t = el['from'], el['to']
    for name in el.get('faces', {}):
        axis, sign = AXIS[name]
        plane = t[axis] if sign > 0 else f[axis]
        others = [a for a in range(3) if a != axis]
        rect = (f[others[0]], f[others[1]], t[others[0]], t[others[1]])
        yield name, axis, sign, plane, others, rect


def inside(el, axis, plane, others, rect):
    f, t = el['from'], el['to']
    if not (f[axis] + EPS < plane < t[axis] - EPS): return False
    return (f[others[0]] - EPS <= rect[0] and rect[2] <= t[others[0]] + EPS and
            f[others[1]] - EPS <= rect[1] and rect[3] <= t[others[1]] + EPS)


def lint_model(els):
    issues = []
    flat = []
    for i, el in enumerate(els):
        rot = json.dumps(el.get('rotation'), sort_keys=True)
        for fc in faces(el):
            flat.append((i, rot) + fc)
    for a in range(len(flat)):
        for b in range(a + 1, len(flat)):
            ia, ra, na, axa, sa, pa, oa, rca = flat[a]
            ib, rb, nb, axb, sb, pb, ob, rcb = flat[b]
            if ia == ib or ra != rb or axa != axb or sa != sb or abs(pa - pb) > EPS: continue
            ov = (max(rca[0], rcb[0]), max(rca[1], rcb[1]), min(rca[2], rcb[2]), min(rca[3], rcb[3]))
            if ov[2] - ov[0] <= EPS or ov[3] - ov[1] <= EPS: continue
            if any(inside(els[k], axa, pa, oa, ov) for k in range(len(els)) if k not in (ia, ib)): continue
            issues.append((ia, ib, na, pa, ov))
    return issues


def main():
    assets = sys.argv[1]
    prefixes = sys.argv[2:]
    root = os.path.join(assets, 'models')
    total = 0; checked = 0
    for p in sorted(glob.glob(os.path.join(root, '**/*.json'), recursive=True)):
        rel = os.path.relpath(p, root)[:-5]
        if prefixes and not any(rel.startswith(x) for x in prefixes): continue
        try: d = load(p)
        except Exception: continue
        els = d.get('elements')
        if not els: continue
        checked += 1
        for ia, ib, face, plane, ov in lint_model(els):
            total += 1
            n1 = els[ia].get('name', ia); n2 = els[ib].get('name', ib)
            print(f'{rel}: {face} faces of "{n1}" and "{n2}" coincide at {plane} over {ov}')
    print(f'checked {checked} models, {total} coplanar overlaps')
    sys.exit(1 if total else 0)


if __name__ == '__main__':
    main()
