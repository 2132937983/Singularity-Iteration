"""Generate the shared multipart geometry and eight IC2 fluid pipe variants.

Run from any directory with Python 3. Existing IC2 grayscale textures are copied
unchanged; no generated raster assets or changes to legacy pipe resources.
"""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"
ASSET = RES / "assets/mio_icif"
DATA = RES / "data/mio_icif"
SIZES = {"tiny": 4, "small": 6, "medium": 8, "large": 10}
SIDES = ("down", "up", "north", "south", "west", "east")


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def element(start, end, tip=None):
    return {"from": start, "to": end, "faces": {
        face: {"texture": "#end" if face == tip else "#side", "tintindex": 0}
        for face in SIDES}}


for size, pixels in SIZES.items():
    lo, hi = (16 - pixels) / 2, (16 + pixels) / 2
    textures = {"particle": "mio_icif:block/pipe/ic2_pipe_side",
                "side": "mio_icif:block/pipe/ic2_pipe_side",
                "end": f"mio_icif:block/pipe/ic2_pipe_{size}"}
    center = element([lo] * 3, [hi] * 3)
    parts = {"center": center}
    for side in SIDES:
        start, end = [lo] * 3, [hi] * 3
        axis = {"west": 0, "east": 0, "down": 1, "up": 1, "north": 2, "south": 2}[side]
        if side in ("west", "down", "north"):
            start[axis], end[axis] = 0, lo
        else:
            start[axis], end[axis] = hi, 16
        part = element(start, end, side)
        # Internal mating faces are never visible and cause unnecessary quads.
        opposite = dict(zip(SIDES, ("up", "down", "south", "north", "east", "west")))[side]
        del part["faces"][opposite]
        part["faces"][side]["uv"] = [0, 0, 16, 16]
        parts[side] = part
    for name, part in parts.items():
        write(ASSET / f"models/block/pipe/fluid_{size}_{name}.json",
              {"parent": "minecraft:block/block", "textures": textures, "elements": [part]})
    # Item form is a short straight pipe, displaying both diameter and material.
    for grade in ("bronze", "steel"):
        write(ASSET / f"models/item/pipe/fluid_pipe_{grade}_{size}.json",
              {"parent": "minecraft:block/block", "textures": textures,
               "elements": [center, parts["up"], parts["down"]]})

source = ROOT / "source/IC2Code 1.12.2/assets/ic2/textures/blocks/transport"
texture_dir = ASSET / "textures/block/pipe"
texture_dir.mkdir(parents=True, exist_ok=True)
for name in ("side", *SIZES):
    shutil.copyfile(source / f"pipe_{name}.png", texture_dir / f"ic2_pipe_{name}.png")

for grade, material in (("bronze", "bronze"), ("steel", "adviron")):
    block_id = f"mio_icif:pipe/fluid_pipe_{grade}"
    multipart, drops = [], []
    for size in SIZES:
        for part in ("center", *SIDES):
            when = {"size": size}
            if part != "center":
                when[part] = "true"
            multipart.append({"when": when, "apply": {"model": f"mio_icif:block/pipe/fluid_{size}_{part}"}})
        item_id = f"{block_id}_{size}"
        drops.append({"type": "minecraft:item", "name": item_id, "conditions": [
            {"condition": "minecraft:block_state_property", "block": block_id, "properties": {"size": size}}]})
        casing = size in ("tiny", "small")
        ingredient = {"item": f"mio_icif:resource/item_{material}_casing"} if casing else {"tag": f"c:plates/{material}"}
        pattern = ["CCC", "   ", "CCC"] if size in ("tiny", "medium") else ["C C", "C C", "C C"]
        count = {"tiny": 6, "small": 3, "medium": 2, "large": 1}[size]
        write(DATA / f"recipe/pipe/fluid_pipe_{grade}_{size}.json", {
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
            "key": {"C": ingredient}, "result": {"id": item_id, "count": count}})
        predicate = ingredient["item"] if casing else "#" + ingredient["tag"]
        write(DATA / f"advancement/recipes/pipe/fluid_pipe_{grade}_{size}.json", {
            "parent": "minecraft:recipes/root", "criteria": {
                "has_material": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": predicate}]}},
                "has_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": item_id}}},
            "requirements": [["has_material", "has_recipe"]], "rewards": {"recipes": [item_id]}})
    write(ASSET / f"blockstates/pipe/fluid_pipe_{grade}.json", {"multipart": multipart})
    write(DATA / f"loot_table/blocks/pipe/fluid_pipe_{grade}.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": drops,
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

tag_path = RES / "data/c/tags/block/wrench_can_damaged.json"
tag = json.loads(tag_path.read_text(encoding="utf-8"))
for grade in ("bronze", "steel"):
    value = f"mio_icif:pipe/fluid_pipe_{grade}"
    if value not in tag["values"]:
        tag["values"].append(value)
write(tag_path, tag)

# Append keys without reformatting the existing large translation files.
for locale in ("zh_cn", "en_us"):
    path = ASSET / f"lang/{locale}.json"
    original = path.read_text(encoding="utf-8")
    existing = json.loads(original)
    entries = {}
    for grade, chinese in (("bronze", "青铜"), ("steel", "钢")):
        entries[f"block.mio_icif.pipe.fluid_pipe_{grade}"] = f"{chinese}管道" if locale == "zh_cn" else f"{grade.title()} Fluid Pipe"
        for size, name in zip(SIZES, ("微型", "小型", "中型", "大型")):
            entries[f"item.mio_icif.pipe.fluid_pipe_{grade}_{size}"] = f"{name}{chinese}管道" if locale == "zh_cn" else f"{size.title()} {grade.title()} Fluid Pipe"
    entries.update({
        "tooltip.mio_icif.fluid_pipe.capacity": "容量：%s mB" if locale == "zh_cn" else "Capacity: %s mB",
        "tooltip.mio_icif.fluid_pipe.rate": "向机器总输出：%s mB/t" if locale == "zh_cn" else "Total machine output: %s mB/t",
        "tooltip.mio_icif.fluid_pipe.connection": "用扳手切换连接；需要机器主动输出流体" if locale == "zh_cn" else "Wrench to toggle ports; requires fluid pushed from a machine",
    })
    missing = {key: value for key, value in entries.items() if key not in existing}
    if missing:
        suffix = json.dumps(missing, ensure_ascii=False, indent=2)[1:-1]
        path.write_text(original.rstrip().removesuffix("}").rstrip() + "," + suffix + "\n}\n", encoding="utf-8")

print("Generated 2 blockstates, 28 shared models, 8 item models/recipes/advancements and 2 loot tables.")
