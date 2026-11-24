"""Render the MOD1 inventory previews. Requires Pillow >= 10.1 and Git.

Run this script after mod1_render.py. The overview compares against the initial
brief, and the second-round sheet compares against the accepted first round.
The Chinese previews use an installed CJK font, optionally selected with --cjk-font.
"""

import argparse
import io
import json
import math
import os
import subprocess
import sys
from pathlib import Path
from zipfile import ZipFile

from PIL import Image, ImageDraw, ImageFont

from mod1_render import ROOT, TEXTURES, batch_groups

BRIEF = "baf296a8997cb39d83dc7a8a2cbf71f4fd36fe96"
ROUND_ONE = "858c25a1302ce7a6efcb4f0feff9be665763debd"
DESTINATION = ROOT / "docs/art/previews"
FAMILIES = ("Quantum modules", "Nano modules", "Shared modules", "Accessories")
CHINESE_FAMILIES = ("量子模块", "纳米模块", "共用模块", "配件")
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


def historical_icons(commit, names):
    path = TEXTURES.relative_to(ROOT).as_posix()
    archive = subprocess.check_output(["git", "-C", str(ROOT), "archive", "--format=zip", commit, "--", path])
    icons = {}
    with ZipFile(io.BytesIO(archive)) as zipped:
        for name in names:
            with zipped.open(f"{path}/{name}.png") as file, Image.open(file) as icon:
                icons[name] = icon.convert("RGBA")
    return icons


def overview(groups, icons):
    originals = historical_icons(BRIEF, (name for group in SAMPLES for name in group))
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
            sheet.paste(slot(originals[name], 2), (cx - 37, y + 22))
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


GUIDE_LABELS = {
    "ore_scanner": "扫描矿脉", "grid_telemetry": "测电压", "entity_esp": "标记生物",
    "ballistic": "预览弹道", "behavior_predictor": "预判走位", "holomap": "全息地图",
    "deflector": "护盾挡伤", "smart_guidance": "弹道追踪", "resonance_field": "行走碎块",
    "phase_jump_drive": "向前瞬移", "phase_modulator": "攻击穿身",
    "coherence_capacitor": "技能蓄能", "entanglement_relay": "僚机齐射", "compute_core": "扩充算力",
    "myofiber": "疾跑冲刺", "thermoregulator": "抗寒防火", "emp_emitter": "电磁脉冲",
    "optical_camo": "光学隐身", "micro_capacitor": "增加储电", "gecko_setae": "贴墙攀爬",
    "shock_gel": "减摔反弹", "heat_sink_fins": "提升热容量", "thermal_camo": "潜行降温",
    "jammer": "干扰锁定", "recon_cloud": "透墙侦察", "kinetic_mesh": "装甲减伤",
    "solar_skin": "日光充电", "blast_warning": "爆炸倒计时", "threat_sensor": "危险警报",
    "toxin_filter": "滤毒防辐射", "self_repair": "自动回血", "hydrophobic": "游泳加速",
    "kinetic_generator": "行走充电", "core_graviton": "重力子壁垒", "core_propulsion": "推进核心",
    "core_solar_array_1": "光伏阵列Ⅰ", "core_solar_array_2": "光伏阵列Ⅱ",
    "core_solar_array_3": "光伏阵列Ⅲ", "quantum_bay_expander": "量子扩展槽",
    "living_nano_lining": "活体内衬", "nano_lattice_densifier": "晶格致密剂",
}


def chinese_font(override):
    windows_fonts = Path(os.environ.get("WINDIR", "C:/Windows")) / "Fonts"
    candidates = [override] if override else [
        windows_fonts / "msyh.ttc",
        windows_fonts / "NotoSansSC-VF.ttf",
        "/System/Library/Fonts/PingFang.ttc",
        "/System/Library/Fonts/STHeiti Medium.ttc",
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
        "NotoSansCJKsc-Regular.otf",
    ]
    for candidate in candidates:
        try:
            font = ImageFont.truetype(str(candidate), 15)
        except OSError:
            continue
        # A Latin-only font generally renders these as identical missing glyphs.
        if bytes(font.getmask("量")) != bytes(font.getmask("纳")):
            return font
    return None


def player_guide(groups, icons, font):
    columns, cell_width = 8, 92
    margin, header, row_height, section_gap = 20, 30, 79, 4
    names_by_group = [[name for name in names if not name.endswith("_mk2")] for names in groups]
    rows_per_group = [math.ceil(len(names) / columns) for names in names_by_group]
    width = margin * 2 + columns * cell_width
    height = margin * 2 + len(groups) * header + sum(rows_per_group) * row_height + (len(groups) - 1) * section_gap
    sheet = Image.new("RGB", (width, height), PAPER)
    draw = ImageDraw.Draw(sheet)
    heading_font = font.font_variant(size=20)
    y = margin
    for family, names, rows in zip(CHINESE_FAMILIES, names_by_group, rows_per_group):
        draw.text((margin, y), family, anchor="lt", font=heading_font, fill=INK)
        line_x = margin + round(draw.textlength(family, font=heading_font)) + 12
        draw.line((line_x, y + 10, width - margin - 1, y + 10), fill=RULE)
        y += header
        for i, name in enumerate(names):
            cx = margin + (i % columns) * cell_width + cell_width // 2
            top = y + (i // columns) * row_height
            square = slot(icons[name], 3)
            sheet.paste(square, (cx - square.width // 2, top))
            key = name.removeprefix("item_").removeprefix("module_")
            draw.text((cx, top + square.height + 5), GUIDE_LABELS[key], anchor="mt", font=font, fill=INK)
        y += rows * row_height + section_gap
    return sheet


def round_two_sheet(groups, icons, font):
    before = historical_icons(ROUND_ONE, icons)
    changed = [[name for name in names if before[name].tobytes() != icons[name].tobytes()] for names in groups]
    names_zh = json.loads((ROOT / "src/main/resources/assets/mio_icif/lang/zh_cn.json").read_text(encoding="utf-8"))
    columns, cell_width, margin = 9, 160, 24
    width = 2 * margin + columns * cell_width
    section_header, row_height = 34, 88
    height = 78 + sum(section_header + math.ceil(len(names) / columns) * row_height
                      for names in changed if names) + 230
    sheet = Image.new("RGB", (width, height), PAPER)
    draw = ImageDraw.Draw(sheet)
    title_font = font.font_variant(size=28)
    section_font = font.font_variant(size=19)
    small_font = font.font_variant(size=14)

    def section(title, top):
        draw.text((margin, top), title, anchor="lt", font=section_font, fill=INK)
        end = margin + round(draw.textlength(title, font=section_font)) + 14
        draw.line((end, top + 10, width - margin - 1, top + 10), fill=RULE)
        return top + section_header

    draw.text((margin, margin), "套装模块第二轮改图", anchor="lt", font=title_font, fill=INK)
    draw.text((width - margin, margin + 8), "左图改前　右图改后　·　3× 物品栏",
              anchor="rt", font=section_font, fill=SECONDARY)
    y = 78
    for family, names in zip(CHINESE_FAMILIES, changed):
        if not names:
            continue
        y = section(family, y)
        for i, name in enumerate(names):
            cx = margin + (i % columns) * cell_width + cell_width // 2
            top = y + (i // columns) * row_height
            sheet.paste(slot(before[name], 3), (cx - 57, top))
            sheet.paste(slot(icons[name], 3), (cx + 3, top))
            label = names_zh["item.mio_icif.module." + name]
            label_font = small_font if draw.textlength(label, font=font) > cell_width - 6 else font
            draw.text((cx, top + 59), label, anchor="mt", font=label_font, fill=INK)
        y += math.ceil(len(names) / columns) * row_height

    y = section("量子模块全览　3×", y)
    quantum = groups[0]
    for i, name in enumerate(quantum):
        x = margin + round(i * (width - 2 * margin - 54) / (len(quantum) - 1))
        sheet.paste(slot(icons[name], 3), (x, y))
    y = section("太阳能阵列核心", y + 72)
    x = margin
    for scale in (1, 3):
        draw.text((x, y + 27), f"{scale}×", anchor="lm", font=section_font, fill=SECONDARY)
        x += 44
        for level, numeral in enumerate(("Ⅰ", "Ⅱ", "Ⅲ"), start=1):
            square = slot(icons[f"item_core_solar_array_{level}"], scale)
            cx = x + 31
            sheet.paste(square, (cx - square.width // 2, y + (54 - square.height) // 2))
            draw.text((cx, y + 60), numeral, anchor="mt", font=font, fill=INK)
            x += 62
        x += 42
    return sheet


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cjk-font", metavar="FILE", help="Chinese .ttf, .ttc or .otf font for the guide and R2 previews")
    args = parser.parse_args()
    groups = batch_groups()
    icons = {}
    for names in groups:
        for name in names:
            with Image.open(TEXTURES / (name + ".png")) as icon:
                icons[name] = icon.convert("RGBA")
    DESTINATION.mkdir(exist_ok=True)
    overview(groups, icons)
    pair_sheet(groups, icons)
    font = chinese_font(args.cjk_font)
    if font is None:
        message = "MOD1-guide.png and MOD1-r2.png require a Chinese font. Supply one with --cjk-font FILE."
        if args.cjk_font:
            parser.error(message)
        print(message, file=sys.stderr)
    else:
        player_guide(groups, icons, font).save(DESTINATION / "MOD1-guide.png")
        round_two_sheet(groups, icons, font).save(DESTINATION / "MOD1-r2.png")


if __name__ == "__main__":
    main()
