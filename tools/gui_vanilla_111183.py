#!/usr/bin/env python3
"""
0.1.11.183: recolours the GUI textures of the "clean modern IC2" theme (tools/si_gui2.py, modern_gui.py) to the vanilla
container look that the code-drawn screens use (VanillaFrame): panel #C6C6C6, black outline with a 2-px white upper-left
and #555555 lower-right bevel, slots #8B8B8B with a #373737 upper-left and white lower-right edge, no groove between the
machine and the inventory. Every pixel keeps its place, so the screens' overlays (fills, arrows, buttons) still fit.

  python tools/gui_vanilla_111183.py <backup dir> [--preview out.png] [--write]

Only the theme's own colours change: its panel grain, outline, bevels, groove, slot and well colours by an exact table,
the other blue-tinted greys (icons, arrows) are made neutral and a little darker; coloured pixels and pure greys (art that
was never in the theme) stay. Without --write nothing is written (a preview only). The originals are copied to the backup
directory first.
"""
from __future__ import annotations
import shutil, sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
GUI = ROOT / 'src/main/resources/assets/mio_icif/textures/gui'
# the textures the screens blit (gui_ncl_rct_fluid, the nuclear reactor's, keeps its own look)
NAMES = """bat_box default blast_furnace wine_barrel weighted_item_distributor weighted_fluid_distributor transformer toolbox
tool_scanner sorter_elc solid_heat_generator solar_destiller scanner_elc replicator_elc reactor_nuke reactor_fluid_port
pattern_storage oil_refinery_elc miner_elc matter_elc matron_elc magnetizer laser_tower kinetic_generator_elc item_buffer
industrial_workbench heat_source_fluid heat_generator_elc harvest_elc generic fluid_regulator_elc fluid_heat_generator
fluid_distributor_elc fermenter_elc electrolyzer_elc chunk_loader batch_crafter armory adv_blast_furnace_elc""".split()

# the theme's palette (si_gui2.py) and its vanilla counterpart
OUTLINE, EDGE_HI, EDGE_LO = (88, 93, 99), (248, 249, 249), (170, 174, 179)
GROOVE_LO, GROOVE_HI = (164, 168, 173), (245, 246, 247)
SLOT_FILL, SLOT_FILL_IN, SLOT_LO, SLOT_HI = (168, 172, 177), (156, 160, 165), (122, 127, 133), (247, 248, 249)
SLOT_CORNER = (184, 188, 191)
WELL_FILL, WELL_GRID = (238, 240, 241), (229, 231, 233)
SHADOW_2 = (109, 114, 120)   # the 2-px panel shadow of the older restyle (modern_gui.py)

V_OUTLINE, V_LIGHT, V_SHADOW, V_PANEL = (0, 0, 0), (255, 255, 255), (85, 85, 85), (198, 198, 198)
V_SLOT, V_SLOT_DARK = (139, 139, 139), (55, 55, 55)
V_PAPER, V_PAPER_GRID = (244, 246, 244), (226, 230, 226)   # the paper of the readout and chart windows

EXACT = {OUTLINE: V_OUTLINE, EDGE_HI: V_LIGHT, (248, 249, 248): V_LIGHT, EDGE_LO: V_SHADOW, SHADOW_2: V_SHADOW,
         SLOT_FILL: V_SLOT, SLOT_FILL_IN: V_SLOT, SLOT_LO: V_SLOT_DARK, SLOT_HI: V_LIGHT, SLOT_CORNER: V_SLOT,
         WELL_FILL: V_PAPER, WELL_GRID: V_PAPER_GRID, (228, 230, 232): V_PAPER_GRID}


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def tinted(c):
    """A grey of the theme: blue-tinted (or almost neutral) but not a pure grey."""
    r, g, b = c
    return not (r == g == b) and max(c) - min(c) <= 14 and b >= r - 1


def panel_grain(c):
    return tinted(c) and max(c) - min(c) <= 6 and 0xCF <= c[0] <= 0xE3


def convert(img: Image.Image) -> Image.Image:
    src = img.convert('RGBA')
    sp = src.load()
    out = src.copy()
    op = out.load()
    w, h = src.size

    def rgb(x, y):
        p = sp[x, y]
        return p[:3] if 0 <= x < w and 0 <= y < h and p[3] else None

    for y in range(h):
        for x in range(w):
            p = sp[x, y]
            if p[3] == 0:
                continue
            c = p[:3]
            if c == GROOVE_LO and rgb(x, y + 1) == GROOVE_HI or c == GROOVE_HI and rgb(x, y - 1) == GROOVE_LO:
                n = V_PANEL                                   # the groove between machine and inventory goes
            elif c == OUTLINE:   # the panel's outline (next to the transparent outside) black, an outline inside #373737
                edge = any(not (0 <= x + dx < w and 0 <= y + dy < h) or sp[x + dx, y + dy][3] == 0
                           for dx in (-1, 0, 1) for dy in (-1, 0, 1))
                n = V_OUTLINE if edge else V_SLOT_DARK
            elif c in EXACT:
                n = EXACT[c]
            elif panel_grain(c):
                n = V_PANEL
            elif tinted(c):
                v = lum(c)
                v = v - 0x18 if v > 0x90 else v * 0.85        # icons and arrows: neutral, a shade darker like the panel
                n = (max(0, min(255, round(v))),) * 3
            else:
                continue
            op[x, y] = n + (p[3],)
    # the vanilla bevel is 2 px wide: thicken the 1-px highlight and shadow inwards over the panel
    for y in range(h):
        for x in range(w):
            c = rgb(x, y)
            if c == EDGE_HI:
                for dx, dy in ((1, 0), (0, 1)):
                    if rgb(x - dx, y - dy) == OUTLINE and rgb(x + dx, y + dy) is not None and panel_grain(rgb(x + dx, y + dy)):
                        op[x + dx, y + dy] = V_LIGHT + (255,)
            elif c == EDGE_LO:
                for dx, dy in ((1, 0), (0, 1)):
                    if rgb(x + dx, y + dy) == OUTLINE and rgb(x - dx, y - dy) is not None and panel_grain(rgb(x - dx, y - dy)):
                        op[x - dx, y - dy] = V_SHADOW + (255,)
    return out


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    backup = Path(argv[1])
    write = '--write' in argv
    preview = argv[argv.index('--preview') + 1] if '--preview' in argv else None
    backup.mkdir(parents=True, exist_ok=True)
    tiles = []
    for n in NAMES:
        p = GUI / f'gui_{n}.png'
        if not p.exists():
            print('missing', p)
            continue
        if not (backup / p.name).exists():
            shutil.copy2(p, backup / p.name)
        before = Image.open(backup / p.name).convert('RGBA')
        after = convert(before)
        tiles.append((n, before, after))
        if write:
            after.save(p)
    if preview:
        cols = 4
        rows = (len(tiles) + cols - 1) // cols
        sheet = Image.new('RGB', (cols * 2 * 180 + cols * 10, rows * 190), (60, 60, 60))
        d = ImageDraw.Draw(sheet)
        for i, (n, b, a) in enumerate(tiles):
            x, y = (i % cols) * (360 + 10), (i // cols) * 190
            for k, im in enumerate((b, a)):
                crop = im.crop((0, 0, 176, 176))
                sheet.paste(crop, (x + k * 180, y + 12), crop)
            d.text((x + 2, y), n, fill=(255, 255, 255))
        sheet.save(preview)
    print(('written ' if write else 'previewed ') + str(len(tiles)))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
