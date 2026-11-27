"""Original pixel atlas and tiled interference film for the quantum tachi."""
from pathlib import Path

from PIL import Image, ImageDraw


ASSETS = Path(__file__).resolve().parents[2] / "src/main/resources/assets/mio_icif/model/slashblade/quantum"
SIZE = (128, 256)
# x, y, length, width. Islands are painted lengthwise and packed vertically.
ISLANDS = {
    "blade": (1, 1, 252, 34), "sheath": (36, 1, 252, 24),
    "blade_light": (62, 1, 252, 34), "grip": (98, 1, 66, 14),
    "frame": (114, 1, 54, 12), "prong": (98, 70, 26, 16),
    "socket": (116, 70, 26, 9), "ceramic": (98, 99, 32, 12),
    "fracture": (112, 99, 14, 14), "grille": (98, 135, 12, 12),
    "emitter": (112, 135, 12, 12), "seam_light": (98, 152, 12, 8),
    "pommel": (108, 152, 12, 12),
}
WHITE, PEARL, MIST, SHADE = "#FFFFFF", "#EDEDE8", "#D6D7D2", "#B4B6B3"
STEEL, INK, DARK, SILVER = "#5A6472", "#1B1E24", "#2B3440", "#9A9DA2"
CYAN, ICE, VIOLET = "#4DE6FF", "#C8FAFF", "#9B7CFF"


def uv(tile, s, t):
    x, y, length, width = ISLANDS[tile]
    return (x + .5 + t*(width-1))/128, 1-(y + .5 + s*(length-1))/256


def paint(name, width, height):
    image = Image.new("RGBA", (width, height))
    d = ImageDraw.Draw(image)
    right, bottom = width-1, height-1
    if name in ("blade", "blade_light"):
        if name == "blade":
            d.rectangle((0, 0, right, bottom), fill=PEARL)
            d.rectangle((0, 0, right, 5), fill="#536975")
            d.rectangle((0, 2, right, 3), fill="#B7C5C8")
            d.polygon(((0, 6), (right, 6), (right, 9), (154, 10), (56, 10), (0, 11)), fill=WHITE)
            d.polygon(((0, 12), (174, 11), (225, 9), (236, 10), (230, 16), (0, 16)), fill=MIST)
            d.polygon(((0, 14), (218, 14), (234, 16), (222, 23), (0, 23)), fill=DARK)
            d.line(((0, 14), (217, 14), (231, 16)), fill="#7F929A")
            d.line(((0, 23), (221, 23), (234, 17)), fill=WHITE)
            d.rectangle((0, 29, right, 32), fill=MIST)
            d.line((0, bottom, right, bottom), fill=STEEL)
            # A short indexing mark and two ceramic mounting tongues near the root.
            d.polygon(((6, 25), (21, 25), (24, 27), (8, 27)), fill="#A7ADAE")
            d.line((9, 25, 14, 25), fill=VIOLET)
            d.polygon(((225, 11), (right, 5), (right, 19), (234, 17)), fill="#E3E9E8")
        else:
            # The paired edge echoes share one flush texture pass. Two clear
            # texels separate them. Both follow just the +Y side of the blade.
            d.rectangle((1, 0, right, 1), fill="#9CF5FF")
            d.rectangle((1, 4, right-2, 5), fill="#66D8EF")
            d.rectangle((0, 17, 223, 20), fill=CYAN)
            d.line(((0, 18), (205, 18), (224, 17)), fill=ICE)
            d.polygon(((222, 17), (231, 16), (224, 19), (222, 20)), fill=CYAN)
            for x in (21, 94, 159):
                d.line((x, 20, x+5, 20), fill=VIOLET)
    elif name == "sheath":
        d.rectangle((0, 0, right, bottom), fill=PEARL)
        # Glazed upper lip and a deep, recessed instrument strip, far enough
        # from the edge that the shell still reads as white ceramic.
        d.polygon(((0, 0), (right, 0), (right, 3), (173, 3), (115, 5), (0, 5)), fill=WHITE)
        d.polygon(((0, 20), (30, 18), (217, 18), (right, 14), (right, bottom), (0, bottom)), fill=MIST)
        d.polygon(((17, 16), (23, 12), (211, 12), (219, 16)), fill="#A9B4B9")
        d.polygon(((21, 15), (25, 13), (210, 13), (214, 15)), fill=STEEL)
        for x in (30, 33, 37, 42, 48, 55, 63, 72, 82, 93, 105, 118,
                  131, 143, 154, 164, 173, 181, 188, 194, 199, 203, 206):
            d.line((x, 12, x, 16), fill=DARK)
        for x in (26, 83, 156, 208):
            d.line((x-11, 7, x-4, 7), fill="#AAB7BB")
            d.line((x-3, 7, x-2, 6), fill="#AAB7BB")
        for s in (.22, .47, .73):
            x = round(s*right)
            for y in range(height):
                k = round(x+(y/bottom-.5)*4)
                d.line((k-2,y,k+2,y),fill=DARK)
                d.point((k+3,y), fill=WHITE)
                d.point((k,y),fill="#3893A7")
        d.line((0, bottom, right, bottom), fill="#BEC4C4")
    elif name == "grip":
        d.rectangle((0, 0, right, bottom), fill=MIST)
        for x in range(-10, width+8, 12):
            d.polygon(((x,0),(x+10,0),(x+8,7),(x+4,bottom),(x-3,bottom)), fill=PEARL)
            d.line(((x+9,0),(x+7,7),(x+3,bottom)), fill="#586B78", width=2)
            d.line(((x+10,1),(x+8,7),(x+4,bottom)), fill="#65C5D3")
            d.line((x+2,1,x+7,1), fill=WHITE)
            d.line((x,10,x+3,8), fill="#B6BFBE")
    elif name == "prong":
        d.rectangle((0, 0, right, bottom), fill=MIST)
        d.polygon(((0, 0), (right, 0), (right-5, 5), (8, 5), (4, 9), (0, 9)),fill=WHITE)
        d.polygon(((0, bottom-2),(8, 10),(right-6, 10),(right, 6),(right, bottom),(0,bottom)),fill=SHADE)
        d.line(((0,bottom-1),(8,11),(right-4,11)),fill=STEEL)
    elif name == "fracture":
        d.rectangle((0, 0, right, bottom), fill=MIST)
        d.polygon(((0, 4),(4, 3),(7, 6),(11, 3),(right, 4),(right, 9),(8, 10),(4, 8),(0, 10)), fill=DARK)
        for x in (2,5,9,12):
            d.line((x,5,x,8),fill="#6A8C96")
        d.line(((0,3),(4,2),(7,5),(10,3)), fill=WHITE)
    elif name in ("grille", "emitter"):
        if name == "grille":
            d.rectangle((0,0,right,bottom),fill=DARK)
            d.line(((0,bottom),(0,0),(right,0)),fill="#7F8F99")
            d.rectangle((2,2,right-2,bottom-2),fill=INK)
        else:
            for x in (2,5,8):
                d.line((x,2,x,bottom-2),fill=CYAN)
            for y in (3,7):
                d.line((2,y,right-2,y),fill=CYAN)
            for xy in ((2,3),(5,7),(8,3)):
                d.point(xy,fill=ICE)
            d.point((8,8),fill=VIOLET)
    elif name == "seam_light":
        d.rectangle((0,0,right,bottom), fill=CYAN)
        d.line((0,2,right,2),fill=ICE)
    elif name == "pommel":
        d.rectangle((0,0,right,bottom),fill=DARK)
        d.polygon(((1,6),(5,1),(10,5),(6,10)),fill="#7A8996")
        d.polygon(((3,6),(5,3),(8,5),(6,8)),fill=CYAN)
        d.point((5,4),fill=ICE)
    else:
        bands = {"ceramic": (WHITE, PEARL, MIST, SHADE), "frame": ("#ACB9BF",STEEL,DARK,INK),
                 "socket": ("#ADB9C0","#697C89",DARK,INK)}
        for y, colour in zip((0, height//6, height//2, 5*height//6), bands[name]):
            d.rectangle((0,y,right,bottom),fill=colour)
    return image


def write_textures():
    atlas = Image.new("RGBA", SIZE)
    for name,(x,y,w,h) in ISLANDS.items():
        atlas.paste(paint(name,w,h).transpose(Image.Transpose.TRANSPOSE),(x,y))
    ASSETS.mkdir(parents=True,exist_ok=True)
    atlas.save(ASSETS/"texture.png",optimize=True)
    # Both diagonal combs have integer periods on the torus. Every tile boundary
    # lands on precisely the same phase as the next repeat.
    charge = Image.new("RGBA",(64,64))
    draw = ImageDraw.Draw(charge)
    for y in range(64):
        for x in range(64):
            a = (x-2*y) % 16 < 2
            b = (x+2*y) % 16 < 2
            if a or b:
                colour = CYAN if a else "#77CFEA"
                if a and b:
                    colour = VIOLET if (x+3*y)%64 == 32 else ICE
                draw.point((x,y),fill=colour)
    charge.save(ASSETS/"charge.png",optimize=True)


if __name__ == "__main__":
    write_textures()
