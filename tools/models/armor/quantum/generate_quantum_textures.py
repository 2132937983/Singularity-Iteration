"""Generate the Quantum Armor's 128×128 albedo and emissive atlases with Pillow.

Run in the model authoring directory:

    python generate_quantum_textures.py

The Blockbench project's embedded PNG supplies the signed-off armour texels.
All add-on charts are drawn from their native face UVs and material records.
"""
import argparse
import base64
from io import BytesIO
import json
from math import ceil, floor
from pathlib import Path

from PIL import Image


SIZE = (128, 128)
WHITE = (228, 234, 240, 255)
RETURN = (194, 204, 216, 255)
SILVER = (166, 179, 194, 255)
GRAPHITE = (52, 62, 74, 255)
OBSIDIAN = (30, 35, 43, 255)
GLASS = (69, 101, 119, 255)
ICE = (76, 230, 255, 255)
CLEAR = (0, 0, 0, 0)
INK = {"w": WHITE, "s": RETURN, "t": SILVER, "m": GRAPHITE, "o": OBSIDIAN, "c": GLASS}

# Thin and square front plates are drawn independently at native resolution.
# The single 'c' is a lens: the albedo shows smoked glass, the emission pass
# lights the same texel. Each drawing is oriented in the face's own UV plane.
PLATES = {
    "ore_scanner":        ("sc", ("wm", "sc")),
    "grid_telemetry":     ("co", ("co", "st")),
    "entity_esp":         ("wc", ("ws", "oc")),
    "ballistic":          ("ct", ("ct", "mo")),
    "blast_warning":      ("tc", ("tc", "oo")),
    "behavior_predictor": ("cw", ("cw", "ws")),
    "holomap":            ("mc", ("ot", "cm")),
    "threat_sensor":      ("oc", ("oc", "ww")),
    "deflector":          ("wc", ("cw", "ss")),
    "smart_guidance":     ("cm", ("cm", "ts")),
    "resonance_field":    ("sc", ("mw", "co")),
    "toxin_filter":       ("mc", ("mm", "sc")),
    "self_repair":        ("cs", ("wc", "sm")),
    "hydrophobic":        ("ct", ("sw", "ct")),
    "kinetic_generator":  ("cm", ("tm", "cw")),
    "phase_jump_drive":   ("wc", ("so", "wc")),
    "phase_modulator":    ("cw", ("ws", "cw")),
    "coherence_capacitor":("sc", ("cs", "ot")),
    "entanglement_relay": ("ct", ("ct", "sm")),
    "compute_core":       ("oc", ("mc", "om")),
    "solar_array_1":      ("oc", ("oc", "om")),
    "solar_array_2":      ("mc", ("mc", "mt")),
    "solar_array_3":      ("tc", ("tc", "ts")),
    "quantum_bay_expander": ("sc", ("sc", "mt")),
}

FLIGHT_STYLES = {
    "rcs_shoulder", "rcs_waist", "rcs_calf", "hover_strip",
    "mach_pod", "mach_rail",
}


def footprint(chart):
    (u, v), (w, h) = chart["uv"], chart["uv_size"]
    return floor(min(u, u+w)), floor(min(v, v+h)), ceil(max(u, u+w)), ceil(max(v, v+h))


def drawing(style, size, mirror=False):
    size = tuple(size)
    if style == "graviton" and size == (3, 2):
        rows = ("tcs", "omo")
    elif style == "propulsion" and size == (3, 1):
        rows = ("mct",)
    else:
        thin, square = PLATES[style]
        if size == (2, 1):
            rows = (thin,)
        elif size == (1, 2):
            rows = tuple(thin)
        elif size == (2, 2):
            rows = square
        elif size == (1, 1):
            rows = ("c",)
        else:
            raise ValueError(f"{style}: no native drawing for {size}")
    return tuple(row[::-1] for row in rows) if mirror else rows


def module_lights(style, face, size, mirror=False):
    """Return native UV rectangles for the module body's unlit indicator."""
    rows = drawing(style, size, mirror)
    return {face: [[u, v, 1, 1] for v, row in enumerate(rows)
                   for u, symbol in enumerate(row) if symbol == "c"]}


def from_rows(rows):
    width, height = len(rows[0]), len(rows)
    pixels = bytes(channel for row in rows for ink in row for channel in INK[ink])
    return Image.frombytes("RGBA", (width, height), pixels)


def housing_color(face):
    return {"up": WHITE, "down": OBSIDIAN, "north": RETURN, "south": RETURN,
            "east": GRAPHITE, "west": GRAPHITE}[face]


def accessory_face(style, row, face, size):
    if face == row["display_face"]:
        return from_rows(drawing(style, size, row.get("mirror", False)))

    if style.startswith("solar_array_") and face == "up":
        # The top is a separate 2×1 coating. It identifies the core tier from
        # above even when a tracking camera cannot see the status window.
        rows = {"solar_array_1": "oo", "solar_array_2": "om", "solar_array_3": "mt"}
        strip = rows[style]
        return from_rows((strip[::-1] if row.get("mirror", False) else strip,))

    color = housing_color(face)
    if style in ("quantum_bay_expander", "compute_core", "holomap", "solar_array_1",
                 "solar_array_2", "solar_array_3") and face == "up":
        color = SILVER
    return Image.new("RGBA", size, color)


def flight_face(style, name, face, size):
    if style.startswith("rcs_"):
        color = GRAPHITE if face == "up" else OBSIDIAN
    elif style == "hover_strip":
        color = GRAPHITE if face in ("south", "up") else OBSIDIAN
    elif style == "mach_pod":
        if face == "south":
            rows = ("sw", "ww", "ws", "mo")
            if name.endswith("_right"):
                rows = tuple(row[::-1] for row in rows)
            return from_rows(rows)
        if face in ("west", "east"):
            return from_rows(("s", "w", "s", "o"))
        color = WHITE if face == "up" else (OBSIDIAN if face == "down" else RETURN)
    elif style == "mach_rail":
        color = SILVER if face in ("east", "west") else GRAPHITE
    else:
        raise ValueError(f"Unknown propulsion material {style}")
    return Image.new("RGBA", size, color)


def paint_addon(row, face, size):
    style = row["style"]
    if style in FLIGHT_STYLES:
        albedo = flight_face(style, row["name"], face, size)
    else:
        albedo = accessory_face(style, row, face, size)
    if albedo.size != size:
        raise ValueError(f'{row["name"]}/{face}: expected native chart size {size}, got {albedo.size}')

    emissive = Image.new("RGBA", size, CLEAR)
    lights = row.get("lights", {}).get(face, ())
    if face in row.get("emit", ()):
        lights = ((0, 0, *size),)
    for u, v, w, h in lights:
        if w <= 0 or h <= 0 or u < 0 or v < 0 or u+w > size[0] or v+h > size[1]:
            raise ValueError(f'{row["name"]}/{face}: lamp extends beyond its UV chart')
        box = u, v, u+w, v+h
        albedo.paste(ICE if style == "mach_rail" else GLASS, box)
        emissive.paste(ICE, box)
    return albedo, emissive


def original_emission(albedo):
    """Recover exactly the approved mask on the original emitting faces."""
    glow = Image.new("RGBA", albedo.size, CLEAR)
    glow.putdata([ICE if albedo.getpixel((x, y)) == ICE else CLEAR
                  for y in range(albedo.height) for x in range(albedo.width)])
    return glow


def generate(geo_path, bb_path, materials_path=None):
    geo_path = Path(geo_path)
    if materials_path is None:
        materials_path = geo_path.with_name("quantum_materials.json")
    materials = json.loads(Path(materials_path).read_bytes())["bones"]
    native = json.loads(Path(bb_path).read_bytes())
    texture, = (t for t in native["textures"] if t["name"] == "quantum_armor.png")
    payload = base64.b64decode(texture["source"].split(",", 1)[1])
    with Image.open(BytesIO(payload)) as stored:
        approved = stored.convert("RGBA")
    model, = json.loads(geo_path.read_bytes())["minecraft:geometry"]
    description = model["description"]
    if approved.size != SIZE or (description["texture_width"], description["texture_height"]) != SIZE:
        raise ValueError("The Quantum Armor rig uses a 128×128 atlas.")

    base = Image.new("RGBA", SIZE, CLEAR)
    glow = Image.new("RGBA", SIZE, CLEAR)
    for bone in model["bones"]:
        owner = bone["name"]
        cubes = bone.get("cubes", ())
        if not cubes:
            continue
        for cube, row in zip(cubes, materials[owner], strict=True):
            for face, chart in cube["uv"].items():
                rect = footprint(chart)
                size = (rect[2] - rect[0], rect[3] - rect[1])
                if owner.startswith("module_"):
                    paint, lamps = paint_addon(row, face, size)
                    glow.paste(lamps, rect)
                else:
                    paint = approved.crop(rect)
                    if owner.endswith("_glow") or (row["name"] == "face_keel" and face == "north"):
                        glow.paste(original_emission(paint), rect)
                base.paste(paint, rect)
    return base, glow


def main():
    root = Path(__file__).resolve().parent
    cli = argparse.ArgumentParser(description=__doc__)
    assets = root.parents[3] / "src/main/resources/assets/mio_icif"
    cli.add_argument("--geo", type=Path, default=assets/"geo/quantum_armor.geo.json")
    cli.add_argument("--bbmodel", type=Path, default=root/"quantum_armor.bbmodel")
    cli.add_argument("--materials", type=Path, default=root/"quantum_materials.json")
    cli.add_argument("--out-dir", type=Path, default=root)
    args = cli.parse_args()
    base, glow = generate(args.geo, args.bbmodel, args.materials)
    args.out_dir.mkdir(parents=True, exist_ok=True)
    base.save(args.out_dir/"quantum_armor.png")
    glow.save(args.out_dir/"quantum_armor_glow.png")


if __name__ == "__main__":
    main()
