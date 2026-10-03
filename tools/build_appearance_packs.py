#!/usr/bin/env python3
"""
0.1.7.27 appearance layout: the classic IC2 look is the mod's DEFAULT, the refined / DSP look is
the optional built-in resource pack `resourcepacks/si_experimental`.

Starting from the 0.1.7.26 layout (refined look in assets/, classic overrides in
resourcepacks/si_classic) this script swaps them:

  * block models that have a classic override: refined copy -> experimental pack, classic -> base
  * machine item models with a flat DSP inventory icon -> experimental pack; the base item model
    becomes the plain block model ({"parent": block}), so the inventory item always shows the
    same model as the placed block in either style
  * textures/item/icon (DSP icons) -> experimental pack
  * textures/block/refined/* only used by moved models -> experimental pack
  * si_status_lamps.json (status lamps) -> experimental pack; the base list is empty
  * resourcepacks/si_classic is removed

Usage: build_appearance_packs.py <src/main/resources>
"""
import json, os, re, shutil, sys

RES = sys.argv[1]
BASE = os.path.join(RES, 'assets', 'mio_icif')
CLASSIC = os.path.join(RES, 'resourcepacks', 'si_classic', 'assets', 'mio_icif')
EXP_ROOT = os.path.join(RES, 'resourcepacks', 'si_experimental')
EXP = os.path.join(EXP_ROOT, 'assets', 'mio_icif')
assert os.path.isdir(CLASSIC), 'expects the 0.1.7.26 layout (si_classic present)'


def move(src, dst):
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.move(src, dst)


def load(p):
    return json.load(open(p, encoding='utf-8-sig'))


def refs(model):
    out = set()
    for v in model.get('textures', {}).values():
        if isinstance(v, str) and not v.startswith('#'):
            out.add(v if ':' in v else 'minecraft:' + v)
    return out


moved_models = 0
for d, _, fs in os.walk(os.path.join(CLASSIC, 'models', 'block')):
    for f in fs:
        rel = os.path.relpath(os.path.join(d, f), CLASSIC)
        move(os.path.join(BASE, rel), os.path.join(EXP, rel))
        move(os.path.join(d, f), os.path.join(BASE, rel))
        moved_models += 1

moved_items = 0
for d, _, fs in os.walk(os.path.join(BASE, 'models', 'item')):
    for f in fs:
        p = os.path.join(d, f)
        m = load(p)
        if m.get('loader') != 'neoforge:separate_transforms': continue
        gui = m.get('perspectives', {}).get('gui', {})
        layer = gui.get('textures', {}).get('layer0', '')
        if '/icon/' not in layer: continue
        parent = m.get('base', {}).get('parent')
        if not parent: continue
        rel = os.path.relpath(p, BASE)
        os.makedirs(os.path.dirname(os.path.join(EXP, rel)), exist_ok=True)
        shutil.copy(p, os.path.join(EXP, rel))
        json.dump({'parent': parent}, open(p, 'w', encoding='utf-8'), indent=2)
        moved_items += 1

icon_dir = os.path.join(BASE, 'textures', 'item', 'icon')
if os.path.isdir(icon_dir):
    move(icon_dir, os.path.join(EXP, 'textures', 'item', 'icon'))

# refined block textures still referenced by base models stay; the rest move
still_used = set()
for d, _, fs in os.walk(os.path.join(BASE, 'models')):
    for f in fs:
        if f.endswith('.json'):
            try: still_used |= refs(load(os.path.join(d, f)))
            except Exception: pass
moved_tex = 0
ref_dir = os.path.join(BASE, 'textures', 'block', 'refined')
for d, _, fs in os.walk(ref_dir):
    for f in fs:
        p = os.path.join(d, f)
        rel_tex = os.path.relpath(p, os.path.join(BASE, 'textures')).replace(os.sep, '/')
        key = 'mio_icif:' + re.sub(r'\.png(\.mcmeta)?$', '', rel_tex)
        if key in still_used: continue
        move(p, os.path.join(EXP, 'textures', rel_tex))
        moved_tex += 1

lamps = os.path.join(BASE, 'si_status_lamps.json')
move(lamps, os.path.join(EXP, 'si_status_lamps.json'))
json.dump({'comment': 'Default (classic) look: no status lamps. The experimental pack ships the list.', 'blocks': []},
          open(lamps, 'w', encoding='utf-8'), indent=1)

shutil.rmtree(os.path.join(RES, 'resourcepacks', 'si_classic'))
json.dump({'pack': {'pack_format': 34, 'description': 'Singularity Iteration - Experimental look (refined machine models, status lamps, DSP icons)'}},
          open(os.path.join(EXP_ROOT, 'pack.mcmeta'), 'w', encoding='utf-8'), indent=2)
print(f'block models swapped {moved_models}, item models -> block parent {moved_items}, refined textures moved {moved_tex}')
