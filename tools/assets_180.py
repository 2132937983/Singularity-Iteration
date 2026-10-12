#!/usr/bin/env python3
"""0.1.8.0 asset table edits (idempotent): sounds.json entries of the new sound events.

Usage: assets_180.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
SOUNDS = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/sounds.json')

NEW_SOUNDS = {
    'armory.seal': 'sfx_armory_seal',
    'machine.loop.uu_fabricator': 'sfx_uu_fabricator',
    'machine.loop.kinetic_drive': 'sfx_kinetic_drive',
}


def main():
    with open(SOUNDS, encoding='utf-8') as f:
        data = json.load(f, object_pairs_hook=OrderedDict)
    for event, file in NEW_SOUNDS.items():
        data[event] = OrderedDict([
            ('subtitle', 'mio_icif.subtitle.' + event),
            ('sounds', [OrderedDict([('name', 'mio_icif:' + file), ('attenuation_distance', 16)])]),
        ])
    with open(SOUNDS, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')
    print('sounds.json: set', len(NEW_SOUNDS), 'events')


if __name__ == '__main__':
    main()
