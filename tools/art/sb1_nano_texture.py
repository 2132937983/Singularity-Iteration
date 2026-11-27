"""Paint the nano blade's packed 128x256 pixel atlas and looping charge lattice."""
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/mio_icif/model/slashblade/nano"
SIZE = (128, 256)
# x, y, length along the model, width across it. All islands pack lengthways
# down the atlas; painting them horizontally makes the two material axes clear.
REGIONS = {
    "blade": (2, 2, 252, 20), "sheath": (25, 2, 252, 24),
    "grip": (52, 2, 72, 14), "guard": (69, 2, 25, 18),
    "metal": (91, 2, 49, 12), "bevel": (106, 2, 49, 7),
    "dark": (117, 2, 49, 8), "fracture": (52, 80, 24, 14),
    "edge_glow": (71, 65, 48, 8), "rail_glow": (82, 65, 48, 8),
    "ceramic": (94, 65, 20, 10), "edge": (107, 65, 50, 6),
    "spine": (116, 65, 50, 9), "sheath_cap": (52, 119, 36, 14),
    "kissaki": (71, 120, 41, 20),
}


def atlas_uv(tile, s, t):
    """Model-space UV: s=grip/mouth to tip, t=spine to cutting edge."""
    x, y, length, width = REGIONS[tile]
    return (x + .5 + t * (width - 1)) / SIZE[0], 1 - (y + .5 + s * (length - 1)) / SIZE[1]


SEAM = "#0E1013"
CARBON = "#24282D"
CARBON_DARK = "#1C1F23"
BEVEL = "#4A5058"
SHADE = "#15171A"
STEEL = "#6F7680"
GLINT = "#A9B0B9"
PZT = "#C9CED4"
PZT_SHADE = "#8E959E"
EMBER = "#FF5A14"
ORANGE = "#FF7A1F"
HOT = "#FFC46B"


def carbon_field(image, box):
    """Fine twill in alternating pairs, confined to the exposed laminate."""
    x0, y0, x1, y1 = box
    draw = ImageDraw.Draw(image)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            cell = (x + y) // 2
            colour = CARBON if cell % 3 else CARBON_DARK
            draw.point((x, y), fill=colour)


def blade(image):
    draw = ImageDraw.Draw(image)
    xmax = image.width - 1
    carbon_field(image, (0, 1, xmax, 10))
    draw.polygon([(0, 0), (xmax, 0), (xmax, 2), (91, 4), (12, 4)], fill="#333940")
    draw.line((0, 0, xmax, 0), fill=BEVEL)
    draw.line([(5, 4), (86, 4), (109, 3)], fill="#41474F")
    # Flush thermal grooves occupy the blade root, below the unbroken spine.
    for x in (15, 23, 31):
        draw.line((x, 9, x + 3, 5), fill=SEAM, width=2)
        draw.line((x + 1, 9, x + 4, 5), fill="#444D54")
    draw.polygon([(0, 11), (146, 10), (xmax, 9), (xmax, 15), (0, 15)], fill="#323941")
    draw.line([(9, 11), (146, 10), (xmax, 9)], fill="#49525B")
    # Three inspection windows interrupt the sealed lower band. Each reveals
    # just two recessed ceramic contacts. The final window ends before the tip.
    draw.rectangle((0, 12, xmax, 15), fill="#1A1F25")
    for x in (53, 121, 183):
        draw.polygon([(x, 15), (x + 2, 12), (x + 13, 12), (x + 11, 15)], fill=SEAM)
        for contact in (x + 3, x + 8):
            draw.rectangle((contact, 13, contact + 2, 14), fill="#78848D")
            draw.line((contact, 13, contact + 2, 13), fill="#A4ABB1")
        draw.line((x + 2, 15, x + 10, 15), fill="#3B454E")
    # Keep the substrate dark under the additive ribbon. A second bright
    # orange layer here would saturate to pale yellow in the in-game pass.
    draw.rectangle((0, 16, xmax, 17), fill="#4F1F0D")
    draw.line((0, 17, xmax, 17), fill="#381708")
    draw.line((0, 18, xmax, 18), fill="#676D77")
    draw.line((0, 19, xmax, 19), fill="#92979C")


def kissaki(image):
    # Separate tip UV island, viewed from either broad side. The diagonal
    # facet is visible even with the emission pass disabled.
    draw = ImageDraw.Draw(image)
    xmax, ymax = image.width - 1, image.height - 1
    draw.rectangle((0, 0, xmax, ymax), fill="#424B56")
    draw.polygon([(0, 0), (xmax, 0), (xmax, 3), (0, 8)], fill="#303944")
    draw.line([(0, 8), (8, 7), (22, 5), (xmax, 3)], fill="#515C68")
    draw.polygon([(0, 12), (xmax, 5), (xmax, 14), (0, 15)], fill="#535D68")
    # The only warm feature on this island lies against the single cutting edge.
    draw.rectangle((0, 16, xmax, 17), fill="#4F1F0D")
    draw.line((0, 17, xmax, 17), fill="#381708")
    draw.line((0, 18, xmax, 18), fill="#646D77")
    draw.line((0, 19, xmax, 19), fill="#77818B")


def sheath(image):
    draw = ImageDraw.Draw(image)
    xmax, ymax = image.width - 1, image.height - 1
    carbon_field(image, (0, 0, xmax, ymax))
    # Four large armour plates. Fine twill remains in the inset band only.
    boundaries = (0, 65, 133, 196, xmax + 1)
    for i, (a, b) in enumerate(zip(boundaries, boundaries[1:])):
        draw.polygon([(a, 0), (b - 1, 0), (b - 4, 6), (a + 9, 6), (a + 2, 10), (a, 10)],
                     fill=("#373D44", "#32383F")[i % 2])
        draw.line([(a + 2, 1), (b - 5, 1)], fill=("#575F68", BEVEL)[i % 2])
        draw.line([(a + 9, 6), (b - 6, 6)], fill="#424951")
        draw.polygon([(a, 19), (a + 15, 16), (b - 10, 16), (b - 5, 13),
                      (b - 1, 13), (b - 1, ymax), (a, ymax)], fill="#2B3036")
        draw.line([(a + 2, 20), (a + 18, 17), (b - 10, 17)], fill="#3D444B")
    # Slot centres match the three diagonal luminous strips on the mesh.
    for anchor in (.26, .53, .78):
        for y in range(4, 20):
            t = y / ymax
            x = round((anchor + .025 - (t - .2) * .05 / .62) * xmax)
            draw.line((x - 3, y, x + 3, y), fill=SEAM)
            draw.point((x - 3, y), fill="#474E56")
            draw.line((x, y, x + 1, y), fill="#AA451C")
            draw.point((x + 2, y), fill="#3D302D")
    draw.line((1, ymax - 1, xmax - 2, ymax - 1), fill="#41474E")
    draw.line((0, ymax, xmax, ymax), fill=SHADE)


def grip(image):
    draw = ImageDraw.Draw(image)
    xmax, ymax = image.width - 1, image.height - 1
    carbon_field(image, (0, 0, xmax, ymax))
    # Broad diagonal wrap, with a single bright selvedge and a tucked shadow.
    for x in range(-15, image.width, 11):
        band = [(x, 0), (x + 4, 0), (x + 11, ymax), (x + 7, ymax)]
        draw.polygon(band, fill="#373C43")
        draw.line((x, 0, x + 7, ymax), fill="#525862")
        draw.line((x + 4, 0, x + 11, ymax), fill=SHADE)
    draw.line((0, ymax, xmax, ymax), fill=CARBON_DARK)


def guard(image):
    draw = ImageDraw.Draw(image)
    xmax, ymax = image.width - 1, image.height - 1
    draw.rectangle((0, 0, xmax, ymax), fill="#2D343C")
    draw.polygon([(1, 5), (6, 1), (19, 1), (23, 5), (23, 12), (18, 16),
                  (6, 16), (1, 12)], fill="#414951")
    draw.polygon([(3, 6), (7, 3), (18, 3), (21, 6), (21, 11), (17, 14),
                  (7, 14), (3, 11)], fill="#323941")
    draw.line([(1, 9), (1, 5), (6, 1), (16, 1)], fill="#65707A")
    draw.line([(23, 8), (23, 12), (18, 16), (9, 16)], fill="#20252C")


def fracture(image):
    draw = ImageDraw.Draw(image)
    xmax, ymax = image.width - 1, image.height - 1
    draw.rectangle((0, 0, xmax, ymax), fill="#1C2027")
    for y in (2, 5, 8, 11):
        draw.line([(0, y + 1), (5, y + 1), (8, y), (16, y), (18, y - 1), (xmax, y - 1)],
                  fill=("#89919A" if y in (2, 8) else "#59616D"))
        draw.line([(1, y + 2), (5, y + 2), (8, y + 1), (14, y + 1)], fill=SEAM)


def facet(image, bands):
    """Glints trace a change of plane, with broad solid areas between them."""
    draw = ImageDraw.Draw(image)
    for row, colour in bands:
        y = round(row * (image.height - 1))
        draw.rectangle((0, y, image.width - 1, image.height - 1), fill=colour)


def glow(image, hot_rail):
    # The first and last rows stay transparent outside the lit ribbon.
    draw = ImageDraw.Draw(image)
    colours = ([EMBER, ORANGE, "#FFAD50", HOT, ORANGE, EMBER] if hot_rail
               else [ORANGE, ORANGE, "#FF8C31", "#FF9A3C", "#FF8C31", ORANGE])
    for y, colour in enumerate(colours, 1):
        draw.line((0, y, image.width - 1, y), fill=colour)
    if not hot_rail:
        for first, last in ((3, 6), (18, 19), (35, 38)):
            draw.line((first, 4, last, 4), fill=HOT)


def texture():
    atlas = Image.new("RGBA", SIZE)
    facets = {
        "metal": ((0, "#89909A"), (.12, "#606975"), (.45, STEEL), (.64, BEVEL), (.85, "#333941")),
        "bevel": ((0, "#7C838B"), (.24, "#666E76"), (.7, "#525A63")),
        "dark": ((0, "#2E333A"), (.22, CARBON), (.60, CARBON_DARK), (.88, SHADE)),
        "ceramic": ((0, "#DFDFDC"), (.22, PZT), (.64, "#ADB4BC"), (.88, PZT_SHADE)),
        "edge": ((0, "#58616B"), (.4, "#9198A1"), (.82, "#B3B8BD")),
        "spine": ((0, "#686E76"), (.2, "#414750"), (.6, "#31363E"), (.86, "#252A31")),
        "sheath_cap": ((0, GLINT), (.12, "#777F89"), (.36, "#58616B"), (.59, "#6B737D"),
                       (.73, "#434950"), (.91, "#282E35")),
    }
    painters = {"blade": blade, "kissaki": kissaki, "sheath": sheath, "grip": grip,
                "guard": guard, "fracture": fracture}
    for name, (x, y, length, width) in REGIONS.items():
        image = Image.new("RGBA", (length, width))
        if name in painters:
            painters[name](image)
        elif name.endswith("_glow"):
            glow(image, name == "rail_glow")
        else:
            facet(image, facets[name])
        atlas.paste(image.transpose(Image.Transpose.TRANSPOSE), (x, y))
    return atlas


def charge():
    # Each node owns three clockwise edges. Neighbouring cells complete the
    # other three; drawing an edge just once keeps every diagonal one texel.
    image = Image.new("RGBA", (64, 64))
    draw = ImageDraw.Draw(image)
    for row in range(-1, 10):
        for column in range(-1, 6):
            x = column * 16 + (row % 2) * 8
            y = row * 8
            draw.line([(x, y - 6), (x + 8, y - 2), (x + 8, y + 2), (x, y + 6)], fill=ORANGE)
    for x, y in ((8, 2), (40, 18), (32, 42), (0, 58)):
        draw.point((x, y), fill=HOT)
        draw.point((x, y - 1), fill=HOT)
    return image


if __name__ == "__main__":
    ASSETS.mkdir(parents=True, exist_ok=True)
    texture().save(ASSETS / "texture.png", optimize=True)
    charge().save(ASSETS / "charge.png", optimize=True)
