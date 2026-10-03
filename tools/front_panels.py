#!/usr/bin/env python3
"""
Front-panel design language for the refined classic machines (0.1.7.24, Phase 1 "A").

Every refined machine texture (refine_models.py: 128x64, the north / front cell at 0..31)
gets a redesigned front panel inside its raised plate, in one shared layout:

    +--------------------------+   header bar: dark label strip, category accent on the left
    | ===                  . . |
    |  +--------------------+  |   function window: recessed, bevelled, category-tinted glass,
    |  |   machine-specific  |  |   with the mechanism that tells what the machine does
    |  |   mechanism         |  |   (piston slot, crusher teeth, nozzle + drop, rotor window,
    |  +--------------------+  |   grill + flame, coil, rollers, saw, magnet, ...)
    |   ==   ==   ==           |   vent slots
    +--------------------------+

Window colour = machine category (blue processing, orange heat, yellow generation, teal
kinetic, green nuclear, cyan grid / storage, violet matter / field, red defense, amber oil).
Running variants ("_on") light the glass and the accents. The IC2 plate, bevels, frame band,
sides and top are untouched, so the classic silhouette stays.

The status slit (bottom frame band) and the category LED (top corner) are separate model
elements (dsp_models.py) - this script only paints texture.

Usage: front_panels.py <assets/mio_icif> <backup_dir>      (backup = pristine refined textures,
       created on first run; every run paints from the backup, so it is repeatable)
"""
import os, sys, json, glob, shutil, math
from PIL import Image

A, BACKUP = sys.argv[1], sys.argv[2]
REF = os.path.join(A, 'textures', 'block', 'refined')
if not os.path.isdir(BACKUP):
    shutil.copytree(REF, BACKUP)

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
src = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'dsp_icons.py')).read()
ns = {}
exec(src[src.index('COL = {'):src.index('# 9x9 pictograms')], ns)
exec(src[src.index('RULES = ['):src.index('def mix(')], ns)
COL, classify = ns['COL'], ns['classify']

# mechanism per function (first match wins); falls back to the category's default
MECH = [
    ('compressor', 'piston'), ('powder', 'crusher'), ('extractor', 'nozzle'), ('centrifuge', 'rotor'),
    ('induction', 'coil'), ('blast_furnace', 'arch'), ('furnace', 'grill'), ('canner', 'can'),
    ('electrolyzer', 'electrodes'), ('washer', 'waves'), ('magnetizer', 'magnet'), ('recycler', 'cycle'),
    ('metal_former', 'rollers'), ('extruding', 'rollers'), ('lathe', 'rollers'), ('cutter', 'saw'),
    ('pump', 'pipe'), ('advanced_miner', 'drill'), ('miner', 'drill'), ('scanner', 'scan'),
    ('replicator', 'chamber'), ('matter', 'chamber'), ('molecular', 'chamber'), ('neutron', 'chamber'),
    ('teleporter', 'ring'), ('future', 'ring'), ('chunk_loader', 'grid'), ('sorter', 'split'),
    ('distributor', 'split'), ('regulator', 'gauge'), ('buffer', 'grid'), ('batch_crafter', 'grid'),
    ('industrial_workbench', 'grid'), ('large_fabricator', 'grid'), ('fermenter', 'tank'), ('oil_refinery', 'tank'),
    ('condenser', 'fins'), ('coolant', 'fins'), ('solar_distiller', 'tank'), ('harvest', 'rollers'), ('terra', 'ring'),
    ('steam', 'gauge'), ('laser', 'lens'), ('armory', 'grid'), ('pattern', 'scan'),
    ('wind', 'fan'), ('water', 'waves'), ('manual_kinetic', 'rollers'), ('stirling', 'pistons2'),
    ('kinetic', 'rotor'), ('solar', 'cells'), ('geomagnetic', 'ring'), ('geo', 'lava'), ('thermal', 'lava'),
    ('heat', 'grill'), ('rt_', 'atom'), ('nuclear', 'atom'), ('reactor', 'atom'), ('semifluid', 'tank'), ('diesel', 'tank'),
    ('experience', 'chamber'), ('drop_generator', 'chamber'), ('quantum', 'ring'), ('unlimit', 'ring'),
    ('transformer', 'updown'), ('energy_converter', 'updown'), ('charger', 'bars'), ('terminal', 'gauge'),
    ('wireless', 'ring'), ('checker', 'scan'), ('esu', 'bars'), ('bat_box', 'bars'), ('mfe', 'bars'), ('mfsu', 'bars'),
    ('generator', 'coil'), ('oil_rig', 'drill'),
]
SKIP = ('block_tesla', 'machine_hull', 'block_barrel', 'armor_showcase', 'large_fabricator_tank', 'large_fabricator_input',
        'large_fabricator_scrap', 'reactor_vessel', 'reactor_chamber', 'oil_rig_base', 'ic_tnt', 'reactor_nuke')


def mech_for(name):
    for key, m in MECH:
        if key in name: return m
    return 'gauge'


def clamp(v): return max(0, min(255, int(round(v))))
def mix(a, b, t): return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))
def shade(c, f): return tuple(clamp(v * f) for v in c[:3])


class Painter:
    def __init__(self, img, x0, y0, cat, lit):
        self.p = img.load(); self.x0 = x0; self.y0 = y0; self.lit = lit
        c = COL[cat]
        self.cat = c
        self.glass_top = mix((14, 20, 28), c, 0.30 if lit else 0.16)
        self.glass_bot = mix((22, 30, 40), c, 0.48 if lit else 0.26)
        self.acc = mix(c, (255, 255, 255), 0.35) if lit else mix(c, (30, 36, 44), 0.30)
        self.acc_dim = mix(c, (20, 26, 34), 0.45 if lit else 0.65)
        self.M, self.MD, self.MH, self.MK = (170, 177, 184), (104, 110, 118), (224, 229, 234), (58, 63, 70)

    def px(self, x, y, c):
        if 0 <= x < 32 and 0 <= y < 32: self.p[self.x0 + x, self.y0 + y] = tuple(c[:3]) + (255,)

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1): self.px(x, y, c)

    def hline(self, x0, x1, y, c): self.rect(x0, y, x1, y, c)
    def vline(self, x, y0, y1, c): self.rect(x, y0, x, y1, c)

    def disk(self, cx, cy, r, c, ring=None):
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d <= r: self.px(x, y, c if ring is None or d < r - 1 else ring)

    def metal_block(self, x0, y0, x1, y1):
        self.rect(x0, y0, x1, y1, self.M)
        self.hline(x0, x1, y0, self.MH); self.vline(x0, y0, y1, self.MH)
        self.hline(x0, x1, y1, self.MD); self.vline(x1, y0, y1, self.MD)


# ------------------------------------------------------------------ mechanisms (window interior 16x12 at (8,11))
def m_piston(P):
    P.rect(8, 15, 23, 18, P.MK)                                  # horizontal press slot
    P.hline(8, 23, 15, P.acc_dim)
    P.metal_block(8, 13, 11, 20); P.metal_block(20, 13, 23, 20)  # two piston heads
    P.rect(12, 16, 13, 17, P.MD); P.rect(18, 16, 19, 17, P.MD)   # rods
    P.rect(14, 16, 17, 17, P.acc)                                # material being pressed (glow)

def m_crusher(P):
    for i in range(8):                                           # upper and lower jaw teeth
        x = 8 + i * 2
        P.rect(x, 12, x + 1, 13, P.M); P.px(x, 14, P.M); P.px(x + 1, 12, P.MH)
        P.rect(x, 20, x + 1, 21, P.M); P.px(x + 1, 19, P.M); P.px(x, 21, P.MD)
    P.hline(8, 23, 11, P.MD); P.hline(8, 23, 22, P.MD)
    for x in range(9, 23, 3): P.px(x, 16 + (x % 2), P.acc)       # grit
    P.hline(10, 21, 17, P.acc_dim)

def m_nozzle(P):
    P.metal_block(12, 11, 19, 13); P.rect(14, 14, 17, 15, P.M); P.rect(15, 16, 16, 16, P.MD)
    for (x, y) in ((15, 18), (14, 19), (15, 19), (16, 19), (14, 20), (15, 20), (16, 20), (17, 20), (15, 21), (16, 21)):
        P.px(x, y, P.acc)                                        # the drop
    P.px(15, 19, mix(P.acc, (255, 255, 255), 0.5))
    P.hline(9, 22, 22, P.acc_dim)

def m_rotor(P):
    P.disk(15.9, 16.5, 6.0, P.glass_bot, ring=P.MD)
    for k in range(3):
        a = k * 2.094 + 0.4
        for r in range(1, 6):
            P.px(int(15.9 + math.cos(a) * r), int(16.5 + math.sin(a) * r), P.M)
            P.px(int(15.9 + math.cos(a + 0.35) * r), int(16.5 + math.sin(a + 0.35) * r), P.MH if r < 3 else P.M)
    P.disk(15.9, 16.5, 1.6, P.acc)

def m_coil(P):
    P.rect(10, 12, 21, 21, P.glass_bot)
    for y in range(12, 22, 2):
        P.hline(10, 21, y, mix(P.acc, (255, 140, 40), 0.35)); P.px(10, y, P.MD); P.px(21, y, P.MD)
    P.rect(14, 11, 17, 22, P.MK); P.vline(15, 11, 22, P.MD)        # core

def m_arch(P):
    for x in range(9, 23):
        h = int(4.5 * math.sqrt(max(0, 1 - ((x - 15.5) / 7.5) ** 2)))
        P.vline(x, 18 - h, 22, P.MK)
        P.px(x, 18 - h, P.M)
    m_flame(P, 15, 21)

def m_flame(P, cx, base):
    hot = mix(P.acc, (255, 230, 120), 0.4)
    for dy, w in ((0, 4), (1, 4), (2, 3), (3, 3), (4, 2), (5, 1)):
        P.hline(cx - w + 1, cx + w, base - dy, P.acc if dy < 4 else hot)
    P.hline(cx, cx + 1, base - 1, hot); P.hline(cx, cx + 1, base - 2, hot)

def m_grill(P):
    P.rect(8, 11, 23, 22, mix(P.glass_bot, P.acc, 0.35))
    m_flame(P, 15, 21)
    for x in range(9, 23, 3): P.vline(x, 11, 22, P.MD)           # grill bars in front of the fire
    P.hline(8, 23, 13, P.MD)

def m_can(P):
    P.rect(12, 12, 19, 22, P.M); P.vline(12, 12, 22, P.MH); P.vline(19, 12, 22, P.MD)
    P.hline(12, 19, 12, P.MH); P.hline(12, 19, 22, P.MD)
    P.rect(13, 15, 18, 19, P.acc); P.hline(13, 18, 15, mix(P.acc, (255, 255, 255), 0.3))

def m_electrodes(P):
    P.rect(8, 16, 23, 22, P.acc_dim)
    P.rect(11, 11, 12, 21, P.M); P.rect(19, 11, 20, 21, P.M)
    for (x, y) in ((14, 20), (16, 18), (15, 15), (17, 21), (14, 17), (17, 13)): P.px(x, y, P.acc)

def m_waves(P):
    for row, y in enumerate((14, 18)):
        for x in range(8, 24):
            P.px(x, y + int(round(math.sin((x + row * 2) * 0.8))), P.acc)
    P.rect(8, 20, 23, 22, P.acc_dim)

def m_magnet(P):
    for x in range(10, 22):
        for y in range(11, 21):
            d = math.hypot(x + 0.5 - 15.95, y + 0.5 - 14.5)
            if 3.0 <= d <= 6.0 and y <= 15: P.px(x, y, P.M)
    P.rect(10, 15, 12, 20, P.M); P.rect(19, 15, 21, 20, P.M)
    P.rect(10, 19, 12, 20, P.acc); P.rect(19, 19, 21, 20, mix(P.acc, (230, 80, 80), 0.6))
    for x in range(13, 19, 2): P.px(x, 21, P.acc_dim)

def m_cycle(P):
    for i in range(28):
        a = i / 28 * math.pi * 2
        if 0.3 < a % math.pi < 2.8:
            P.px(int(15.9 + math.cos(a) * 5.2), int(16.5 + math.sin(a) * 5.2), P.acc)
    for (x, y) in ((20, 13), (21, 14), (19, 14), (11, 19), (10, 18), (12, 18)): P.px(x, y, P.acc)
    P.disk(15.9, 16.5, 1.4, P.MD)

def m_rollers(P):
    for cy in (14, 19):
        P.disk(15.9, cy + 0.5, 2.7, P.M, ring=P.MD)
        P.hline(9, 22, cy, P.MD)
    P.hline(8, 23, 17, P.acc)

def m_saw(P):
    P.disk(15.9, 16.5, 5.2, P.M, ring=P.MD)
    for i in range(12):
        a = i / 12 * math.pi * 2
        P.px(int(15.9 + math.cos(a) * 6.3), int(16.5 + math.sin(a) * 6.3), P.MH)
    P.disk(15.9, 16.5, 1.5, P.acc)
    P.hline(8, 23, 22, P.acc_dim)

def m_pipe(P):
    P.rect(14, 11, 17, 22, P.M); P.vline(14, 11, 22, P.MH); P.vline(17, 11, 22, P.MD)
    for i, y in enumerate(range(13, 22, 3)):
        P.hline(15, 16, y, P.acc); P.px(15, y + 1, P.acc_dim); P.px(16, y + 1, P.acc_dim)
    P.rect(9, 20, 12, 22, P.acc_dim); P.rect(19, 20, 22, 22, P.acc_dim)

def m_drill(P):
    for dy in range(10):
        w = max(0, 4 - dy // 2)
        P.hline(15 - w, 16 + w, 12 + dy, P.M if dy % 2 == 0 else P.MH)
    P.rect(13, 11, 18, 11, P.MD)
    P.hline(8, 23, 22, P.acc)

def m_scan(P):
    P.rect(8, 11, 23, 22, P.glass_bot)
    for y in range(12, 22, 2): P.hline(9, 22, y, P.acc_dim)
    P.hline(9, 22, 16, P.acc); P.hline(9, 22, 17, mix(P.acc, (255, 255, 255), 0.3))
    for (x, y) in ((9, 11), (22, 11), (9, 22), (22, 22)): P.px(x, y, P.MH)

def m_chamber(P):
    P.rect(10, 11, 21, 22, mix(P.glass_bot, P.acc, 0.25))
    P.vline(10, 11, 22, P.MD); P.vline(21, 11, 22, P.MD)
    P.disk(15.9, 16.5, 2.5, P.acc)
    for (x, y) in ((15, 12), (16, 21), (12, 16), (19, 17)): P.px(x, y, mix(P.acc, (255, 255, 255), 0.5))

def m_ring(P):
    P.disk(15.9, 16.5, 5.5, P.glass_bot, ring=P.acc)
    P.disk(15.9, 16.5, 3.2, mix(P.glass_bot, P.acc, 0.4), ring=P.acc_dim)
    P.disk(15.9, 16.5, 1.0, mix(P.acc, (255, 255, 255), 0.4))

def m_grid(P):
    for y in range(12, 22, 3):
        for x in range(9, 23, 3):
            P.rect(x, y, x + 1, y + 1, P.acc if (x + y) % 2 else P.acc_dim)

def m_split(P):
    P.vline(15, 11, 15, P.acc); P.vline(16, 11, 15, P.acc)
    for i in range(6):
        P.px(15 - i, 16 + i, P.acc); P.px(16 + i, 16 + i, P.acc); P.px(15, 16 + i, P.acc_dim)

def m_gauge(P):
    P.disk(15.9, 18.0, 5.5, mix(P.glass_bot, (200, 210, 220), 0.15), ring=P.MD)
    for i in range(7):
        a = math.pi + i / 6 * math.pi
        P.px(int(15.9 + math.cos(a) * 4.5), int(18 + math.sin(a) * 4.5), P.MH)
    for r in range(4): P.px(int(15.9 + r * 0.8), int(18 - r * 0.7), P.acc)
    P.rect(9, 21, 22, 22, P.acc_dim)

def m_tank(P):
    P.rect(11, 11, 20, 22, P.MK); P.rect(12, 12, 19, 21, P.glass_top)
    P.rect(12, 16, 19, 21, P.acc); P.hline(12, 19, 16, mix(P.acc, (255, 255, 255), 0.35))
    for y in (13, 18): P.hline(10, 21, y, P.MD)

def m_fins(P):
    for x in range(9, 23, 2): P.vline(x, 11, 22, P.M); P.px(x, 11, P.MH)
    P.hline(8, 23, 16, P.acc)

def m_lens(P):
    P.disk(15.9, 16.5, 5.0, P.MK, ring=P.M)
    P.disk(15.9, 16.5, 3.0, P.acc_dim)
    P.disk(15.9, 16.5, 1.5, P.acc); P.px(14, 15, (255, 255, 255))

def m_fan(P):
    P.disk(15.9, 16.5, 6.0, P.glass_bot, ring=P.MD)
    for k in range(4):
        a = k * math.pi / 2 + 0.6
        for r in range(1, 6):
            for w in (-0.25, 0, 0.25):
                P.px(int(15.9 + math.cos(a + w + r * 0.08) * r), int(16.5 + math.sin(a + w + r * 0.08) * r), P.M)
    P.disk(15.9, 16.5, 1.3, P.acc)

def m_pistons2(P):
    for x in (10, 18):
        P.rect(x, 11, x + 3, 22, P.MK); P.metal_block(x, 13 if x == 10 else 17, x + 3, 16 if x == 10 else 20)
    P.hline(8, 23, 22, P.acc)

def m_cells(P):
    for y in (12, 17):
        for x in (9, 16):
            P.rect(x, y, x + 5, y + 3, P.acc_dim); P.hline(x, x + 5, y, P.acc)

def m_lava(P):
    P.rect(8, 11, 23, 22, P.MK)
    for x in range(8, 24):
        top = 15 + int(round(math.sin(x * 0.9) * 1.2))
        P.vline(x, top, 22, P.acc)
        P.px(x, top, mix(P.acc, (255, 240, 150), 0.5))

def m_atom(P):
    P.disk(15.9, 16.5, 5.6, P.glass_bot, ring=P.acc_dim)
    for i in range(30):
        a = i / 30 * math.pi * 2
        P.px(int(15.9 + math.cos(a) * 5.0), int(16.5 + math.sin(a) * 2.0), P.acc)
        P.px(int(15.9 + math.cos(a) * 2.0), int(16.5 + math.sin(a) * 5.0), P.acc)
    P.disk(15.9, 16.5, 1.2, mix(P.acc, (255, 255, 255), 0.5))

def m_updown(P):
    for i in range(4):
        P.hline(12 - i, 12 + i, 12 + i, P.acc)                 # up arrow (left)
        P.hline(19 - i, 19 + i, 21 - i, P.acc_dim)             # down arrow (right)
    P.vline(12, 15, 21, P.acc); P.vline(19, 12, 18, P.acc_dim)

def m_bars(P):
    for i, x in enumerate((10, 14, 18)):
        P.rect(x, 11, x + 2, 22, P.MK)
        P.rect(x, 22 - (3 + i * 3), x + 2, 22, P.acc)
        P.hline(x, x + 2, 22 - (3 + i * 3), mix(P.acc, (255, 255, 255), 0.4))


MECHS = {k[2:]: v for k, v in globals().items() if k.startswith('m_')}


def clear_panel(img):
    """Repaints the raised panel interior (texels 4..27) as clean plate in the machine's own casing
    tint (most common interior colour), so no stray pixels of the old front survive around the new
    layout. The frame band and the panel bevel stay as refine_models.py drew them."""
    from collections import Counter
    px = img.load()
    # refine_models.py drew the panel's top / left bevel as plate x 1.10: recover the plate colour from it
    cnt = Counter([px[i, 3][:3] for i in range(4, 28)] + [px[3, i][:3] for i in range(4, 28)])
    base = tuple(clamp(c / 1.10) for c in cnt.most_common(1)[0][0])
    for y in range(4, 28):
        for x in range(4, 28):
            t = y / 31
            k = (1.04 - 0.08 * t) / (1.04 - 0.08 * 0.5)
            n = ((x * 7919 + y * 104729) % 5 - 2) * 0.006
            px[x, y] = tuple(clamp(c * (k + n)) for c in base) + (255,)
    return base


# ------------------------------------------------------------------ physical fronts (0.1.7.25)
# Machines whose front IS a physical interface get no display window:
#   storage (BatBox ... MFSU, LESU, EESU, GESU ports): the high-voltage OUTPUT socket,
#   kinetic machines: the shaft coupling, heat machines: the copper heat interface.
def front_style(name, folder):
    if folder == 'wiring' and any(k in name for k in ('bat_box', 'cesu', 'mfe', 'mfsu', 'lesu', 'eesu', 'gesu')): return 'socket'
    if folder == 'kugenerator' or 'kinetic_generator' in name: return 'shaft'
    if folder == 'hugenerator' or name in ('block_stirling_generator', 'block_advanced_stirling_generator'): return 'heatport'
    return None


def bolts(P, pts, plate):
    for (x, y) in pts:
        P.px(x, y, shade(plate, 0.62)); P.px(x + 1, y, shade(plate, 0.80)); P.px(x, y + 1, shade(plate, 0.80)); P.px(x + 1, y + 1, shade(plate, 1.12))


def paint_socket(P, plate, lit, cat):
    """Heavy-duty HV output terminal: armoured collar, rubber gasket, copper contact block with
    three insulated pins, a hazard band and a rating plate."""
    # hazard band (top)
    for x in range(6, 26):
        P.rect(x, 5, x, 7, (226, 178, 40) if ((x + 0) // 2) % 2 == 0 else (40, 40, 44))
    P.hline(6, 25, 8, shade(plate, 0.78))
    # armoured collar (square, chamfered) with a round socket
    P.rect(8, 10, 23, 25, shade(plate, 0.70))
    P.hline(8, 23, 10, shade(plate, 1.12)); P.vline(8, 10, 25, shade(plate, 1.08))
    P.hline(8, 23, 25, shade(plate, 0.52)); P.vline(23, 10, 25, shade(plate, 0.56))
    for (x, y) in ((8, 10), (23, 10), (8, 25), (23, 25)): P.px(x, y, shade(plate, 0.9))
    P.disk(15.9, 17.9, 6.6, (28, 30, 34))                                   # rubber gasket
    P.disk(15.9, 17.9, 5.4, (150, 156, 162), ring=(96, 100, 106))           # steel ring
    copper = (214, 120, 52) if not lit else (240, 150, 70)
    P.disk(15.9, 17.9, 4.2, copper, ring=(150, 78, 34))                     # copper contact block
    for (x, y) in ((15, 15), (13, 19), (18, 19)):                           # three insulated pins
        P.rect(x, y, x + 1, y + 1, (30, 32, 36)); P.px(x, y, (70, 74, 80))
    P.px(14, 16, mix(copper, (255, 255, 255), 0.5))                          # contact glint
    bolts(P, ((9, 11), (21, 11), (9, 23), (21, 23)), plate)
    # rating plate under the collar
    P.rect(11, 26, 20, 27, (52, 57, 64)); P.hline(12, 12 + (3 if not lit else 7), 26, P.acc)


def paint_shaft(P, plate, lit, cat):
    """Kinetic coupling: toothed flange, bearing ring, hex shaft end, keyway; no screen."""
    P.rect(6, 6, 25, 26, shade(plate, 0.80))
    P.hline(6, 25, 6, shade(plate, 1.10)); P.vline(6, 6, 26, shade(plate, 1.06))
    P.hline(6, 25, 26, shade(plate, 0.58)); P.vline(25, 6, 26, shade(plate, 0.60))
    cx, cy = 15.9, 16.1
    for i in range(16):                                                      # gear teeth around the flange
        a = i / 16 * math.pi * 2
        for r in (8.3, 8.9):
            P.px(int(cx + math.cos(a) * r), int(cy + math.sin(a) * r), (126, 132, 140))
    P.disk(cx, cy, 7.8, (150, 156, 164), ring=(92, 98, 106))                 # flange
    for i in range(6):                                                       # flange bolts
        a = i / 6 * math.pi * 2 + 0.5
        bx, by = int(cx + math.cos(a) * 5.9), int(cy + math.sin(a) * 5.9)
        P.px(bx, by, (64, 68, 74)); P.px(bx + 1, by + 1, (196, 200, 206))
    P.disk(cx, cy, 4.2, (54, 58, 64), ring=(34, 36, 40))                    # bearing housing
    for (dx, dy) in ((-2, -1), (-1, -2), (0, -2), (1, -2), (2, -1), (2, 0), (2, 1), (1, 2), (0, 2), (-1, 2), (-2, 1), (-2, 0)):
        P.px(int(cx) + dx, int(cy) + dy, (188, 194, 200))                   # hex shaft end
    P.rect(int(cx) - 1, int(cy) - 1, int(cx) + 1, int(cy) + 1, (150, 156, 164))
    P.px(int(cx), int(cy) - 2, (30, 32, 36))                                # keyway
    if lit:
        for i in range(4):
            a = i / 4 * math.pi * 2 + 0.3
            P.px(int(cx + math.cos(a) * 3.2), int(cy + math.sin(a) * 3.2), P.acc)
    bolts(P, ((7, 7), (23, 7), (7, 24), (23, 24)), plate)


def paint_heatport(P, plate, lit, cat):
    """Copper heat interface: a copper pad with radial fins and a central hot core; no screen."""
    cu_hi, cu, cu_lo = (236, 150, 82), (200, 110, 50), (140, 70, 30)
    P.rect(7, 7, 24, 24, (60, 64, 70))
    P.hline(7, 24, 7, (44, 48, 54)); P.vline(7, 7, 24, (44, 48, 54)); P.hline(7, 24, 24, shade(plate, 1.10))
    P.rect(8, 8, 23, 23, cu)
    for i in range(8, 24, 3):                                               # machined grooves
        P.hline(8, 23, i, cu_lo); P.hline(8, 23, i + 1, cu_hi)
    P.disk(15.9, 15.9, 4.6, cu_lo, ring=(110, 52, 22))
    core = mix((255, 120, 40), (255, 230, 150), 0.5) if lit else (190, 80, 36)
    P.disk(15.9, 15.9, 3.0, core)
    if lit: P.disk(15.9, 15.9, 1.4, (255, 240, 190))
    for (x, y) in ((9, 9), (22, 9), (9, 22), (22, 22)): P.px(x, y, (90, 44, 18))
    # heat-flow chevrons below the pad
    for i, x0 in enumerate((10, 15, 20)):
        P.px(x0, 26, P.acc); P.px(x0 + 1, 27, P.acc); P.px(x0 - 1, 27, P.acc)
    bolts(P, ((6, 5), (24, 5)), plate)


def paint_front(img, name, folder, lit):
    _, cat = classify(name, folder)
    clear_panel(img)
    P = Painter(img, 0, 0, cat, lit)
    plate = img.getpixel((16, 4))[:3]
    style = front_style(name, folder)
    if style:
        {'socket': paint_socket, 'shaft': paint_shaft, 'heatport': paint_heatport}[style](P, plate, lit, cat)
        return cat
    # header bar
    P.rect(6, 5, 25, 7, (64, 70, 78)); P.hline(6, 25, 5, (52, 57, 64)); P.hline(6, 25, 8, shade(plate, 1.06))
    P.rect(6, 5, 8, 7, P.acc); P.px(23, 6, (150, 156, 164)); P.px(25, 6, (150, 156, 164))
    # recessed window frame
    P.rect(6, 9, 25, 24, (46, 51, 58))
    P.hline(6, 25, 9, (36, 40, 46)); P.vline(6, 9, 24, (36, 40, 46))
    P.hline(6, 25, 25, shade(plate, 1.10)); P.vline(26, 9, 25, shade(plate, 1.06))
    for y in range(10, 24):                                  # glass gradient
        P.hline(7, 24, y, mix(P.glass_top, P.glass_bot, (y - 10) / 13))
    P.hline(7, 24, 10, mix(P.glass_top, (255, 255, 255), 0.08))
    MECHS[mech_for(name)](P)
    # glass glint
    P.px(23, 11, mix(P.glass_top, (255, 255, 255), 0.45)); P.px(22, 11, mix(P.glass_top, (255, 255, 255), 0.25))
    # vents
    for x0 in (8, 14, 20):
        P.hline(x0, x0 + 3, 26, (58, 63, 70)); P.hline(x0, x0 + 3, 27, shade(plate, 1.08))
    return cat


done = 0
for path in sorted(glob.glob(os.path.join(BACKUP, '**', '*.png'), recursive=True)):
    rel = os.path.relpath(path, BACKUP)
    folder, fname = rel.split(os.sep)[0], os.path.basename(path)[:-4]
    name = fname[:-3] if fname.endswith('_on') else fname
    if any(s in name for s in SKIP): continue
    model = os.path.join(A, 'models', 'block', rel[:-4] + '.json')
    if not os.path.exists(model): continue
    d = json.load(open(model, encoding='utf-8-sig'))
    if not any(e.get('name') == 'panels_ns' for e in d.get('elements', [])): continue
    img = Image.open(path).convert('RGBA')
    paint_front(img, name, folder, fname.endswith('_on'))
    img.save(os.path.join(REF, rel))
    done += 1
print(done, 'fronts painted')
