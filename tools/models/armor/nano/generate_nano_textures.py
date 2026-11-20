#!/usr/bin/env python3
"""Paint the suit's two vanilla-density UV pages and their emissive masks.

Python 3.9+ and Pillow:
    python generate_nano_textures.py nano_suit_pure.geo.json --out-dir textures

The material maps live beside the geometry or the script. All sampling is
nearest-texel pixel art. Every UV span is checked against the physical face,
including the geometry on initially hidden bones.
"""
import argparse
import json
import math
from pathlib import Path
import random
import zlib

from PIL import Image, ImageDraw, ImageOps


ATLAS = (128, 128)
EPSILON = 0.000002
FACE_AXES = {
    "north": (0, 1), "south": (0, 1), "east": (2, 1),
    "west": (2, 1), "up": (0, 2), "down": (0, 2),
}
MIRRORED_FACES = {"north", "south", "up", "down"}
CARBON = ("#15171C", "#282D36", "#373E49", "#101216")
# Face fill, one-pixel lit edge, one-pixel shaded edge, top, underside.
FINISH = {
    "plate": ("#23272E", "#3B424B", "#191D23", "#2C323B", "#1C2026"),
    "silver": ("#A6B0BD", "#CCD4DC", "#788492", "#B8C3CE", "#87929F"),
    "brass": ("#C59B43", "#DEC16D", "#8D6B36", "#D1AA52", "#A07B3B"),
}
LENS, LIT = "#17231E", "#62DE9D"
BRASS_INK = "#AD8848"
BRASS_RECESS = "#755E3D"
SILVER_INK = "#788591"
GUNMETAL = "#4A5560"
CHANNEL = "#171B22"


def cube_key(bone, origin, size):
    return bone, tuple(round(v, 6) for v in origin), tuple(round(v, 6) for v in size)


def read_materials(data):
    if isinstance(data, (Path, str)):
        data = json.loads(Path(data).read_bytes())
    rows = {}
    for bone, parts in data["bones"].items():
        for entry in parts:
            name, mat, origin, size = entry[:4]
            emits = frozenset(entry[4] if len(entry) > 4 else ())
            decals = entry[5] if len(entry) == 6 else {}
            if (mat not in {*FINISH, "carbon", "glow"} or len(entry) not in (4, 5, 6)
                    or emits - FACE_AXES.keys() or decals.keys() - FACE_AXES.keys()):
                raise ValueError(f"Invalid material or emission for {bone}/{name}")
            key = cube_key(bone, origin, size)
            if key in rows:
                raise ValueError(f"Ambiguous part identity: {key}")
            rows[key] = name, mat, emits, decals
    return rows


def bounds(chart):
    u, v = chart["uv"]
    du, dv = chart["uv_size"]
    if du == 0 or dv == 0 or chart.get("uv_rotation", 0):
        raise ValueError(f"Expected an unrotated per-face UV island: {chart}")
    return (math.floor(min(u, u+du) + EPSILON), math.floor(min(v, v+dv) + EPSILON),
            math.ceil(max(u, u+du) - EPSILON), math.ceil(max(v, v+dv) - EPSILON))


def face_size(cube, face):
    a, b = FACE_AXES[face]
    extra = 2 * cube.get("inflate", 0)
    return cube["size"][a] + extra, cube["size"][b] + extra


def check_density(chart, face_wh, label):
    width, height = face_wh
    if min(width, height) < 1 - EPSILON:
        raise ValueError(f"{label}: physical face below one model unit")
    if any(abs(abs(span) - length) > EPSILON for span, length in zip(chart["uv_size"], face_wh)):
        raise ValueError(f"{label}: expected vanilla UV spans {face_wh}")
    u, v = chart["uv"]
    du, dv = chart["uv_size"]
    if any(abs(min(start, start+span) - round(min(start, start+span))) > EPSILON
           for start, span in ((u, du), (v, dv))):
        raise ValueError(f"{label}: begin the UV island on an integer texel boundary")


def canonical_part(name, face):
    left = name.startswith("left_") or name.endswith("_l")
    name = name.removeprefix("left_").removeprefix("right_")
    if name.endswith(("_l", "_r")):
        name = name[:-2]
    if left:
        face = {"east":"west", "west":"east"}.get(face, face)
    return name, face, left


def carbon_tile(size, area, part, face, seed):
    tile = Image.new("RGBA", size, CARBON[0])
    x0, y0, x1, y1 = area
    w, h = x1-x0, y1-y0
    if min(w, h) < 2 or w*h < 8:
        return tile
    rng = random.Random(zlib.crc32(f"{seed}:{part}:{face}".encode("utf-8")))
    positions = [(x,y) for y in range(y0, y1) for x in range(x0, x1)]
    rng.shuffle(positions)
    budget = max(1, round(w*h*.15))
    blocked = set()
    pen = ImageDraw.Draw(tile)
    for x, y in positions:
        if (x, y) in blocked:
            continue
        chip = [(x,y)]
        if budget >= 2 and rng.random() < .64:
            steps = [(0,1), (1,0)]
            rng.shuffle(steps)
            for dx, dy in steps:
                end = x+dx, y+dy
                if end[0] < x1 and end[1] < y1 and end not in blocked:
                    chip.append(end)
                    break
        tone = rng.choices(CARBON[1:], weights=(6, 2, 3))[0]
        for px, py in chip:
            pen.point((px, py), fill=tone)
            # Keep each 1x1 / 1x2 carbon chip distinct even along a diagonal.
            blocked.update((px+i, py+j) for i in (-1,0,1) for j in (-1,0,1))
        budget -= len(chip)
        if budget <= 0:
            break
    return tile


def paint_mark(pen, mark, box, material):
    """Paint a single manufacturing feature in the canonical right-hand orientation.

    The box contains only complete texture cells. A compact face is a one-cell
    material swatch, permitted to receive one colour change.
    """
    x, y, end_x, end_y = box
    w, h = end_x - x, end_y - y
    if w <= 0 or h <= 0:
        return
    right, bottom = end_x-1, end_y-1
    mid_x, mid_y = x+(w-1)//2, y+(h-1)//2
    if mark in ("optic", "status", "core", "terminal", "spine_node"):
        return  # The emission pass owns the lit pixels and its matching mask.
    if mark == "core_frame":
        # The bright bevel reads around the actual recessed energy tile.
        pen.line((x,y,right,y), fill=FINISH["silver"][1])
        pen.line((x,bottom,right,bottom), fill=FINISH["silver"][2])
        if w >= 3:
            pen.point((x,bottom), fill=FINISH["brass"][0])
            pen.point((right,bottom), fill=FINISH["brass"][0])
    elif mark == "collar_edge":
        # A stamped graphite lip separates the dark collar from the hood above it.
        # Leave the middle of the rear rail dark, with a bevel at either end.
        pen.point((x,y),fill=FINISH["plate"][1])
        if w >= 3:
            pen.point((right,y),fill=FINISH["plate"][1])
    elif mark == "collar_gasket":
        # Stitched graphite seals trace the two shoulders of the carbon yoke.
        span = max(1,w//4)
        pen.line((x,y,x+span-1,y),fill=CARBON[2])
        pen.line((right-span+1,y,right,y),fill=CARBON[2])
        if w >= 6 and h >= 2:
            pen.point((x+span,y+1),fill=CARBON[1])
            pen.point((right-span,y+1),fill=CARBON[1])
    elif mark == "edge":
        if h >= 2:
            pen.line((x, y, right, y), fill=FINISH[material][3])
    elif mark in ("chest_vents", "radiator", "heat_sink"):
        # The vent on the mirrored chest pair follows the inside edge.
        if mark == "chest_vents":
            pen.point((x,mid_y), fill=BRASS_RECESS)
            pen.point((x,min(bottom,mid_y+1)), fill=BRASS_INK)
        else:
            for col in range(x, end_x, 2):
                pen.line((col,y,col,min(y+2,bottom)), fill=BRASS_INK)
    elif mark in ("rivet", "lock", "mk2"):
        color = FINISH["brass"][2] if material == "brass" else BRASS_INK
        if mark == "rivet":
            pen.point((x, y), fill=color)
        elif mark == "lock":
            pen.point((mid_x,bottom),fill=color)
        else:
            pen.line((x, bottom, min(right, x+1), bottom), fill=color)
    elif mark == "link":
        if h >= w:
            pen.line((mid_x,mid_y,mid_x,min(bottom,mid_y+1)), fill=SILVER_INK)
        else:
            pen.line((mid_x,mid_y,min(right,mid_x+1),mid_y), fill=SILVER_INK)
    elif mark == "vertebra":
        # A transverse recess at the lower outside corner on a raised vertebra.
        if w >= 2:
            pen.point((right,bottom), fill=FINISH["silver"][2])
            if w >= 3:
                pen.point((x,bottom), fill=FINISH["silver"][2])
    elif mark in ("seam", "filter", "grip"):
        color = GUNMETAL if material in ("carbon", "plate") else CHANNEL
        if mark == "filter":
            for col in range(x, end_x, 2):
                pen.point((col, mid_y), fill=color)
        elif mark == "grip":
            for row in range(y, end_y, 2):
                pen.line((x,row,min(right,x+1),row), fill=color)
        else:
            pen.line((mid_x,y,mid_x,mid_y), fill=color)
    elif mark in ("solar", "coil", "bus", "fiber", "sensor"):
        if mark == "solar":
            pen.line((x,bottom,right,bottom), fill=BRASS_INK)
            if h >= 3:
                pen.line((x,y,right,y), fill=GUNMETAL)
        elif mark == "coil":
            pen.line((mid_x,y,right,y), fill=BRASS_INK)
            pen.line((right,y,right,bottom), fill=BRASS_INK)
            if w >= 2 and h >= 2:
                pen.point((mid_x,bottom), fill=BRASS_INK)
        elif mark == "bus":
            pen.point((x, mid_y), fill=BRASS_INK)
            if w >= 3:
                pen.point((right, mid_y), fill=BRASS_INK)
        elif mark == "fiber":
            pen.line((mid_x,y,mid_x,bottom), fill=GUNMETAL)
            if h >= 3:
                pen.point((mid_x,bottom), fill=SILVER_INK)
        else:
            pen.point((mid_x,mid_y), fill="#3C5A55")
    else:
        raise ValueError(f"Unknown paint mark: {mark}")


def face_tile(name, material, face, physical, chart, rect, emitting, marks, seed):
    x, y, right, bottom = rect
    width, height = right-x, bottom-y
    du, dv = chart["uv_size"]
    part, side, left = canonical_part(name, face)
    flip_u = (du < 0) != (left and face in MIRRORED_FACES)
    flip_v = dv < 0
    marks = (marks,) if isinstance(marks, str) else marks

    # Keep partial edge texels flat, so a painted mark always spans a full pixel.
    full_w = min(width, int(math.floor(abs(du) + EPSILON)))
    full_h = min(height, int(math.floor(abs(dv) + EPSILON)))
    x0 = width-full_w if flip_u else 0
    y0 = height-full_h if flip_v else 0
    box = x0,y0,x0+full_w,y0+full_h

    if material == "carbon":
        tile = carbon_tile((width,height), box, part, side, seed)
    else:
        fill = LENS if material == "glow" else FINISH[material][{"up":3,"down":4}.get(side,0)]
        tile = Image.new("RGBA", (width,height), fill)
        if material in FINISH and part != "visor_oronasal_facet" and full_w >= 3 and full_h >= 3:
            pen = ImageDraw.Draw(tile)
            if y0 == 0:
                pen.line((x0,0,box[2]-2,0), fill=FINISH[material][1])
            if box[3] == height:
                pen.line((x0+1,height-1,box[2]-1,height-1), fill=FINISH[material][2])

    pen = ImageDraw.Draw(tile)
    for mark in marks:
        if mark == "optic" and not emitting:
            # The optical camouflage plate carries an unlit coated lens.
            mark = "sensor"
        paint_mark(pen, mark, box, material)
    light = Image.new("RGBA",(width,height))
    if emitting:
        # Coordinates are local to this face before its left/right UV reflection.
        # The four patterns match the visor strip, chest cell, wrist LCD and
        # single-cell indicators. Only their alpha enters the fullbright pass.
        ink = ImageDraw.Draw(light)
        if "optic" in marks:
            if full_w < 2:
                raise ValueError(f"{name}/{face}: visor requires a horizontal light strip")
            ink.line((x0,y0,box[2]-1,y0),fill=LIT)
        elif "core" in marks:
            if min(full_w,full_h) < 2:
                raise ValueError(f"{name}/{face}: chest cell needs at least 2x2 texels")
            core_h = min(3,full_h)
            u,v = x0+(full_w-2)//2,y0+(full_h-core_h)//2
            ink.rectangle((u,v,u+1,v+core_h-1), fill=LIT)
        elif "terminal" in marks:
            if full_w < 2 or full_h < 1:
                raise ValueError(f"{name}/{face}: terminal needs a 2x1 display")
            # Leave the inboard bottom row of the 3x2 case for its dark bezel.
            u = x0+(full_w-2)//2
            ink.line((u,y0,u+1,y0), fill=LIT)
        else:
            ink.point((x0+(full_w-1)//2,y0+(full_h-1)//2),fill=LIT)
        # The albedo represents the coated lens in ambient light. Runtime
        # energisation adds the bright texels from the independent glow map.
        lens_tint = "#479975" if "optic" in marks else "#498D70"
        tile.paste(lens_tint, (0,0,width,height), light.getchannel("A"))

    if flip_u:
        tile, light = ImageOps.mirror(tile), ImageOps.mirror(light)
    if flip_v:
        tile, light = ImageOps.flip(tile), ImageOps.flip(light)
    return tile, light


def render_page(bones, materials_path, seed):
    materials = read_materials(materials_path)
    base = Image.new("RGBA", ATLAS)
    glow = Image.new("RGBA", ATLAS)
    occupied = Image.new("L", ATLAS)
    count = 0
    for bone in bones:
        for cube in bone.get("cubes", []):
            if min(cube["size"]) < 1 - EPSILON:
                raise ValueError(f"{bone['name']}: cube below the one-unit minimum")
            key = cube_key(bone["name"], cube["origin"], cube["size"])
            if key not in materials:
                raise ValueError(f"Missing material row: {key}")
            name, mat, emits, decals = materials[key]
            uv = cube["uv"]
            if not isinstance(uv, dict) or uv.keys() - FACE_AXES.keys() or (emits | decals.keys()) - uv.keys():
                raise ValueError(f"{name} requires per-face UVs for all emitting faces")
            for face, chart in uv.items():
                physical = face_size(cube, face)
                check_density(chart, physical, f"{name}/{face}")
                rect = bounds(chart)
                x, y, x2, y2 = rect
                if not (0 <= x < x2 <= ATLAS[0] and 0 <= y < y2 <= ATLAS[1]):
                    raise ValueError(f"UV outside atlas: {name}/{face} {rect}")
                if occupied.crop(rect).getbbox():
                    raise ValueError(f"UV overlap: {name}/{face}")
                occupied.paste(255, rect)
                tile, light = face_tile(name, mat, face, physical, chart, rect,
                                        face in emits, decals.get(face, ()), seed)
                base.paste(tile, rect)
                if face in emits:
                    glow.paste(light, rect)
                count += 1
    return base, glow, count


def find_map(geo_path, explicit_path, filename):
    if explicit_path is not None:
        return Path(explicit_path)
    sibling = geo_path.with_name(filename)
    return sibling if sibling.is_file() else Path(__file__).with_name(filename)


def generate_pages(geo_path, materials_path=None, seed=73, module_materials_path=None):
    geo_path = Path(geo_path)
    models = json.loads(geo_path.read_bytes())["minecraft:geometry"]
    if len(models) != 1:
        raise ValueError("Supply one geometry per input file")
    desc = models[0]["description"]
    if (desc["texture_width"], desc["texture_height"]) != ATLAS:
        raise ValueError("Expected two 128x128 UV pages")
    groups = {"nano_suit": [], "nano_modules": []}
    for bone in models[0]["bones"]:
        if bone.get("cubes"):
            groups["nano_modules" if bone["name"].startswith("module_") else "nano_suit"].append(bone)
    maps = {"nano_suit": materials_path, "nano_modules": module_materials_path}
    return {page:render_page(bones, find_map(geo_path, maps[page], page+"_materials.json"), seed)
            for page, bones in groups.items() if bones}


def generate(geo_path, materials_path, seed=73):
    """Render only the suit page for a base-only geometry."""
    return generate_pages(geo_path, materials_path, seed)["nano_suit"]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("geo", type=Path, help="The geometry with vanilla-density per-face UVs")
    parser.add_argument("--materials", type=Path, help="Override nano_suit_materials.json")
    parser.add_argument("--module-materials", type=Path, help="Override nano_modules_materials.json")
    parser.add_argument("--out-dir", type=Path, default=Path.cwd())
    parser.add_argument("--seed", type=int, default=73, help="Seed for forged carbon chips")
    args = parser.parse_args()
    pages = generate_pages(args.geo, args.materials, args.seed, args.module_materials)
    args.out_dir.mkdir(parents=True, exist_ok=True)
    for page, (base, glow, faces) in pages.items():
        base.save(args.out_dir/(page+".png"))
        glow.save(args.out_dir/(page+"_glow.png"))
        print(f"{page}: {faces} faces, {base.getchannel('A').histogram()[255]} painted texels, "
              f"{glow.getchannel('A').histogram()[255]} emissive texels")


if __name__ == "__main__":
    main()
