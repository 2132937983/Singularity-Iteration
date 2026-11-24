#!/usr/bin/env python3
"""Check the suit-module icons against docs/art/style-suit-modules.md (batch MOD1).

Usage: python3 tools/art/check_modules.py
Prints OK, or one line per problem and exits 1.
Needs Pillow.
"""
import os
import re
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TEX = os.path.join(ROOT, "src/main/resources/assets/mio_icif/textures/item/module")
BATCH = os.path.join(ROOT, "docs/art/batches/MOD1-suit-modules.md")
MAX_COLOURS = 16


def expected_files():
    text = open(BATCH, encoding="utf-8").read()
    return sorted(set(re.findall(r"`(item_[a-z0-9_]+\.png)`", text)))


def check(name):
    problems = []
    path = os.path.join(TEX, name)
    if not os.path.exists(path):
        return [f"{name}: missing"]
    im = Image.open(path)
    if im.size != (16, 16):
        problems.append(f"{name}: size {im.size[0]}x{im.size[1]}, expected 16x16")
    if im.mode != "RGBA":
        problems.append(f"{name}: mode {im.mode}, expected RGBA")
        im = im.convert("RGBA")
    if im.info.get("interlace"):
        problems.append(f"{name}: interlaced PNG")
    w, h = im.size
    px = im.load()
    colours = set()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a not in (0, 255):
                problems.append(f"{name}: partial alpha {a} at ({x},{y})")
                break
            if a == 0:
                continue
            if (r, g, b) == (0, 0, 0):
                problems.append(f"{name}: pure black at ({x},{y})")
            colours.add((r, g, b))
            if x in (0, w - 1) or y in (0, h - 1):
                problems.append(f"{name}: opaque pixel on the 1px border at ({x},{y})")
    if len(colours) > MAX_COLOURS:
        problems.append(f"{name}: {len(colours)} colours, at most {MAX_COLOURS}")
    if os.path.exists(path + ".mcmeta"):
        problems.append(f"{name}: has .mcmeta, this batch has no animation")
    # keep the report short: one line per kind of problem
    seen, short = set(), []
    for p in problems:
        kind = p.split(" at ")[0]
        if kind not in seen:
            seen.add(kind)
            short.append(p)
    return short


def main():
    files = expected_files()
    if len(files) != 69:
        print(f"batch list has {len(files)} files, expected 69")
        return 1
    problems = [p for f in files for p in check(f)]
    extra = sorted(set(f for f in os.listdir(TEX) if f.endswith(".png")) - set(files))
    problems += [f"{f}: not in the batch list" for f in extra]
    if problems:
        print("\n".join(problems))
        print(f"{len(problems)} problem(s)")
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
