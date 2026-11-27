#!/usr/bin/env python3
"""Render AN1 from the exported GeckoLib rig and clips (NumPy + Pillow)."""
import argparse
import json
import math
from functools import lru_cache
from itertools import product
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parents[2]
SPEC = ROOT / "docs/art/an1/spec.json"
PREVIEWS = ROOT / "docs/art/previews"
FPS = 20
# Contract space: Y up, face towards -Z, the rig's right arm is at -X.
# Each row of a camera matrix gives screen right, screen up and depth towards the viewer.
FRONT = np.array([[1, 0, 0], [0, 1, 0], [0, 0, -1]])
RIGHT = np.array([[0, 0, -1], [0, 1, 0], [-1, 0, 0]])
CORNERS = np.array(list(product((0, 1), repeat=3)))
FACES = (
    ("north", (2, 6, 4, 0), (0, 0, -1)), ("south", (7, 3, 1, 5), (0, 0, 1)),
    ("west", (3, 2, 0, 1), (-1, 0, 0)), ("east", (6, 7, 5, 4), (1, 0, 0)),
    ("up", (3, 7, 6, 2), (0, 1, 0)), ("down", (0, 4, 5, 1), (0, -1, 0)),
)
# Face patterns use one texel per model unit. They are preview materials on the
# six exported cuboids and do not add parts or write a skin to the resource pack.
SWATCH = np.array([
    (36, 48, 56), (58, 76, 85), (91, 114, 121), (151, 172, 172),
    (210, 223, 218), (242, 241, 222), (31, 129, 140), (85, 229, 218),
    (190, 113, 53), (247, 182, 92),
], dtype=float)
LIGHT = np.array([-.38, .67, -.63])
CANVAS, TOP, INK, MUTE, RULE = "#edf1ec", "#203238", "#23383d", "#50686b", "#becbc6"
STAGE, GROUND, EVENT = "#dbe4de", "#6d8781", "#e1363e"
UI_FONT = None

# The overview keeps full exported clip names under the representative poses.
ROWS = (
    ("校准与待机", ("calibrate", "idle_stand", "idle_look", "idle_check")),
    ("地面移动", ("walk", "run", "skate", "cover_crouch")),
    ("起跳与腾空", ("jump_takeoff", "thrust_hop", "air_rise", "air_fall")),
    ("落点与回稳", ("land_light", "land_heavy", "step_up", "stagger")),
    ("原地转向", ("turn_l", "turn_r", "lowpower_slump", "lowpower_wake")),
    ("受击方向", ("hit_front", "hit_back", "hit_left", "hit_right")),
    ("锁定与连击", ("glare", "melee_1", "melee_2", "melee_3")),
    ("重击与位移", ("melee_heavy", "kick", "dash_stab", "shove")),
    ("下砸与掩体", ("slam_fall", "slam_impact", "peek_left", "peek_right")),
    ("瞄准与后坐", ("aim_rifle", "aim_pistol", "recoil_light", "recoil_heavy")),
    ("装填与弓弩", ("reload", "crossbow_reload", "bow_draw", "bow_release")),
    ("投掷与反应", ("spear_throw", "intercept", "kill_flourish")),
)
ZH = dict(zip((key for _, row in ROWS for key in row), (
    "坐标校准", "站立呼吸", "环顾", "系统自检", "步行", "跑步", "推进滑行", "掩体半蹲",
    "普通起跳", "推进跳", "上升", "下落", "轻落地", "重落地", "跨台阶", "踉跄回稳",
    "左转", "右转", "低电量跪落", "重新上电", "正面受击", "背面受击", "左侧受击", "右侧受击",
    "锁定目标", "连击一 · 斜劈", "连击二 · 横扫", "连击三 · 下劈",
    "双手重击", "正蹬", "冲刺突刺", "护主肩撞",
    "空中蓄势", "下砸接地", "向左探身", "向右探身",
    "双手瞄准", "单手瞄准", "轻后坐", "重后坐",
    "更换弹匣", "弩上弦", "拉弓", "松弦",
    "投掷三叉戟", "拦截", "收势点头",
)))


@lru_cache(maxsize=None)
def font(size, bold=False, mono=False):
    if mono:
        candidates = ("consolab.ttf" if bold else "consola.ttf", "DejaVuSansMono.ttf")
    else:
        variant = "Bold" if bold else "Regular"
        candidates = (UI_FONT, "msyhbd.ttc" if bold else "msyh.ttc",
                      f"NotoSansCJK-{variant}.ttc", f"/usr/share/fonts/opentype/noto/NotoSansCJK-{variant}.ttc",
                      "/System/Library/Fonts/PingFang.ttc")
    for candidate in filter(None, candidates):
        try:
            return ImageFont.truetype(candidate, size)
        except OSError:
            continue
    if mono:
        return ImageFont.load_default(size=size)
    raise SystemExit("Install a CJK font or use --font <path>")


def load_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def face_pixels(bone, side, w, h):
    # Indexed appearance for visible seams, visor, glove and foot; rear faces
    # are intentionally different so a turned head/hand remains legible in profile.
    img = Image.new("L", (w, h), 3)
    d = ImageDraw.Draw(img)
    front = side == "north"
    if side in ("up", "down"):
        d.rectangle((0, 0, w-1, h-1), fill=4 if side == "up" else 1)
        if side == "up" and w > 2 and h > 2:
            d.line((1, 1, w-2, 1), fill=3)
    elif bone == "head":
        d.rectangle((0, 0, w-1, h-1), fill=4)
        d.rectangle((0, 2, w-1, 4), fill=0 if front else 1)
        if front:
            d.line((1, 3, w-2, 3), fill=7)
            d.rectangle((2, h-2, w-3, h-1), fill=2)
        elif side in ("west", "east"):
            d.rectangle((w//2-1, 3, w//2, 5), fill=2)
            d.point((w//2-1, 3), fill=3)
            front_edge = w - 1 if side == "west" else 0
            d.point((front_edge, 3), fill=7)
    elif bone == "body":
        d.rectangle((0, 0, w-1, h-1), fill=1)
        if front:
            d.polygon(((0, 0), (w-1, 0), (w-1, 3), (w-3, 5), (2, 5), (0, 3)), fill=4)
            d.line((2, 2, w-3, 2), fill=6)
            d.line((1, h-2, w-2, h-2), fill=2)
        else:
            d.rectangle((0, 1, w-1, 4), fill=3 if side != "south" else 2)
            d.line((0, 6, w-1, 6), fill=2)
    else:
        arm = bone.endswith("arm")
        trim = 9 if bone.startswith("right") else 7
        d.rectangle((0, 0, w-1, h-1), fill=2)
        d.rectangle((0, 0, w-1, 3 if arm else 4), fill=4 if arm else 3)
        d.line((0, 2 if arm else 3, w-1, 2 if arm else 3), fill=trim)
        d.rectangle((0, 5, w-1, h-4), fill=3)
        if not arm:
            d.rectangle((0, h-3, w-1, h-1), fill=1)
        else:
            d.rectangle((0, h-3, w-1, h-1), fill=0)
            d.line((0, h-4, w-1, h-4), fill=trim)
    return np.asarray(img)


def rig_geometry(geo):
    bones = geo["minecraft:geometry"][0]["bones"]
    geometry = {}
    for b in bones:
        cubes = []
        for c in b.get("cubes", []):
            size = np.array(c["size"], dtype=float)
            origin = np.array(c["origin"], dtype=float)
            inflate = c.get("inflate", 0)
            vertices = origin - inflate + CORNERS * (size + 2*inflate)
            materials = []
            for side, indices, normal in FACES:
                p = vertices[list(indices)]
                w, h = (max(1, round(np.linalg.norm(p[j]-p[0]))) for j in (1, 3))
                materials.append((list(indices), np.array(normal), face_pixels(b["name"], side, w, h)))
            cubes.append((vertices, materials))
        geometry[b["name"]] = (b.get("parent"), np.array(b["pivot"], dtype=float), cubes)
    return geometry


def tracks(clip):
    result = {}
    for name, channels in clip["bones"].items():
        result[name] = {}
        for channel, keys in channels.items():
            # AN1 exports numeric vectors, staggered or densely sampled to
            # hand-author accelerations; GeckoLib's default between them is linear.
            ordered = sorted((float(t), value) for t, value in keys.items())
            result[name][channel] = (np.array([t for t, _ in ordered]), np.array([v for _, v in ordered]))
    return result


def interpolate(channel, t):
    if channel is None:
        return np.zeros(3)
    times, values = channel
    return np.array([np.interp(t, times, axis) for axis in values.T])


def world_pose(rig, channels, t):
    matrices = {}
    def transform(name):
        if name in matrices:
            return matrices[name]
        parent, pivot, _ = rig[name]
        ch = channels.get(name, {})
        angles = interpolate(ch.get("rotation"), t) * [-1, 1, -1]
        x, y, z = np.deg2rad(angles)
        sx, sy, sz = np.sin((x, y, z))
        cx, cy, cz = np.cos((x, y, z))
        # Rz(-rawZ) @ Ry(rawY) @ Rx(-rawX) in Bedrock JSON geometry coordinates.
        rotation = np.array([[cz*cy, cz*sy*sx-sz*cx, cz*sy*cx+sz*sx],
                             [sz*cy, sz*sy*sx+cz*cx, sz*sy*cx-cz*sx],
                             [-sy, cy*sx, cy*cx]])
        local = np.eye(4)
        local[:3,:3] = rotation
        local[:3,3] = pivot + interpolate(ch.get("position"), t) - rotation @ pivot
        matrices[name] = transform(parent) @ local if parent else local
        return matrices[name]
    for name in rig:
        transform(name)
    return matrices


def posed_cubes(rig, channels, t):
    matrices = world_pose(rig, channels, t)
    result = []
    for name, (_, _, cubes) in rig.items():
        matrix = matrices[name]
        for vertices, material in cubes:
            world = vertices @ matrix[:3,:3].T + matrix[:3,3]
            result.append((world, material, matrix[:3,:3]))
    return result


def mannequin(cubes, camera, size, scale, ground):
    w,h = size
    rgb = np.zeros((h,w,3), dtype=np.uint8)
    depth = np.full((h,w), -np.inf)
    for world, materials, rotation in cubes:
        screen = world @ camera.T
        screen[:,0] = screen[:,0]*scale + w/2
        screen[:,1] = ground - screen[:,1]*scale
        for indices, local_normal, tex in materials:
            normal = rotation @ local_normal
            if normal @ camera[2] < 1e-7:
                continue
            quad = screen[indices]
            x0,y0 = np.maximum(0, np.floor(quad[:,:2].min(axis=0)).astype(int))
            x1,y1 = np.minimum((w,h), np.ceil(quad[:,:2].max(axis=0)).astype(int))
            if x0 >= x1 or y0 >= y1:
                continue
            a,b = quad[1]-quad[0], quad[3]-quad[0]
            det = a[0]*b[1]-a[1]*b[0]
            if abs(det) < 1e-9:
                continue
            yy,xx = np.ogrid[y0:y1,x0:x1]
            dx,dy = xx+.5-quad[0,0], yy+.5-quad[0,1]
            u,v = (dx*b[1]-dy*b[0])/det, (dy*a[0]-dx*a[1])/det
            z = quad[0,2] + u*a[2]+v*b[2]
            region = np.s_[y0:y1,x0:x1]
            mask = (u>=0)&(u<1)&(v>=0)&(v<1)&(z>depth[region])
            th,tw = tex.shape
            colors = SWATCH[tex[np.clip((v*th).astype(int),0,th-1),np.clip((u*tw).astype(int),0,tw-1)]]
            shade = .77 + .23 * max(0., normal @ LIGHT)
            rgb[region][mask] = np.rint(colors[mask]*shade).astype(np.uint8)
            depth[region][mask] = z[mask]
    body_mask = Image.fromarray(np.isfinite(depth).astype(np.uint8)*255)
    return Image.fromarray(rgb), body_mask


def stage(cubes, camera, size, scale, ground, marked=False):
    image = Image.new("RGB", size, STAGE)
    w,h = size
    draw = ImageDraw.Draw(image)
    draw.line((0,ground,w,ground), fill=GROUND)
    stride = max(6, round(4*scale))
    for x in range(w//2 % stride,w,stride):
        draw.line((x,ground+1,x,ground+3),fill=RULE)
    figure,mask = mannequin(cubes,camera,size,scale,ground)
    contour = mask.filter(ImageFilter.MaxFilter(5 if marked else 3))
    image.paste(EVENT if marked else "#819490", mask=contour)
    image.paste(figure, mask=mask)
    return image


def timeline(clip):
    length = clip["animation_length"]
    count = round(length * FPS)
    looping = clip.get("loop") in (True, "loop")
    duration = count * 2 if looping else count + FPS // 2
    marks = {}
    for when, values in clip.get("timeline", {}).items():
        tick = math.floor(float(when) * FPS + 0.5)
        values = values if isinstance(values, list) else [values]
        label = " + ".join(str(value).rstrip(";") for value in values)
        marks[tick] = (label, float(when))
    frames = []
    for i in range(duration):
        tick = i % count if looping else min(i, count)
        event = marks.get(tick) if looping or i <= count else None
        held_frames = i - count if not looping and i >= count else None
        frames.append((min(tick / FPS, length), event, held_frames))
    return frames


def fit(poses, size, ground, max_scale):
    vertices = np.concatenate([world for pose in poses for world, _, _ in pose])
    radius = max(np.abs(vertices[:, [0, 2]]).max(), 1.0)
    height = max(32.0, vertices[:, 1].max())
    return min(max_scale, (ground - 9) / height, (size[0] / 2 - 7) / radius)


def representative(name, channels, samples, length):
    # Strike contacts must use the event frame even if the recoil or compression
    # that follows has greater joint displacement (notably slam_impact at 0 s).
    for i, (_, event, _) in enumerate(samples):
        if event and "impact" in event[0].split(" + "):
            return i
    times = [frame[0] for frame in samples]
    # Pick readable contacts, anticipation and departures. Use maximum joint
    # displacement for clips without a designated pose in the contact sheet.
    chosen = {
        "calibrate": length, "lowpower_slump": length, "lowpower_wake": length * 0.42,
        "land_heavy": 0.20, "land_light": 0.10, "walk": 0.0, "run": 0.25,
        "jump_takeoff": 0.20, "thrust_hop": 0.10, "step_up": 0.20,
        "glare": 0.10, "slam_fall": 0.15, "peek_left": length, "peek_right": length,
        "aim_rifle": 0.0, "aim_pistol": 0.0, "recoil_light": 0.05, "recoil_heavy": 0.10,
        "reload": 0.55, "crossbow_reload": 0.90, "bow_draw": length, "bow_release": 0.05,
        "spear_throw": 0.30, "intercept": 0.10, "kill_flourish": 0.40,
    }
    if name in chosen:
        return min(range(len(times)), key=lambda i: abs(times[i] - chosen[name]))

    def energy(t):
        return sum(np.linalg.norm(interpolate(data, t)) * (4 if channel == "position" else 1)
                   for bone in channels.values() for channel, data in bone.items())

    return max(range(len(times)), key=lambda i: energy(times[i]))


def animation_frame(name, info, pose, scale, index, total, length):
    t,event,hold=info
    frame=Image.new("RGB",(544,338),CANVAS)
    draw=ImageDraw.Draw(frame)
    draw.rectangle((0,0,543,44),fill=TOP)
    draw.text((14,22),"animation.wingman."+name,font=font(17,True,True),fill="#eff5ec",anchor="lm")
    if event:
        draw.text((529,22),f"{event[0]} @ {event[1]:.2f}s",font=font(15,True,True),fill="#ff8584",anchor="rm")
    panels=((FRONT,"正面"),(RIGHT,"右侧面"))
    for k,(camera,title) in enumerate(panels):
        left=7+k*268
        view=stage(pose,camera,(261,247),scale,222, bool(event))
        frame.paste(view,(left,51))
        draw.text((left+9,59),title,font=font(13,True),fill=MUTE)
    draw.text((11,314),f"{t:0.2f} / {length:0.2f} s",font=font(14,True,True),fill=INK,anchor="lm")
    if hold is not None:
        draw.text((171,314),f"停留  {(hold+1)/FPS:.2f} s",font=font(12),fill=MUTE,anchor="lm")
    for x,color,txt in ((321,"#cf894a","右肢"),(391,"#35aeb1","左肢")):
        draw.rectangle((x,308,x+8,316),fill=color)
        draw.text((x+13,312),txt,font=font(12),fill=MUTE,anchor="lm")
    draw.text((532,312),"20 FPS",font=font(12,mono=True),fill=MUTE,anchor="rm")
    draw.rectangle((0,333, round(543*(index+1)/total),337),fill="#398b8b")
    return frame


def gif(directory, name, clip, rig, channels, samples):
    seconds = sorted({frame[0] for frame in samples})
    unique = {t: posed_cubes(rig, channels, t) for t in seconds}
    poses = [unique[frame[0]] for frame in samples]
    scale = fit(unique.values(), (261, 247), 222, 6.1)
    length = clip["animation_length"]
    frames = [animation_frame(name, info, pose, scale, i, len(samples), length)
              for i, (info, pose) in enumerate(zip(samples, poses))]
    # A common palette prevents flicker. Include light/dark poses, frame text,
    # and bright event outlines even if the first frame has no event.
    event_index = next((i for i, frame in enumerate(samples) if frame[1]), 0)
    swatch_frames = frames[::4] + [frames[event_index]]
    swatches = Image.new("RGB", (160, 120 * len(swatch_frames)))
    for i, frame in enumerate(swatch_frames):
        swatches.paste(frame.resize((160, 120), Image.Resampling.NEAREST), (0, 120 * i))
    palette = swatches.quantize(240)
    indexed = [frame.quantize(palette=palette, dither=Image.Dither.NONE) for frame in frames]
    indexed[0].save(directory / f"wingman_{name}.gif", save_all=True, append_images=indexed[1:],
                    duration=1000 // FPS, loop=0, optimize=False, disposal=1)


def overview(data):
    rows = [(group, [name for name in names if name in data]) for group, names in ROWS]
    rows = [(group, names) for group, names in rows if names]
    w, h = 1370, 120 + 205 * len(rows)
    page=Image.new("RGB",(w,h),CANVAS)
    pen=ImageDraw.Draw(page)
    pen.rectangle((0,0,w,89),fill=TOP)
    pen.text((25,38),"僚机动作",font=font(36,True),fill="#ecf3ec",anchor="lm")
    subtitle = "移动、待机与受击" if "melee_1" not in data else "移动、待机、受击与战斗"
    pen.text((205,42),subtitle,font=font(23),fill="#bdd0ca",anchor="lm")
    pen.text((1344,43),f"AN1   /   {len(data)} 组关键帧",font=font(18),fill="#d3dfda",anchor="rm")
    for r,(group,names) in enumerate(rows):
        y=100+r*205
        pen.text((27,y+13),group,font=font(18,True),fill=INK,anchor="lm")
        if group == "原地转向":
            pen.text((699,y+13),"动力状态",font=font(18,True),fill=INK,anchor="lm")
        for col,name in enumerate(names):
            pose, t, event = data[name]
            left=25+col*335
            pen.rounded_rectangle((left,y+32,left+314,y+191),radius=5,fill="#fbfcf8",outline=RULE)
            scale=fit([pose],(148,108),98,2.75)
            for j,camera in enumerate((FRONT,RIGHT)):
                panel=stage(pose,camera,(148,108),scale,98)
                page.paste(panel,(left+7+j*150,y+39))
            pen.text((left+9,y+163),ZH[name],font=font(15,True),fill=INK,anchor="lm")
            stamp = f"{event[0]}  {t:.2f} s" if event else f"{t:.2f} s"
            pen.text((left+305,y+163),stamp,font=font(12,mono=True),fill=MUTE,anchor="rm")
            pen.text((left+9,y+182),"animation.wingman."+name,font=font(12,mono=True),fill=MUTE,anchor="lm")
    pen.text((26,h-20),"每格依次为正面、右侧面。琥珀色标示右肢，青色标示左肢。",font=font(15),fill=MUTE,anchor="lm")
    return page


def main():
    global UI_FONT
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--font",help="Chinese TrueType/OpenType font for preview captions")
    selection = parser.add_mutually_exclusive_group()
    selection.add_argument("--part", type=int, choices=(1, 2), help="Only rewrite the GIFs in this part")
    selection.add_argument("--clip", nargs="+", metavar="NAME", help="Only rewrite the named GIFs")
    args=parser.parse_args()
    UI_FONT=args.font
    spec=load_json(SPEC)
    rig=rig_geometry(load_json(ROOT/spec["rig"]["file"]))
    clips=load_json(ROOT/spec["files"]["wingman"]["path"])["animations"]
    dest=PREVIEWS/"AN1"
    dest.mkdir(parents=True,exist_ok=True)
    prefix = spec["files"]["wingman"]["prefix"]
    entries = [e for e in spec["animations"] if e["file"] == "wingman" and prefix + e["name"] in clips]
    names = {e["name"] for e in entries}
    selected = names if args.clip is None else set(args.clip)
    if missing := selected - names:
        parser.error("No exported wingman clip named " + ", ".join(sorted(missing)))
    if args.part:
        selected = {e["name"] for e in entries if e["part"] == args.part}
        if not selected:
            parser.error(f"No exported wingman clips for part {args.part}")
    data = {}
    for entry in entries:
        name = entry["name"]
        clip = clips[prefix + name]
        channels = tracks(clip)
        samples = timeline(clip)
        if name in selected:
            gif(dest, name, clip, rig, channels, samples)
        index = representative(name, channels, samples, clip["animation_length"])
        t, event, _ = samples[index]
        data[name] = posed_cubes(rig, channels, t), t, event
    overview(data).save(PREVIEWS/"AN1.png",optimize=True)


if __name__=="__main__":
    main()
