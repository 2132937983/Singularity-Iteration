"""Render the two MOD1 inventory contact sheets. Requires Pillow >= 10.1 and Git.

From any directory, run this script after mod1_render.py. The old samples come
from the brief's Git commit, so regenerating the sheets never changes the baseline.
"""

import io
import math
import subprocess

from PIL import Image, ImageDraw, ImageFont

from mod1_render import ROOT, TEXTURES, batch_groups

BRIEF = "baf296a8997cb39d83dc7a8a2cbf71f4fd36fe96"
DESTINATION = ROOT / "docs/art/previews"
FAMILIES = ("Quantum modules", "Nano modules", "Shared modules", "Accessories")
SAMPLES = (
    ("item_module_ore_scanner", "item_module_entity_esp", "item_module_entanglement_relay",
     "item_module_phase_jump_drive_mk2"),
    ("item_module_gecko_setae", "item_module_optical_camo", "item_module_heat_sink_fins",
     "item_module_micro_capacitor_mk2"),
    ("item_module_blast_warning", "item_module_toxin_filter_mk2", "item_module_self_repair",
     "item_module_kinetic_generator"),
    ("item_core_graviton", "item_core_solar_array_3", "item_living_nano_lining",
     "item_nano_lattice_densifier"),
)

PAPER, INK, SECONDARY, RULE = "#F4F4F0", "#1F292E", "#46565E", "#C5CBC9"
MARGIN = 36
COLUMN = 170
WIDTH = MARGIN * 2 + 10 * COLUMN
TOP = 98
SECTION_HEADER = 48
ROW = 211
FONTS = {size: ImageFont.load_default(size=size) for size in (12, 14, 19, 25, 39)}


def slot(icon, scale):
    """Composite on an 18px beveled inventory slot before integer scaling."""
    tile = Image.new("RGB", (18, 18), "#8B8B8B")
    draw = ImageDraw.Draw(tile)
    draw.line([(0, 17), (0, 0), (17, 0)], fill="#373737")
    draw.line([(1, 17), (17, 17), (17, 1)], fill="#FFFFFF")
    tile.paste(icon, (1, 1), icon)
    return tile.resize((18 * scale, 18 * scale), Image.Resampling.NEAREST)


def words_on_lines(draw, name, width, font):
    """Wrap at underscores while retaining every character of a texture ID."""
    lines, current = [], ""
    parts = name.split("_")
    for i, part in enumerate(parts):
        piece = part + ("_" if i + 1 < len(parts) else "")
        if current and draw.textlength(current + piece, font=font) > width:
            lines.append(current)
            current = ""
        current += piece
    return lines + [current]


def filename(draw, name, cx, top, width, *, size=14):
    font = FONTS[size]
    for line_number, line in enumerate(words_on_lines(draw, name, width, font)):
        draw.text((cx, top + line_number * (size + 3)), line, anchor="mt", font=font, fill=INK)


def page(title, detail, height):
    sheet = Image.new("RGB", (WIDTH, height), PAPER)
    draw = ImageDraw.Draw(sheet)
    draw.text((MARGIN, 30), title, font=FONTS[39], anchor="lt", fill=INK)
    draw.text((WIDTH - MARGIN, 46), detail, font=FONTS[19], anchor="rt", fill=SECONDARY)
    draw.line((MARGIN, TOP - 10, WIDTH - MARGIN, TOP - 10), fill=RULE, width=2)
    return sheet, draw


def heading(draw, y, title, detail):
    draw.text((MARGIN, y + 5), title, anchor="lt", font=FONTS[25], fill=INK)
    draw.text((WIDTH - MARGIN, y + 11), detail, anchor="rt", font=FONTS[14], fill=SECONDARY)


def labeled_tile(sheet, draw, icons, name, cx, top, label_width):
    square = slot(icons[name], 8)
    sheet.paste(square, (cx - square.width // 2, top))
    filename(draw, name + ".png", cx, top + square.height + 8, label_width)


def old_icon(name):
    path = (TEXTURES / (name + ".png")).relative_to(ROOT).as_posix()
    data = subprocess.check_output(["git", "-C", str(ROOT), "show", f"{BRIEF}:{path}"])
    with Image.open(io.BytesIO(data)) as icon:
        return icon.convert("RGBA")


def overview(groups, icons):
    families_height = sum(SECTION_HEADER + ROW * math.ceil(len(names) / 10) for names in groups)
    sheet, draw = page("MOD1 suit module icons", "69 textures   /   8× inventory slots",
                       TOP + families_height + 282)
    y = TOP
    for family, names in zip(FAMILIES, groups):
        heading(draw, y, family, f"{len(names)} textures")
        y += SECTION_HEADER
        for i, name in enumerate(names):
            cx = MARGIN + COLUMN * (i % 10) + COLUMN // 2
            labeled_tile(sheet, draw, icons, name, cx, y + ROW * (i // 10), COLUMN - 8)
        y += ROW * math.ceil(len(names) / 10)

    heading(draw, y, "Native size", "1×   /   Batch order")
    y += SECTION_HEADER
    # Preserve a single, left-to-right run of 69 actual-size slots.
    x = MARGIN
    for family, names in zip(FAMILIES, groups):
        draw.text((x, y), family, font=FONTS[12], anchor="lt", fill=SECONDARY)
        for name in names:
            sheet.paste(slot(icons[name], 1), (x, y + 19))
            x += 18
        x += 27
    y += 63
    draw.line((MARGIN, y, WIDTH - MARGIN, y), fill=RULE)
    y += 11
    heading(draw, y, "Original and redraw", "2×   /   Original at left, redraw at right")
    y += SECTION_HEADER
    for group_index, (family, samples) in enumerate(zip(FAMILIES, SAMPLES)):
        start = MARGIN + group_index * (10 * COLUMN // 4)
        draw.text((start, y), family, anchor="lt", font=FONTS[14], fill=SECONDARY)
        for i, name in enumerate(samples):
            pair_width = 10 * COLUMN / 16
            cx = start + round((i + 0.5) * pair_width)
            sheet.paste(slot(old_icon(name), 2), (cx - 37, y + 22))
            sheet.paste(slot(icons[name], 2), (cx + 1, y + 22))
            short_name = name.removeprefix("item_module_").removeprefix("item_")
            filename(draw, short_name, cx, y + 65, int(pair_width - 6), size=12)
    sheet.save(DESTINATION / "MOD1.png")


def pair_sheet(groups, icons):
    pairs = [[name for name in names if name + "_mk2" in icons] for names in groups[:3]]
    height = TOP + 20 + sum(SECTION_HEADER + ROW * math.ceil(len(names) / 5) for names in pairs)
    count = sum(map(len, pairs))
    sheet, draw = page("MOD1 upgrade pairs", f"{count} pairs   /   Mk.I left, Mk.II right   /   8×", height)
    y = TOP
    for family, names in zip(FAMILIES, pairs):
        heading(draw, y, family, f"{len(names)} pairs")
        y += SECTION_HEADER
        for i, name in enumerate(names):
            cx = MARGIN + (2 * (i % 5) + 1) * COLUMN
            top = y + ROW * (i // 5)
            labeled_tile(sheet, draw, icons, name, cx - 77, top, 150)
            labeled_tile(sheet, draw, icons, name + "_mk2", cx + 77, top, 150)
        y += ROW * math.ceil(len(names) / 5)
    sheet.save(DESTINATION / "MOD1-pairs.png")


def main():
    groups = batch_groups()
    icons = {}
    for names in groups:
        for name in names:
            with Image.open(TEXTURES / (name + ".png")) as icon:
                icons[name] = icon.convert("RGBA")
    DESTINATION.mkdir(exist_ok=True)
    overview(groups, icons)
    pair_sheet(groups, icons)


if __name__ == "__main__":
    main()
