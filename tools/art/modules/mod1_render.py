"""Redraw MOD1 from its checked-in pixel designs. Requires Python 3 and Pillow.

From the repository root run python tools/art/modules/mod1_render.py,
then python tools/art/modules/mod1_preview.py to rebuild both contact sheets.
"""

import re
from pathlib import Path

from PIL import Image, ImageColor, ImageDraw

from mod1_accessories import accessories
from mod1_nano import GLYPHS as NANO
from mod1_quantum import GLYPHS as QUANTUM

ROOT = Path(__file__).resolve().parents[3]
TEXTURES = ROOT / "src/main/resources/assets/mio_icif/textures/item/module"
BATCH = ROOT / "docs/art/batches/MOD1-suit-modules.md"
GRAPHITE = "#2B3440"
WHITE = "#FFFFFF"
WEAVE_DARK = "#1C1F23"
WEAVE_LIGHT = "#24282D"
GOLD = "#F2C14E"
GOLD_SHADE = "#8E6A1F"
CARD_OUTLINE = "#1B1E24"

# The seam remains visible through openings in each pictogram.
SHARED = {
    "blast_warning": ("#FF5A36", (
        "....#....",
        "...#.#...",
        "...#.#...",
        "..#.#.#..",
        "..#.#.#..",
        ".#.....#.",
        ".#..#..#.",
        "#########",
    )),
    "threat_sensor": ("#FF3B30", (
        "...###...",
        ".##...##.",
        ".........",
        "..#####..",
        ".#..#..#.",
        "#..###..#",
        ".#..#..#.",
        "..#####..",
    )),
    "toxin_filter": ("#9CFF57", (
        "..-----..",
        ".#.....#.",
        "###...###",
        "###...###",
        "....#....",
        "-.......-",
        ".-.###.-.",
        "..-###-..",
    )),
    "self_repair": ("#69F0AE", (
        "......#..",
        "..##.###.",
        "..##..#..",
        "######...",
        "######...",
        "..##...#.",
        "..##..###",
        ".......#.",
    )),
    "hydrophobic": ("#40C4FF", (
        "...#.....",
        "..#+#....",
        ".#+..#...",
        ".#####..#",
        "..###..#.",
        "........#",
        ".....###.",
        "..###....",
    )),
    "kinetic_generator": ("#FFEE58", (
        ".###.....",
        "####...##",
        "####..##.",
        ".##..##..",
        ".....####",
        "..##...#.",
        "..##..#..",
        ".....#...",
    )),
}


def batch_groups():
    """Keep rendering and both previews in the order specified by the art brief."""
    groups = []
    for section in re.split(r"^## ", BATCH.read_text(encoding="utf-8"), flags=re.M):
        names = re.findall(r"^[|] `(item_[a-z0-9_]+)\.png`", section, flags=re.M)
        if names:
            groups.append(names)
    return groups


def blend(color, target, fraction):
    first, second = ImageColor.getrgb(color), ImageColor.getrgb(target)
    return tuple(round(a + (b - a) * fraction) for a, b in zip(first, second))


def weave(x, y):
    # Two over, two under. Shift each two-pixel course one tow to the left.
    return WEAVE_LIGHT if (x // 2 + y // 2) % 4 < 2 else WEAVE_DARK


def shell(image, cut, colors):
    """Draw the continuous outline and the one-pixel bevel around the face."""
    outline, lit_bevel, dark_bevel, face = colors
    def inside(x, y):
        dx, dy = min(x - 1, 14 - x), min(y - 1, 14 - y)
        return min(dx, dy) >= 0 and dx + dy >= cut

    draw = ImageDraw.Draw(image)
    for y in range(1, 15):
        for x in range(1, 15):
            if not inside(x, y):
                continue
            if not all(inside(x + dx, y + dy) for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1))):
                color = outline
            elif not inside(x + 2, y) or not inside(x, y + 2):
                color = dark_bevel
            elif not inside(x - 2, y) or not inside(x, y - 2):
                color = lit_bevel
            else:
                color = face(x, y)
            draw.point((x, y), fill=color)


def ceramic(x, y):
    return "#D6D7D2" if x >= 11 or y >= 11 else "#EDEDE8"


def ceramic_shell():
    image = Image.new("RGBA", (16, 16))
    shell(image, 1, (CARD_OUTLINE, "#EDEDE8", "#B4B6B3", ceramic))
    draw = ImageDraw.Draw(image)
    draw.line((3, 2, 10, 2), fill=WHITE)
    draw.line((2, 3, 2, 7), fill=WHITE)
    draw.line((3, 3, 4, 3), fill=WHITE)
    return image


def quantum_base(color):
    image = ceramic_shell()
    draw = ImageDraw.Draw(image)
    for x in (4, 6, 8):
        draw.point((x, 13), fill="#C9A55A")
        draw.point((x, 14), fill="#8E7438")
    draw.rectangle((11, 12, 12, 13), fill=color)
    draw.point((11, 12), fill=blend(color, WHITE, 0.4))
    return image


def nano_base():
    image = Image.new("RGBA", (16, 16))
    shell(image, 3, ("#0E1013", "#4A5058", "#15171A", weave))
    return image


def shared_base():
    image = ceramic_shell()
    draw = ImageDraw.Draw(image)
    for y in range(2, 14):
        for x in range(2, 14):
            if x + y == 15:
                draw.point((x, y), fill=CARD_OUTLINE)
            elif x + y > 15:
                color = "#15171A" if x == 13 or y == 13 else weave(x, y)
                draw.point((x, y), fill=color)
    return image


def draw_glyph(image, rows, inks, family):
    pixels = {(x, y): inks[symbol]
              for y, row in enumerate(rows, 4)
              for x, symbol in enumerate(row, 3) if symbol != "."}
    draw = ImageDraw.Draw(image)
    for x, y in pixels:
        for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
            xx, yy = x + dx, y + dy
            if family == "shared" and xx + yy < 15:
                draw.point((xx, yy), fill=GRAPHITE)
            elif family == "nano" and 3 <= xx <= 12 and 4 <= yy <= 11:
                draw.point((xx, yy), fill=WEAVE_DARK)
    for pos, ink in pixels.items():
        draw.point(pos, fill=ink)


def mark_mk2(image, family):
    draw = ImageDraw.Draw(image)
    if family == "nano":
        draw.line((2, 4, 4, 2), fill=GOLD)
    else:
        draw.line((3, 2, 7, 2), fill=GOLD)
    for x in (9, 11):
        draw.point((x, 3), fill=GOLD_SHADE)
        draw.point((x, 2), fill=GOLD)


def module(name, glyphs, family):
    key = name.removeprefix("item_module_").removesuffix("_mk2")
    color, rows = glyphs[key]
    if family == "quantum":
        image = quantum_base(color)
        inks = {"#": GRAPHITE, "+": "#5A6472"}
    else:
        image = nano_base() if family == "nano" else shared_base()
        inks = {"#": color, "+": blend(color, WHITE, 0.4), "-": blend(color, WEAVE_DARK, 0.45)}
    draw_glyph(image, rows, inks, family)
    if name.endswith("_mk2"):
        mark_mk2(image, family)
    return image


def main():
    quantum, nano, shared, parts = batch_groups()
    for names, glyphs, family in ((quantum, QUANTUM, "quantum"), (nano, NANO, "nano"),
                                   (shared, SHARED, "shared")):
        for name in names:
            module(name, glyphs, family).save(TEXTURES / f"{name}.png")
    parts_by_name = accessories(quantum_base)
    for name in parts:
        parts_by_name[name].save(TEXTURES / f"{name}.png")


if __name__ == "__main__":
    main()
