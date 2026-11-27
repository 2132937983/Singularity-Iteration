"""Render the SB1 review sheets from the delivered game OBJ and PNG textures.

Python + NumPy + Pillow. Orthographic, nearest-texel, per-pixel depth testing.
Inventory samples render directly at each requested framebuffer resolution.
"""
import argparse
from collections import defaultdict
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from tempfile import TemporaryDirectory

import numpy as np
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/mio_icif/model/slashblade"
PREVIEWS = ROOT / "docs/art/previews"
PAPER, PANEL, LINE = "#F3F4F1", "#EAEEEB", "#CBD2CE"
INK, MUTED, ACCENT = "#182323", "#52645F", "#B54F17"
SLOT = "#8B8B8B"
ITEMS = ("item_blade", "item_damaged", "item_bladens")
ITEM_ZH = ("完整刀与鞘", "断刀与鞘", "无鞘刀身")
NAMES = {"nano": "纳米刃", "quantum": "量子刃", "singularity": "奇点刃"}


@lru_cache(None)
def font(size, bold=False):
    for name in (("msyhbd.ttc" if bold else "msyh.ttc"),
                 ("/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc" if bold
                  else "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"),
                 "/System/Library/Fonts/PingFang.ttc"):
        try:
            return ImageFont.truetype(name, size=size)
        except OSError:
            continue
    raise RuntimeError("A font with Chinese glyphs (Microsoft YaHei or Noto Sans CJK) is required")


def text(im, x, y, value, size=18, color=INK, bold=False, anchor="lt"):
    ImageDraw.Draw(im).text((x, y), value, font=font(size, bold), fill=color, anchor=anchor)


def page(width, height, title, sub):
    im = Image.new("RGB", (width, height), PAPER)
    draw = ImageDraw.Draw(im)
    draw.rectangle((0, 0, width, 9), fill=INK)
    draw.rectangle((0, 0, 125, 9), fill="#EF813B")
    text(im, 37, 32, title, 34, bold=True)
    text(im, width-38, 53, sub, 17, MUTED, anchor="rm")
    draw.line((36, 88, width-36, 88), fill=LINE, width=1)
    return im


def panel(im, xy, fill=PANEL):
    ImageDraw.Draw(im).rounded_rectangle(xy, radius=9, fill=fill, outline=LINE)


def rotation(x=0, y=0, z=0):
    """Match Minecraft ItemTransform's Quaternionf.rotationXYZ composition."""
    a, b, c = np.radians([x, y, z])
    sx, sy, sz = np.sin([a, b, c])
    cx, cy, cz = np.cos([a, b, c])
    return (np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]) @
            np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]) @
            np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]))


@dataclass
class Mesh:
    xyz: np.ndarray                 # [triangle, corner, axis]
    uv: np.ndarray
    layer: str                     # albedo, luminous or effect
    name: str

    def placed(self, matrix, offset=(0, 0, 0)):
        return Mesh(self.xyz @ matrix.T + offset, self.uv, self.layer, self.name)


def meshes(path):
    return meshes_from_lines(path.read_text(encoding="utf-8-sig").splitlines())


def meshes_from_lines(lines):
    vertices, texcoords = [], []
    triangles = defaultdict(list)
    current = None
    for line in lines:
        words = line.split()
        if not words:
            continue
        if words[0] == "v":
            vertices.append(tuple(map(float, words[1:4])))
        elif words[0] == "vt":
            texcoords.append(tuple(map(float, words[1:3])))
        elif words[0] in ("o", "g"):
            current = words[1]
        elif words[0] == "f":
            refs = [tuple(int(n)-1 for n in corner.split("/")[:2]) for corner in words[1:]]
            for i in range(1, len(refs)-1):
                triangles[current].append([refs[0], refs[i], refs[i+1]])
    vs, uv = np.array(vertices), np.array(texcoords)
    result = {}
    for name, raw in triangles.items():
        refs = np.array(raw)
        layer = "luminous" if name.endswith("_luminous") else "effect" if name == "effect" else "albedo"
        result[name] = Mesh(vs[refs[:, :, 0]], uv[refs[:, :, 1]], layer, name)
    return result


def cube(lo, hi, color):
    # Neutral block-hand prop for the broken blade contact sheet only.
    x, y, z = lo
    a, b, c = hi
    vs = np.array([(x,y,z),(a,y,z),(a,b,z),(x,b,z),(x,y,c),(a,y,c),(a,b,c),(x,b,c)])
    quads = [(0,3,2,1),(4,5,6,7),(0,4,7,3),(1,2,6,5),(3,7,6,2),(0,1,5,4)]
    faces = [t for i,j,k,l in quads for t in ((i,j,k),(i,k,l))]
    return Mesh(vs[np.array(faces)], np.zeros((12, 3, 2)), color, "preview_hand")


class Model:
    def __init__(self, kind):
        self.kind = kind
        self.title = NAMES[kind]
        self.background = "#D1DAD7" if kind == "quantum" else PANEL
        folder = ASSETS / kind
        self.objects = meshes(folder / "model.obj")
        self.atlas = np.asarray(Image.open(folder / "texture.png").convert("RGBA"))
        self.charge = np.asarray(Image.open(folder / "charge.png").convert("RGBA"))

    def parts(self, *names):
        return [self.objects[name] for name in names]

    def lit(self, *names):
        # The body pass writes depth first. All luminous surfaces can then share
        # its depth test, including when the blade sits inside its sheath.
        return self.parts(*names) + self.parts(*(name+"_luminous" for name in names))

    def raster(self, scene, size, camera=None, *, pixels=None, center=None, origin=None,
               pad=7, phase=0, gui_light=False, supersample=1):
        camera = np.eye(3) if camera is None else camera
        scene = sorted(scene, key=lambda mesh: mesh.layer in ("effect", "luminous"))
        points = np.concatenate([m.xyz.reshape(-1, 3) for m in scene]) @ camera.T
        lo, hi = points.min(axis=0), points.max(axis=0)
        w, h = size
        if pixels is None:
            pixels = min((w-2*pad)/max(hi[0]-lo[0], 1e-6), (h-2*pad)/max(hi[1]-lo[1], 1e-6))
        if center is None:
            center = (lo + hi)[:2] / 2
        if origin is None:
            origin = (w/2, h/2)
        ss = supersample
        width, height = w*ss, h*ss
        screen = np.zeros((height, width, 4), dtype=np.uint8)
        depth = np.full((height, width), -np.inf)
        base_rgb = None
        light = np.array([-.32, .62, .72] if not gui_light else [-.4, .65, .65])
        light /= np.linalg.norm(light)
        for mesh in scene:
            overlay = mesh.layer in ("effect", "luminous")
            if overlay and base_rgb is None:
                base_rgb = screen[:,:,:3].copy()
            tex = self.charge if mesh.layer == "effect" else self.atlas
            projected = mesh.xyz @ camera.T
            normals = np.cross(projected[:,1] - projected[:,0], projected[:,2] - projected[:,0])
            norm = np.maximum(np.linalg.norm(normals, axis=1), 1e-10)
            normal = normals / norm[:, None]
            normal = np.where(normal[:,2:3] < 0, -normal, normal)
            brightness = .70 + .41 * np.maximum(0, normal @ light)
            projected[:,:,0] = (origin[0] + (projected[:,:,0] - center[0])*pixels)*ss
            projected[:,:,1] = (origin[1] - (projected[:,:,1] - center[1])*pixels)*ss
            for tri, uvs, shade in zip(projected, mesh.uv, brightness):
                x0, x1 = max(0, int(np.floor(tri[:,0].min()))), min(width, int(np.ceil(tri[:,0].max())))
                y0, y1 = max(0, int(np.floor(tri[:,1].min()))), min(height, int(np.ceil(tri[:,1].max())))
                if x1 <= x0 or y1 <= y0:
                    continue
                (a,b,_), (c,d,_), (e,f,_) = tri
                area = (c-a)*(f-b) - (d-b)*(e-a)
                if abs(area) < 1e-8:
                    continue
                yy, xx = np.ogrid[y0:y1,x0:x1]
                v = ((xx+.5-a)*(f-b) - (yy+.5-b)*(e-a)) / area
                t = ((c-a)*(yy+.5-b) - (d-b)*(xx+.5-a)) / area
                u = 1-v-t
                z = u*tri[0,2] + v*tri[1,2] + t*tri[2,2]
                region = depth[y0:y1,x0:x1]
                mask = (np.minimum(np.minimum(u,v),t) >= -1e-7) & (z >= region-1e-6)
                if not mask.any():
                    continue
                if mesh.layer.startswith("#"):
                    rgb = [int(mesh.layer[k:k+2],16) for k in (1,3,5)]
                    sample = np.broadcast_to(np.array([*rgb,255]), (*z.shape,4))
                else:
                    coords = u[...,None]*uvs[0] + v[...,None]*uvs[1] + t[...,None]*uvs[2]
                    if mesh.layer == "effect":
                        coords[:,:,0] += phase
                        coords %= 1
                    tx = np.clip((coords[:,:,0] * tex.shape[1]).astype(int), 0, tex.shape[1]-1)
                    ty = np.clip(((1-coords[:,:,1]) * tex.shape[0]).astype(int), 0, tex.shape[0]-1)
                    sample = tex[ty,tx]
                mask &= sample[:,:,3] > 0
                if not mask.any():
                    continue
                region[mask] = z[mask]
                target = screen[y0:y1,x0:x1]
                source = sample[:,:,:3][mask].astype(float)
                if overlay:
                    # Add full-bright color to the nearest visible base. Avoid
                    # double-adding shared edges of adjacent coplanar triangles.
                    source += base_rgb[y0:y1,x0:x1][mask]
                else:
                    source *= shade
                target[:,:,:3][mask] = np.clip(source,0,255).astype(np.uint8)
                target[:,:,3][mask] = 255
        img = Image.fromarray(screen)
        if ss > 1:
            img = img.resize((w,h), Image.Resampling.LANCZOS)
        return img

    def gui(self, which, res):
        return self.raster(self.lit(which), (res,res), rotation(15,-25,-5),
                           center=(0,0), pixels=res*.008*.65,
                           origin=(res*(.5+2/16),res*(.5-3/16)), gui_light=True)

    def save(self, img, kind):
        # Windows viewers can keep an existing PNG memory-mapped. Replace it
        # after encoding so the preview can stay open while adjusting the model.
        target = PREVIEWS / f"SB1-{self.kind}_{kind}.png"
        with TemporaryDirectory(dir=PREVIEWS) as folder:
            rendered = Path(folder) / target.name
            img.save(rendered, optimize=True)
            rendered.replace(target)


def paste(im, stamp, xy):
    im.paste(stamp, tuple(int(i) for i in xy), stamp)


def slot(im, xy, stamp, zoom=1):
    x,y = xy
    sz = stamp.width * zoom
    d = ImageDraw.Draw(im)
    d.rectangle((x-3,y-3,x+sz+2,y+sz+2), fill="#E1E2DD")
    d.rectangle((x-2,y-2,x+sz+1,y+sz+1), fill="#424642")
    d.rectangle((x,y,x+sz-1,y+sz-1), fill=SLOT)
    d.line((x+sz,y,x+sz,y+sz,x,y+sz), fill="#F7F8F3", width=2)
    if zoom != 1:
        stamp = stamp.resize((sz,sz),Image.Resampling.NEAREST)
    paste(im,stamp,(x,y))


def views(m):
    im = page(1400, 1282, f"{m.title}   /   外观对照", "SlashBlade Resharped 2.2.1")
    blade = m.lit("blade")
    closed = m.lit("blade", "sheath")
    front, edge, back, oblique = rotation(), rotation(-90), rotation(y=180), rotation(20,-24,-6)
    hero = rotation(14,-5,1)
    bounds = np.concatenate([mesh.xyz.reshape(-1,3) for mesh in closed]) @ hero.T
    span = np.ptp(bounds,axis=0)
    hero_scale = min(886/span[0],106/span[1])
    hero_center = (bounds.min(axis=0)+bounds.max(axis=0))[:2]/2
    for top, label, scene in [(110,"出鞘",blade),(300,"入鞘",closed)]:
        panel(im,(36,top,995,top+171),m.background)
        text(im,57,top+18,label,20,bold=True)
        stamp = m.raster(scene,(910,130), hero,pixels=hero_scale,center=hero_center,supersample=2)
        paste(im,stamp,(58,top+35))
    panel(im,(1013,110,1364,471))
    text(im,1036,128,"物品栏",20,bold=True)
    text(im,1340,139,"128 px · 2×",15,MUTED,anchor="rm")
    slot(im,(1061,177),m.gui("item_blade",128),2)

    # Keep the tip at the same model-space focus and pixels per unit in
    # both close-ups. The oblique view looks along the cutting-edge facet.
    text(im,37,507,"局部对照",25,bold=True)
    text(im,1363,520,"刀尖两图采用相同倍率",15,MUTED,anchor="rm")
    blade_vertices = m.objects["blade"].xyz.reshape(-1,3)
    tip_x = blade_vertices[:,0].min()
    near_tip = blade_vertices[blade_vertices[:,0] < tip_x+60]
    tip = np.array([tip_x + 31, (near_tip[:,1].max()+near_tip[:,1].min())/2, 0])
    tip_views = [("刀尖 · 正面",front),("刀尖 · 斜视",rotation(-35,-26,-9))]
    ends = blade_vertices[blade_vertices[:,0] <= tip_x+45]
    tip_scale = min(5.4, *(96/max(np.ptp((ends @ cam.T)[:,1]),1) for _,cam in tip_views))
    details = []
    for label,cam in tip_views:
        ey = (ends @ cam.T)[:,1]
        details.append((label,cam,((cam @ tip)[0],(ey.min()+ey.max())/2),tip_scale))
    guard_view = rotation(12,53,-3)
    details.append(("刀镡 · 斜视",guard_view,(guard_view @ [-32,1,0])[:2],3.5))
    for i,(label,cam,center,magnification) in enumerate(details):
        left = 36 + 447*i
        panel(im,(left,554,left+433,742))
        text(im,left+16,571,label,18,INK,bold=True)
        # The rest of the blade naturally continues out through this window.
        box = (left+13,609,left+420,730)
        ImageDraw.Draw(im).rectangle(box,fill=m.background if m.kind == "quantum" else "#E0E6E2",outline=LINE)
        stamp = m.raster(blade, (405,119), cam, pixels=magnification,
                         center=center, supersample=2)
        paste(im,stamp,(box[0]+1,box[1]+1))

    text(im,37,772,"四视图",25,bold=True)
    for i,(label,cam) in enumerate([("正面",front),("侧面 · 刀背",edge),("背面",back),("斜视",oblique)]):
        left = 36+i*336
        text(im,left+158,826,label,18,MUTED,anchor="mm")
        for j,state in enumerate(("出鞘","入鞘")):
            top = 854+j*193
            panel(im,(left,top,left+318,top+164),m.background)
            text(im,left+12,top+13,state,15,MUTED)
            if j == 0:
                # Show both parts separately. Offset each normal to the image
                # plane's common horizontal to keep the side view readable.
                open_scene = m.lit("blade","sheath")
                delta = cam.T @ np.array([0,-48,0])
                scene = [mesh.placed(np.eye(3),delta) if mesh.name.startswith("sheath") else mesh
                         for mesh in open_scene]
            else:
                scene = closed
            paste(im,m.raster(scene,(296,113),cam,pad=6,supersample=2),(left+11,top+35))
    m.save(im,"views")


def parts(m):
    rows = [("blade","blade_damaged","blade_fragment"),
            ("blade_luminous","blade_damaged_luminous","blade_fragment_luminous"),
            ("sheath","sheath_luminous","effect"),
            ("item_blade","item_damaged","item_bladens"),
            ("item_blade_luminous","item_damaged_luminous","item_bladens_luminous")]
    im = page(1400, 1064, f"{m.title}   /   OBJ 部件", "15 个对象 · 单独绘制")
    for j,row in enumerate(rows):
        top = 108 + j*189
        for i,name in enumerate(row):
            x = 36+i*447
            emission = name.endswith("_luminous") or name=="effect"
            panel(im,(x,top,x+430,top+173), "#253132" if emission else m.background)
            view = rotation(15,-25,-5) if name.startswith("item_") else rotation(13,-13,-4)
            paste(im,m.raster(m.parts(name),(407,127),view,supersample=2),(x+11,top+10))
            text(im,x+13,top+141,name,17,"#EBEEEA" if emission else INK)
    m.save(im,"parts")


def inventory(m):
    im = page(1160, 1072, f"{m.title}   /   物品栏", "游戏显示变换 · 原尺寸采样")
    cells = (32,48,64,128)
    cols = (398,555,733,950)
    for cx,n in zip(cols,cells):
        text(im,cx,128,f"{n} × {n}",18,MUTED,anchor="mm")
    for j,name in enumerate(ITEMS):
        y = 150+j*172
        panel(im,(36,y,1124,y+155))
        text(im,60,y+52,ITEM_ZH[j],22,bold=True)
        text(im,60,y+90,name,16,MUTED)
        for cx,n in zip(cols,cells):
            slot(im,(cx-n//2,y+(155-n)//2),m.gui(name,n))
    text(im,37,691,"场景显示",25,bold=True)
    for left,title in ((36,"展示框 · 背面"),(589,"掉落 · 离地悬浮")):
        panel(im,(left,742,left+534,1033))
        text(im,left+18,760,title,19,bold=True)
    item = m.lit("item_blade")
    # The fixed display displays the reverse. The scale constant is applied
    # before choosing a world-space camera magnification for this scene.
    fixed = [p.placed(rotation(y=180)*.0095) for p in item]
    framed = m.raster(fixed,(214,214),center=(0,0),pixels=124,supersample=2)
    d = ImageDraw.Draw(im)
    d.rectangle((195,802,414,1021),fill="#765440",outline="#694839",width=5)
    d.rectangle((204,811,405,1012),fill="#AB8462",outline="#C9A888",width=2)
    paste(im,framed,(198,805))
    ground = [p.placed(np.eye(3)*.005,(0,.15,0)) for p in item]
    view = rotation(18,-18,0)
    # Place the ground plane beneath the bounding box of the upright item.
    floor = item[0].xyz[:,:,1].min()*.005
    contact_y = ((view @ [0,floor,0])[1])
    d.ellipse((772,986,936,1000),fill="#C2CBC5")
    stamp = m.raster(ground,(490,222),view,center=(0,0),pixels=200,
                     origin=(245,202+contact_y*200),supersample=2)
    paste(im,stamp,(610,796))
    m.save(im,"inventory")


def broken(m):
    im = page(1220,705,f"{m.title}   /   断刀与碎片","断刀持握与碎片落地")
    for x,title in [(36,"手持断刀"),(628,"落地碎片")]:
        panel(im,(x,113,x+555,668),m.background)
        text(im,x+20,135,title,23,bold=True)
    glove = [cube((1,-17,-9),(19,3,2),"#485757"), cube((1,-31,-9),(19,-19,4),"#536768"),
             cube((0,-20,-10),(20,-14,5),"#324548"), cube((1,-24,3),(19,-22,5),"#84938D"),
             cube((14,-7,2),(21,3,8),"#616F6E")]
    for x in (1,5,9):
        glove.append(cube((x,-5,3),(x+3,3,7),"#657473"))
    held = glove + m.lit("blade_damaged")
    paste(im,m.raster(held,(520,260),rotation(14,-15,-12),pad=16,supersample=2),(53,212))
    frag = m.lit("blade_fragment")
    render = m.raster(frag,(515,165),rotation(57,-12,4),pad=16,supersample=2)
    # Its contact shadow is the fragment's own projected silhouette.
    shadow = Image.new("RGBA",render.size,"#9FAAA4")
    shadow.putalpha(render.getchannel("A").point(lambda a: a//2))
    paste(im,shadow,(651,283))
    paste(im,render,(649,280))
    # See each actual capped fracture obliquely from its open end.
    for x,name,side in [(36,"blade_damaged",-1),(628,"blade_fragment",1)]:
        mesh = m.objects[name]
        vertices = mesh.xyz.reshape(-1,3)
        end_x = vertices[:,0].min() if side < 0 else vertices[:,0].max()
        end = vertices[np.abs(vertices[:,0]-end_x) < 4]
        aim = (end.min(axis=0)+end.max(axis=0))/2
        view = rotation(10,-side*65,0)
        center = (view @ aim)[:2]
        panel(im,(x+20,483,x+271,635), "#DCE3DF")
        paste(im,m.raster(m.lit(name),(231,128),view,pixels=4,center=center,
                          supersample=2),(x+30,495))
        text(im,x+297,525,"断口近景",16,MUTED)
        text(im,x+297,555,name,18,INK,bold=True)
    m.save(im,"broken")


def charge(m):
    im = page(1230,735,f"{m.title}   /   蓄力流光","鞘体与蓄力层")
    cam = rotation(12,-5,1)
    shell = m.lit("sheath") + m.parts("effect")
    projected = np.concatenate([part.xyz.reshape(-1,3) for part in shell]) @ cam.T
    extent = np.ptp(projected,axis=0)
    scale = min(1071/extent[0],103/extent[1])
    center = (projected.max(axis=0)+projected.min(axis=0))[:2]/2
    for i,(label,phase) in enumerate([("静止刀鞘",None),("蓄力 · 初始相位",0),("蓄力 · 流光前移",.08)]):
        top = 110 + i*198
        panel(im,(36,top,1194,top+179),m.background)
        text(im,56,top+17,label,20,bold=True)
        scene = m.lit("sheath")
        if phase is not None:
            scene += m.parts("effect")
        paste(im,m.raster(scene,(1085,117),cam,pixels=scale,center=center,phase=phase or 0,
                          supersample=2),(73,top+43))
    m.save(im,"charge")


def checker(size, cell=16):
    y,x = np.indices((size[1],size[0]))
    return Image.fromarray(np.where(((x//cell+y//cell)%2)[:,:,None], [183,192,185],[209,215,207]).astype(np.uint8))


def uv_sheet(m):
    im = page(1350,820,f"{m.title}   /   贴图与 UV","主体图集与蓄力外壳")
    for x,title in [(36,"主体贴图"),(473,"UV 线框叠加"),(914,"蓄力纹样")]:
        text(im,x,122,title,23,bold=True)
    tex = Image.fromarray(m.atlas)
    ratio = min(400/tex.width, 520/tex.height)
    size = tuple(round(x*ratio) for x in tex.size)
    bg = checker(size)
    scaled = tex.resize(size,Image.Resampling.NEAREST)
    bg.paste(scaled,(0,0),scaled)
    im.paste(bg,(36,182))
    edges = Image.new("RGBA",size)
    d = ImageDraw.Draw(edges)
    seen = set()
    for name,mesh in m.objects.items():
        if mesh.layer == "effect":
            continue
        for tri in mesh.uv:
            pts = [(round(u*(size[0]-1)),round((1-v)*(size[1]-1))) for u,v in tri]
            for a,b in zip(pts,pts[1:]+pts[:1]):
                edge = tuple(sorted((a,b)))
                if edge not in seen:
                    d.line(edge,fill=(105,245,231,125))
                    seen.add(edge)
    bg.paste(edges,(0,0),edges)
    im.paste(bg,(473,182))
    pattern = Image.fromarray(m.charge)
    tiny = pattern.resize((192,192),Image.Resampling.NEAREST)
    tiled = Image.new("RGB",(384,384),"#293535")
    for x in (0,192):
        for y in (0,192):
            tiled.paste(tiny,(x,y),tiny)
    im.paste(tiled,(914,182))
    text(im,36,188+size[1],f"texture.png   {tex.width} × {tex.height}",16,MUTED)
    text(im,473,188+size[1],"主体、各状态与发光面",16,MUTED)
    text(im,914,576,"charge.png   64 × 64  ·  2 × 2 平铺",16,MUTED)
    # The scrolling shell wraps horizontally, so use its real unwrapped range
    # and sample the repeating charge texture behind the edges.
    uvs = m.objects["effect"].uv
    lo,hi = uvs.min(axis=(0,1)),uvs.max(axis=(0,1))
    ww = 384
    hh = min(115,max(45,round(ww*(hi[1]-lo[1])/(hi[0]-lo[0]))))
    v,u = np.mgrid[0:hh,0:ww]
    uu = lo[0]+u/(ww-1)*(hi[0]-lo[0]); vv = hi[1]-v/(hh-1)*(hi[1]-lo[1])
    pixels = m.charge[((1-vv)%1*64).astype(int),((uu%1)*64).astype(int)]
    shell = Image.new("RGB",(ww,hh),"#293535")
    tiled = Image.fromarray(pixels)
    shell.paste(tiled,(0,0),tiled)
    lines = Image.new("RGBA",shell.size)
    pen = ImageDraw.Draw(lines)
    for tri in uvs:
        pts = [(round((u-lo[0])/(hi[0]-lo[0])*(ww-1)),
               round((hi[1]-v)/(hi[1]-lo[1])*(hh-1))) for u,v in tri]
        pen.line(pts+[pts[0]],fill=(105,245,231,155))
    shell.paste(lines,(0,0),lines)
    im.paste(shell,(914,624))
    text(im,914,635+hh,"effect · 连续 UV 展开",16,MUTED)
    m.save(im,"uv")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("blade",choices=tuple(NAMES),default="nano",nargs="?")
    parser.add_argument("--sheet", choices=("views","parts","inventory","broken","charge","uv"))
    args = parser.parse_args()
    m = Model(args.blade)
    for name,fn in [("views",views),("parts",parts),("inventory",inventory),
                    ("broken",broken),("charge",charge),("uv",uv_sheet)]:
        if args.sheet in (None,name):
            fn(m)
            print(f"{args.blade}/{name}")


if __name__ == "__main__":
    main()
