#!/usr/bin/env python3
"""
0.1.8.0: Create Ponder scene schematics (vanilla structure NBT, gzip), no dependencies.

Layer y = 0 is the base plate (white concrete); the machines stand on y >= 1. The storyboards in
SiPonderScenes show these blocks section by section.
Output: src/main/resources/assets/mio_icif/ponder/<name>.nbt
Usage: ponder_schematics_180.py <repo root>
"""
import gzip
import io
import os
import struct
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
OUT = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/ponder')
DATA_VERSION = 3955  # Minecraft 1.21.1


# ---------------------------------------------------------------- minimal NBT writer
def _name(buf, s):
    b = s.encode('utf-8')
    buf.write(struct.pack('>H', len(b)))
    buf.write(b)


def _payload(buf, tag, value):
    if tag == 3:
        buf.write(struct.pack('>i', value))
    elif tag == 8:
        _name(buf, value)
    elif tag == 9:
        elem, items = value
        buf.write(struct.pack('>bi', elem if items else 0, len(items)))
        for it in items:
            _payload(buf, elem, it)
    elif tag == 10:
        for key, (t, v) in value.items():
            buf.write(struct.pack('>b', t))
            _name(buf, key)
            _payload(buf, t, v)
        buf.write(b'\x00')
    else:
        raise ValueError(tag)


def compound(**kw):
    return (10, kw)


def write_nbt(path, root):
    buf = io.BytesIO()
    buf.write(struct.pack('>b', 10))
    _name(buf, '')
    _payload(buf, 10, root[1])
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with gzip.open(path, 'wb') as f:
        f.write(buf.getvalue())


def structure(size, blocks):
    """blocks: dict (x, y, z) -> (block id, {property: value})."""
    palette, index = [], {}
    entries = []
    for (x, y, z), (name, props) in sorted(blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
        key = (name, tuple(sorted(props.items())))
        if key not in index:
            index[key] = len(palette)
            state = {'Name': (8, name)}
            if props:
                state['Properties'] = (10, {k: (8, v) for k, v in props.items()})
            palette.append(state)
        entries.append({'pos': (9, (3, [x, y, z])), 'state': (3, index[key])})
    return compound(
        DataVersion=(3, DATA_VERSION),
        size=(9, (3, list(size))),
        palette=(9, (10, palette)),
        blocks=(9, (10, entries)),
        entities=(9, (10, [])))


def plate(w, d):
    return {(x, 0, z): ('minecraft:white_concrete', {}) for x in range(w) for z in range(d)}


def main():
    scenes = {}
    b = plate(5, 5)
    b[(2, 1, 2)] = ('mio_icif:producer/block_armory', {'facing': 'south', 'lit': 'true'})
    b[(3, 1, 2)] = ('mio_icif:producer/block_armor_showcase', {})
    b[(1, 1, 2)] = ('minecraft:redstone_block', {})
    scenes['armory'] = ((5, 2, 5), b)

    b = plate(5, 5)
    b[(2, 1, 2)] = ('mio_icif:generator/block_nuclear_reactor_generator', {'facing': 'south'})
    for p in ((1, 1, 2), (3, 1, 2), (2, 1, 1), (2, 1, 3), (2, 2, 2)):
        b[p] = ('mio_icif:reactor/block_reactor_chamber', {})
    scenes['nuclear_reactor'] = ((5, 3, 5), b)

    b = plate(7, 7)
    b[(1, 1, 1)] = ('mio_icif:producer/block_blueprint_builder', {'facing': 'south'})
    b[(5, 1, 1)] = ('mio_icif:producer/block_material_interface', {})
    b[(5, 1, 2)] = ('minecraft:chest', {'facing': 'west'})
    for x in range(2, 5):                       # the small house the drones build
        for z in range(3, 6):
            b[(x, 1, z)] = ('minecraft:stone_bricks', {})
    for x in range(2, 5):
        for z in range(3, 6):
            if x in (2, 4) or z in (3, 5):
                b[(x, 2, z)] = ('minecraft:oak_planks', {})
    for x in range(2, 5):
        for z in range(3, 6):
            b[(x, 3, z)] = ('minecraft:oak_slab', {'type': 'bottom'})
    scenes['blueprint_builder'] = ((7, 4, 7), b)

    b = plate(5, 5)
    b[(1, 1, 2)] = ('minecraft:chest', {'facing': 'east'})
    b[(2, 1, 2)] = ('mio_icif:pipe/block_pipe_ejector', {'facing': 'east'})
    scenes['ejector_pipe'] = ((5, 2, 5), b)

    for name, (size, blocks) in scenes.items():
        write_nbt(os.path.join(OUT, name + '.nbt'), structure(size, blocks))
        print('wrote', name, len(blocks), 'blocks')


if __name__ == '__main__':
    main()
