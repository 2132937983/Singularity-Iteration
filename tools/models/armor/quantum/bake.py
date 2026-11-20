"""Bake the quantum suit and its individually selectable mounts for the SI armor renderer."""
import json
import sys
from collections import defaultdict
from pathlib import Path
from tempfile import TemporaryDirectory

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
ASSETS = ROOT / "src/main/resources/assets/mio_icif"
sys.path.insert(0, str(ROOT / "tools"))
from gen_suit_armor import convert_gecko
from gen_suit_palette import split, read_png, write_png
from generate_quantum_textures import generate


def save(path, obj):
    path.write_text(json.dumps(obj, separators=(",", ":"), allow_nan=False) + "\n", encoding="utf8")


def bounds(u, v):
    return tuple(round(f * 128, 3) for f in (min(u), min(v), max(u), max(v)))


def bake():
    bb = HERE / "quantum_armor.bbmodel"
    rig = json.loads(bb.read_bytes())
    bindings = json.loads((ASSETS / "armor_models/quantum_bindings.json").read_bytes())
    mounts = {m["bone"]: m for m in bindings["modules"]}
    sockets = {(s["part"], s["face"]): s["bone"] for s in bindings["flight"]["sockets"]}
    groups = {g["uuid"]: g["name"] for g in rig["groups"]}
    ancestry = {}

    def walk(nodes, path):
        for node in nodes:
            if isinstance(node, str):
                ancestry[node] = path
            else:
                walk(node["children"], path + [groups[node["uuid"]]])
    walk(rig["outliner"], [])
    faces = {}
    for e in rig["elements"]:
        chain = ancestry[e["uuid"]]
        for face, chart in e["faces"].items():
            u, v, x, y = chart["uv"]
            key = bounds((u / 128, x / 128), (v / 128, y / 128))
            if key in faces:
                raise ValueError(f"Shared UV between independently baked faces: {key}")
            faces[key] = (chain, e["name"], face, e["visibility"])
        e["visibility"] = True

    with TemporaryDirectory() as tmp:
        draft = Path(tmp) / bb.name
        save(draft, rig)
        pieces, _ = convert_gecko(draft, apply_arm_clearance=False)

    tex = ASSETS / "textures/models/armor"
    model = ASSETS / "armor_models"
    base, glow = generate(ASSETS / "geo/quantum_armor.geo.json", bb, HERE / "quantum_materials.json")
    glow.save(tex / "quantum_mounts_glow.png")
    fixed_glow = glow.copy()
    for socket in bindings["flight"]["sockets"]:
        u, v, w, h = socket["atlas_rect"]
        fixed_glow.paste((0, 0, 0, 0), (u, v, u + w, v + h))

    segments = {}
    segment_quads = defaultdict(list)
    slots = dict(helmet="head", chestplate="chest", leggings="legs", boots="feet")
    for piece, baked in pieces.items():
        static = defaultdict(list)
        for part, quads in baked["parts"].items():
            for q in quads:
                verts = q["v"]
                key = bounds(verts[3::5], verts[4::5])
                chain, cube, face, stowed = faces[key]
                if stowed:
                    static[part].append(q)
                unit = next((s for s in chain if s in mounts), None)
                kind, id_, moving = "", "", False
                if unit:
                    kind, id_ = mounts[unit]["kind"], mounts[unit]["id"]
                    name = unit
                elif (cube, face) in sockets:
                    name = id_ = sockets[cube, face]
                    kind = "rcs"
                elif cube.startswith(("mach_pod_", "mach_rail_", "hover_strip_")):
                    name = cube
                    kind, id_ = cube.rsplit("_", 1)
                    moving = kind == "mach_pod"
                elif "armorBody_float" in chain or "armorHead_float" in chain:
                    name = kind = "wing" if "armorBody_float" in chain else "halo"
                    moving = True
                else:
                    continue
                segments[name] = dict(name=name, kind=kind, id=id_, slot=slots[piece],
                                      part=part, moving=moving)
                segment_quads[name].append(q)
        baked["parts"] = static
        save(model / f"quantum_{piece}.json", baked)
        texture = tex / f"quantum_{piece}.png"
        base.save(texture)
        fixed_glow.save(tex / f"quantum_{piece}_glow.png")
        w, h, pixels = read_png(texture)
        plating, accent, _ = split(pixels)
        write_png(tex / f"quantum_{piece}_plating.png", w, h, plating)
        write_png(tex / f"quantum_{piece}_accent.png", w, h, accent)
    save(model / "quantum_mounts.json", dict(segments=list(segments.values()), parts=segment_quads))


if __name__ == "__main__":
    bake()
