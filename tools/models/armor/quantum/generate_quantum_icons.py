"""Draw the 16px quantum inventory sprites, generated-item models and 16× slot preview.

Requires Pillow. Run python generate_quantum_icons.py, or pass --assets and --preview.
"""
import argparse
import json
from pathlib import Path

from PIL import Image, ImageColor, ImageDraw


HERE = Path(__file__).resolve().parent
PALETTE = {
    ".": (0, 0, 0, 0),
    "#": "#1B1E24",  # silhouette
    "x": "#383F4A",  # obsidian liner
    "l": "#4A5362",  # lit edge of the liner
    "d": "#7A8A9B",  # deepest ceramic bevel
    "t": "#98A8B9",  # cool shadow
    "s": "#B5C3D1",  # ceramic half-shadow
    "w": "#CED8E2",  # titanium midtone
    "h": "#EAF0F6",  # ceramic facing the light
    "p": "#FFFFFF",  # specular glint
    "c": "#4CE6FF",  # quantum light
}

# The eight left pixels define each row. Reflect the silhouette, then shade the
# right-hand ceramic facets for an upper-left light. The liner and lamps keep their colors.
RIGHT_CERAMIC = str.maketrans({"p": "w", "h": "w", "s": "t", "t": "d"})
HALVES = {
    "helmet": (
        "........",
        ".....###",
        "...##pph",
        "..#sphhw",
        "..#lshhw",
        ".#slshww",
        ".#txswcc",
        ".#lxwwww",
        ".#sxswcc",
        "..#xswww",
        "..#xtwcc",
        "..#xxtsw",
        "...#ltss",
        "....##xt",
        "......##",
        "........",
    ),
    "chestplate": (
        "........",
        "........",
        "..####..",
        ".#ppsh##",
        ".#whhwxx",
        ".#tswphw",
        ".#shhwxc",
        ".##twwsc",
        "...#swxc",
        "...#sswx",
        "...#stsw",
        "...#lxts",
        "....#xxt",
        ".....###",
        "........",
        "........",
    ),
    "leggings": (
        "........",
        "........",
        "....####",
        "...#hxxs",
        "...#phxx",
        "...#phsx",
        "...#wsl#",
        "...#ht#.",
        "...#ws#.",
        "...#ct#.",
        "...#ws#.",
        "...#sx#.",
        "...#tx#.",
        "...####.",
        "........",
        "........",
    ),
    "boots": (
        "........",
        "........",
        "........",
        "........",
        "........",
        "........",
        "...####.",
        "...#ph#.",
        "...#cl#.",
        "...#xw#.",
        "..#hws#.",
        ".#shst#.",
        ".#lxxx#.",
        ".######.",
        "........",
        "........",
    ),
}


def sprite(half):
    if len(half) != 16 or any(len(row) != 8 for row in half):
        raise ValueError("A sprite requires 16 rows of eight half-width pixels.")
    rows = [row + row[::-1].translate(RIGHT_CERAMIC) for row in half]
    colors = {key: ImageColor.getcolor(value, "RGBA") if isinstance(value, str) else value
              for key, value in PALETTE.items()}
    image = Image.new("RGBA", (16, 16))
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key == ".":
                continue
            # Includes the inside of the collar and the gap between the legs.
            neighbors = ((x-1, y), (x+1, y), (x, y-1), (x, y+1))
            edge = any(not (0 <= i < 16 and 0 <= j < 16) or rows[j][i] == "."
                       for i, j in neighbors)
            image.putpixel((x, y), colors["#" if edge else key])
    return image


def slot_preview(icons):
    # Vanilla slots have a recessed 16px interior and a 1px bevel on all sides.
    sheet = Image.new("RGB", (4 + 18 * len(icons), 22), "#C6C6C6")
    pen = ImageDraw.Draw(sheet)
    for index, icon in enumerate(icons):
        x, y = 2 + 18 * index, 2
        pen.rectangle((x, y, x + 17, y + 17), fill="#8B8B8B")
        pen.line((x, y + 16, x, y, x + 16, y), fill="#373737")
        pen.line((x + 1, y + 17, x + 17, y + 17, x + 17, y + 1), fill="#FFFFFF")
        sheet.paste(icon, (x + 1, y + 1), icon.getchannel("A"))
    return sheet.resize((sheet.width * 16, sheet.height * 16), Image.Resampling.NEAREST)


def generate(assets, preview):
    textures, models = assets / "textures/item", assets / "models/item"
    textures.mkdir(parents=True, exist_ok=True)
    models.mkdir(parents=True, exist_ok=True)
    icons = []
    for piece, half in HALVES.items():
        name = f"quantum_{piece}"
        icon = sprite(half)
        icon.save(textures / f"{name}.png")
        model = {"parent": "minecraft:item/generated", "textures": {"layer0": f"mio_icif:item/{name}"}}
        (models / f"{name}.json").write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8", newline="\n")
        icons.append(icon)
    preview.parent.mkdir(parents=True, exist_ok=True)
    slot_preview(icons).save(preview)


if __name__ == "__main__":
    cli = argparse.ArgumentParser(description=__doc__)
    # In a source checkout write to the mod; a standalone copy writes next to the script.
    mod_assets = (HERE / "../../../../src/main/resources/assets/mio_icif").resolve()
    assets = mod_assets if mod_assets.is_dir() else HERE / "assets/mio_icif"
    cli.add_argument("--assets", type=Path, default=assets, help="Path to the mio_icif namespace directory")
    cli.add_argument("--preview", type=Path, default=HERE / "quantum_icons_preview.png")
    args = cli.parse_args()
    generate(args.assets, args.preview)
