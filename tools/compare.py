import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
import refine_models as rm
from PIL import Image, ImageDraw
CUR = '/home/claude/work/src/src/main/resources/assets/mio_icif'
ORG = '/home/claude/orig/src/main/resources/assets/mio_icif'
ids = sys.argv[2:]; out = sys.argv[1]
def tex_of(assets, model):
    t = model['textures'].get('r') or model['textures'].get('all')
    if t is None:
        t = [v for k, v in model['textures'].items() if k != 'particle'][0]
    return Image.open(os.path.join(assets, 'textures', t.split(':')[1] + '.png')).convert('RGBA')
def draw(assets, rel):
    m = rm.load(os.path.join(assets, 'models/block', rel + '.json'))
    if 'elements' not in m:
        par = m['parent'].split(':')[1][len('block/'):]
        base = rm.load(os.path.join(assets, 'models/block', par + '.json')); base['textures'].update(m.get('textures', {})); m = base
    tex = tex_of(assets, m)
    rm.TW, rm.TH = tex.size
    img = rm.render(m, tex, scale=6)
    rm.TW, rm.TH = 128, 64
    return img
tiles = []
for rel in ids:
    tiles.append((draw(ORG, rel), draw(CUR, rel), rel))
tw, th = tiles[0][0].size
cols = 4
rows = (len(tiles) + cols - 1) // cols
sheet = Image.new('RGBA', (cols * (2 * tw + 16), rows * (th + 14) + 4), (118, 140, 108, 255))
d = ImageDraw.Draw(sheet)
for i, (a, b, rel) in enumerate(tiles):
    x = (i % cols) * (2 * tw + 16); y = (i // cols) * (th + 14)
    sheet.alpha_composite(a, (x, y + 12)); sheet.alpha_composite(b, (x + tw, y + 12))
    d.text((x + 4, y), rel.split('/')[-1].replace('block_', '') + '   before | after', fill=(255, 255, 255, 255))
sheet.save(out)
