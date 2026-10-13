#!/usr/bin/env python3
"""0.1.8.3 intake of the user's resources.zip (Downloads) into the source tree.

The archive is a full `src/main/resources` snapshot of an older code base (old mixin package,
older recipes, tags and lang files). Copying it whole would roll the mod back, so only the art
that was redrawn for this update is taken:

* entries under assets/mio_icif/textures/ with the extension .png or .png.mcmeta,
* whose archive date is 2026-09-17 or later (the redraw period; older entries are stale copies,
  for example the 2022 tachyon disruptor icon and the 16x16 electric lighter),
* whose bytes differ from the file in the tree,
* not in EXCLUDE: dated in the period but byte-equal to an older shipped version (0.1.7.24), so
  the tree already holds a newer drawing.

Scripts and notes inside the texture folders (*.py, *.txt) are not copied. Replaced files are
saved to <backup dir> first. Prints one line per copied file.

Usage: intake_resources_183.py <resources.zip> <repo root> <backup dir>
"""
import datetime
import os
import sys
import zipfile

PREFIX = 'assets/mio_icif/textures/'
SINCE = (2026, 9, 17)
EXCLUDE = {
    PREFIX + 'item/item_trinket_energy_crystal_belt.png',
    PREFIX + 'item/item_trinket_lapotron_crystal_belt.png',
}


def main():
    if len(sys.argv) != 4:
        print(__doc__)
        sys.exit(2)
    archive, root, backup = sys.argv[1:]
    res = os.path.join(root, 'src/main/resources')
    copied = 0
    with zipfile.ZipFile(archive) as z:
        for e in z.infolist():
            name = e.filename
            if e.is_dir() or not name.startswith(PREFIX):
                continue
            if not (name.endswith('.png') or name.endswith('.png.mcmeta')):
                continue
            if e.date_time[:3] < SINCE or name in EXCLUDE:
                continue
            data = z.read(e)
            target = os.path.join(res, name)
            old = open(target, 'rb').read() if os.path.exists(target) else None
            if old == data:
                continue
            if old is not None:
                keep = os.path.join(backup, name)
                os.makedirs(os.path.dirname(keep), exist_ok=True)
                with open(keep, 'wb') as f:
                    f.write(old)
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with open(target, 'wb') as f:
                f.write(data)
            copied += 1
            print('%s %s %s' % (datetime.date(*e.date_time[:3]), 'new' if old is None else 'replaced', name[len(PREFIX):]))
    print('copied:', copied)


if __name__ == '__main__':
    main()
