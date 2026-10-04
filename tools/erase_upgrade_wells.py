"""Erases the old upgrade-slot wells from machine GUI textures (upgrade slots moved to the
left utility dock). Input: upgrade_layout.txt dumped by the capture client
(block|screen|x,y;...|textures). Each well (18x18 at slot-1) is filled with the dominant
panel colour of the ring just around the group of wells."""
import sys, os
from collections import Counter
from PIL import Image
layout, gui_dir = sys.argv[1], sys.argv[2]
backup = sys.argv[3] if len(sys.argv) > 3 else None
tex = {}
# textures whose 'wells' are part of a shared design (the steam generator uses the same art)
SKIP = {'gui_steam_generator_elc.png'}
for line in open(layout):
    parts = line.strip().split('|')
    if len(parts) < 4 or not parts[2]: continue
    pos = [tuple(map(int, p.split(','))) for p in parts[2].split(';') if p]
    for t in parts[3].split(';'):
        if t.startswith('mio_icif:textures/gui/'):
            tex.setdefault(t.split('/')[-1], set()).update(pos)
for name, pos in sorted(tex.items()):
    path = os.path.join(gui_dir, name)
    im = Image.open(path).convert('RGBA'); px = im.load()
    if backup: im.save(os.path.join(backup, name))
    W = min(im.width, 176)
    mask = set()
    for (x, y) in pos:
        if x + 17 > W - 3: continue                       # outside the panel: nothing baked in
        if name in SKIP: continue
        for yy in range(y - 1, y + 17):
            for xx in range(x - 1, x + 17): mask.add((xx, yy))
    if not mask: continue
    ring = Counter()
    for (xx, yy) in mask:
        for dx in (-2, 2):
            for dy in (-2, 0, 2):
                q = (xx + dx, yy + dy)
                if q in mask or not (0 <= q[0] < im.width and 0 <= q[1] < im.height): continue
                if px[q][3] > 0: ring[px[q]] += 1
    # panel colour: the most common LIGHT colour around the wells (never a dark inset frame)
    light = [(c, n) for c, n in ring.most_common() if sum(c[:3]) / 3 > 150]
    fill = (light or ring.most_common())[0][0]
    for q in mask: px[q] = fill
    im.save(path)
    print(name, len(pos), 'wells ->', fill)
