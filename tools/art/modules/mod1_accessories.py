"""Original MOD1 auxiliary item sprites. Requires Pillow."""

from PIL import Image, ImageDraw


CERAMIC = {
    "o": "#1B1E24", "w": "#FFFFFF", "c": "#EDEDE8",
    "m": "#D6D7D2", "s": "#B4B6B3",
}


def stamp(image, position, rows, colors):
    """Stamp a small pixel pattern, leaving dots untouched."""
    left, top = position
    pen = ImageDraw.Draw(image)
    for dy, row in enumerate(rows):
        for dx, letter in enumerate(row):
            if letter != ".":
                pen.point((left + dx, top + dy), fill=colors[letter])


def blank():
    return Image.new("RGBA", (16, 16))


def ceramic_shell():
    shell = blank()
    stamp(shell, (1, 1), [
        ".....oooo.....",
        "...oowwwwoo...",
        "..owwwccccmo..",
        ".owwccccccmmo.",
        ".owcccccccmmo.",
        "owcccccccccmso",
        "owcccccccccmso",
        "occcccccccmmso",
        "occccccccmmmso",
        ".omcccccmmmso.",
        ".omccccmmmsso.",
        "..ommmmmmsso..",
        "...oossssoo...",
        ".....oooo.....",
    ], CERAMIC)
    return shell


def graviton(shell):
    image = shell.copy()
    # The inclined orbit passes in front of the crystal at its lower left.
    stamp(image, (3, 3), [
        "......rrr.",
        "....rr..dr",
        "...r.....d",
        "..d......d",
        ".d......d.",
        "d......d..",
        "d.....d...",
        "d...dd....",
        ".ddd......",
    ], {"r": "#596575", "d": "#303948"})
    stamp(image, (5, 5), [
        "..hh..",
        ".hlvv.",
        "hlvvvs",
        "lvvvvs",
        ".vvss.",
        "..ss..",
    ], {"h": "#DED1FF", "l": "#B69AFF", "v": "#8F6BFF", "s": "#6644B5"})
    ImageDraw.Draw(image).line((5, 10, 8, 10), fill="#303948")
    return image


def propulsion(shell):
    image = shell.copy()
    stamp(image, (5, 3), [
        "..hh..",
        ".hlvv.",
        ".lvvs.",
        "lvvvss",
        "lvvvss",
        ".vvss.",
        "..ss..",
    ], {"h": "#D2F5FF", "l": "#8EE5FA", "v": "#4FC3F7", "s": "#327DA9"})
    stamp(image, (5, 10), [
        ".dddd.",
        ".msmd.",
        "ommsdo",
        "oddddo",
        ".oooo.",
    ], {"d": "#343E4A", "m": "#8C969C", "s": "#65717A", "o": "#1B1E24"})
    return image


def solar_core(shell, level):
    image = shell.copy()
    stamp(image, (5, 4), [
        ".hhhhh.",
        "hlvgvvs",
        "hvvgvvs",
        "ggggggs",
        "lvvgvvs",
        "lvvgvvs",
        ".sssss.",
    ], {"h": "#FFEAA1", "l": "#FFD867", "v": "#FFC83D", "s": "#AB6D28", "g": "#BF831E"})
    pen = ImageDraw.Draw(image)
    for x in (6, 8, 10)[:level]:
        pen.point((x, 12), fill="#2B3440")
    return image


def expander(quantum_base):
    image = quantum_base("#9B7CFF")
    stamp(image, (3, 4), [
        "dddddd....",
        "dssssm..d.",
        "dssssm.ddd",
        "dddddm..d.",
        ".mmmmm....",
    ], {"d": "#2B3440", "s": "#5A6472", "m": "#9A9DA2"})
    return image


def lining():
    image = blank()
    # A curled upper right lip and a sagging lower hem give the cloth a soft silhouette.
    rows = [
        "...ooooooooo..",
        ".oohfhfhfhffo.",
        ".ohtttttttmfoo",
        "ohfttttttttfmo",
        "ofttttttttttfo",
        "ohttttttttttfo",
        "ofttttttttttfo",
        "ohttttttttttfo",
        ".ofttttttttfmo",
        ".omffhfhfhfmo.",
        "..oommsssmoo..",
        "....oooooo....",
    ]
    colors = {
        "o": "#0E1013", "f": "#32383E", "m": "#424952",
        "h": "#626C72", "t": "#24282D", "s": "#1C1F23",
    }
    stamp(image, (1, 2), rows, colors)
    pen = ImageDraw.Draw(image)
    for y, row in enumerate(rows):
        for x, letter in enumerate(row):
            if letter == "t" and (x - y) % 4 < 2:
                pen.point((x + 1, y + 2), fill=colors["s"])
    stamp(image, (3, 5), [
        "....lg....",
        "...g..g...",
        "hgg...g..h",
        "......g.g.",
        ".......g..",
    ], {"h": "#BEFFE0", "l": "#9DFFD0", "g": "#5DFFB0"})
    return image


def densifier():
    image = blank()
    stamp(image, (3, 1), [
        "...oooo...",
        "..owwwmo..",
        "..omsmso..",
        "...osso...",
        "..ofbbfo..",
        ".ofbttbfo.",
        ".obtttbfo.",
        ".oblhgsbo.",
        ".obggtsbo.",
        ".obggtsbo.",
        ".obggtsbo.",
        ".obsssbfo.",
        ".obtttbfo.",
        "..oooooo..",
    ], {
        "o": "#0E1013", "w": "#F6FAF5", "m": "#B7C0BD", "s": "#56646B",
        "f": "#434D53", "b": "#24282D", "t": "#1C1F23", "l": "#ACFFE9",
        "h": "#E1FFF6", "g": "#64FFDA",
    })
    return image


def accessories(quantum_base):
    shell = ceramic_shell()
    result = {
        "item_core_graviton": graviton(shell),
        "item_core_propulsion": propulsion(shell),
        "item_quantum_bay_expander": expander(quantum_base),
        "item_living_nano_lining": lining(),
        "item_nano_lattice_densifier": densifier(),
    }
    for level in range(1, 4):
        result[f"item_core_solar_array_{level}"] = solar_core(shell, level)
    return result
