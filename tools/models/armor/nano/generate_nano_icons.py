#!/usr/bin/env python3
"""Draw the four 16px inventory icons from the Pure Vanilla+ suit palette.

Requires Pillow and generate_nano_textures.py beside this file.
    python generate_nano_icons.py
    python generate_nano_icons.py --preview nano_icons_preview.png

Each character is one final texel. The rows run from the top to the bottom.
"." is transparent and "#" is the one-pixel forged-carbon outline.
"""
import argparse
import json
from pathlib import Path

from PIL import Image, ImageColor, ImageDraw, ImageOps
import generate_nano_textures as suit


PALETTE = {
    ".": (0, 0, 0, 0),
    "#": suit.CARBON[3], "C": suit.CARBON[0], "M": suit.CARBON[1], "T": suit.CARBON[2],
    "P": suit.FINISH["plate"][0], "H": suit.FINISH["plate"][1],
    "p": suit.FINISH["plate"][2], "m": suit.FINISH["plate"][3], "u": suit.GUNMETAL,
    "s": suit.FINISH["silver"][0], "l": suit.FINISH["silver"][1], "t": suit.FINISH["silver"][2],
    "G": suit.FINISH["brass"][0], "b": suit.FINISH["brass"][2], "E": suit.LIT,
}
ART = {
    "nano_helmet": (
        "................",
        "................",
        "...###....###...",
        "..#uHH####TTH#..",
        ".#lsmTHHTTTmss#.",
        ".#sGHmPPPPmTGs#.",
        ".#sEEEmPPmEEEs#.",
        ".#sPmEEPPEEmPs#.",
        "..#tHmPCCPmTt#..",
        "..#stHPPPPTts#..",
        "...#stHPPTts#...",
        "....#tMmmMt#....",
        ".....#HmmT#.....",
        "......####......",
        "................",
        "................",
    ),
    "nano_chestplate": (
        "................",
        "................",
        "..####....####..",
        ".#uTH#....#HmP#.",
        ".#Hmll#..#stHm#.",
        ".#mPHss##ssHmP#.",
        ".#PmHHGssGbHPp#.",
        ".##PmGTEETbPm##.",
        "...#mGPEEPbP#...",
        "...#uPmttmPp#...",
        "...#THmmCCMP#...",
        "...#mPTHHMPP#...",
        "....#HmCCpP#....",
        ".....######.....",
        "................",
        "................",
    ),
    "nano_leggings": (
        "................",
        "................",
        "....########....",
        "...#uHtHHmHP#...",
        "...#HTmPPCmH#...",
        "...#uHmCCmTH#...",
        "...#HmP##PHm#...",
        "...#Tm#..#mT#...",
        "...#MC#..#CP#...",
        "...#Gs#..#sb#...",
        "...#lt#..#ts#...",
        "...#uM#..#mH#...",
        "...#HP#..#mp#...",
        "...####..####...",
        "................",
        "................",
    ),
    "nano_boots": (
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "....###..###....",
        "...#uH#..#Hm#...",
        "...#sC#..#Cs#...",
        "...#lt#..#ts#...",
        "..#uHm#..#mTH#..",
        ".#uTHP#..#PmTH#.",
        ".#Hmp##..##pmT#.",
        ".####......####.",
        "................",
        "................",
    ),
}


def paint(name, rows):
    if len(rows) != 16 or any(len(row) != 16 for row in rows):
        raise ValueError(f"{name}: expected a 16x16 grid")
    colors = {key: ImageColor.getcolor(value, "RGBA") if isinstance(value, str) else value
              for key, value in PALETTE.items()}
    icon = Image.new("RGBA", (16, 16))
    icon.putdata([colors[key] for row in rows for key in row])
    mask = icon.getchannel("A")
    bounds = mask.getbbox()
    if (not bounds or bounds[0]+bounds[2] != 16
            or mask.tobytes() != ImageOps.mirror(mask).tobytes()):
        raise ValueError(f"{name}: silhouette must be symmetric about the centre columns")
    top, bottom = bounds[1], bounds[3]
    if name == "nano_boots":
        # The outward-facing toes rest low in the slot. Pillow bounds exclude the bottom row.
        if bottom - top not in (7, 8) or bottom - 1 not in (13, 14) or mask.crop((7, 0, 9, 16)).getbbox():
            raise ValueError("Boots need short, low soles separated by the two centre columns")
    elif top + bottom != 16:
        raise ValueError(f"{name}: top and bottom margins must match")
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key in ".#":
                continue
            adjacent = ((x-1,y), (x+1,y), (x,y-1), (x,y+1))
            if any(not (0 <= i < 16 and 0 <= j < 16) or rows[j][i] == "." for i,j in adjacent):
                raise ValueError(f"{name}: missing outline at {(x,y)}")
    return icon


def slot_preview(pngs):
    # An 18px vanilla slot has a 16px interior and a one-pixel inset bevel.
    sheet = Image.new("RGB", (4 + 18*len(pngs), 22), "#C6C6C6")
    pen = ImageDraw.Draw(sheet)
    for index, png in enumerate(pngs):
        x, y = 2 + 18*index, 2
        pen.rectangle((x,y,x+17,y+17), fill="#8B8B8B")
        pen.line((x,y,x+16,y), fill="#373737")
        pen.line((x,y,x,y+16), fill="#373737")
        pen.line((x+1,y+17,x+17,y+17), fill="#FFFFFF")
        pen.line((x+17,y+1,x+17,y+17), fill="#FFFFFF")
        with Image.open(png) as icon:
            sheet.paste(icon, (x+1,y+1), icon.getchannel("A"))
    return sheet.resize((sheet.width*16, sheet.height*16), Image.Resampling.NEAREST)


def transform(rotation, translation, scale):
    return {"rotation": rotation, "translation": translation, "scale": [scale]*3}


def registered_model(name):
    # SI .74 registers armor/item_armor_nano_* and uses a custom item renderer.
    # These are its display transforms. Only the GUI perspective uses the sprite.
    gui_angles = {"nano_helmet": [18,-35,0], "nano_chestplate": [12,-25,0],
                  "nano_leggings": [10,-25,0], "nano_boots": [22,-40,0]}
    third = transform([75,45,0], [0,2.5,0], .32)
    base = {
        "parent": "builtin/entity", "gui_light": "front",
        "textures": {"particle": f"mio_icif:item/armor/item_armor_{name}"},
        "display": {
            "gui": transform(gui_angles[name], [0,0,0], 1),
            "ground": transform([0,0,0], [0,2,0], .5),
            "fixed": transform([0,180,0], [0,0,0], .9),
            "head": transform([0,180,0], [0,13,7], 1),
            "thirdperson_righthand": third, "thirdperson_lefthand": third,
            "firstperson_righthand": transform([0,-35,0], [2,3,-1], .3),
            "firstperson_lefthand": transform([0,35,0], [-2,3,-1], .3),
        },
    }
    return {"loader": "neoforge:separate_transforms", "gui_light": "front",
            "base": base, "perspectives": {"gui": {"parent": f"mio_icif:item/{name}"}}}


def write_json(path, data):
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def generate(root, preview=None):
    textures = root / "textures" / "item"
    models = root / "models" / "item"
    textures.mkdir(parents=True, exist_ok=True)
    (models / "armor").mkdir(parents=True, exist_ok=True)
    pngs = []
    for name, rows in ART.items():
        png = textures / (name + ".png")
        paint(name, rows).save(png)
        model = models / (name + ".json")
        write_json(model, {"parent": "minecraft:item/generated",
                           "textures": {"layer0": f"mio_icif:item/{name}"}})
        write_json(models / "armor" / ("item_armor_" + name + ".json"), registered_model(name))
        pngs.append(png)
    if preview:
        slot_preview(pngs).save(preview)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--assets", type=Path, default=Path(__file__).resolve().parents[4] / "src/main/resources/assets/mio_icif")
    parser.add_argument("--preview", type=Path, help="Optional 16x inventory slot preview")
    args = parser.parse_args()
    generate(args.assets, args.preview)
