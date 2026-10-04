"""Builds Molecular-Transformer-style GUI backgrounds for the advanced machines.
Clean MT panel, MT slot wells at the menu slot positions (slot x,y = item origin), nothing else:
progress arrows, energy bars and buttons are drawn by the screens."""
import sys, os
from PIL import Image
G = sys.argv[1]
mt = Image.open(os.path.join(G, 'gui_molecular_transformer.png')).convert('RGBA')
WELL = mt.crop((54, 34, 72, 52))
def clean():
    im = mt.copy(); px = im.load()
    for (x0, y0) in ((54, 34), (114, 34)):
        for y in range(y0, y0 + 18):
            for x in range(x0, x0 + 18): px[x, y] = px[30, y]
    # the small corner mark near the bottom right of the MT machine area
    for y in range(64, 76):
        for x in range(136, 150): px[x, y] = px[30, y]
    return im
def build(name, slots):
    im = clean()
    for (x, y) in slots: im.paste(WELL, (x - 1, y - 1))
    im.save(os.path.join(G, name + '.png'))
UP4 = []   # upgrade slots live in the left utility dock (0.1.7.22)
single = [(55, 35), (115, 35), (17, 58)] + UP4
build('gui_adv_compressor', single)
build('gui_adv_powder', single)
build('gui_adv_blast_furnace_elc', single)
build('gui_adv_metal_former', [(37, 35), (115, 35), (17, 58)] + UP4)
build('gui_adv_induction', [(37, 35), (55, 35), (115, 35), (133, 35), (17, 58), ])
# geomagnetic generator: one charge (buffer) slot, energy bar under it
build('gui_adv_geomagnetic', [(115, 35)])

build('gui_adv_solar', [(115, 35)])
