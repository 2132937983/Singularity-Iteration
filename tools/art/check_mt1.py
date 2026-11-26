#!/usr/bin/env python3
"""Check batch MT1 (molecular transformer block and its inventory model), docs/art/batches/MT1.md.

Block models (block_molecular_transformer.json and _on.json, a neoforge:composite model's children counted together):
  - at most 40 elements and 160 faces each;
  - no hidden face (flat against another opaque element's facing face, or inside an opaque closed box);
  - every face on the block's outer boundary carries "cullface";
  - every solid box is at least 1 unit along each axis (a 0-thick plane is allowed);
  - rotations supported by the Minecraft 1.21.1 block model loader;
  - the two models have the same elements by name and position (the working model only changes textures and light);
  - every texture a model uses exists under textures/block/producer/molecular_transformer/.
Textures: 8-bit RGBA, not interlaced, no pure black; alpha only 0 or 255, except files whose name contains "glass";
  The inventory item inherits the idle block. The chosen design is the four-corner chamber in cyan (0.1.11.212).
The blockstate, the item model and the Java code are not part of the batch; the script checks that they are unchanged in
the parts the batch relies on.

Usage, from the repository root:  python3 tools/art/check_mt1.py [--list]
Prints OK, or one line per problem and exits 1. Needs Pillow.
"""
import json
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src/main/resources/assets/mio_icif")
MODELS = [os.path.join(ASSETS, "models/block/producer", n)
          for n in ("block_molecular_transformer.json", "block_molecular_transformer_on.json")]
TEX_DIR = os.path.join(ASSETS, "textures/block/producer/molecular_transformer")
TEX_PREFIX = "mio_icif:block/producer/molecular_transformer/"
ITEM_MODEL = os.path.join(ASSETS, "models/item/producer/block_molecular_transformer.json")
BBMODELS = [os.path.join(ROOT, "tools/models", n)
            for n in ("molecular_transformer.bbmodel", "molecular_transformer_on.bbmodel")]
PREVIEW = os.path.join(ROOT, "docs/art/previews/MT1-quadrupole.png")
MAX_ELEMENTS, MAX_FACES = 40, 160

OPPOSITE = {"down": "up", "up": "down", "north": "south", "south": "north", "west": "east", "east": "west"}
AXIS = {"down": (1, 0), "up": (1, 1), "north": (2, 0), "south": (2, 1), "west": (0, 0), "east": (0, 1)}


def elements(model):
    found = [(e, model.get("render_type")) for e in model.get("elements", [])]
    for child in model.get("children", {}).values():
        found += [(e, child.get("render_type", model.get("render_type"))) for e in child.get("elements", [])]
    return found


def textures(model):
    tex = dict(model.get("textures", {}))
    for child in model.get("children", {}).values():
        tex.update(child.get("textures", {}))
    return tex


def rotated(e):
    return bool(e.get("rotation")) and e["rotation"].get("angle", 0) != 0


def solid(render_type):
    return render_type is None or "solid" in str(render_type)


def hidden(face, e, others):
    if rotated(e):
        return False
    axis, side = AXIS[face]
    a, b = e["from"], e["to"]
    plane = b[axis] if side else a[axis]
    across = [i for i in range(3) if i != axis]
    for other, rt in others:
        if other is e or rotated(other) or not solid(rt):
            continue
        oa, ob = other["from"], other["to"]
        if not all(oa[i] <= a[i] and ob[i] >= b[i] for i in across):
            continue
        faces = other.get("faces", {})
        touching = (oa[axis] == plane) if side else (ob[axis] == plane)
        if (touching and OPPOSITE[face] in faces) or (oa[axis] < plane < ob[axis] and len(faces) == 6):
            return True
    return False


def outer(face, e):
    if rotated(e):
        return False
    axis, side = AXIS[face]
    return (e["to"][axis] == 16) if side else (e["from"][axis] == 0)


def check_model(path, listing, *, texture_dir=TEX_DIR, texture_prefix=TEX_PREFIX,
                max_elements=MAX_ELEMENTS, max_faces=MAX_FACES):
    name = os.path.basename(path)
    problems, notes = [], []
    if not os.path.exists(path):
        return [f"{name}: missing"], None
    model = json.load(open(path, encoding="utf-8"))
    found = elements(model)
    faces = 0
    for e, _ in found:
        label = f"{name} '{e.get('name', '?')}'"
        size = [e["to"][i] - e["from"][i] for i in range(3)]
        if any(s < 0 for s in size):
            problems.append(f"{label}: from > to")
        thin = [s for s in size if 0 < s < 1]
        if thin and 0 not in size:
            problems.append(f"{label}: thinner than 1 unit {size} (use a 0-thick plane or a texture)")
        rot = e.get("rotation")
        if rot and rot.get("angle") not in (-45, -22.5, 0, 22.5, 45):
            problems.append(f"{label}: unsupported block rotation {rot.get('angle')}")
        for face, data in e.get("faces", {}).items():
            faces += 1
            if hidden(face, e, found):
                problems.append(f"{label}: hidden face {face}")
            if outer(face, e) and "cullface" not in data:
                problems.append(f"{label}: outer face {face} has no cullface")
        notes.append(f"    {e.get('name', '?'):24} {e['from']}..{e['to']} faces={len(e.get('faces', {}))}")
    if len(found) > max_elements:
        problems.append(f"{name}: {len(found)} elements, at most {max_elements}")
    if faces > max_faces:
        problems.append(f"{name}: {faces} faces, at most {max_faces}")
    for key, ref in textures(model).items():
        if ref.startswith("#"):
            continue
        if not ref.startswith(texture_prefix):
            problems.append(f"{name}: texture '{key}' = {ref}, expected under {texture_prefix}")
            continue
        if not os.path.exists(os.path.join(texture_dir, ref[len(texture_prefix):] + ".png")):
            problems.append(f"{name}: texture '{key}' = {ref} does not exist")
    print(f"{name:42} elements {len(found):>3}  faces {faces:>4}")
    if listing:
        print("\n".join(notes))
    return problems, [(e.get("name"), e["from"], e["to"]) for e, _ in found]


def check_png(path):
    rel = os.path.relpath(path, ROOT)
    problems = []
    im = Image.open(path)
    if im.mode != "RGBA":
        problems.append(f"{rel}: mode {im.mode}, expected RGBA")
        im = im.convert("RGBA")
    if im.info.get("interlace"):
        problems.append(f"{rel}: interlaced PNG")
    w, h = im.size
    glass = "glass" in os.path.basename(path)
    px = im.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a not in (0, 255) and not glass:
                problems.append(f"{rel}: partial alpha {a} at ({x},{y})")
            if a and (r, g, b) == (0, 0, 0):
                problems.append(f"{rel}: pure black at ({x},{y})")
    seen, short = set(), []
    for p in problems:
        kind = p.split(" at ")[0]
        if kind not in seen:
            seen.add(kind)
            short.append(p)
    return short


def pngs(folder):
    out = []
    for base, _, files in os.walk(folder):
        out += [os.path.relpath(os.path.join(base, f), folder) for f in files if f.endswith(".png")]
    return sorted(out)


def check_fixed():
    """The parts outside the batch that the batch relies on."""
    problems = []
    bs = json.load(open(os.path.join(ASSETS, "blockstates/producer/block_molecular_transformer.json"), encoding="utf-8"))
    want = {f"lit={l},facing={f}" for l in ("false", "true") for f in ("north", "south", "east", "west")}
    if set(bs.get("variants", {})) != want:
        problems.append("blockstate: variants changed (the batch must not touch the blockstate)")
    if not os.path.isfile(ITEM_MODEL):
        problems.append("item model is missing")
    else:
        with open(ITEM_MODEL, encoding="utf-8") as stream:
            item = json.load(stream)
        if (item.get("parent") != "mio_icif:block/producer/block_molecular_transformer"
                or "loader" in item or "elements" in item or item.get("overrides")):
            problems.append("inventory must directly inherit the idle block model")
    return problems


def main():
    listing = "--list" in sys.argv
    problems = []
    shapes = []
    for path in MODELS:
        p, shape = check_model(path, listing)
        problems += p
        shapes.append(shape)
    if all(shapes) and sorted(map(str, shapes[0])) != sorted(map(str, shapes[1])):
        problems.append("the idle and working models differ in elements (same names and positions expected)")
    if not os.path.isdir(TEX_DIR):
        problems.append(f"{os.path.relpath(TEX_DIR, ROOT)}: missing")
    else:
        for f in pngs(TEX_DIR):
            problems += check_png(os.path.join(TEX_DIR, f))
    for path in BBMODELS + [PREVIEW]:
        if not os.path.exists(path):
            problems.append(f"{os.path.relpath(path, ROOT)}: missing")
    problems += check_fixed()
    if problems:
        print("\n".join(problems))
        print(f"{len(problems)} problem(s)")
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
