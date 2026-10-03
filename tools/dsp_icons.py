#!/usr/bin/env python3
"""
SI machine icons: IC2 classic x "holographic infographic" refinement (0.1.7.22).

Input: the 32x32 isometric machine icons (rendered from the block models, so the IC2
silhouette, layout and colours stay), kept unmodified in a source folder.
Output, per icon:
  * a crisp 1-px dark contour around the silhouette (clean cut lines),
  * a thin glowing accent line on the cube's top front edges in the machine's
    category colour (blue = processing, orange = heat, yellow = generation,
    teal = kinetic, green = nuclear, cyan = storage / grid, violet = matter / field,
    red = defense),
  * a round dark "holo" badge in the lower-right corner with a glowing ring and a bold
    mechanical pictogram of what the machine does (crush, compress, flame, bolt, sun,
    blades, wave, atom, battery, up/down arrows, magnet, drop, gear, ...), so each
    machine is identifiable at a glance even at 16 px.

All art is procedural (no external assets). Usage:
  dsp_icons.py <icon_src_dir> <icon_out_dir> [--montage out.png]
"""
import os, sys, math
from PIL import Image

SRC, OUT = sys.argv[1], sys.argv[2]
MONTAGE = sys.argv[sys.argv.index('--montage') + 1] if '--montage' in sys.argv else None
# 0.1.7.23: the corner badge is off by default (it read as foreign on a Minecraft item icon); --badge restores it
BADGE = '--badge' in sys.argv

COL = {
    'proc': (79, 168, 255), 'heat': (255, 150, 52), 'gen': (255, 210, 63), 'kin': (64, 224, 196),
    'nuke': (110, 255, 122), 'grid': (79, 227, 255), 'field': (176, 124, 255), 'def': (255, 90, 90),
    'oil': (255, 184, 64), 'neutral': (200, 214, 226),
}

# 9x9 pictograms ('#' = lit, '+' = dim)
G = {
 'crush': ["#.#.#.#.#", "#########", ".........", "..#...#..", ".###.###.", "..#...#..", ".........", "#########", "#.#.#.#.#"],
 'compress': ["....#....", "....#....", "..#####..", "...###...", "#########", "...###...", "..#####..", "....#....", "....#...."],
 'extract': ["....#....", "...###...", "..#####..", ".#######.", ".#######.", ".###.###.", "..#####..", "...###...", "........."],
 'flame': ["....#....", "...##....", "...###...", "..####.#.", "..#####..", ".###+###.", ".##+++##.", "..#+++#..", "...###..."],
 'blast': ["#.......#", "#..#....#", "#..##...#", "#.####..#", "#.#####.#", "#.##+##.#", "#.#+++#.#", "#########", "#########"],
 'coil': [".#######.", "#.......#", ".#######.", "#.......#", ".#######.", "#.......#", ".#######.", "....#....", "...###..."],
 'bolt': ["....###..", "...###...", "..###....", ".#######.", "....###..", "...###...", "..###....", ".##......", ".#......."],
 'sun': ["....#....", ".#..#..#.", "..#...#..", "...###...", "####+####", "...###...", "..#...#..", ".#..#..#.", "....#...."],
 'blades': ["....#....", "....##...", "....###..", "#...#....", "##.#+#.##", "....#...#", "..###....", "...##....", "....#...."],
 'wave': [".........", ".##...##.", "#..#.#..#", "....#....", ".........", ".##...##.", "#..#.#..#", "....#....", "........."],
 'drop': ["....#....", "....#....", "...###...", "..#####..", ".###+###.", ".##+++##.", ".###+###.", "..#####..", "...###..."],
 'lava': ["....#....", "...###...", "..##+##..", ".##+++##.", ".#+++++#.", ".##+++##.", "..#####..", ".........", "#########"],
 'atom': ["...###...", "..#...#..", ".#.###.#.", "#.#...#.#", "#.#.#.#.#", "#.#...#.#", ".#.###.#.", "..#...#..", "...###..."],
 'battery': ["...###...", ".#######.", ".#.....#.", ".#+++++#.", ".#.....#.", ".#+++++#.", ".#.....#.", ".#+++++#.", ".#######."],
 'updown': ["..#......", ".###.....", "#####....", "..#......", "..#...#..", "......#..", "....#####", ".....###.", "......#.."],
 'gear': ["...#.#...", ".#######.", ".##...##.", "###.#.###", "#...#...#", "###.#.###", ".##...##.", ".#######.", "...#.#..."],
 'magnet': [".##...##.", ".##...##.", ".##...##.", ".##...##.", ".##...##.", ".###.###.", "..#####..", "...###...", "+++...+++"],
 'blade': [".......##", "......###", ".....###.", "....###..", "...###...", ".####....", "####.....", ".##......", "#........"],
 'roller': [".#######.", "#+++++++#", ".#######.", ".........", "#########", ".........", ".#######.", "#+++++++#", ".#######."],
 'spin': ["...###...", ".##...##.", ".#.....#.", "#...#...#", "#..###..#", "#...#...#", ".#.....#.", ".##..###.", "...####.."],
 'bubbles': ["..#......", ".#.#..##.", "..#..#..#", "......##.", "...#.....", "..#.#....", "...#.###.", ".....#..#", "......##."],
 'can': ["..#####..", ".#.....#.", ".#######.", ".#.....#.", ".#.+++.#.", ".#.+++.#.", ".#.....#.", ".#######.", "..#####.."],
 'pick': [".#######.", "#...#...#", "....#....", "....#....", "....#....", "....#....", "....#....", "....#....", "...###..."],
 'drill': ["#########", ".#######.", "..#####..", "..#+#+#..", "...###...", "...#+#...", "....#....", "....#....", "....#...."],
 'pump': ["....#....", "...###...", "..#####..", "....#....", ".#..#..#.", ".#.....#.", ".##...##.", "..#####..", "........."],
 'cycle': ["..#####..", ".#.....#.", "#......##", "#.....###", "#.......#", "###.....#", "##......#", ".#.....#.", "..#####.."],
 'copy': ["#####....", "#...#....", "#.#####..", "#.#...#..", "###.#.#..", "..#...#..", "..#####..", ".........", "+++++++++"],
 'scan': ["##.....##", "#.......#", "..+++++..", ".........", "#########", ".........", "..+++++..", "#.......#", "##.....##"],
 'matter': ["#.......#", "#.......#", "#.......#", "#.......#", "#..#.#..#", "#.##.##.#", "#########", ".........", "#########"],
 'ring': ["..#####..", ".#.....#.", "#..###..#", "#.#...#.#", "#.#.#.#.#", "#.#...#.#", "#..###..#", ".#.....#.", "..#####.."],
 'zap': ["#...#...#", ".#..#..#.", "..#.#.#..", "...###...", "#########", "...###...", "..#.#.#..", ".#..#..#.", "#...#...#"],
 'target': ["....#....", "..#####..", ".#..#..#.", ".#.....#.", "###.#.###", ".#.....#.", ".#..#..#.", "..#####..", "....#...."],
 'split': ["....#....", "....#....", "....#....", "...###...", "..#.#.#..", ".#..#..#.", "#...#...#", "#...#...#", "#...#...#"],
 'box': ["#########", "#.......#", "#.#####.#", "#.#...#.#", "#.#...#.#", "#.#...#.#", "#.#####.#", "#.......#", "#########"],
 'snow': ["....#....", ".#..#..#.", "..#.#.#..", "...###...", "#########", "...###...", "..#.#.#..", ".#..#..#.", "....#...."],
 'leaf': [".......##", ".....####", "...#####.", "..####.#.", ".####.#..", ".###.#...", "..#.#....", ".#.......", "#........"],
 'grid': ["#.#.#.#.#", ".........", "#.#####.#", "..#...#..", "#.#.#.#.#", "..#...#..", "#.#####.#", ".........", "#.#.#.#.#"],
 'shield': ["#########", "#.......#", "#..###..#", "#..###..#", "#.#####.#", ".#.###.#.", ".#..#..#.", "..#...#..", "...###..."],
 'helmet': ["..#####..", ".#######.", "#########", "##.....##", "##.###.##", "##.#.#.##", "#.......#", ".#.....#.", "........."],
 'flask': ["...###...", "....#....", "....#....", "...#.#...", "..#...#..", ".#+++++#.", "#+++++++#", "#+++++++#", "#########"],
 'tower': ["....#....", "...###...", "....#....", "...#.#...", "...#.#...", "..#...#..", "..#.#.#..", ".#.....#.", "#########"],
 'gauge': ["..#####..", ".#.....#.", "#.......#", "#..+...##", "#...+##.#", "#....#..#", "#.......#", ".#.....#.", "..#####.."],
 'antenna': ["#...#...#", ".#.....#.", "..#...#..", "....#....", "...###...", "....#....", "....#....", "...###...", "..#####.."],
 'xp': ["...###...", "..#####..", ".##+#+##.", ".###+###.", ".##+#+##.", "..#####..", "...###...", ".........", "........."],
 'engine': [".#.#.#.#.", "#########", "#.......#", "#.#####.#", "#.#+++#.#", "#.#####.#", "#.......#", "#########", ".##...##."],
 'hand': ["...#.#...", "..##.##..", "..##.##..", "..#####..", ".######..", ".#######.", ".######..", "..#####..", "...###..."],
 'hull': ["#########", "#+.....+#", "#.......#", "#.......#", "#.......#", "#.......#", "#.......#", "#+.....+#", "#########"],
 'tnt': ["....#.#..", ".....#...", "....#....", ".#######.", ".#######.", ".##+#+##.", ".#######.", ".#######.", "........."],
 'craft': ["###.###..", "#.#.#.#..", "###.###..", ".........", "###.###..", "#.#.#.##.", "###.###.#", "......###", ".......#."],
}

RULES = [  # (substring in name, glyph, category) - first match wins
    ('armor_showcase', 'helmet', 'field'), ('armory', 'shield', 'field'),
    ('nuke', 'tnt', 'def'), ('ic_tnt', 'tnt', 'def'), ('reactor', 'atom', 'nuke'), ('nuclear', 'atom', 'nuke'),
    ('laser', 'target', 'def'), ('tesla', 'zap', 'def'),
    ('replicator', 'copy', 'field'), ('scanner', 'scan', 'field'), ('matter', 'matter', 'field'), ('pattern', 'box', 'field'),
    ('teleporter', 'ring', 'field'), ('molecular', 'atom', 'field'), ('neutron', 'atom', 'field'), ('quantum_generator', 'atom', 'field'),
    ('unlimit', 'atom', 'field'), ('future', 'ring', 'field'), ('chunk_loader', 'grid', 'field'), ('large_fabricator', 'craft', 'field'),
    ('blast_furnace', 'blast', 'heat'), ('induction', 'coil', 'heat'), ('furnace', 'flame', 'heat'),
    ('steam_repressurizer', 'gauge', 'heat'), ('steam', 'gauge', 'heat'),
    ('coolant_injector', 'snow', 'nuke'), ('condenser', 'snow', 'grid'),
    ('powder', 'crush', 'proc'), ('compressor', 'compress', 'proc'), ('extractor', 'extract', 'proc'),
    ('metal_former', 'roller', 'proc'), ('extruding', 'roller', 'proc'), ('lathe', 'gear', 'proc'),
    ('block_cutter', 'blade', 'proc'), ('cutter', 'blade', 'proc'), ('canner', 'can', 'proc'), ('centrifuge', 'spin', 'proc'),
    ('electrolyzer', 'bubbles', 'proc'), ('fermenter', 'flask', 'heat'), ('washer', 'wave', 'proc'),
    ('magnetizer', 'magnet', 'proc'), ('recycler', 'cycle', 'proc'), ('sorter', 'split', 'proc'),
    ('distributor', 'split', 'proc'), ('regulator', 'gauge', 'proc'), ('buffer', 'box', 'proc'), ('barrel', 'flask', 'proc'),
    ('batch_crafter', 'craft', 'proc'), ('industrial_workbench', 'craft', 'proc'), ('oil_refinery', 'flask', 'oil'),
    ('advanced_miner', 'drill', 'proc'), ('miner', 'pick', 'proc'), ('pump', 'pump', 'proc'), ('harvest', 'leaf', 'proc'),
    ('terra', 'leaf', 'proc'), ('solar_distiller', 'sun', 'grid'), ('machine_hull', 'hull', 'neutral'),
    ('oil_rig', 'drill', 'oil'),
    ('manual_kinetic', 'hand', 'kin'), ('wind', 'blades', 'kin'), ('water_kinetic', 'wave', 'kin'), ('water', 'wave', 'gen'),
    ('stirling_kinetic', 'engine', 'kin'), ('kinetic_generator_elc', 'gear', 'kin'), ('kinetic', 'gear', 'kin'),
    ('solar', 'sun', 'gen'), ('geomagnetic', 'antenna', 'gen'), ('geo', 'lava', 'heat'), ('thermal', 'lava', 'heat'),
    ('heat', 'flame', 'heat'), ('rt_', 'atom', 'nuke'), ('semifluid', 'drop', 'oil'), ('diesel', 'drop', 'oil'),
    ('stirling', 'engine', 'gen'), ('experience', 'xp', 'gen'), ('drop_generator', 'drop', 'gen'),
    ('transformer', 'updown', 'grid'), ('energy_converter', 'updown', 'grid'), ('charger', 'bolt', 'grid'),
    ('wireless', 'antenna', 'grid'), ('energy_terminal', 'gauge', 'grid'), ('checker', 'scan', 'grid'),
    ('gesu', 'battery', 'grid'), ('esu', 'battery', 'grid'), ('bat_box', 'battery', 'grid'), ('mfe', 'battery', 'grid'),
    ('mfsu', 'battery', 'grid'), ('generator', 'bolt', 'gen'),
]

def classify(name, folder):
    for key, glyph, cat in RULES:
        if key in name: return glyph, cat
    return 'gear', {'generator': 'gen', 'kugenerator': 'kin', 'hugenerator': 'heat', 'reactor': 'nuke', 'wiring': 'grid', 'oilrig': 'oil'}.get(folder, 'proc')

def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))

def refine(im, glyph, color):
    im = im.convert('RGBA')
    W, H = im.size
    px = im.load()
    alpha = [[px[x, y][3] > 40 for x in range(W)] for y in range(H)]
    out = im.copy(); o = out.load()
    # 1) crisp dark contour around the silhouette
    for y in range(H):
        for x in range(W):
            if alpha[y][x]: continue
            if any(0 <= x + dx < W and 0 <= y + dy < H and alpha[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                o[x, y] = (22, 28, 36, 255)
    # 2) holographic accent on the top front edges: the first opaque pixel from the top in each
    #    column traces the cube's upper outline; light it in the category colour (brightest at the apex)
    tops = []
    for x in range(W):
        for y in range(H):
            if alpha[y][x]: tops.append((x, y)); break
    if tops:
        ymin = min(t[1] for t in tops)
        for x, y in tops:
            if y - ymin > 9: continue
            k = 1 - (y - ymin) / 12
            r, g, b, a = o[x, y]
            o[x, y] = mix((r, g, b), color, 0.55 * k + 0.25) + (255,)
    if not BADGE:
        return out
    # 3) the badge: dark holo disc with a glowing ring and the pictogram, lower-right corner
    cx, cy, R = W - 7.0, H - 7.0, 6.6
    for y in range(H - 15, H):
        for x in range(W - 15, W):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= R + 1.4:
                if d > R + 0.4:                       # outer contour
                    o[x, y] = (12, 16, 22, 255)
                elif d > R - 0.8:                     # glowing ring
                    o[x, y] = mix(color, (255, 255, 255), 0.25) + (255,)
                else:                                 # dark disc with a soft inner glow towards the ring
                    t = max(0.0, (d - (R - 4)) / 4)
                    o[x, y] = mix((14, 26, 38), color, 0.28 * t) + (255,)
            elif d <= R + 2.4:                        # faint outer glow onto the machine
                r, g, b, a = o[x, y]
                if a > 0: o[x, y] = mix((r, g, b), color, 0.35) + (a,)
    rows = G[glyph]
    gx0, gy0 = int(cx - 4.5 + 0.5), int(cy - 4.5 + 0.5)
    lit = mix(color, (255, 255, 255), 0.72)
    dim = mix(color, (14, 26, 38), 0.25)
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch == '#': o[gx0 + i, gy0 + j] = lit + (255,)
            elif ch == '+': o[gx0 + i, gy0 + j] = dim + (255,)
    return out

results = []
for folder in sorted(os.listdir(SRC)):
    sd = os.path.join(SRC, folder)
    if not os.path.isdir(sd): continue
    os.makedirs(os.path.join(OUT, folder), exist_ok=True)
    for f in sorted(os.listdir(sd)):
        if not f.endswith('.png'): continue
        name = f[:-4]
        glyph, cat = classify(name, folder)
        im = refine(Image.open(os.path.join(sd, f)), glyph, COL[cat])
        im.save(os.path.join(OUT, folder, f))
        results.append((folder, name, glyph, cat, im))
print(len(results), 'icons refined')
if MONTAGE:
    cols = 16; rows = (len(results) + cols - 1) // cols
    m = Image.new('RGBA', (cols * 72, rows * 72), (139, 139, 139, 255))
    for i, r in enumerate(results):
        big = r[4].resize((64, 64), Image.NEAREST)
        m.paste(big, ((i % cols) * 72 + 4, (i // cols) * 72 + 4), big)
    m.save(MONTAGE)
