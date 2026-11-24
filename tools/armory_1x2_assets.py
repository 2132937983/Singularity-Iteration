#!/usr/bin/env python3
"""
Generates the 1x2 Armory (武装库) 16x16 textures, lower/upper block models,
blockstate, and combined 1x2 item model for Singularity Iteration.
"""

import base64
import json
import math
import os
import sys
import uuid
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "mio_icif"
TEX_DIR = ASSETS / "textures" / "block" / "armory"
MODEL_DIR = ASSETS / "models" / "block" / "producer"
ITEM_MODEL = ASSETS / "models" / "item" / "producer" / "block_armory.json"
BLOCKSTATE = ASSETS / "blockstates" / "producer" / "block_armory.json"
IRIS_TEX = ASSETS / "textures" / "block" / "armory_iris.png"
BBMODEL_OUT = ROOT / "tools" / "models" / "armory_1x2.bbmodel"
FORCE = "--force" in sys.argv


def save_png(img: Image.Image, out: Path):
    """Preserve hand-edited textures (in Blockbench or a paint editor) unless --force is passed."""
    out.parent.mkdir(parents=True, exist_ok=True)
    if out.exists() and not FORCE:
        old = Image.open(out).convert("RGBA")
        if old.size != img.size or old.tobytes() != img.convert("RGBA").tobytes():
            print("kept", out.relative_to(ROOT), "(differs from generated: hand-edited? pass --force to replace)")
            return
    img.save(out)


def rgba(hex_str, a=255):
    h = hex_str.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def make_textures():
    TEX_DIR.mkdir(parents=True, exist_ok=True)

    # 1. armory_white_panel.png (16x16 matte white armor panel with crisp seam & rivets)
    im = Image.new("RGBA", (16, 16), rgba("#E4EAF0"))
    px = im.load()
    for y in range(16):
        for x in range(16):
            # subtle top-to-bottom shading
            base = (232 - y, 238 - y, 244 - y, 255)
            px[x, y] = base
    # bevel border
    for i in range(16):
        px[i, 0] = rgba("#F4F7FA")
        px[0, i] = rgba("#EEF2F6")
        px[i, 15] = rgba("#B8C3D0")
        px[15, i] = rgba("#C2CCD8")
    # subtle inner recessed seam frame
    for i in range(2, 14):
        px[i, 2] = rgba("#CCD5DF")
        px[i, 13] = rgba("#D3DCE6")
    # horizontal tech seam at y=8
    for x in range(1, 15):
        px[x, 8] = rgba("#A9B5C4")
        px[x, 9] = rgba("#EFF3F8")
    # 4 corner gunmetal micro-rivets
    for rx, ry in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        px[rx, ry] = rgba("#47515E")
    save_png(im, TEX_DIR / "armory_white_panel.png")

    # 2. armory_gunmetal.png (16x16 dark charcoal military chassis)
    im = Image.new("RGBA", (16, 16), rgba("#2B313A"))
    px = im.load()
    for y in range(16):
        for x in range(16):
            # subtle brushed metallic grain
            d = ((x * 7 + y * 13) % 5) - 2
            r = max(20, min(70, 43 + d * 2))
            g = max(24, min(78, 49 + d * 2))
            b = max(30, min(88, 58 + d * 2))
            px[x, y] = (r, g, b, 255)
    for i in range(16):
        px[i, 0] = rgba("#3D4652")
        px[0, i] = rgba("#38404B")
        px[i, 15] = rgba("#1C2026")
        px[15, i] = rgba("#20252C")
    # machined horizontal groove
    for x in range(2, 14):
        px[x, 5] = rgba("#1B2027")
        px[x, 6] = rgba("#3A434F")
        px[x, 11] = rgba("#1B2027")
    for rx, ry in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        px[rx, ry] = rgba("#546070")
    save_png(im, TEX_DIR / "armory_gunmetal.png")

    # 3. armory_side_vent.png (16x16 recessed cooling louvers)
    im = Image.new("RGBA", (16, 16), rgba("#191E24"))
    px = im.load()
    for i in range(16):
        px[i, 0] = px[i, 15] = px[0, i] = px[15, i] = rgba("#39424E")
        if 1 <= i <= 14:
            px[i, 1] = px[i, 14] = px[1, i] = px[14, i] = rgba("#242B34")
    for y in range(2, 14):
        c = rgba("#36404D") if y % 2 == 0 else rgba("#13171C")
        for x in range(2, 14):
            px[x, y] = c
    # central vertical brace
    for y in range(2, 14):
        px[7, y] = rgba("#2F3844")
        px[8, y] = rgba("#262D37")
    save_png(im, TEX_DIR / "armory_side_vent.png")

    # 4. armory_front_screen.png (OFF) & armory_front_screen_on.png (ON)
    for on in (False, True):
        im = Image.new("RGBA", (16, 16), rgba("#0C1219") if on else rgba("#12171E"))
        px = im.load()
        # outer dark bezel
        for i in range(16):
            px[i, 0] = px[i, 15] = px[0, i] = px[15, i] = rgba("#252C36")
        if not on:
            # faint dead screen grid
            for y in range(2, 14):
                for x in range(2, 14):
                    if x % 3 == 0 or y % 3 == 0:
                        px[x, y] = rgba("#18212B")
            # dim standby LED
            px[13, 13] = rgba("#2D4858")
        else:
            # faint HUD grid
            for y in range(1, 15):
                for x in range(1, 15):
                    if x % 3 == 0 or y % 3 == 0:
                        px[x, y] = rgba("#102A3A")
            # top status header bar
            for x in range(2, 14):
                px[x, 2] = rgba("#26D8FF") if x < 10 else rgba("#0E6E8C")
            # Stark-style mini armor silhouette on left (x=3..7, y=4..12)
            suit_pixels = [
                (5, 4), (4, 5), (5, 5), (6, 5),
                (3, 6), (4, 6), (5, 6), (6, 6), (7, 6),
                (4, 7), (5, 7), (6, 7),
                (4, 8), (5, 8), (6, 8),
                (4, 9), (6, 9),
                (4, 10), (6, 10),
                (4, 11), (6, 11),
            ]
            for sx, sy in suit_pixels:
                px[sx, sy] = rgba("#42E8FF")
            px[5, 6] = rgba("#D0FCFF")  # arc reactor chest core!
            # 6-suit telemetry bars on right (x=9..13, y=4..12)
            for row, length in [(4, 5), (6, 4), (8, 5), (10, 3), (12, 4)]:
                for dx in range(length):
                    px[9 + dx, row] = rgba("#1CC8F2") if dx < length - 1 else rgba("#90F6FF")
            # bottom pulse line
            for x in range(2, 14):
                px[x, 13] = rgba("#1488AA")
            px[13, 13] = rgba("#7CF4FF")
        name = "armory_front_screen_on.png" if on else "armory_front_screen.png"
        save_png(im, TEX_DIR / name)

    # 5. armory_light_off.png & armory_light_on.png (16x16 flush cyan strip)
    for on in (False, True):
        im = Image.new("RGBA", (16, 16), rgba("#19242E"))
        px = im.load()
        for y in range(16):
            for x in range(16):
                edge = x in (0, 15) or y in (0, 15)
                if not on:
                    px[x, y] = rgba("#28333F") if edge else rgba("#1A2B36")
                else:
                    if edge:
                        px[x, y] = rgba("#1298C4")
                    elif 4 <= x <= 11 and 2 <= y <= 13:
                        px[x, y] = rgba("#98F8FF")
                    else:
                        px[x, y] = rgba("#30E2FF")
        save_png(im, TEX_DIR / ("armory_light_on.png" if on else "armory_light_off.png"))

    # 6. armory_well.png & armory_well_on.png (16x16 recessed silo pit floor)
    for on in (False, True):
        im = Image.new("RGBA", (16, 16), rgba("#1A2028"))
        px = im.load()
        for y in range(16):
            for x in range(16):
                dx = x - 7.5
                dy = y - 7.5
                dist = math.hypot(dx, dy)
                if dist < 2.2:
                    px[x, y] = rgba("#A8FAFF") if on else rgba("#131922")
                elif dist < 3.6:
                    px[x, y] = rgba("#24D8FF") if on else rgba("#222C38")
                elif 4.8 <= dist <= 6.2:
                    px[x, y] = rgba("#1CBEE8") if on else rgba("#2E3744")
                elif abs(dx) < 0.8 or abs(dy) < 0.8:
                    px[x, y] = rgba("#2D3642")
                else:
                    px[x, y] = rgba("#1A2028")
        save_png(im, TEX_DIR / ("armory_well_on.png" if on else "armory_well.png"))

    # 7. armory_iris.png (16x16 crisp white/gunmetal armored iris blades with cyan lock ring)
    im = Image.new("RGBA", (16, 16), rgba("#2A313B"))
    px = im.load()
    for y in range(16):
        for x in range(16):
            dx = x - 7.5
            dy = y - 7.5
            dist = math.hypot(dx, dy)
            if dist > 7.6:
                px[x, y] = rgba("#1F252E")
            elif dist > 6.2:
                px[x, y] = rgba("#3B4552")
            elif 4.4 <= dist <= 5.4:
                px[x, y] = rgba("#DCE4EC")  # matte white armor trim ring on the blades
            elif 2.3 <= dist <= 3.1:
                px[x, y] = rgba("#24C4E8")  # cyan lock ring
            elif dist < 2.3:
                px[x, y] = rgba("#242B34")
            else:
                px[x, y] = rgba("#303844")
    # subtle radial blade seam lines
    for i in range(16):
        if abs(i - 7.5) > 1.5:
            px[i, 7] = rgba("#1E242C")
            px[7, i] = rgba("#1E242C")
    save_png(im, IRIS_TEX)


def box(frm, to, tex_map, rot=None, emissive=False, shade=True):
    """Build a standard Minecraft 1.21.1 block model element."""
    faces = {}
    x0, y0, z0 = frm
    x1, y1, z1 = to
    for face, tex in tex_map.items():
        if tex is None:
            continue
        if isinstance(tex, tuple):
            t_ref, uv = tex
        else:
            t_ref = tex
            # Auto world-aligned 16x16 UV clamping within [0, 16]
            if face in ("north", "south"):
                u0, v0, u1, v1 = x0, 16 - y1, x1, 16 - y0
            elif face in ("east", "west"):
                u0, v0, u1, v1 = z0, 16 - y1, z1, 16 - y0
            else:
                u0, v0, u1, v1 = x0, z0, x1, z1
            uv = [
                round(max(0.0, min(16.0, u0)), 2),
                round(max(0.0, min(16.0, v0)), 2),
                round(max(0.0, min(16.0, u1)), 2),
                round(max(0.0, min(16.0, v1)), 2),
            ]
        f_obj = {"uv": uv, "texture": t_ref}
        if emissive:
            f_obj["neoforge_data"] = {"block_light": 15, "sky_light": 15, "ambient_occlusion": False}
        faces[face] = f_obj
    el = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to], "faces": faces}
    if rot is not None:
        el["rotation"] = rot
    if not shade:
        el["shade"] = False
    return el


def all_faces(tex):
    return {f: tex for f in ("north", "south", "east", "west", "up", "down")}


def build_lower_elements(on: bool):
    W = "#white"
    G = "#gunmetal"
    V = "#vent"
    S = "#screen"
    L = "#light"
    P = "#well"

    els = []

    # 1. Heavy beveled gunmetal base plinth (y=0..3.5)
    els.append(box([0, 0, 0], [16, 2.5, 16], all_faces(G)))
    els.append(box([0.5, 2.5, 0.5], [15.5, 3.5, 15.5], all_faces(G)))
    # 4 corner heavy armored foot bumpers
    for x0, x1 in [(0, 3.2), (12.8, 16)]:
        for z0, z1 in [(0, 3.2), (12.8, 16)]:
            els.append(box([x0, 0, z0], [x1, 3.6, z1], all_faces(G)))

    # 2. Inner recessed mechanical core (y=3.5..11.2)
    els.append(box([1.2, 3.5, 1.2], [14.8, 11.2, 14.8], all_faces(G)))

    # 3. Recessed cooling vents (West, East, South rear)
    els.append(box([0.8, 5.2, 4.0], [1.4, 10.4, 12.0], {"west": V, "up": G, "down": G, "north": G, "south": G}))
    els.append(box([14.6, 5.2, 4.0], [15.2, 10.4, 12.0], {"east": V, "up": G, "down": G, "north": G, "south": G}))
    els.append(box([3.8, 4.2, 14.6], [12.2, 10.8, 15.3], {"south": V, "up": G, "down": G, "west": G, "east": G}))

    # 4. Segmented matte-white corner armor pillars (y=3.5..15.5)
    for x0, x1 in [(0.2, 3.4), (12.6, 15.8)]:
        for z0, z1 in [(0.2, 3.4), (12.6, 15.8)]:
            els.append(box([x0, 3.5, z0], [x1, 15.5, z1], all_faces(W)))

    # 5. Matte-white upper side cheeks (supporting twin gantry mounts on West & East)
    els.append(box([0.2, 11.4, 3.4], [2.4, 16.0, 12.6], all_faces(W)))
    els.append(box([13.6, 11.4, 3.4], [15.8, 16.0, 12.6], all_faces(W)))
    # Front upper brow & rear upper parapet framing the recessed well
    els.append(box([3.4, 12.4, 0.2], [12.6, 15.5, 2.4], all_faces(W)))
    els.append(box([3.4, 11.8, 13.6], [12.6, 15.5, 15.8], all_faces(W)))
    # Lower white armor chin & side skirts
    els.append(box([3.4, 3.5, 0.3], [12.6, 5.0, 1.5], all_faces(W)))
    els.append(box([0.3, 3.5, 3.6], [1.3, 5.1, 12.4], all_faces(W)))
    els.append(box([14.7, 3.5, 3.6], [15.7, 5.1, 12.4], all_faces(W)))

    # 6. Front recessed Stark-style console bezel & screen (North face)
    els.append(box([3.8, 5.0, 0.6], [12.2, 12.4, 1.4], all_faces(G)))
    els.append(box(
        [4.5, 5.8, 0.45], [11.5, 11.6, 0.7],
        {"north": (S, [0, 0, 16, 16]), "up": G, "down": G, "west": G, "east": G},
        emissive=on, shade=not on
    ))

    # 7. Flush cyan status light strips (front vertical, side horizontal, rear horizontal)
    # Front vertical strips flanking the console
    els.append(box([3.25, 5.2, 0.15], [3.75, 12.2, 0.55], all_faces(L), emissive=on, shade=not on))
    els.append(box([12.25, 5.2, 0.15], [12.75, 12.2, 0.55], all_faces(L), emissive=on, shade=not on))
    # Side horizontal strips above vents
    els.append(box([0.15, 10.6, 4.0], [0.75, 11.25, 12.0], all_faces(L), emissive=on, shade=not on))
    els.append(box([15.25, 10.6, 4.0], [15.85, 11.25, 12.0], all_faces(L), emissive=on, shade=not on))
    # Rear horizontal status strip
    els.append(box([4.2, 11.0, 15.4], [11.8, 11.65, 15.85], all_faces(L), emissive=on, shade=not on))

    # 8. True recessed octagonal launch silo well (y=11.2..15.5)
    # Glowing core floor at y=11.5
    els.append(box(
        [2.4, 11.0, 2.4], [13.6, 11.5, 13.6],
        {"up": (P, [0, 0, 16, 16])},
        emissive=on, shade=not on
    ))
    # Inner gunmetal well walls (N, S, W, E)
    els.append(box([2.4, 11.5, 2.2], [13.6, 15.6, 2.8], all_faces(G)))
    els.append(box([2.4, 11.5, 13.2], [13.6, 15.6, 13.8], all_faces(G)))
    els.append(box([2.2, 11.5, 2.8], [2.8, 15.6, 13.2], all_faces(G)))
    els.append(box([13.2, 11.5, 2.8], [13.8, 15.6, 13.2], all_faces(G)))
    # 4 diagonal 45° corner baffles forming an octagonal silo pit (tucked cleanly inside corner pillars)
    for cx, cz, ang in [(3.6, 3.6, 45), (12.4, 3.6, -45), (3.6, 12.4, -45), (12.4, 12.4, 45)]:
        els.append(box(
            [cx - 1.2, 11.5, cz - 0.5], [cx + 1.2, 15.5, cz + 0.5],
            all_faces(G),
            rot={"origin": [cx, 13.5, cz], "axis": "y", "angle": ang}
        ))

    return els


def build_upper_elements(on: bool):
    """
    Direction 1: Twin Linear Electromagnetic Railgun Pylons (双立柱直线电磁导轨).
    Open-top vertical mass-driver corridor with heavy recoil buttresses, rigid vertical
    conductance tracks, 3-stage electromagnetic stator clamps, mid-rail maintenance laser
    emitters, and outward-flared anti-arc discharge horns.
    """
    W = "#white"
    G = "#gunmetal"
    V = "#vent"
    L = "#light"

    els = []

    # 1. Heavy recoil-absorbing base sockets & angled buttress braces (y=0..4.5)
    # West base socket + cooling vent + angled outer recoil brace
    els.append(box([0.2, 0.0, 4.0], [3.3, 3.6, 12.0], all_faces(G)))
    els.append(box([0.0, 0.0, 4.8], [1.4, 4.6, 11.2], all_faces(W)))
    els.append(box([0.3, 0.4, 5.6], [1.6, 4.4, 10.4], all_faces(W),
                   rot={"origin": [1.0, 0.4, 8.0], "axis": "z", "angle": -22.5}))
    # East base socket + angled outer recoil brace
    els.append(box([12.7, 0.0, 4.0], [15.8, 3.6, 12.0], all_faces(G)))
    els.append(box([14.6, 0.0, 4.8], [16.0, 4.6, 11.2], all_faces(W)))
    els.append(box([14.4, 0.4, 5.6], [15.7, 4.4, 10.4], all_faces(W),
                   rot={"origin": [15.0, 0.4, 8.0], "axis": "z", "angle": 22.5}))

    # 2. Rigid vertical electromagnetic rail columns (y=3.6..13.6)
    # West pylon: outer white armor spine + recessed side vent + inner gunmetal conductive rail
    els.append(box([0.4, 4.2, 5.2], [2.1, 13.4, 10.8], all_faces(W)))
    els.append(box([0.25, 5.4, 6.2], [0.6, 11.6, 9.8], {"west": V, "up": G, "down": G, "north": G, "south": G}))
    # Front & back white rib flanges framing the conductive rail
    els.append(box([1.8, 3.6, 5.0], [3.1, 13.2, 6.3], all_faces(G)))
    els.append(box([1.8, 3.6, 9.7], [3.1, 13.2, 11.0], all_faces(G)))
    # Inner recessed conductive track core (facing +X toward launch axis)
    els.append(box([2.0, 3.6, 6.3], [2.9, 13.4, 9.7], all_faces(G)))
    # Vertical continuous cyan electromagnetic acceleration bus inside West rail channel
    els.append(box([2.85, 3.8, 7.4], [3.15, 13.0, 8.6], all_faces(L), emissive=on, shade=not on))

    # East pylon (exact mirror)
    els.append(box([13.9, 4.2, 5.2], [15.6, 13.4, 10.8], all_faces(W)))
    els.append(box([15.4, 5.4, 6.2], [15.75, 11.6, 9.8], {"east": V, "up": G, "down": G, "north": G, "south": G}))
    els.append(box([12.9, 3.6, 5.0], [14.2, 13.2, 6.3], all_faces(G)))
    els.append(box([12.9, 3.6, 9.7], [14.2, 13.2, 11.0], all_faces(G)))
    els.append(box([13.1, 3.6, 6.3], [14.0, 13.4, 9.7], all_faces(G)))
    els.append(box([12.85, 3.8, 7.4], [13.15, 13.0, 8.6], all_faces(L), emissive=on, shade=not on))

    # 3. 3-Stage Electromagnetic Stator Clamps (y=4.4, 8.0, 11.6) wrapping each vertical rail
    for sy in (4.4, 8.0, 11.6):
        # West stator collar + inner emitter pad + outer white accent cap
        els.append(box([0.2, sy, 4.7], [3.4, sy + 1.1, 11.3], all_faces(G)))
        els.append(box([0.1, sy + 0.15, 5.3], [1.6, sy + 0.95, 10.7], all_faces(W)))
        els.append(box([3.25, sy + 0.2, 6.5], [3.65, sy + 0.9, 9.5], all_faces(L), emissive=on, shade=not on))
        # East stator collar + inner emitter pad + outer white accent cap
        els.append(box([12.6, sy, 4.7], [15.8, sy + 1.1, 11.3], all_faces(G)))
        els.append(box([14.4, sy + 0.15, 5.3], [15.9, sy + 0.95, 10.7], all_faces(W)))
        els.append(box([12.35, sy + 0.2, 6.5], [12.75, sy + 0.9, 9.5], all_faces(L), emissive=on, shade=not on))

    # 4. Mid-rail precision maintenance laser emitter heads (between stage 1 and stage 2)
    els.append(box([3.0, 6.1, 6.8], [3.85, 7.4, 9.2], all_faces(G)))
    els.append(box([3.75, 6.4, 7.4], [4.0, 7.1, 8.6], all_faces(L), emissive=on, shade=not on))
    els.append(box([12.15, 6.1, 6.8], [13.0, 7.4, 9.2], all_faces(G)))
    els.append(box([12.0, 6.4, 7.4], [12.25, 7.1, 8.6], all_faces(L), emissive=on, shade=not on))

    # 5. Open-top outward-flared anti-arc discharge horns / magnetic guide vanes (y=13.0..15.8)
    # West top cap & outward-angled (+22.5° around Z leans outward to -X) discharge horn
    els.append(box([0.7, 13.0, 5.6], [2.8, 14.2, 10.4], all_faces(G)))
    els.append(box([0.7, 13.6, 6.0], [2.2, 15.9, 10.0], all_faces(W),
                   rot={"origin": [1.6, 13.6, 8.0], "axis": "z", "angle": 22.5}))
    els.append(box([2.2, 13.6, 7.1], [2.9, 15.4, 8.9], all_faces(L),
                   rot={"origin": [2.2, 13.6, 8.0], "axis": "z", "angle": 22.5},
                   emissive=on, shade=not on))

    # East top cap & outward-angled (-22.5° around Z leans outward to +X) discharge horn
    els.append(box([13.2, 13.0, 5.6], [15.3, 14.2, 10.4], all_faces(G)))
    els.append(box([13.8, 13.6, 6.0], [15.3, 15.9, 10.0], all_faces(W),
                   rot={"origin": [14.4, 13.6, 8.0], "axis": "z", "angle": -22.5}))
    els.append(box([13.1, 13.6, 7.1], [13.8, 15.4, 8.9], all_faces(L),
                   rot={"origin": [13.8, 13.6, 8.0], "axis": "z", "angle": -22.5},
                   emissive=on, shade=not on))

    return els


def textures_dict(on: bool):
    return {
        "particle": "mio_icif:block/armory/armory_white_panel",
        "white": "mio_icif:block/armory/armory_white_panel",
        "gunmetal": "mio_icif:block/armory/armory_gunmetal",
        "vent": "mio_icif:block/armory/armory_side_vent",
        "screen": f"mio_icif:block/armory/armory_front_screen{'_on' if on else ''}",
        "light": f"mio_icif:block/armory/armory_light_{'on' if on else 'off'}",
        "well": f"mio_icif:block/armory/armory_well{'_on' if on else ''}",
    }


def write_models():
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    for on in (False, True):
        suffix = "_on" if on else ""
        lower_model = {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": textures_dict(on),
            "elements": build_lower_elements(on),
        }
        upper_model = {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": textures_dict(on),
            "elements": build_upper_elements(on),
        }
        (MODEL_DIR / f"block_armory{suffix}.json").write_text(
            json.dumps(lower_model, indent=2) + "\n", encoding="utf-8"
        )
        (MODEL_DIR / f"block_armory_upper{suffix}.json").write_text(
            json.dumps(upper_model, indent=2) + "\n", encoding="utf-8"
        )

    # Combined 1x2 item model (lit=true silhouette looks sleek in inventory & guidebook!)
    item_elements = []
    # Scale both halves by 0.5 around center (8, 0, 8) so the full 1x2 structure fits in [4..12, 0..16, 4..12]?
    # Better: MC item models allow coordinates in [-16, 32]! Lower is y=0..16, upper is y=16..30.1!
    # All coordinates stay strictly within [0, 31], and display transforms center & scale it nicely!
    for el in build_lower_elements(True):
        item_elements.append(el)
    for el in build_upper_elements(True):
        shifted = json.loads(json.dumps(el))
        shifted["from"][1] = round(shifted["from"][1] + 16.0, 3)
        shifted["to"][1] = round(shifted["to"][1] + 16.0, 3)
        if "rotation" in shifted:
            shifted["rotation"]["origin"][1] = round(shifted["rotation"]["origin"][1] + 16.0, 3)
        item_elements.append(shifted)

    item_model = {
        "parent": "minecraft:block/block",
        "textures": textures_dict(True),
        "elements": item_elements,
        "display": {
            "gui": {"rotation": [28, 135, 0], "translation": [0, -3.2, 0], "scale": [0.44, 0.44, 0.44]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 1.5, 0], "scale": [0.22, 0.22, 0.22]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, -3.5, 0], "scale": [0.42, 0.42, 0.42]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 1.5, -0.5], "scale": [0.28, 0.28, 0.28]},
            "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 1.5, -0.5], "scale": [0.28, 0.28, 0.28]},
            "firstperson_righthand": {"rotation": [0, 135, 0], "translation": [0, -1.0, 0], "scale": [0.30, 0.30, 0.30]},
            "firstperson_lefthand": {"rotation": [0, 135, 0], "translation": [0, -1.0, 0], "scale": [0.30, 0.30, 0.30]},
        },
    }
    ITEM_MODEL.write_text(json.dumps(item_model, indent=2) + "\n", encoding="utf-8")

    # Blockstate: facing x lit x half
    variants = {}
    y_rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    for half in ("lower", "upper"):
        for lit in ("false", "true"):
            for facing, rot in y_rot.items():
                model = "mio_icif:block/producer/block_armory"
                if half == "upper":
                    model += "_upper"
                if lit == "true":
                    model += "_on"
                entry = {"model": model}
                if rot != 0:
                    entry["y"] = rot
                variants[f"facing={facing},half={half},lit={lit}"] = entry
    BLOCKSTATE.write_text(json.dumps({"variants": variants}, indent=2) + "\n", encoding="utf-8")


def write_bbmodel():
    """Export an editable Blockbench java_block project (tools/models/armory_1x2.bbmodel) with embedded 16x16 textures."""
    BBMODEL_OUT.parent.mkdir(parents=True, exist_ok=True)
    tex_keys = [
        ("white", TEX_DIR / "armory_white_panel.png"),
        ("gunmetal", TEX_DIR / "armory_gunmetal.png"),
        ("vent", TEX_DIR / "armory_side_vent.png"),
        ("screen", TEX_DIR / "armory_front_screen_on.png"),
        ("light", TEX_DIR / "armory_light_on.png"),
        ("well", TEX_DIR / "armory_well_on.png"),
    ]
    textures = []
    key_to_idx = {}
    for idx, (key, path) in enumerate(tex_keys):
        key_to_idx[key] = idx
        b64 = base64.b64encode(path.read_bytes()).decode("ascii")
        textures.append({
            "path": str(path.relative_to(ROOT)).replace("\\", "/"),
            "name": path.name,
            "folder": "block/armory",
            "namespace": "mio_icif",
            "id": str(idx),
            "width": 16,
            "height": 16,
            "uv_width": 16,
            "uv_height": 16,
            "particle": key == "white",
            "visible": True,
            "mode": "bitmap",
            "saved": True,
            "uuid": str(uuid.uuid5(uuid.NAMESPACE_DNS, f"mio_icif.armory.tex.{key}")),
            "source": f"data:image/png;base64,{b64}",
        })

    bb_elements = []
    lower_ids = []
    upper_ids = []

    def convert(els, y_off, prefix, id_list):
        for i, el in enumerate(els):
            uid = str(uuid.uuid5(uuid.NAMESPACE_DNS, f"mio_icif.armory.{prefix}.{i}"))
            id_list.append(uid)
            frm = [el["from"][0], round(el["from"][1] + y_off, 3), el["from"][2]]
            to = [el["to"][0], round(el["to"][1] + y_off, 3), el["to"][2]]
            faces = {}
            for fname in ("north", "east", "south", "west", "up", "down"):
                if fname in el["faces"]:
                    fobj = el["faces"][fname]
                    tkey = fobj["texture"].lstrip("#")
                    faces[fname] = {"uv": fobj["uv"], "texture": key_to_idx[tkey]}
                else:
                    faces[fname] = {"uv": [0, 0, 16, 16], "texture": None}
            obj = {
                "name": f"{prefix}_{i:02d}",
                "box_uv": False,
                "rescale": False,
                "locked": False,
                "light_emission": 15 if any("neoforge_data" in f for f in el["faces"].values()) else 0,
                "from": frm,
                "to": to,
                "autouv": 0,
                "color": 0,
                "origin": [8, 8 + y_off, 8],
                "faces": faces,
                "type": "cube",
                "uuid": uid,
            }
            if "rotation" in el:
                rot = el["rotation"]
                obj["origin"] = [rot["origin"][0], round(rot["origin"][1] + y_off, 3), rot["origin"][2]]
                ax = rot["axis"]
                obj["rotation"] = [
                    rot["angle"] if ax == "x" else 0,
                    rot["angle"] if ax == "y" else 0,
                    rot["angle"] if ax == "z" else 0,
                ]
            bb_elements.append(obj)

    convert(build_lower_elements(True), 0.0, "lower_silo", lower_ids)
    convert(build_upper_elements(True), 16.0, "upper_railgun", upper_ids)

    bb = {
        "meta": {"format_version": "4.10", "model_format": "java_block", "box_uv": False},
        "name": "armory_1x2",
        "model_identifier": "mio_icif:producer/block_armory",
        "visible_box": [1, 2, 1],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 16, "height": 16},
        "elements": bb_elements,
        "outliner": [
            {
                "name": "lower_silo",
                "origin": [8, 8, 8],
                "color": 0,
                "uuid": str(uuid.uuid5(uuid.NAMESPACE_DNS, "mio_icif.armory.grp.lower")),
                "export": True,
                "mirror_uv": False,
                "isOpen": True,
                "locked": False,
                "visibility": True,
                "autouv": 0,
                "children": lower_ids,
            },
            {
                "name": "upper_railgun_pylons",
                "origin": [8, 24, 8],
                "color": 1,
                "uuid": str(uuid.uuid5(uuid.NAMESPACE_DNS, "mio_icif.armory.grp.upper")),
                "export": True,
                "mirror_uv": False,
                "isOpen": True,
                "locked": False,
                "visibility": True,
                "autouv": 0,
                "children": upper_ids,
            },
        ],
        "textures": textures,
    }
    BBMODEL_OUT.write_text(json.dumps(bb, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    make_textures()
    write_models()
    write_bbmodel()
    print("Generated Armory 1x2 textures, models, item model, blockstate, and armory_1x2.bbmodel successfully.")
