"""Pixel atlas and tileable charge field for the Singularity odachi."""
from math import cos, pi, sin
from pathlib import Path

from PIL import Image, ImageDraw


ASSETS = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/mio_icif/model/slashblade/singularity'
SIZE = (128, 256)
# Islands run down the page. Coordinates on the drawing table run from
# hilt to tip (s) and from cutting edge to back (t).
ISLANDS = {
    'blade': (2, 2, 250, 17), 'case': (22, 2, 250, 20),
    'sear': (45, 2, 250, 8), 'grip': (56, 2, 66, 13),
    'metal': (72, 2, 20, 15), 'bronze': (91, 2, 11, 12),
    'dark': (107, 2, 17, 10), 'port': (56, 73, 21, 14),
    'light': (74, 73, 16, 8), 'coil': (88, 73, 24, 15),
    'core': (106, 73, 18, 12), 'impact': (57, 106, 17, 16),
    'hot_break': (77, 106, 17, 16), 'claw': (99, 105, 32, 14),
    'edge_lip': (119, 2, 250, 8), 'coil_meter': (56, 210, 24, 15),
    'hook': (75, 210, 22, 9), 'vent_back': (88, 210, 16, 12),
    'vent_light': (103, 210, 16, 12),
    **{f'meter_{i+1}': (56+19*(i%3), 150+27*(i//3), 24, 15) for i in range(5)},
}
STEEL = '#4B4458'
LILAC = '#8C84A0'
MAGENTA = '#FF2BD6'
WARM = '#FF8A3D'
WHITE = '#FFE6FA'


def uv(tile, s, t):
    x, y, length, width = ISLANDS[tile]
    return ((x + .5 + (width-1)*t)/SIZE[0], 1-(y+.5+(length-1)*s)/SIZE[1])


def paint(tile, length, width):
    img = Image.new('RGBA', (length, width))
    d = ImageDraw.Draw(img)
    end, bot = length-1, width-1
    if tile == 'blade':
        d.rectangle((0, 0, end, bot), fill='#100C19')
        d.polygon([(0, 4), (end, 3), (end, 7), (58, 9), (0, 9)], fill='#0A0612')
        d.polygon([(0, 10), (105, 9), (end, 8), (206, 11), (77, 12), (0, 13)], fill='#251F33')
        d.line([(12, 10), (88, 10), (132, 9)], fill='#3A3550')
        d.line([(166, 9), (218, 8), (240, 8)], fill='#353045')
        d.line((0, bot, end, bot), fill='#221E30')
        # Unevenly spaced compression lamellae are densest by the guard.
        for x in (6,10,15,21,28,36,46,58,72,89,109,133,161,194,232):
            d.line([(x,5),(x,7),(x+1,9)], fill='#2E2538')
            d.point((x+1,9), fill='#43334D')
        # The rails and all the tapered tributaries lead back towards s=0.
        d.line((7, 13, end-7, 13), fill='#090610')
        d.line((8, 14, end-9, 14), fill='#423448')
        for x in (13,40,68,99,132,169,210):
            d.line([(x+9,11),(x+6,11),(x+3,13),(x,13)], fill='#5A4260')
            d.line([(x+6,15),(x+3,13),(x,13)], fill='#39283F')
    elif tile == 'edge_lip':
        # On the edge facet this swatch and its emission have identical UVs
        # in every state. The violet lip and black gap each occupy one texel.
        d.rectangle((0,0,end,bot),fill='#120A19')
        d.line((0,0,end,0), fill='#5B2A6E')
        d.line((0,1,end,1), fill='#040207')
    elif tile == 'case':
        d.rectangle((0, 0, end, bot), fill='#2A2433')
        d.polygon([(0, 2), (end, 2), (end, 5), (18, 6)], fill='#3E364B')
        d.line((0, 1, end, 1), fill='#595266')
        d.polygon([(0, 12), (end, 10), (end, bot), (0, bot)], fill='#191522')
        for left, right in ((12, 49), (65, 105), (122, 159), (176, 206)):
            d.line([(left, 11), (left+4, 7), (right, 7)], fill='#17121F')
            d.line([(left+2, 13), (left+6, 9), (right-3, 9)], fill='#494154')
            d.line([(right-8, 14), (right-3, 14), (right, 11)], fill='#362C42')
        d.line((0, bot-1, end, bot-1), fill='#302B3B')
    elif tile == 'sear':
        for left, right in ((2, 9), (14, 24), (30, 37), (43, 58), (65, 72), (78, end)):
            d.polygon([(left, 7), (left+2, 2), (right, 2), (right-1, 7)], fill=MAGENTA)
            d.line((left+2, 4, right-1, 4), fill=WHITE)
            d.line((left+2, 5, right-1, 5), fill='#FF89E4')
    elif tile == 'hook':
        d.polygon([(2,4),(5,3),(13,3),(17,2),(19,2),(16,4),(9,4),(4,5)], fill=MAGENTA)
        d.line((6,3,12,3), fill='#FFB1EB')
    elif tile == 'coil_meter':
        img = paint('coil',length,width)
        d = ImageDraw.Draw(img)
        d.rounded_rectangle((12,2,22,11),radius=2,fill='#1A1522',outline='#80758D')
        d.rectangle((14,4,20,9),fill='#37213D')
    elif tile.startswith('meter_'):
        # Keep the old running seam and an independently addressable inset
        # lamp in one face. Only the lamp rectangle is gated by the HUD.
        img.paste(paint('light',7,width), (5,0))
        d.rectangle((14,4,20,9), fill=MAGENTA)
        d.rectangle((15,5,19,7), fill='#FF96E9')
        d.line((15,5,19,5), fill=WHITE)
    elif tile == 'vent_back':
        d.rectangle((0,0,end,bot), fill='#241828')
        d.polygon([(1,2),(3,0),(end,0),(end,1),(3,2),(1,bot)],fill='#72647C')
        d.rectangle((3,3,end-3,bot-3),fill='#0B0710')
    elif tile == 'vent_light':
        d.polygon([(3,5),(5,3),(end-3,3),(end-3,6),(end-5,8),(3,8)],fill=MAGENTA)
        d.line((5,5,end-4,5),fill=WHITE)
        d.line((5,6,end-5,6),fill='#FFA6EE')
    elif tile == 'grip':
        d.rectangle((0, 0, end, bot), fill='#2C2936')
        for x in range(-14, length, 9):
            d.polygon([(x, 0), (x+5, 0), (x+16, bot), (x+11, bot)], fill='#181221')
            d.line((x+5, 0, x+16, bot), fill='#51405B')
            d.line((x+8, 4, x+12, 8), fill='#695278')
        d.line((0, bot, end, bot), fill='#17121C')
    elif tile in ('metal', 'coil', 'claw', 'dark'):
        d.rectangle((0, 0, end, bot), fill=STEEL if tile != 'dark' else '#211B2C')
        for points, color in [
                ([(0, 0), (end, 0), (end, 2), (end//3, 4), (0, 4)], '#696175'),
                ([(0, bot-3), (end//2, bot-5), (end, bot-4), (end, bot), (0, bot)], '#322B3D')]:
            d.polygon(points, fill=color if tile != 'dark' else '#2B2536')
        if tile != 'dark':
            d.line([(1, 1), (end//2, 1), (end-2, 0)], fill=LILAC)
        if tile == 'coil':
            for i in (6, 12, 18):
                d.line((i, 2, i-2, bot-2), fill='#302638', width=2)
                d.line((i+1, 3, i-1, bot-3), fill='#645A70')
        if tile == 'claw':
            d.polygon([(0, 3), (end, bot//2), (2, 8)], fill='#62576F')
    elif tile == 'core':
        d.rectangle((0, 0, end, bot), fill='#050308')
        d.line((0, 2, end, 2), fill='#5B2A6E')
    elif tile == 'light':
        d.rectangle((0, 0, end, bot), fill=MAGENTA)
        d.rectangle((0, 2, end, 4), fill='#FF95E8')
        d.line((0, 3, end, 3), fill=WHITE)
    elif tile == 'bronze':
        d.rectangle((0, 0, end, bot), fill=WARM)
        d.rectangle((0, 3, end, 5), fill='#FFE3B9')
    elif tile == 'port':
        d.rectangle((0, 0, end, bot), fill='#2B2137')
        d.polygon([(0, 1), (end, 1), (end-2, 4), (1, 4)], fill='#62536D')
        d.rectangle((2, 6, end-2, 8), fill='#411631')
    else:
        if tile == 'impact':
            d.rectangle((0, 0, end, bot), fill='#171020')
            d.line([(0, 3), (6, 6), (10, 5), (end, 9)], fill='#66506B', width=2)
        d.polygon([(2, 7), (6, 6), (9, 8), (11, 5), (end-2, 5), (end-3, 9),
                   (12, 9), (10, 11), (6, 9), (2, 10)], fill=MAGENTA)
        d.line([(4, 8), (7, 8), (10, 9), (12, 7), (14, 7)], fill=WHITE)
    return img


def charge_texture():
    # Repeat a continuous two-arm spiral on a torus. Drawing wrapped pixels
    # keeps the lines connected when the renderer scrolls across a tile.
    image = Image.new('RGBA', (64, 64))
    for arm in (0, pi):
        for k in range(520):
            t = k/519
            angle = arm + .35 + 5.9*t
            radius = 4 + 34*t
            x = round(23 + radius*cos(angle))
            y = round(31 + radius*sin(angle))
            for dx, dy in ((0, 0), (1, 0), (0, 1)):
                image.putpixel(((x+dx) % 64, (y+dy) % 64), (255, 43, 214, 255))
            if k % 76 in range(6, 24):
                ix = round(23 + (radius-1.4)*cos(angle))
                iy = round(31 + (radius-1.4)*sin(angle))
                image.putpixel((ix % 64, iy % 64), (255, 138, 61, 255))
    return image


def save():
    ASSETS.mkdir(parents=True, exist_ok=True)
    atlas = Image.new('RGBA', SIZE)
    for tile, (x,y,length,width) in ISLANDS.items():
        atlas.paste(paint(tile, length, width).transpose(Image.Transpose.TRANSPOSE), (x,y))
    atlas.save(ASSETS / 'texture.png', optimize=True)
    charge_texture().save(ASSETS / 'charge.png', optimize=True)


if __name__ == '__main__':
    save()
