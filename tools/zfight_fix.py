"""Pushes the smaller of two coincident same-facing faces 0.02 px outward until the lint is clean."""
import json, sys, os
sys.path.insert(0, os.path.dirname(__file__))
import zfight_lint as z
for path in sys.argv[1:]:
    d = z.load(path); els = d['elements']
    for _ in range(20):
        issues = z.lint_model(els)
        if not issues: break
        ia, ib, face, plane, ov = issues[0]
        axis, sign = z.AXIS[face]
        def area(el):
            o = [a for a in range(3) if a != axis]
            return (el['to'][o[0]] - el['from'][o[0]]) * (el['to'][o[1]] - el['from'][o[1]])
        k = ia if area(els[ia]) <= area(els[ib]) else ib
        if os.environ.get("SHRINK"): k = ib if k == ia else ia; sign = -sign
        if sign > 0: els[k]['to'][axis] = round(els[k]['to'][axis] + 0.02, 4)
        else: els[k]['from'][axis] = round(els[k]['from'][axis] - 0.02, 4)
    json.dump(d, open(path, 'w'), indent=1)
    print(path, 'remaining', len(z.lint_model(els)))
