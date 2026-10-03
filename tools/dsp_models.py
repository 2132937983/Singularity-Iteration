#!/usr/bin/env python3
"""
Adds "holographic infographic" status lights to the refined classic machine models (0.1.7.22).

Every model produced by refine_models.py (IC2 silhouette, clean raised panels) gets:
  * a slim status slit in the bottom frame band of the front face, in the machine's
    category colour (blue processing, orange heat, yellow generation, teal kinetic, green
    nuclear, cyan grid, violet matter / field, red defense, amber oil);
  * a small square indicator LED in the top-right corner of the front frame band;
  * on the running ("_on") variants both glow at full brightness (NeoForge per-face
    block/sky light 15, no ambient occlusion), on the idle variants they are dim glass.
They sit 0.25 px proud of the body inside the 1.5 px frame band, clear of the raised
panels, so nothing z-fights. Re-running replaces the previous lights (elements "dsp_*").
Also writes the 16x16 light texture (one row pair per colour: lit / idle).

Usage: dsp_models.py <assets/mio_icif>
"""
import json, os, sys, glob
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
A = sys.argv[1]

CATS = ['proc', 'heat', 'gen', 'kin', 'nuke', 'grid', 'field', 'def', 'oil', 'neutral']
COL = {'proc': (79, 168, 255), 'heat': (255, 150, 52), 'gen': (255, 210, 63), 'kin': (64, 224, 196), 'nuke': (110, 255, 122),
       'grid': (79, 227, 255), 'field': (176, 124, 255), 'def': (255, 90, 90), 'oil': (255, 184, 64), 'neutral': (200, 214, 226)}

# reuse the icon classifier so a machine's light matches its icon badge
import importlib.util
spec = importlib.util.spec_from_file_location('dsp_icons_rules', os.path.join(os.path.dirname(os.path.abspath(__file__)), 'dsp_icons.py'))
src = open(spec.origin).read()
rules_src = src[src.index('RULES = ['):src.index('def mix(')]
ns = {}
exec(rules_src, ns)
classify = ns['classify']

# texture: 16 columns x 16 rows; row 2*i = lit colour i, row 2*i+1 = idle colour i (8 colours used + spare)
tex = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
px = tex.load()
for i, c in enumerate(CATS[:8]):
    lit = tuple(min(255, int(v * 0.55 + 255 * 0.45)) for v in COL[c])
    idle = tuple(int(v * 0.62 + 20) for v in COL[c])
    for x in range(16):
        edge = x in (0, 15)
        px[x, 2 * i] = (lit if not edge else COL[c]) + (255,)
        px[x, 2 * i + 1] = idle + (255,)
os.makedirs(os.path.join(A, 'textures', 'block', 'refined'), exist_ok=True)
tex.save(os.path.join(A, 'textures', 'block', 'refined', 'status_lights.png'))

def face(row, lit, uvx=(1, 15)):
    v = row * 2 + (0 if lit else 1)
    f = {'uv': [uvx[0], v + 0.1, uvx[1], v + 0.9], 'texture': '#dsp'}
    if lit: f['neoforge_data'] = {'block_light': 15, 'sky_light': 15, 'ambient_occlusion': False}
    return f

count = 0
for path in glob.glob(os.path.join(A, 'models', 'block', '**', '*.json'), recursive=True):
    d = json.load(open(path, encoding='utf-8-sig'))
    if 'refine_models.py' not in d.get('credit', '') or 'elements' not in d: continue
    rel = os.path.relpath(path, os.path.join(A, 'models', 'block'))
    folder = rel.split(os.sep)[0]
    name = os.path.basename(path)[:-5]
    lit = name.endswith('_on') or name.endswith('_active') or name.endswith('_working')
    base = name[:-3] if name.endswith('_on') else name
    _, cat = classify(base, folder)
    row = CATS.index(cat) if CATS.index(cat) < 8 else CATS.index('grid')
    d['elements'] = [e for e in d['elements'] if not e.get('name', '').startswith('dsp_')]
    d['textures']['dsp'] = 'mio_icif:block/refined/status_lights'
    side = {'uv': [0, row * 2 + 1, 1, row * 2 + 2], 'texture': '#dsp'}
    # (0.1.7.24) the bottom slit is now the multi-state status lamp, added at bake time from
    # block/status/lamp_* by the client (MachineStatus model data) - see STATUS LAMPS below
    d['elements'].append({'name': 'dsp_indicator', 'from': [0.5, 14.5, -0.3], "to": [1.5, 15.5, 0],
                          'faces': {'north': face(row, lit, (6, 8)), 'up': dict(side), 'down': dict(side), 'east': dict(side), 'west': dict(side)}})
    json.dump(d, open(path, 'w'), indent=1)
    count += 1
print(count, 'models given status lights')


# ------------------------------------------------------------------ STATUS LAMPS (0.1.7.24)
# Four lamp models (off / running / no power (blinking) / blocked) for the bottom slit, and the
# list of blocks + blockstate variants (with their y rotation) that get one. The client wraps
# those variants' baked models and appends the lamp matching the machine's synced status.
LAMPS = {'off': ((46, 52, 60), (34, 38, 44), False), 'run': ((90, 255, 120), (40, 140, 60), True),
         'nopower': ((255, 190, 40), (90, 62, 18), True), 'blocked': ((255, 70, 60), (120, 30, 26), True)}
sd = os.path.join(A, 'textures', 'block', 'status'); os.makedirs(sd, exist_ok=True)
md = os.path.join(A, 'models', 'block', 'status'); os.makedirs(md, exist_ok=True)
for key, (hi, lo, glow) in LAMPS.items():
    frames = 2 if key == 'nopower' else 1
    im = Image.new('RGBA', (16, 16 * frames), (0, 0, 0, 255))
    px = im.load()
    for f in range(frames):
        on = f == 0
        for y in range(16):
            for x in range(16):
                c = hi if on else lo
                if y in (0, 15): c = tuple(int(v * 0.75) for v in c)          # slit lips
                elif y in (6, 7, 8, 9) and on and glow: c = tuple(min(255, int(v * 0.6 + 255 * 0.4)) for v in c)  # hot core line
                px[x, f * 16 + y] = c + (255,)
    im.save(os.path.join(sd, 'lamp_' + key + '.png'))
    if frames > 1:
        json.dump({'animation': {'frametime': 8, 'interpolate': False}}, open(os.path.join(sd, 'lamp_' + key + '.png.mcmeta'), 'w'))
    else:
        mc = os.path.join(sd, 'lamp_' + key + '.png.mcmeta')
        if os.path.exists(mc): os.remove(mc)
    front = {'uv': [0, 0, 16, 16], 'texture': '#lamp'}
    if glow: front['neoforge_data'] = {'block_light': 15, 'sky_light': 15, 'ambient_occlusion': False}
    side = {'uv': [0, 0, 16, 1], 'texture': '#lamp'}
    model = {'credit': 'Singularity Iteration status lamp (dsp_models.py)', 'textures': {'lamp': 'mio_icif:block/status/lamp_' + key,
             'particle': 'mio_icif:block/status/lamp_' + key},
             'elements': [{'name': 'status_slit', 'from': [2.5, 0.3, -0.3], 'to': [13.5, 1.2, 0], 'shade': False,
                           'faces': {'north': front, 'up': dict(side), 'down': dict(side), 'east': dict(side), 'west': dict(side)}}]}
    json.dump(model, open(os.path.join(md, 'lamp_' + key + '.json'), 'w'), indent=1)

refined = set()
for path in glob.glob(os.path.join(A, 'models', 'block', '**', '*.json'), recursive=True):
    try: d = json.load(open(path, encoding='utf-8-sig'))
    except Exception: continue
    if 'refine_models.py' in d.get('credit', '') and any(e.get('name') == 'panels_ns' for e in d.get('elements', [])):
        refined.add('mio_icif:block/' + os.path.relpath(path, os.path.join(A, 'models', 'block'))[:-5].replace(os.sep, '/'))
entries = []
for path in sorted(glob.glob(os.path.join(A, 'blockstates', '**', '*.json'), recursive=True)):
    try: bs = json.load(open(path, encoding='utf-8-sig'))
    except Exception: continue
    variants = bs.get('variants')
    if not isinstance(variants, dict): continue
    rots = {}
    for key, v in variants.items():
        v = v[0] if isinstance(v, list) else v
        if v.get('model') not in refined or v.get('x', 0): continue
        props = dict(kv.split('=') for kv in key.split(',') if '=' in kv)
        norm = ','.join(k + '=' + props[k] for k in sorted(props))
        rots[norm] = int(v.get('y', 0)) % 360
    if rots:
        bid = 'mio_icif:' + os.path.relpath(path, os.path.join(A, 'blockstates'))[:-5].replace(os.sep, '/')
        entries.append({'block': bid, 'variants': rots})
json.dump({'comment': 'Blocks whose front carries the multi-state status lamp (generated by dsp_models.py)', 'blocks': entries},
          open(os.path.join(A, 'si_status_lamps.json'), 'w'), indent=1)
print(len(entries), 'blocks get a status lamp')
