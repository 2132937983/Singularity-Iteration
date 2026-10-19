#!/usr/bin/env python3
"""0.1.10.13: static audit of everything a Wingman can hold (the arm and hand jitter report).

1. Items: every item this mod registers gets the hold kind of WingmanPose (RANGED: an IWeaponItem whose
   isRanged() returns true, a bow, crossbow or other projectile weapon, a trident; HELD: anything
   else). The kinds come from the class hierarchy in the sources. A ranged weapon whose isRanged() is
   not a constant is listed (its kind is decided at run time).
2. Item models: the third-person transforms (own or inherited from a model of this mod) must be finite,
   in range (rotation within 360 degrees, translation within 80, scale above 0 and at most 4). A
   left-hand transform that differs from the right-hand one is listed; one that is the right-hand
   transform already mirrored (y and z rotation negated) is an error: the game mirrors the left hand
   itself, so it would show the item flipped in a left hand.
3. Client hooks that change a held item while it is drawn: item property functions, custom item
   renderers and hand transforms must not use a clock or a random source outside the first person.
4. The Wingman code: no walking swing, no vanilla attack pose switch, fail-safe smoothing, no arm
   swing per shot, a running swing is not restarted, one shared ranged test.
Exit code 1 on an error. Usage: wingman_hold_audit_1113.py <repo root> [--list]
"""
import json
import os
import re
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LIST = '--list' in sys.argv
JAVA = os.path.join(ROOT, 'src/main/java/com/miophas/singularity_iteration')
MODELS = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/models')
errors, notes = [], []

sources = {}
for dp, _, fns in os.walk(JAVA):
    for f in fns:
        if f.endswith('.java'):
            sources[f[:-5]] = open(os.path.join(dp, f), encoding='utf-8-sig', errors='replace').read()

# ---------------------------------------------------------------- 1. items and their hold kind
hierarchy = {}
for name, text in sources.items():
    # the extends / implements lists may span several lines
    m = re.search(r'\bclass\s+' + re.escape(name) + r'\b(?:<[^>{]*>)?\s*(?:extends\s+([\w.<>,?\s]+?))?\s*(?:implements\s+([\w.<>,?\s]+?))?\s*\{', text)
    if m:
        parent = (m.group(1) or '').split('<')[0].strip().split('.')[-1]
        faces = [x.strip().split('<')[0].split('.')[-1] for x in (m.group(2) or '').split(',') if x.strip()]
        hierarchy[name] = (parent, faces)

RANGED_BASE = {'BowItem', 'CrossbowItem', 'ProjectileWeaponItem', 'TridentItem'}


def chain(name):
    seen = []
    while name and name not in seen:
        seen.append(name)
        name = hierarchy.get(name, ('', []))[0]
    return seen


def kind(cls):
    names = chain(cls)
    if any(n in RANGED_BASE for n in names):
        return 'RANGED', 'vanilla projectile weapon'
    weapon = any('IWeaponItem' in hierarchy.get(n, ('', []))[1] for n in names)
    if weapon:
        for n in names:
            body = sources.get(n, '')
            m = re.search(r'boolean\s+isRanged\(\)\s*\{\s*return\s+([^;]+);', body)
            if m:
                value = m.group(1).strip()
                if value == 'true':
                    return 'RANGED', 'IWeaponItem.isRanged() true'
                if value == 'false':
                    return 'HELD', 'IWeaponItem.isRanged() false'
                return 'RANGED?', 'IWeaponItem.isRanged() returns ' + value
        return 'HELD', 'IWeaponItem without isRanged'
    return 'HELD', ''


items = []
REG = re.compile(r'ITEMS\.(register\w*)\(\s*"([^"]+)"\s*(?:,\s*(?:\(\)\s*->\s*new\s+([\w.]+)|([\w.]+)::new))?', re.S)
for name, text in sources.items():
    for m in REG.finditer(text):
        cls = (m.group(3) or m.group(4) or ('Item' if 'Simple' in m.group(1) else 'Item')).split('.')[-1]
        items.append((m.group(2), cls))
block_items = sum(len(re.findall(r'\bregisterBlock\("([a-z0-9_/]+)"', t)) for t in sources.values())
counts = {}
for path, cls in items:
    k, why = kind(cls)
    counts[k] = counts.get(k, 0) + 1
    if k == 'RANGED?':
        notes.append(f'item {path} ({cls}): {why}; the kind is decided at run time')
    if LIST:
        print(f'    {k:7s} {path} ({cls}) {why}')
print(f'items: {len(items)} registered items + {block_items} block items; hold kinds '
      + ', '.join(f'{k} {v}' for k, v in sorted(counts.items())) + f', HELD (blocks) {block_items}')

# ---------------------------------------------------------------- 2. item model transforms
def load_model(ref):
    if not ref.startswith('mio_icif:'):
        return None
    path = os.path.join(MODELS, ref.split(':', 1)[1] + '.json')
    if not os.path.exists(path):
        return None
    try:
        return json.load(open(path, encoding='utf-8-sig'))
    except ValueError as e:
        errors.append(f'model {ref}: {e}')
        return None


def display_of(model, depth=0):
    """The display block of a model, with the parts inherited from parents of this mod."""
    own = dict(model.get('display', {}))
    parent = model.get('parent')
    if parent and depth < 16:
        p = load_model(parent if ':' in parent else 'minecraft:' + parent)
        if p is not None:
            inherited = display_of(p, depth + 1)
            inherited.update(own)
            return inherited
    return own


def finite_numbers(values, n):
    return isinstance(values, list) and len(values) == n and all(isinstance(v, (int, float)) and abs(v) < 1e9 for v in values)


checked = flipped = asym = custom = 0
ITEM_MODELS = os.path.join(MODELS, 'item')
for dp, _, fns in os.walk(ITEM_MODELS):
    for f in fns:
        if not f.endswith('.json'):
            continue
        rel = os.path.relpath(os.path.join(dp, f), ITEM_MODELS).replace('\\', '/')[:-5]
        model = load_model('mio_icif:item/' + rel)
        if model is None:
            continue
        if model.get('parent') == 'builtin/entity' or model.get('parent') == 'minecraft:builtin/entity':
            custom += 1
        display = display_of(model)
        for ctx in ('thirdperson_righthand', 'thirdperson_lefthand'):
            t = display.get(ctx)
            if t is None:
                continue
            checked += 1
            rot, tr, sc = t.get('rotation', [0, 0, 0]), t.get('translation', [0, 0, 0]), t.get('scale', [1, 1, 1])
            if not (finite_numbers(rot, 3) and finite_numbers(tr, 3) and finite_numbers(sc, 3)):
                errors.append(f'model item/{rel} {ctx}: malformed transform {t}')
                continue
            if any(abs(v) > 360 for v in rot):
                errors.append(f'model item/{rel} {ctx}: rotation beyond 360 degrees {rot}')
            if any(abs(v) > 80 for v in tr):
                errors.append(f'model item/{rel} {ctx}: translation beyond 80 {tr} (the game clamps it)')
            if any(v <= 0 or v > 4 for v in sc):
                errors.append(f'model item/{rel} {ctx}: scale {sc} (0 or below hides or flips the item)')
        r, l = display.get('thirdperson_righthand'), display.get('thirdperson_lefthand')
        if r is not None and l is not None and r != l:
            rr, lr = r.get('rotation', [0, 0, 0]), l.get('rotation', [0, 0, 0])
            if len(rr) == 3 and len(lr) == 3 and (rr[1] or rr[2]) and lr[0] == rr[0] and lr[1] == -rr[1] and lr[2] == -rr[2]:
                errors.append(f'model item/{rel}: the left-hand transform is the right-hand one mirrored; the game mirrors '
                              f'it again, so the item is flipped in a left hand ({rr} / {lr})')
                flipped += 1
            else:
                asym += 1
                notes.append(f'model item/{rel}: left-hand transform differs from the right-hand one ({r} / {l})')
print(f'models: {checked} third-person transforms checked, {flipped} flipped left hands, {asym} asymmetric, '
      f'{custom} drawn by a custom renderer')

# ---------------------------------------------------------------- 3. client hooks
CLOCK = re.compile(r'System\.nanoTime|System\.currentTimeMillis|Util\.getMillis|getGameTime\(\)|\.tickCount|Math\.random|RandomSource|new Random\(')
client = sources.get('Singularity_Iteration_Client', '')
props = 0
for m in re.finditer(r'ItemProperties\.register\(', client):
    end = client.find(');', m.end())
    call = client[m.start():end]
    props += 1
    if CLOCK.search(call):
        errors.append(f'item property uses a clock or random source: {call[:120]}')
prop_classes = [n for n, t in sources.items() if re.search(r'implements\s+[^{]*(Clamped)?ItemPropertyFunction', t)]
for n in prop_classes:
    if CLOCK.search(sources[n]):
        errors.append(f'item property class {n} uses a clock or random source')
hands = 0
for n, t in sources.items():
    for m in re.finditer(r'applyForgeHandTransform\s*\(([^)]*)\)', t):
        hands += 1
        if 'LocalPlayer' not in m.group(1):
            errors.append(f'{n}: applyForgeHandTransform without a first-person (LocalPlayer) signature')
renderers = [n for n, t in sources.items() if re.search(r'extends\s+BlockEntityWithoutLevelRenderer', t)]
for n in renderers:
    body = sources[n]
    if CLOCK.search(body) and re.search(r'THIRD_PERSON', body):
        errors.append(f'custom item renderer {n} moves the item with a clock in third person')
print(f'client hooks: {props} item property registrations + {len(prop_classes)} property classes without a clock, '
      f'{hands} hand transforms first person only, {len(renderers)} custom item renderers')

# ---------------------------------------------------------------- 4. Wingman code
renderer = sources.get('WingmanRenderer', '')
entity = sources.get('WingmanEntity', '')
pose = sources.get('WingmanPose', '')
fx = sources.get('WingmanFx', '')


def body(text, signature):
    i = text.find(signature)
    if i < 0:
        return ''
    j = text.find('{', i)
    depth = 0
    for k in range(j, len(text)):
        depth += {'{': 1, '}': -1}.get(text[k], 0)
        if depth == 0:
            return text[j + 1:k]
    return ''


setup = body(renderer, 'public void setupAnim(')
rules = [
    ('super.setupAnim(entity, 0, 0, age, netHeadYaw, headPitch)' in setup, 'renderer: no walking swing'),
    ('attackTime = 0;' in setup and 'attackTime <= 0' not in renderer, 'renderer: no switch to the vanilla attack pose'),
    ('WingmanPose.smooth(' in setup and 'WingmanPose.base(' in setup, 'renderer: pose from WingmanPose, smoothed'),
    ('WingmanPose.swing(' in setup and 'WingmanPose.recoil(' in setup and 'WingmanPose.slash(' in setup, 'renderer: additive cut, recoil, slash'),
    ('swing(' not in body(entity, 'public void entangledShot('), 'entity: no swing per entangled shot'),
    ('swing(' not in body(entity, 'private int fire('), 'entity: no swing per shot'),
    (re.search(r'public void swing\(InteractionHand hand, boolean updateSelf\)\s*\{\s*if \(swinging\) return;', entity) is not None,
     'entity: a running swing is not restarted'),
    ('WingmanPose.ranged(w)' in entity and 'WingmanPose.ranged(w.getMainHandItem())' in fx, 'one shared ranged test'),
    ('Float.isFinite' in pose and 'MAX_RATE' in body(pose, 'public static void smooth('), 'pose: fail-safe (finite values, rate limit)'),
    ('Mth.sqrt(a)' not in pose, 'pose: no infinite-slope twist at the start of a swing'),
]
for ok, what in rules:
    if not ok:
        errors.append('wingman: ' + what)
print(f'wingman code: {sum(1 for ok, _ in rules if ok)} of {len(rules)} rules hold')

for n in notes:
    print('note: ' + n)
for e in errors:
    print('ERROR: ' + e)
print(f'{len(errors)} errors, {len(notes)} notes')
sys.exit(1 if errors else 0)
