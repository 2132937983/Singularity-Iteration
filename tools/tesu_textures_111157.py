#!/usr/bin/env python3
"""0.1.11.157: the TESU (太级储电罐) block textures, in the GESU family's look.

The casing and the ports start from this mod's own GESU textures (one 16 x 16 face of each strip), recoloured: the pale mint
panel becomes pale silver-violet, the input port's gem violet, the output port's gem amber (the casing has the gem painted
out). The energy cell and the controller's front (a screen with a bar graph and a status lamp) are drawn here (0.1.11.166: the
top electrode is gone). Running it again writes the same files.

0.1.11.165: the casing (block_tesu_casing.png) is now hand-drawn and this script only reads it, never writes it. The input,
output and wireless ports keep their GESU emblem (the gem and its dark frame: the pixels that differ from the old GESU-panel
casing and are coloured or dark) laid over the hand-drawn casing; the controller's front is drawn on it too.

0.1.11.167: the casing has a metallic sheen (a sharper bevel, deeper rings, a diagonal glint); the relay is a pylon now, its
textures drawn by tools/tesu_relay_pillar_111167.py.

0.1.11.169: the sources (three GESU textures, the electric wireless manager, the transformer upgrade) are read from
tools/tesu_sources/, copies of the mod's textures as they were: SCXSYGL's machine pack redrew the GESU and the manager, and
the TESU textures keep the look they were made with. A file that differs from what this script makes (the user redraws
the ports and the controller's front by hand) is kept; --force replaces it.
"""
import colorsys
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
TEX = ROOT / 'src/main/resources/assets/mio_icif/textures/block/wiring'
SOURCES = ROOT / 'tools/tesu_sources'   # 0.1.11.169: frozen inputs


FORCE = '--force' in sys.argv


def save_png(img, out):
    """0.1.11.169: writes {@code img} unless the file there differs from it (a hand edit, kept); --force replaces it."""
    out.parent.mkdir(parents=True, exist_ok=True)
    if out.exists() and not FORCE:
        old = Image.open(out).convert('RGBA')
        if old.size != img.size or old.tobytes() != img.convert('RGBA').tobytes():
            print('kept', out.relative_to(ROOT), '(differs from the generated image: hand-edited? --force replaces it)')
            return
    img.save(out)
    print('wrote', out.relative_to(ROOT))


def face(name):
    return Image.open(SOURCES / (name + '.png')).convert('RGBA').crop((0, 0, 16, 16))


def recolour(img, panel_hue, gem_hue, gem_sat=None):
    """Turns the GESU palette: pale (low saturation) pixels to the panel hue, saturated ones (the gem) to the gem hue."""
    out = img.copy()
    px = out.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s < 0.35:   # the panel and its tile lines
                h, s = panel_hue, min(1.0, s * 0.9)
            else:          # the gem
                h = gem_hue
                if gem_sat is not None:
                    s = gem_sat
            r2, g2, b2 = colorsys.hsv_to_rgb(h, s, v)
            px[x, y] = (round(r2 * 255), round(g2 * 255), round(b2 * 255), a)
    return out


def paint_out_gem(img):
    """The casing: every saturated pixel (the gem and its dark ring) takes the panel tile colour two pixels to its left."""
    out = img.copy()
    px = out.load()
    src = img.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = src[x, y]
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s >= 0.35 or v < 0.35:
                # copy from the same tile position in the panel pattern (the GESU tiles repeat every 4 pixels)
                for dx in (4, -4, 8, -8):
                    sx = x + dx
                    if 0 <= sx < 16:
                        r2, g2, b2, a2 = src[sx, y]
                        h2, s2, v2 = colorsys.rgb_to_hsv(r2 / 255, g2 / 255, b2 / 255)
                        if s2 < 0.35 and v2 >= 0.35:
                            px[x, y] = (r2, g2, b2, a2)
                            break
    return out


def emblem_on(casing, port, panel):
    """0.1.11.165: the port's emblem over {@code casing}. The emblem is every pixel of {@code port} that differs from
    {@code panel} (the old GESU-panel casing the port was drawn on) and is coloured or dark; the pale panel is left out."""
    out = casing.copy()
    px, src, ref = out.load(), port.load(), panel.load()
    for y in range(16):
        for x in range(16):
            if src[x, y] == ref[x, y]:
                continue
            r, g, b, a = src[x, y]
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s >= 0.35 or v < 0.70:
                px[x, y] = src[x, y]
    return out


def cell():
    """A dark violet crystal block, veined with glowing lines, brighter at the middle."""
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            d = abs(x - 7.5) + abs(y - 7.5)
            base = (34, 20, 56) if not edge else (64, 56, 80)
            glow = max(0.0, 1 - d / 9)
            r = base[0] + int(70 * glow)
            g = base[1] + int(20 * glow)
            b = base[2] + int(110 * glow)
            px[x, y] = (min(255, r), min(255, g), min(255, b), 255)
    veins = [(3, 3), (4, 4), (5, 5), (6, 6), (9, 9), (10, 10), (11, 11), (12, 12), (12, 3), (11, 4), (10, 5), (5, 10), (4, 11), (3, 12),
             (7, 2), (8, 2), (7, 13), (8, 13), (2, 7), (2, 8), (13, 7), (13, 8)]
    for x, y in veins:
        px[x, y] = (200, 120, 255, 255)
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px[x, y] = (246, 222, 255, 255)
    return img


def controller_front(casing):
    """The casing with a screen: three violet bars rising to the right and a green status lamp."""
    img = casing.copy()
    px = img.load()
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = (40, 30, 62, 255) if x in (3, 12) or y in (3, 12) else (22, 16, 38, 255)
    for i, height in enumerate((3, 5, 7)):
        x0 = 5 + i * 2
        for y in range(11 - height, 11):
            px[x0, y] = (186, 110, 255, 255)
    px[11, 4] = (110, 240, 130, 255)
    return img


def item_recolour(path, hue, sat_floor=0.0):
    """An item texture with every coloured pixel turned to {@code hue} (greys stay)."""
    img = Image.open(path).convert('RGBA')
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s >= 0.2:
                r2, g2, b2 = colorsys.hsv_to_rgb(hue, max(s, sat_floor), v)
                px[x, y] = (round(r2 * 255), round(g2 * 255), round(b2 * 255), a)
    return img


def main():
    PANEL, VIOLET, AMBER, CYAN = 0.74, 0.77, 0.10, 0.52
    panel = paint_out_gem(recolour(face('block_gesu_core'), PANEL, VIOLET))   # the old casing, to lift the port emblems off
    casing = Image.open(TEX / 'block_tesu_casing.png').convert('RGBA')        # 0.1.11.165: hand-drawn, read only
    items = ROOT / 'src/main/resources/assets/mio_icif/textures/item'
    # 0.1.11.158: the wireless field's items (from this mod's transformer upgrade and electric wireless manager)
    for name, img in {
            'upgrade/wireless_upgrade': item_recolour(SOURCES / 'transformer_upgrade.png', CYAN, 0.55),
            'normal/item_tesu_connector': item_recolour(SOURCES / 'item_electric_wireless_manager.png', VIOLET, 0.45)}.items():
        save_png(img, items / (name + '.png'))
    outputs = {
        'block_tesu_wireless_port': emblem_on(casing, recolour(face('block_gesu_output_luv'), PANEL, CYAN, gem_sat=0.75), panel),
        'block_tesu_cell': cell(),
        'block_tesu_controller_front': controller_front(casing),
        'block_tesu_input': emblem_on(casing, recolour(face('block_gesu_input_iv'), PANEL, VIOLET), panel),
        'block_tesu_output': emblem_on(casing, recolour(face('block_gesu_output_luv'), PANEL, AMBER, gem_sat=0.85), panel),
    }
    for name, img in outputs.items():
        save_png(img, TEX / (name + '.png'))


if __name__ == '__main__':
    main()
