"""Bake the two-page nano suit into native armour quads and dye/emission masks."""
import argparse
from collections import defaultdict
import json
import math
from pathlib import Path
import sys

from PIL import Image, ImageChops, ImageColor, ImageOps

from generate_nano_textures import BRASS_INK, BRASS_RECESS, FINISH, generate_pages

SOURCE = Path(__file__).resolve().parent
ROOT = SOURCE.parents[3]
sys.path.insert(0, str(ROOT / "tools"))
from gen_suit_armor import GECKO_BONES, PIVOT, gecko_quads, rotate_about

SLOTS = ("helmet", "chestplate", "leggings", "boots")
FACES = ("north", "south", "east", "west", "up", "down")
HOOD = "module_sealed_helmet"


def exported_elements(bb):
    groups = {group["uuid"]: group for group in bb["groups"]}
    elements = {element["uuid"]: element for element in bb["elements"]}

    def visit(node, chain):
        if isinstance(node, dict):
            group = groups[node["uuid"]]
            if group.get("export", True):
                for child in node["children"]:
                    yield from visit(child, chain + [group])
        else:
            element = elements[node]
            if element.get("export", True):
                yield element, chain

    for node in bb["outliner"]:
        yield from visit(node, [])


def native_quad(vertices, transforms, limb):
    px, py, pz = PIVOT[limb]
    coordinates = []
    for point, u, v in vertices:
        for rot, origin in transforms:
            point = rotate_about(point, rot, origin)
        x, y, z = point
        coordinates.append((-x - px, 24 - y - py, z - pz, u, v))
    # The Blockbench face is wound inward. Reverse positions and UVs together.
    a, b, c, d = coordinates
    ux, uy, uz = (d[i] - a[i] for i in range(3))
    vx, vy, vz = (c[i] - a[i] for i in range(3))
    normal = (uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx)
    length = math.hypot(*normal)
    return {"v": [round(value, 7) for vertex in (a, d, c, b) for value in vertex],
            "n": [round(value / length, 7) for value in normal]}


def bake(assets):
    bb = json.loads((SOURCE / "nano_suit_pure.bbmodel").read_bytes())
    mounts = json.loads((SOURCE / "module_bindings.json").read_bytes())
    by_bone = {row["bone"] + suffix: row for row in mounts["modules"] for suffix in ("", "_mk2", "_glow")}
    by_bone.update({key: {"bone": HOOD, "part": "head"} for key in (HOOD, HOOD + "_glow")})
    pages = generate_pages(SOURCE / "nano_suit_pure.geo.json")
    base, glow, _ = pages["nano_suit"]
    casing, lamps, _ = pages["nano_modules"]
    size = base.size
    housing = casing.copy()
    housing.paste((34, 50, 42, 255), (0, 0, *size), lamps.getchannel("A"))
    base_parts = {slot: defaultdict(list) for slot in SLOTS}
    used = {slot: Image.new("L", size) for slot in SLOTS}
    modules = defaultdict(list)
    for cube, chain in exported_elements(bb):
        owner = chain[-1]["name"]
        mount = by_bone.get(owner)
        slot, limb = next(GECKO_BONES[g["name"]] for g in reversed(chain) if g["name"] in GECKO_BONES)
        if mount:
            limb = mount["part"]
        elif limb.endswith("_arm") and "metacarpal" in cube["name"]:
            limb = limb.replace("_arm", "_hand")
        transforms = [(g["rotation"], g["origin"]) for g in [cube, *reversed(chain)] if any(g.get("rotation", ()))]
        faces = {face: cube["faces"][face] for face in FACES if cube["faces"].get(face, {}).get("texture") is not None}
        for face, (vertices, _) in zip(faces.values(), gecko_quads(dict(cube, faces=faces), *size), strict=True):
            u, v, u2, v2 = face["uv"]
            rect = (math.floor(min(u, u2) + 1e-7), math.floor(min(v, v2) + 1e-7),
                    math.ceil(max(u, u2) - 1e-7), math.ceil(max(v, v2) - 1e-7))
            quad = native_quad(vertices, transforms, limb)
            if not mount:
                base_parts[slot][limb].append(quad)
                used[slot].paste(255, rect)
            elif owner.endswith("_glow"):
                modules[mount["bone"]].append(quad)
                if lamps.getchannel("A").crop(rect).getbbox():
                    modules[owner].append(quad)
            else:
                modules[owner].append(quad)

    models = assets / "armor_models"
    textures = assets / "textures/models/armor"
    for path in (models, textures):
        path.mkdir(parents=True, exist_ok=True)
    for name, parts in [("nano_modules", modules), *(("nano_" + slot, parts) for slot, parts in base_parts.items())]:
        model = {"format": "mio_icif:baked_armor/1", "texture_size": list(size), "source": "nano_suit_pure.bbmodel", "parts": parts}
        (models / (name + ".json")).write_text(json.dumps(model, separators=(",", ":")), encoding="utf-8")
    (models / "nano_module_bindings.json").write_text(json.dumps(mounts, separators=(",", ":")), encoding="utf-8")
    housing.save(textures / "nano_modules.png", optimize=True)
    lamps.save(textures / "nano_modules_glow.png", optimize=True)
    brass = {ImageColor.getrgb(c) for c in (*FINISH["brass"], BRASS_INK, BRASS_RECESS)}
    accent = glow.getchannel("A").copy()
    for x in range(size[0]):
        for y in range(size[1]):
            r, g, b, a = base.getpixel((x, y))
            if a and (r, g, b) in brass:
                accent.putpixel((x, y), a)
    luma = ImageOps.grayscale(base)
    for slot, mask in used.items():
        normal = base.copy()
        normal.putalpha(mask)
        emissive = glow.copy()
        emissive.putalpha(ImageChops.darker(glow.getchannel("A"), mask))
        trim = ImageChops.darker(accent, mask)
        plating = ImageChops.subtract(mask, trim)
        for suffix, image in (("", normal), ("_glow", emissive), ("_plating", Image.merge("RGBA", (luma, luma, luma, plating))),
                              ("_accent", Image.merge("RGBA", (luma, luma, luma, trim)))):
            image.save(textures / ("nano_" + slot + suffix + ".png"), optimize=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--assets", type=Path, default=ROOT / "src/main/resources/assets/mio_icif")
    bake(parser.parse_args().assets)
