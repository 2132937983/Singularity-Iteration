# 0.1.10.11 items 1 and 6: the element models of models/item/3d replace their separate_transforms
# wrappers in models/item (so the game uses the model's own GUI transform), multi-axis / free
# element rotations (which the 1.21.1 model loader rejects: the saber showed the missing model) become
# valid single-axis rotations, and the quantum swords get a GUI transform.
# Run: python -I -X utf8 models_1111.py <assets/mio_icif>
import json, math, os, sys

A = sys.argv[1]
M = os.path.join(A, 'models', 'item')
D3 = os.path.join(M, '3d')
WEAPONS = {'weapon/nanosaber': 'item_tool_nanosaber', 'weapon/nanosaber_active': 'item_tool_nanosaber_active',
           'weapon/quantum_sword': 'item_tool_quantum_sword', 'weapon/quantum_sword_hyper': 'item_tool_quantum_sword_hyper'}
ANGLES = (-45.0, -22.5, 0.0, 22.5, 45.0)


def load(p):
    return json.load(open(p, encoding='utf-8-sig'))


def save(p, data):
    text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
    open(p, 'wb').write(text.encode('utf-8'))


def fix_rotations(model, name):
    fixed = 0
    for i, e in enumerate(model.get('elements', [])):
        r = e.get('rotation')
        if r is None:
            continue
        if 'axis' in r and r.get('angle', 0) in ANGLES:
            continue
        if 'axis' in r:
            axis, angle = r['axis'], float(r.get('angle', 0))
        else:
            # free rotation {x, y, z}: keep the component with the largest angle
            comps = [(abs(float(r.get(a, 0))), a, float(r.get(a, 0))) for a in ('x', 'y', 'z')]
            _, axis, angle = max(comps)
        snapped = min(ANGLES, key=lambda a: abs(a - angle))
        e['rotation'] = {'angle': snapped, 'axis': axis, 'origin': r.get('origin', [8, 8, 8])}
        if 'rescale' in r: e['rotation']['rescale'] = r['rescale']
        print(f'  {name}: element {i} rotation {json.dumps({k: v for k, v in r.items() if k != "origin"})} -> {axis} {snapped}')
        fixed += 1
    return fixed


def rot_xyz(rx, ry, rz):
    # Quaternionf.rotationXYZ: R = Rx * Ry * Rz (applied to a column vector)
    cx, sx = math.cos(math.radians(rx)), math.sin(math.radians(rx))
    cy, sy = math.cos(math.radians(ry)), math.sin(math.radians(ry))
    cz, sz = math.cos(math.radians(rz)), math.sin(math.radians(rz))
    X = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Y = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Z = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    mul = lambda a, b: [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return mul(mul(X, Y), Z)


def gui_transform(model, rotation, fill=0.92):
    """Scale and translation so the model, turned by rotation, fills the GUI slot and is centred."""
    R = rot_xyz(*rotation)
    pts = []
    for e in model['elements']:
        f, t = e['from'], e['to']
        for x in (f[0], t[0]):
            for y in (f[1], t[1]):
                for z in (f[2], t[2]):
                    v = [x / 16 - 0.5, y / 16 - 0.5, z / 16 - 0.5]
                    pts.append([sum(R[i][k] * v[k] for k in range(3)) for i in range(3)])
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    extent = max(max(xs) - min(xs), max(ys) - min(ys))
    s = round(fill / extent, 3)
    cx, cy = (max(xs) + min(xs)) / 2 * s, (max(ys) + min(ys)) / 2 * s
    return {'rotation': rotation, 'translation': [round(-cx * 16, 2), round(-cy * 16, 2), 0], 'scale': [s, s, s]}


moved = []
for dp, _, fns in os.walk(D3):
    for f in sorted(fns):
        if not f.endswith('.json'):
            continue
        src = os.path.join(dp, f)
        rel = os.path.relpath(src, D3).replace('\\', '/')[:-5]
        dest_rel = WEAPONS.get(rel, rel)
        dest = os.path.join(M, dest_rel + '.json')
        model = load(src)
        wrapper = load(dest) if os.path.exists(dest) else None
        assert wrapper is None or wrapper.get('loader') == 'neoforge:separate_transforms', (dest, 'is not a wrapper')
        if wrapper is not None:
            assert wrapper['base']['parent'] == 'mio_icif:item/3d/' + rel, (dest, wrapper['base'])
            if 'overrides' in wrapper:
                model['overrides'] = wrapper['overrides']
        fix_rotations(model, rel)
        if 'gui' not in model.get('display', {}) and model.get('elements'):
            fixed = model['display'].get('fixed', {})
            g = gui_transform(model, fixed.get('rotation', [0, 0, 0]))
            model.setdefault('display', {})['gui'] = g
            print(f'  {rel}: gui transform {g}')
        save(dest, model)
        os.remove(src)
        moved.append((rel, dest_rel))
for dp, dns, fns in sorted(os.walk(D3), key=lambda x: -len(x[0])):
    if not os.listdir(dp):
        os.rmdir(dp)
print(len(moved), 'models moved:')
for a, b in moved:
    print('  3d/' + a, '->', b)
assert not os.path.exists(D3), 'models/item/3d still exists'
