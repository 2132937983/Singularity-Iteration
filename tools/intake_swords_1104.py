#!/usr/bin/env python3
"""
0.1.10.4: imports the user's sword art (3D Blockbench models and 16x16 inventory icons).

Inputs (extracted archives, passed as paths):
  <gui dir>      gui贴图-剑.zip: nanosaber.png, nanosaber_active.png, quantum_sword.png (+ .mcmeta),
                 "quantum_sword - 1.png" (+ "quantum_sword.png - 1.mcmeta") = the hyper (overclocked) icon
  <quantum dir>  量子剑.zip: 量子剑.json, 量子剑-普通.png (normal), 量子剑-超频.png (hyper)
  <nano dir>     纳米剑.zip: item_tool_nanosaber.json, item_tool_nanosaber_active.json,
                 item_nanosaber_active.png, item_nanosaber_deactive.png

Outputs (under src/main/resources/assets/mio_icif):
  models/item/3d/weapon/{nanosaber,nanosaber_active,quantum_sword,quantum_sword_hyper}.json
  textures/item/item_nanosaber_deactive.png, item_nanosaber_active.png      (3D, replaced in place)
  textures/item/item_tool_quantum_sword.png, item_tool_quantum_sword_hyper.png  (3D)
  textures/item/weapon/{nanosaber,nanosaber_active,quantum_sword,quantum_sword_hyper}.png (+ mcmeta)
  models/item/item_tool_nanosaber(_active).json, item_tool_quantum_sword(_hyper).json:
      neoforge:separate_transforms, base = the 3D model, gui = the 2D icon

Conversion of the Blockbench 26.x export for 1.21.1:
  * texture references -> mio_icif:item/... (the export names the PNG files, partly in Chinese)
  * "light_emission" (1.21.2+) -> "neoforge_data": {"block_light": n}
  * "shade_direction_override" (not a 1.21.1 key) and the "on_shelf" display (1.21.9+) are dropped
  * the quantum sword has no "ground" display: it gets the one of the nanosaber (same 2-block geometry)
  * the nanosaber files of the archive are named the other way round from their textures
    (item_tool_nanosaber.json uses the green "active" texture). Each file keeps the UVs it was
    made for; the model with the steel blade becomes the "off" model, the green one the "on" model.
"""
import json
import os
import shutil
import sys

ROOT, GUI, QUANTUM, NANO = sys.argv[1:5]
A = os.path.join(ROOT, 'src/main/resources/assets/mio_icif')
DROP_ELEMENT_KEYS = {'shade_direction_override'}
KNOWN_DISPLAY = {'thirdperson_righthand', 'thirdperson_lefthand', 'firstperson_righthand', 'firstperson_lefthand',
                 'gui', 'head', 'ground', 'fixed'}


def load(path):
    with open(path, encoding='utf-8-sig') as f:
        return json.load(f)


def save(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write('\n')


def copy(src, rel):
    dst = os.path.join(A, rel)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copyfile(src, dst)


def remove(rel):
    path = os.path.join(A, rel)
    if os.path.exists(path):
        os.remove(path)


def convert(src, texture, ground=None):
    m = load(src)
    out = {'credit': 'Made with Blockbench (user art, imported by tools/intake_swords_1104.py)',
           'texture_size': m.get('texture_size', [16, 16]),
           'render_type': 'minecraft:cutout',
           'textures': {'0': texture, 'particle': texture},
           'elements': []}
    for e in m['elements']:
        e = {k: v for k, v in e.items() if k not in DROP_ELEMENT_KEYS}
        light = e.pop('light_emission', 0)
        if light:
            e['neoforge_data'] = {'block_light': int(light)}
        rot = e.get('rotation')
        if rot and rot.get('angle', 0) not in (-45, -22.5, 0, 22.5, 45):
            raise SystemExit('rotation angle not allowed in 1.21.1: %s' % rot)
        for k in ('from', 'to'):
            if any(c < -16 or c > 32 for c in e[k]):
                raise SystemExit('element outside -16..32: %s' % e[k])
        for face in e['faces'].values():
            face['texture'] = '#0'
        out['elements'].append(e)
    display = {k: v for k, v in m.get('display', {}).items() if k in KNOWN_DISPLAY}
    if ground and 'ground' not in display:
        display['ground'] = ground
    out['display'] = display
    return out


def item_model(base, icon, translucent_icon, override=None):
    gui = {'parent': 'minecraft:item/generated', 'textures': {'layer0': icon}}
    if translucent_icon:
        gui['render_type'] = 'minecraft:translucent'
    model = {'loader': 'neoforge:separate_transforms',
             'base': {'parent': base},
             'perspectives': {'gui': gui}}
    if override:
        model['overrides'] = [override]
    return model


def main():
    # --- nanosaber: the file with the steel ("deactive") texture is the off model
    nano_a = os.path.join(NANO, 'item_tool_nanosaber.json')
    nano_b = os.path.join(NANO, 'item_tool_nanosaber_active.json')
    by_texture = {}
    for path in (nano_a, nano_b):
        by_texture[load(path)['textures']['0']] = path
    off_src, on_src = by_texture['item_nanosaber_deactive'], by_texture['item_nanosaber_active']
    off = convert(off_src, 'mio_icif:item/item_nanosaber_deactive')
    on = convert(on_src, 'mio_icif:item/item_nanosaber_active')
    save(os.path.join(A, 'models/item/3d/weapon/nanosaber.json'), off)
    save(os.path.join(A, 'models/item/3d/weapon/nanosaber_active.json'), on)
    copy(os.path.join(NANO, 'item_nanosaber_deactive.png'), 'textures/item/item_nanosaber_deactive.png')
    copy(os.path.join(NANO, 'item_nanosaber_active.png'), 'textures/item/item_nanosaber_active.png')
    remove('textures/item/item_nanosaber_active.png.mcmeta')          # the old 3D texture was a strip
    ground = off['display'].get('ground')

    # --- quantum sword: one model, two textures (normal / hyper)
    q_src = os.path.join(QUANTUM, '量子剑.json')
    save(os.path.join(A, 'models/item/3d/weapon/quantum_sword.json'), convert(q_src, 'mio_icif:item/item_tool_quantum_sword', ground))
    save(os.path.join(A, 'models/item/3d/weapon/quantum_sword_hyper.json'),
         convert(q_src, 'mio_icif:item/item_tool_quantum_sword_hyper', ground))
    copy(os.path.join(QUANTUM, '量子剑-普通.png'), 'textures/item/item_tool_quantum_sword.png')
    copy(os.path.join(QUANTUM, '量子剑-超频.png'), 'textures/item/item_tool_quantum_sword_hyper.png')

    # --- inventory icons
    copy(os.path.join(GUI, 'nanosaber.png'), 'textures/item/weapon/nanosaber.png')
    copy(os.path.join(GUI, 'nanosaber_active.png'), 'textures/item/weapon/nanosaber_active.png')
    remove('textures/item/weapon/nanosaber_active.png.mcmeta')         # 16x16, one frame
    copy(os.path.join(GUI, 'quantum_sword.png'), 'textures/item/weapon/quantum_sword.png')
    copy(os.path.join(GUI, 'quantum_sword.png.mcmeta'), 'textures/item/weapon/quantum_sword.png.mcmeta')
    copy(os.path.join(GUI, 'quantum_sword - 1.png'), 'textures/item/weapon/quantum_sword_hyper.png')
    copy(os.path.join(GUI, 'quantum_sword.png - 1.mcmeta'), 'textures/item/weapon/quantum_sword_hyper.png.mcmeta')

    # --- item models
    save(os.path.join(A, 'models/item/item_tool_nanosaber.json'), item_model(
        'mio_icif:item/3d/weapon/nanosaber', 'mio_icif:item/weapon/nanosaber', False,
        {'predicate': {'mio_icif:nanosaber_active': 1}, 'model': 'mio_icif:item/item_tool_nanosaber_active'}))
    save(os.path.join(A, 'models/item/item_tool_nanosaber_active.json'), item_model(
        'mio_icif:item/3d/weapon/nanosaber_active', 'mio_icif:item/weapon/nanosaber_active', True))
    save(os.path.join(A, 'models/item/item_tool_quantum_sword.json'), item_model(
        'mio_icif:item/3d/weapon/quantum_sword', 'mio_icif:item/weapon/quantum_sword', True,
        {'predicate': {'mio_icif:quantum_sword_hyper': 1}, 'model': 'mio_icif:item/item_tool_quantum_sword_hyper'}))
    save(os.path.join(A, 'models/item/item_tool_quantum_sword_hyper.json'), item_model(
        'mio_icif:item/3d/weapon/quantum_sword_hyper', 'mio_icif:item/weapon/quantum_sword_hyper', True))
    print('swords imported')


if __name__ == '__main__':
    main()
