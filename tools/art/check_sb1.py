#!/usr/bin/env python3
"""Checks the SlashBlade blade art of batch SB1 (docs/art/batches/SB1.md).

For each blade folder under assets/mio_icif/model/slashblade/<blade>/ (or the blades named on the command line) it
checks what can be checked without the game:

- model.obj, texture.png and charge.png exist;
- the OBJ parses the way SlashBlade's WavefrontObject reads it: faces of 3 or 4 vertices, each with a UV, object
  names of letters, digits and underscores only;
- all main objects of section 2.1 exist; every state object has a _luminous partner when the blade has any
  luminous object (section 2.2);
- the origin and grip match the template (blade tip toward -X, the grip end at x about 41.74) and the length is
  inside the range section 6 gives the blade;
- effect is a shell larger than the sheath on every side, not a copy of it, of at most 60 vertices (section 2.3);
- the item_* objects stay inside the outline of the default blade's item objects (section 2.5);
- vertex budgets (section 3.2): all objects <= 3000, item_* together <= 1200;
- textures: power-of-two sizes, charge.png 64x64, alpha only 0 or 255, UVs inside 0..1 except effect;
- luminous objects sample bright texels (section 2.2: additive, dark texels hardly light up) - a warning.

Exit status 1 when an error is found. Warnings do not change the status.
    python3 tools/art/check_sb1.py              # every blade folder
    python3 tools/art/check_sb1.py nano         # one blade
"""
from __future__ import annotations

import re
import struct
import sys
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MODELS = ROOT / "src/main/resources/assets/mio_icif/model/slashblade"
BLADES = ["nano", "quantum", "singularity"]
MAIN = ["blade", "blade_damaged", "blade_fragment", "sheath", "effect", "item_blade", "item_damaged", "item_bladens"]
LUMINOUS_STATES = ["blade", "blade_damaged", "blade_fragment", "item_blade", "item_damaged", "item_bladens"]
ITEMS = ["item_blade", "item_damaged", "item_bladens"]
NAME = re.compile(r"^[A-Za-z0-9_]+$")
GRIP_X = 41.74
TEMPLATE_LENGTH = GRIP_X - (-290.96)  # the template blade, grip end to tip
# Section 6: the length of each blade against the template (a little slack for the tip's shape).
LENGTH = {"nano": (0.97, 1.08), "quantum": (1.02, 1.13), "singularity": (1.07, 1.17)}
# The item_* frame: the outline of SlashBlade's default blade item objects (model/blade.obj).
FRAME_X = (-88.0, 72.0)
FRAME_Y = (-84.0, 81.0)
BRIGHT = 150


def read_png(path: Path):
    """Width, height and the RGBA rows (None for palette or 16-bit images)."""
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    pos, idat, width, height, depth, colour, interlace = 8, b"", 0, 0, 0, 0, 0
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        kind, chunk = data[pos + 4:pos + 8], data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b"IHDR":
            width, height, depth, colour = struct.unpack(">IIBB", chunk[:10])
            interlace = chunk[12]
        elif kind == b"IDAT":
            idat += chunk
    channels = {6: 4, 2: 3, 4: 2, 0: 1}.get(colour)
    if depth != 8 or channels is None or interlace:
        return width, height, None
    raw, stride = zlib.decompress(idat), width * channels
    previous, rows, at = bytearray(stride), [], 0
    for _ in range(height):
        kind, line = raw[at], bytearray(raw[at + 1:at + 1 + stride])
        at += 1 + stride
        for x in range(stride):
            left = line[x - channels] if x >= channels else 0
            up, corner = previous[x], previous[x - channels] if x >= channels else 0
            if kind == 1:
                line[x] = (line[x] + left) & 255
            elif kind == 2:
                line[x] = (line[x] + up) & 255
            elif kind == 3:
                line[x] = (line[x] + (left + up) // 2) & 255
            elif kind == 4:
                p = left + up - corner
                pa, pb, pc = abs(p - left), abs(p - up), abs(p - corner)
                line[x] = (line[x] + (left if pa <= pb and pa <= pc else up if pb <= pc else corner)) & 255
        row = []
        for x in range(width):
            px = line[x * channels:(x + 1) * channels]
            if channels == 4:
                row.append(tuple(px))
            elif channels == 3:
                row.append((px[0], px[1], px[2], 255))
            elif channels == 2:
                row.append((px[0], px[0], px[0], px[1]))
            else:
                row.append((px[0], px[0], px[0], 255))
        rows.append(row)
        previous = line
    return width, height, rows


def read_obj(path: Path, errors: list[str]):
    vertices, uvs, objects, current = [], [], {}, None
    for number, line in enumerate(path.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
        parts = line.split()
        if not parts or parts[0].startswith("#"):
            continue
        if parts[0] == "v":
            vertices.append(tuple(map(float, parts[1:4])))
        elif parts[0] == "vt":
            uvs.append(tuple(map(float, parts[1:3])))
        elif parts[0] in ("o", "g"):
            current = parts[1] if len(parts) > 1 else ""
            if not NAME.match(current):
                errors.append(f"line {number}: object name {current!r} is not letters, digits and underscores")
            objects.setdefault(current, {"v": set(), "uv": set(), "faces": []})
        elif parts[0] == "f":
            if current is None:
                errors.append(f"line {number}: face before any object")
                continue
            if len(parts) - 1 not in (3, 4):
                errors.append(f"line {number}: face with {len(parts) - 1} vertices (only 3 or 4 are read)")
            face = []
            for corner in parts[1:]:
                fields = corner.split("/")
                if len(fields) < 2 or not fields[1]:
                    errors.append(f"line {number}: face without UV in {current}")
                    face = None
                    break
                objects[current]["v"].add(int(fields[0]) - 1)
                objects[current]["uv"].add(int(fields[1]) - 1)
                face.append(int(fields[1]) - 1)
            if face:
                objects[current]["faces"].append(face)
    return vertices, uvs, objects


def bounds(vertices, indices):
    points = [vertices[i] for i in indices]
    return [(min(p[a] for p in points), max(p[a] for p in points)) for a in range(3)] if points else None


def brightness(rows, width, height, uvs, faces):
    """Mean luminance of the opaque texels the faces cover (None when they cover none)."""
    total, count = 0.0, 0
    for face in faces:
        tri = [uvs[i] for i in face]
        for a, b, c in ([tri[0], tri[1], tri[2]],) + (([tri[0], tri[2], tri[3]],) if len(tri) == 4 else ()):
            pts = [(u * width, (1 - v) * height) for u, v in (a, b, c)]
            x0, x1 = int(max(0, min(p[0] for p in pts))), int(min(width - 1, max(p[0] for p in pts)))
            y0, y1 = int(max(0, min(p[1] for p in pts))), int(min(height - 1, max(p[1] for p in pts)))
            (ax, ay), (bx, by), (cx, cy) = pts
            area = (bx - ax) * (cy - ay) - (cx - ax) * (by - ay)
            hit = False
            for y in range(y0, y1 + 1):
                for x in range(x0, x1 + 1):
                    px, py = x + 0.5, y + 0.5
                    if area:
                        w1 = ((bx - px) * (cy - py) - (cx - px) * (by - py)) / area
                        w2 = ((cx - px) * (ay - py) - (ax - px) * (cy - py)) / area
                        if w1 < -0.01 or w2 < -0.01 or w1 + w2 > 1.01:
                            continue
                    r, g, bl, al = rows[y][x]
                    if al:
                        total += 0.299 * r + 0.587 * g + 0.114 * bl
                        count += 1
                        hit = True
            if not hit:  # a thin sliver: sample its corners
                for px, py in pts:
                    x, y = min(width - 1, max(0, int(px))), min(height - 1, max(0, int(py)))
                    r, g, bl, al = rows[y][x]
                    if al:
                        total += 0.299 * r + 0.587 * g + 0.114 * bl
                        count += 1
    return total / count if count else None


def check(blade: str) -> tuple[list[str], list[str]]:
    folder, errors, warnings = MODELS / blade, [], []
    if blade not in BLADES:
        errors.append(f"{blade} is not one of {', '.join(BLADES)}")
    for name in ["model.obj", "texture.png", "charge.png"]:
        if not (folder / name).is_file():
            errors.append(f"{name} is missing")
    if not (folder / "model.obj").is_file():
        return errors, warnings
    vertices, uvs, objects = read_obj(folder / "model.obj", errors)
    for name in MAIN:
        if name not in objects:
            errors.append(f"object {name} is missing")
    luminous = [name for name in objects if name.endswith("_luminous")]
    if luminous:
        for state in LUMINOUS_STATES:
            if state in objects and state + "_luminous" not in objects:
                errors.append(f"{state} has no {state}_luminous although the blade has {', '.join(luminous)}")
    else:
        warnings.append("no _luminous objects (section 6 gives every blade luminous parts)")
    for name in luminous:
        if name[:-len("_luminous")] not in objects:
            errors.append(f"{name} has no base object {name[:-len('_luminous')]}")
        if name == "effect_luminous":
            errors.append("effect_luminous is never drawn")

    if "blade" in objects:
        box = bounds(vertices, objects["blade"]["v"])
        if box and abs(box[0][1] - GRIP_X) > 3.0:
            errors.append(f"blade ends at x {box[0][1]:.2f}, the template grip end is {GRIP_X}: origin moved")
        if box and box[0][0] > -200:
            errors.append(f"blade tip at x {box[0][0]:.2f}: the blade must point toward -X")
        if box and blade in LENGTH:
            ratio = (box[0][1] - box[0][0]) / TEMPLATE_LENGTH
            low, high = LENGTH[blade]
            if not low <= ratio <= high:
                errors.append(f"blade is {ratio:.2f} of the template length, section 6 gives {low:.2f}..{high:.2f}")
    if "effect" in objects and "sheath" in objects:
        effect, sheath = objects["effect"]["v"], objects["sheath"]["v"]
        eb, sb = bounds(vertices, effect), bounds(vertices, sheath)
        if {vertices[i] for i in effect} == {vertices[i] for i in sheath}:
            errors.append("effect is a copy of sheath; it must be a larger shell (section 2.3)")
        elif eb and sb:
            for axis, label in ((1, "y"), (2, "z")):
                if eb[axis][1] - sb[axis][1] < 0.8 or sb[axis][0] - eb[axis][0] < 0.8:
                    errors.append(f"effect {label} {eb[axis][0]:.2f}..{eb[axis][1]:.2f} is not 0.8 or more outside the "
                                  f"sheath {label} {sb[axis][0]:.2f}..{sb[axis][1]:.2f}")
        if len(effect) > 60:
            errors.append(f"effect has {len(effect)} vertices, at most 60")
    if "sheath" in objects and "blade" in objects:
        sb, bb = bounds(vertices, objects["sheath"]["v"]), bounds(vertices, objects["blade"]["v"])
        if sb and bb and sb[0][0] > bb[0][0] - 2:
            warnings.append(f"the sheath ends at x {sb[0][0]:.1f}, the blade tip is at {bb[0][0]:.1f}: does it fit?")
    for name in ITEMS + [n + "_luminous" for n in ITEMS]:
        if name in objects:
            box = bounds(vertices, objects[name]["v"])
            if box and (box[0][0] < FRAME_X[0] or box[0][1] > FRAME_X[1]
                        or box[1][0] < FRAME_Y[0] or box[1][1] > FRAME_Y[1]):
                errors.append(f"{name} x {box[0][0]:.1f}..{box[0][1]:.1f} y {box[1][0]:.1f}..{box[1][1]:.1f} "
                              f"leaves the frame x {FRAME_X} y {FRAME_Y} of the default blade")
            if box and box[2][1] < 5.0:
                warnings.append(f"{name} stays behind z 5 (front at {box[2][1]:.1f}); the durability plate may hide it")
    total = len({i for o in objects.values() for i in o["v"]})
    items = len({i for n in ITEMS if n in objects for i in objects[n]["v"]})
    if total > 3000:
        errors.append(f"{total} vertices in all, at most 3000")
    if items > 1200:
        errors.append(f"item_* objects use {items} vertices, at most 1200")
    for name, data in objects.items():
        if name == "effect":
            continue
        for i in data["uv"]:
            u, v = uvs[i]
            if not (-0.001 <= u <= 1.001 and -0.001 <= v <= 1.001):
                errors.append(f"{name} has a UV outside 0..1 ({u:.3f}, {v:.3f})")
                break

    texture = None
    for name in ["texture.png", "charge.png"]:
        path = folder / name
        if not path.is_file():
            continue
        try:
            width, height, rows = read_png(path)
        except (ValueError, zlib.error) as exception:
            errors.append(f"{name}: {exception}")
            continue
        if width & (width - 1) or height & (height - 1):
            errors.append(f"{name} is {width}x{height}, not a power of two")
        if name == "charge.png" and (width, height) != (64, 64):
            errors.append(f"charge.png is {width}x{height}, must be 64x64")
        if name != "charge.png" and max(width, height) > 512:
            errors.append(f"{name} is {width}x{height}, at most 256x512")
        if rows is None:
            warnings.append(f"{name} is not 8-bit RGBA/RGB/grey or is interlaced: alpha not checked")
            continue
        alphas = {px[3] for row in rows for px in row}
        if not alphas <= {0, 255}:
            errors.append(f"{name} has semi-transparent pixels (alpha {sorted(alphas - {0, 255})[:5]} ...)")
        if name == "texture.png":
            texture = (width, height, rows)
    if texture:
        for name in luminous:
            mean = brightness(texture[2], texture[0], texture[1], uvs, objects[name]["faces"])
            if mean is None:
                warnings.append(f"{name} samples no opaque texel: it will not show")
            elif mean < BRIGHT:
                warnings.append(f"{name} samples texels of mean brightness {mean:.0f}, under {BRIGHT}: "
                                "additive light hardly shows (section 2.2)")
    summary = f"{len(objects)} objects, {total} vertices (item_* {items}, effect {len(objects.get('effect', {}).get('v', ()))})"
    warnings.insert(0, summary)
    return errors, warnings


def main() -> int:
    blades = sys.argv[1:] or ([p.name for p in sorted(MODELS.iterdir()) if p.is_dir()] if MODELS.is_dir() else [])
    if not blades:
        print(f"no blade folders under {MODELS}")
        return 1
    failed = False
    for blade in blades:
        errors, warnings = check(blade)
        print(f"== {blade}: {'OK' if not errors else str(len(errors)) + ' error(s)'}")
        for line in errors:
            print("  ERROR " + line)
        for line in warnings:
            print("  note  " + line)
        failed |= bool(errors)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
