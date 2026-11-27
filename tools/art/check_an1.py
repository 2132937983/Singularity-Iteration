#!/usr/bin/env python3
"""Check batch AN1 (GeckoLib keyframe animations for the Wingman and the suit), docs/art/batches/AN1.md.

The contract is docs/art/an1/spec.json (read, never written). Checks:
  rig     - the geo file exists, has the identifier, exactly the bones of the spec with their parents and pivots;
            cubes are allowed (a reference mannequin), bone rotations at rest are 0;
  anims   - every animation of the checked parts is in its file under its prefixed name, with the loop mode and the length
            of the spec (within length_tolerance); no animation outside the spec;
          - only the bones of its group are animated, only "rotation" and "position" channels (no scale), numbers only
            (no Molang strings), keyframe times within [0, length];
          - rotations within rotation_abs_max, root positions within limits.root_position, other bone positions within
            other_position_abs_max;
          - every marker of the spec is an instruction keyframe in "timeline" at its time (within marker_tolerance), and no
            other timeline entries;
          - a "loop" animation ends where it starts (same value at 0 and at its length, for every channel);
  files   - tools/models/si_humanoid_rig.bbmodel, tools/art/an1_preview.py and one preview GIF per animation of the checked
            parts under docs/art/previews/AN1/ (named <file>_<name>.gif).

Usage, from the repository root:  python3 tools/art/check_an1.py [--part 1] [--part 2] ... [--list]
Without --part every part is checked. Prints OK, or one line per problem and exits 1. Standard library only.
"""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SPEC = os.path.join(ROOT, "docs/art/an1/spec.json")
BBMODEL = os.path.join(ROOT, "tools/models/si_humanoid_rig.bbmodel")
PREVIEW_SCRIPT = os.path.join(ROOT, "tools/art/an1_preview.py")
PREVIEW_DIR = os.path.join(ROOT, "docs/art/previews/AN1")
CHANNELS = ("rotation", "position")

problems = []


def problem(msg):
    problems.append(msg)


def load(path):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        problem(f"missing: {os.path.relpath(path, ROOT)}")
    except json.JSONDecodeError as e:
        problem(f"not JSON: {os.path.relpath(path, ROOT)}: {e}")
    return None


def vec(value, where):
    """A keyframe value: [x, y, z] of numbers, or {"vector": [...]} / {"post": [...]} (the GeckoLib forms)."""
    if isinstance(value, dict):
        for key in ("vector", "post", "pre"):
            if key in value:
                return vec(value[key], where)
        problem(f"{where}: keyframe object without vector/post")
        return None
    if isinstance(value, (int, float)):
        return [float(value)] * 3
    if isinstance(value, list) and len(value) == 3 and all(isinstance(v, (int, float)) for v in value):
        return [float(v) for v in value]
    problem(f"{where}: value must be three numbers (no Molang): {value!r}")
    return None


def keyframes(channel, where):
    """Time -> value of a channel (a single value means constant)."""
    if isinstance(channel, dict) and not any(k in channel for k in ("vector", "post", "pre")):
        out = {}
        for t, v in channel.items():
            try:
                tt = float(t)
            except ValueError:
                problem(f"{where}: keyframe time is not a number: {t!r}")
                continue
            vv = vec(v, f"{where} @{t}")
            if vv is not None:
                out[tt] = vv
        return out
    vv = vec(channel, where)
    return {0.0: vv} if vv is not None else {}


def check_rig(spec):
    rig = spec["rig"]
    geo = load(os.path.join(ROOT, rig["file"]))
    if geo is None:
        return
    geos = geo.get("minecraft:geometry", [])
    if len(geos) != 1:
        problem("rig: expected exactly one entry in minecraft:geometry")
        return
    g = geos[0]
    ident = g.get("description", {}).get("identifier")
    if ident != rig["identifier"]:
        problem(f"rig: identifier {ident!r}, expected {rig['identifier']!r}")
    bones = {b.get("name"): b for b in g.get("bones", [])}
    want = {b["name"]: b for b in rig["bones"]}
    for name in sorted(set(bones) - set(want)):
        problem(f"rig: unexpected bone {name!r}")
    for name, w in want.items():
        b = bones.get(name)
        if b is None:
            problem(f"rig: missing bone {name!r}")
            continue
        if b.get("parent") != w["parent"]:
            problem(f"rig: bone {name!r} parent {b.get('parent')!r}, expected {w['parent']!r}")
        pivot = b.get("pivot", [0, 0, 0])
        if any(abs(a - e) > rig["pivot_tolerance"] for a, e in zip(pivot, w["pivot"])):
            problem(f"rig: bone {name!r} pivot {pivot}, expected {w['pivot']}")
        if any(abs(r) > 1e-6 for r in b.get("rotation", [0, 0, 0])):
            problem(f"rig: bone {name!r} has a rest rotation; keep the rest pose at 0 and animate instead")


def check_animation(spec, entry, anim, where):
    limits = spec["limits"]
    allowed = set(spec["groups"][entry["group"]])
    # GeckoLib's forms: true or "loop", "hold_on_last_frame", false / "play_once" / absent
    loop = {True: "loop", False: "play_once", None: "play_once"}.get(anim.get("loop"), anim.get("loop"))
    if loop != entry["loop"]:
        problem(f"{where}: loop {anim.get('loop')!r}, expected {entry['loop']!r}"
                + (" (write true)" if entry["loop"] == "loop" else ""))
    length = anim.get("animation_length")
    if not isinstance(length, (int, float)) or abs(length - entry["length"]) > limits["length_tolerance"]:
        problem(f"{where}: animation_length {length!r}, expected {entry['length']}")
        length = entry["length"]
    for bone, channels in anim.get("bones", {}).items():
        if bone not in allowed:
            problem(f"{where}: bone {bone!r} is outside group {entry['group']!r} {sorted(allowed)}")
            continue
        for ch, data in channels.items():
            if ch not in CHANNELS:
                problem(f"{where}: {bone}.{ch}: only rotation and position may be animated")
                continue
            kf = keyframes(data, f"{where}: {bone}.{ch}")
            for t, v in kf.items():
                if t < -1e-6 or t > length + 1e-6:
                    problem(f"{where}: {bone}.{ch} keyframe at {t} outside 0..{length}")
                if ch == "rotation" and any(abs(c) > limits["rotation_abs_max"] for c in v):
                    problem(f"{where}: {bone}.rotation {v} beyond +-{limits['rotation_abs_max']} at {t}")
                if ch == "position":
                    if bone == "root":
                        for axis, c in zip("xyz", v):
                            lo, hi = limits["root_position"][axis]
                            if c < lo or c > hi:
                                problem(f"{where}: root.position.{axis} {c} outside {lo}..{hi} at {t}")
                    elif any(abs(c) > limits["other_position_abs_max"] for c in v):
                        problem(f"{where}: {bone}.position {v} beyond +-{limits['other_position_abs_max']} at {t}")
            if entry["loop"] == "loop" and len(kf) > 1:
                first, last = kf[min(kf)], kf[max(kf)]
                if min(kf) > 1e-6 or abs(max(kf) - length) > 1e-6:
                    problem(f"{where}: {bone}.{ch}: a loop needs keyframes at 0 and at {length}")
                elif any(abs(a - b) > 1e-4 for a, b in zip(first, last)):
                    problem(f"{where}: {bone}.{ch}: loop does not close ({first} at 0, {last} at {length})")
    timeline = anim.get("timeline", {})
    seen = {}
    for t, value in timeline.items():
        for marker in (value if isinstance(value, list) else [value]):
            name = str(marker).strip().rstrip(";")
            try:
                seen[name] = float(t)
            except ValueError:
                problem(f"{where}: timeline time is not a number: {t!r}")
    for name, t in entry["markers"].items():
        if name not in seen:
            problem(f"{where}: missing marker {name!r} (timeline instruction at {t})")
        elif abs(seen[name] - t) > limits["marker_tolerance"]:
            problem(f"{where}: marker {name!r} at {seen[name]}, expected {t}")
    for name in sorted(set(seen) - set(entry["markers"])):
        problem(f"{where}: unexpected timeline entry {name!r}")


def main(argv):
    parts = {int(argv[i + 1]) for i, a in enumerate(argv) if a == "--part" and i + 1 < len(argv)}
    spec = load(SPEC)
    if spec is None:
        print("\n".join(problems))
        return 1
    entries = [e for e in spec["animations"] if not parts or e["part"] in parts]
    if "--list" in argv:
        for e in entries:
            print(f"part {e['part']}  {spec['files'][e['file']]['prefix']}{e['name']:<16} {e['loop']:<19} {e['length']:>5}s  "
                  f"{e['group']:<10} {e['markers']}")
        return 0
    check_rig(spec)
    by_file = {}
    for e in entries:
        by_file.setdefault(e["file"], []).append(e)
    all_names = {(e["file"], e["name"]) for e in spec["animations"]}
    for key, group in by_file.items():
        info = spec["files"][key]
        data = load(os.path.join(ROOT, info["path"]))
        if data is None:
            continue
        anims = data.get("animations", {})
        for full in sorted(anims):
            short = full[len(info["prefix"]):] if full.startswith(info["prefix"]) else None
            if short is None or (key, short) not in all_names:
                problem(f"{key}: animation {full!r} is not in the spec")
        for e in group:
            full = info["prefix"] + e["name"]
            if full not in anims:
                problem(f"{key}: missing animation {full!r}")
                continue
            check_animation(spec, e, anims[full], full)
            gif = os.path.join(PREVIEW_DIR, f"{key}_{e['name']}.gif")
            if not os.path.isfile(gif):
                problem(f"missing preview: {os.path.relpath(gif, ROOT)}")
    for path in (BBMODEL, PREVIEW_SCRIPT):
        if not os.path.isfile(path):
            problem(f"missing: {os.path.relpath(path, ROOT)}")
    if problems:
        print("\n".join(problems))
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
