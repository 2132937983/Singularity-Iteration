"""Rebuild the editable Blockbench 5 GeckoLib project from the AN1 contract and clips."""
import json
import uuid
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PROJECT = ROOT / "tools/models/si_humanoid_rig.bbmodel"

# Bedrock origins, sizes and the standard 64x64 player box-UV layout.
PARTS = {
    "body": ([-4, 12, -2], [8, 12, 4], [16, 16], 4),
    "head": ([-4, 24, -4], [8, 8, 8], [0, 0], 3),
    "right_arm": ([-8, 12, -2], [4, 12, 4], [40, 16], 5),
    "left_arm": ([4, 12, -2], [4, 12, 4], [32, 48], 5),
    "right_leg": ([-4, 0, -2], [4, 12, 4], [0, 16], 7),
    "left_leg": ([0, 0, -2], [4, 12, 4], [16, 48], 7),
}
LOOPS = {False: "once", True: "loop", "play_once": "once",
         "loop": "loop", "hold_on_last_frame": "hold"}


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


def uid(*parts):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, "mio_icif/an1/" + "/".join(parts)))


def geometry(rig):
    bones = []
    for entry in rig["bones"]:
        name = entry["name"]
        bone = {"name": name, "pivot": entry["pivot"]}
        if entry["parent"] is not None:
            bone["parent"] = entry["parent"]
        if name in PARTS:
            origin, size, uv, _ = PARTS[name]
            bone["cubes"] = [{"origin": origin, "size": size, "uv": uv}]
        bones.append(bone)
    description = {"identifier": rig["identifier"], "texture_width": 64, "texture_height": 64,
                   "visible_bounds_width": 3, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1, 0]}
    return {"format_version": "1.12.0", "minecraft:geometry": [{"description": description, "bones": bones}]}


def edit_vector(channel, vector):
    # The Bedrock geometry/position codec reflects X. Its angle codec instead
    # negates rotation X and Y. BB5 keyframe data stores the editor-side values.
    x, y, z = vector
    return [-x, -y if channel == "rotation" else y, z]


def transform_keys(animation, bone, channel, keys):
    frames = []
    for time, value in keys.items():
        vector = value["vector"] if isinstance(value, dict) else value
        point = dict(zip("xyz", edit_vector(channel, vector)))
        frame = {"uuid": uid(animation, bone, channel, time), "time": float(time),
                 "channel": channel, "interpolation": "linear", "data_points": [point]}
        if isinstance(value, dict) and "easing" in value:
            frame["easing"] = value["easing"]
            if "easingArgs" in value:
                frame["easingArgs"] = value["easingArgs"]
        frames.append(frame)
    return frames


def edit_animation(name, clip):
    animators = {}
    for bone, channels in clip["bones"].items():
        frames = []
        for channel, keys in channels.items():
            if isinstance(keys, list) or "vector" in keys:
                keys = {"0": keys}
            frames.extend(transform_keys(name, bone, channel, keys))
        animators[uid("bone", bone)] = {"name": bone, "type": "bone", "keyframes": frames}
    if clip.get("timeline"):
        frames = []
        for time, event in clip["timeline"].items():
            script = "\n".join(event) if isinstance(event, list) else event
            frames.append({"uuid": uid(name, "timeline", time), "time": float(time),
                           "channel": "timeline", "data_points": [{"script": script}]})
        animators["effects"] = {"name": "effects", "type": "effect", "keyframes": frames}
    return {"uuid": uid("clip", name), "name": name, "loop": LOOPS[clip.get("loop", False)],
            "length": clip["animation_length"], "snapping": 100, "animators": animators}


def edit_model(rig, clips):
    elements, groups, roots = [], [], []
    nodes = {bone["name"]: {"uuid": uid("bone", bone["name"]), "isOpen": True, "children": []}
             for bone in rig["bones"]}
    for bone in rig["bones"]:
        name = bone["name"]
        node = nodes[name]
        pivot = edit_vector("position", bone["pivot"])
        groups.append({"uuid": node["uuid"], "name": name, "origin": pivot, "children": []})
        if bone["parent"] is None:
            roots.append(node)
        else:
            nodes[bone["parent"]]["children"].append(node)
        if name in PARTS:
            origin, size, uv, color = PARTS[name]
            low = [-origin[0] - size[0], origin[1], origin[2]]
            element_id = uid("cube", name)
            elements.append({"type": "cube", "uuid": element_id, "name": name, "box_uv": True,
                             "uv_offset": uv, "from": low, "to": [a + b for a, b in zip(low, size)],
                             "origin": pivot, "color": color})
            node["children"].append(element_id)
    identifier = rig["identifier"].removeprefix("geometry.")
    return {"meta": {"format_version": "5.0", "model_format": "geckolib_model", "box_uv": True},
            "name": "si_humanoid_rig", "model_identifier": identifier, "geckolib_model_type": "Entity",
            "geckolib_modid": "mio_icif", "visible_box": [3, 4, 1],
            "resolution": {"width": 64, "height": 64}, "groups": groups, "elements": elements,
            "outliner": roots, "textures": [],
            "animations": [edit_animation(name, clip) for name, clip in clips.items()]}


def main():
    spec = read(ROOT / "docs/art/an1/spec.json")
    clips = {}
    for key, file in spec["files"].items():
        path = ROOT / file["path"]
        if key == "wingman" or path.is_file():
            clips.update(read(path)["animations"])
    write(ROOT / spec["rig"]["file"], geometry(spec["rig"]))
    write(PROJECT, edit_model(spec["rig"], clips))
    print(f"{len(spec['rig']['bones'])} bones, {len(clips)} animations in {PROJECT.name}")


if __name__ == "__main__":
    main()
