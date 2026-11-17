#!/usr/bin/env python3
"""0.1.11.33 (cable audit W4): writes the casing parts of every cable blockstate from the `casing` property (WireCasing).
Keeps the core and the side parts of each file; replaces every part whose model is under mio_icif:block/build/ with one
part per drawn casing value (the disguised values draw no casing: the disguise model wrapper draws the disguise).
Idempotent; keeps the line endings. With --check it only reports files that would change.
Usage: gen_cable_casing_blockstates.py <repo root> [--check]
"""
import glob
import json
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
DIR = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/blockstates/wiring')
CASINGS = [
    ('construction_wall', 'mio_icif:block/build/block_construction_wall'),
    ('cf_wall', 'mio_icif:block/build/block_cf_wall'),
    ('scaffold', 'mio_icif:block/build/block_scaffold_iron'),
    ('foam', 'mio_icif:block/build/block_construction_foam'),
    ('reinforced_foam', 'mio_icif:block/build/block_construction_foam_reinforced'),
]


def model_of(part):
    apply = part['apply']
    return (apply[0] if isinstance(apply, list) else apply)['model']


def main():
    files = sorted(glob.glob(os.path.join(DIR, '*_cable.json')) + glob.glob(os.path.join(DIR, 'cable', '*.json')))
    changed = 0
    for path in files:
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'))
        parts = data['multipart']
        if not any(model_of(p).startswith('mio_icif:block/build/') for p in parts):
            continue
        core = [p for p in parts if 'when' not in p]
        sides = [p for p in parts if 'when' in p and not model_of(p).startswith('mio_icif:block/build/')]
        casing = [{'when': {'casing': value}, 'apply': {'model': model}} for value, model in CASINGS]
        data['multipart'] = core + casing + sides
        text = (json.dumps(data, indent=2) + '\n').replace('\n', eol)
        if text.encode('utf-8') != raw:
            changed += 1
            if CHECK:
                print('would change', os.path.relpath(path, ROOT))
            else:
                open(path, 'wb').write(text.encode('utf-8'))
    print(('files to change: ' if CHECK else 'files changed: ') + str(changed), 'of', len(files))
    if CHECK and changed:
        sys.exit(1)


if __name__ == '__main__':
    main()
