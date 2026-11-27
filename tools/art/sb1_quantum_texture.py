"""Original pixel atlas and tiled interference film for the quantum tachi."""
from math import pi, sin
from pathlib import Path

from PIL import Image, ImageDraw


ASSETS = Path(__file__).resolve().parents[2] / "src/main/resources/assets/mio_icif/model/slashblade/quantum"
SIZE = (128, 256)
# x, y, length, width. Islands are painted lengthwise and packed vertically.
ISLANDS = {
    "blade": (1, 1, 252, 34), "sheath": (36, 1, 252, 24),
    "blade_light": (62, 1, 252, 34), "grip": (98, 1, 66, 14),
    "frame": (114, 1, 54, 12), "tsuba": (98, 70, 26, 16),
    "socket": (116, 70, 26, 9), "ceramic": (98, 99, 32, 12),
    "fracture": (112, 99, 14, 14), "ruler": (98, 135, 23, 9),
    "ruler_light": (109, 135, 23, 9), "seam_light": (98, 162, 12, 8),
    "pommel": (108, 162, 12, 12),
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
            # Cool sky reflection over the shinogi, a bright edge-side ground
            # and a warm, narrow crest where the polishing planes meet.
            d.rectangle((0,0,right,bottom),fill="#8FA0B2")
            d.polygon(((0,19),(51,17),(172,19),(right,17),(right,26),
                       (147,28),(82,24),(0,27)),fill="#73899E")
            d.polygon(((0,28),(89,26),(165,30),(right,27),(right,bottom),(0,bottom)),fill="#5E6B7A")
            d.rectangle((0,0,right,16),fill="#C2CCD6")
            d.polygon(((0,9),(61,11),(125,8),(203,11),(right,9),
                       (right,15),(121,13),(75,16),(0,14)),fill="#E6ECF2")
            d.line(((0,17),(72,17),(103,16),(210,16),(235,15)),fill="#FFF8EE")
            # The short bo-hi on the root section ends with a rounded stop.
            d.polygon(((4,21),(8,19),(72,19),(78,21),(72,23),(8,23)),fill="#5E6B7A")
            d.line(((7,23),(70,23),(75,22)),fill="#B8C7D1")
            # The small tip has its own polished plane and a single yokote.
            d.polygon(((231,0),(right,0),(right,bottom),(240,bottom)),fill="#A8B8C8")
            d.polygon(((231,0),(right,0),(right,13),(235,14)),fill="#D4DFE7")
            d.line(((231,0),(240,bottom)),fill="#FFF8EE",width=1)
        # A single band along the edge, with alternating lit and unlit lobes.
        # Increasing phase frequency compresses their spacing toward the tip.
        for x in range(right+1):
            s = x/right
            cycles = 5*s+9*s*s
            phase = cycles % 1
            # Offset the crest so the long fall remains readable at 2x.
            peak = .40+.045*sin(2*pi*cycles/3)
            crest = phase/peak if phase<peak else (1-phase)/(1-peak)
            middle = 4.2+6.1*crest
            # The cyan tongue tapers into an unlit silver bed at either end.
            tongue = max(0,1-abs((phase-.48)/.42))
            thickness = 4.9*tongue**.68
            top,bottom_y=round(middle-2.4),round(middle+2.4)
            low,high=round(middle-thickness*.5),round(middle+thickness*.5)
            if name == "blade":
                d.line((x,top,x,bottom_y),fill="#6F8193")
                if thickness>.65:
                    d.line((x,low,x,high),fill="#19404E")
                d.line((x,0,x,1),fill="#46707D")
            else:
                d.line((x,0,x,1),fill=ICE)
                if thickness>.65:
                    d.line((x,low,x,high),fill=CYAN)
                    if thickness>3.4:
                        d.point((x,low+1),fill="#9AF1FF")
        if name == "blade":
            d.rectangle((247,6,247,10),fill="#51516A")
            d.rectangle((250,0,right,bottom),fill="#294454")
        else:
            d.rectangle((247,6,247,10),fill=VIOLET)
            d.rectangle((250,0,right,bottom),fill=CYAN)
    elif name == "sheath":
        # A soft ivory face contrasts with the colder, strongly split steel.
        d.rectangle((0,0,right,bottom),fill=PEARL)
        d.polygon(((0,0),(right,0),(right,3),(170,3),(96,4),(0,4)),fill=WHITE)
        d.polygon(((0,18),(51,18),(121,20),(211,18),(right,16),
                   (right,bottom),(0,bottom)),fill=MIST)
        for s in (.24,.49,.75):
            x=round(right*s)
            for y in range(height):
                d.point((x+round(3*(y/bottom-.5)),y), fill="#727F8B")
        d.line((0,bottom,right,bottom),fill="#BCC2C3")
    elif name == "grip":
        # Interleaved graphite-blue ribbons expose the ivory foundation
        # through repeated diamond windows. The menuki is visible on each cheek.
        d.rectangle((0,0,right,bottom),fill=DARK)
        for x in range(-7,width+8,13):
            d.polygon(((x,7),(x+5,3),(x+10,7),(x+5,11)),fill="#E2E3DB")
            d.line(((x+1,7),(x+5,4),(x+8,6)),fill=WHITE)
            d.line(((x-2,3),(x+2,0)),fill=STEEL)
            d.line(((x+7,12),(x+11,9),(x+14,6)),fill="#556373")
        d.polygon(((30,6),(32,4),(35,4),(37,6),(35,9),(32,9)),fill="#9BA8AA")
        d.line(((30,6),(32,4),(35,4)),fill="#EEEBDD")
        d.line(((33,8),(35,8),(36,6)),fill="#596977")
    elif name == "tsuba":
        d.rectangle((0,0,right,bottom),fill=DARK)
        d.polygon(((0,0),(right,0),(right,3),(4,4),(0,8)),fill="#3D4B5A")
        d.line(((1,3),(3,1),(right-4,1)),fill="#647485")
        d.line(((1,bottom-2),(right-5,bottom-2),(right-2,bottom-5)),fill="#1E2732")
    elif name == "fracture":
        d.rectangle((0,0,right,bottom),fill="#C2CCD6")
        d.polygon(((0,0),(7,0),(5,4),(9,8),(7,bottom),(0,bottom)),fill="#E6ECF2")
        d.line(((6,0),(5,4),(8,8),(6,bottom)),fill="#566676",width=2)
        d.line(((4,0),(3,4),(6,8)),fill="#FFF8EE")
    elif name in ("ruler","ruler_light"):
        if name=="ruler":
            d.rectangle((0,0,right,bottom),fill="#CBD1CE")
            d.rectangle((0,0,right,1),fill="#A8B3B4")
            d.line((0,bottom,right,bottom),fill=WHITE)
        for x in (2,8,13,17,20):
            d.line((x,2,x,bottom-2),fill=CYAN if name=="ruler_light" else "#284454")
            if name=="ruler_light" and x in (2,13):
                d.point((x,2),fill=ICE)
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
