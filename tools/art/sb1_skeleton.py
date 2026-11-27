"""Overlay the delivered OBJ on the user-supplied reference, in game coordinates.

python tools/art/sb1_skeleton.py path/to/template.zip nano
The external OBJ is read in memory and only its silhouette and section lines are drawn.
"""
import argparse
from pathlib import Path
from zipfile import ZipFile

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

from sb1_preview import INK, LINE, MUTED, NAMES, Model, meshes, meshes_from_lines, page, panel, text


ACCENTS = {"nano": "#A14D22", "quantum": "#106F80", "singularity": "#923779"}
TINTS = {"nano": "#E6C3A6", "quantum": "#AED8D8", "singularity": "#DEC0D6"}
REF = "#596C74"
CUTS = (.1, .3, .5, .7, .9)


def reference_meshes(path):
    if path.suffix.lower() == ".obj":
        return meshes(path)
    with ZipFile(path) as archive:
        obj, = [item for item in archive.namelist() if item.lower().endswith(".obj")]
        return meshes_from_lines(archive.read(obj).decode("utf-8-sig").splitlines())


def silhouette(mesh, size, origin, scale):
    mask = Image.new("L", size)
    pen = ImageDraw.Draw(mask)
    xy = mesh.xyz[:, :, :2] * [scale, -scale] + origin
    for triangle in xy:
        pen.polygon([tuple(p) for p in triangle], fill=255)
    return mask


def section(mesh, fraction):
    # Use the same span and plane positions as the batch's nine-section check.
    x = -60 + fraction * (mesh.xyz[:, :, 0].min() + 60)
    segments = []
    for tri in mesh.xyz:
        if tri[:, 0].min() > x or tri[:, 0].max() < x:
            continue
        hits = []
        for a, b in zip(tri, np.roll(tri, -1, axis=0)):
            if (a[0] <= x < b[0]) or (b[0] <= x < a[0]):
                hits.append(a[1:] + (b[1:] - a[1:]) * ((x-a[0]) / (b[0]-a[0])))
        if len(hits) == 2:
            segments.append(hits)
    return x, np.array(segments)


def skeleton(model, template):
    color, tint = ACCENTS[model.kind], TINTS[model.kind]
    im = page(1360, 918, model.title + "   /   骨架对照", "XY 正投影    ·    模型单位")
    draw = ImageDraw.Draw(im)
    draw.line((770,115,808,115),fill=color,width=4)
    text(im,820,114,model.title,17,INK,anchor="lm")
    draw.line((975,115,1014,115),fill=REF,width=2)
    text(im,1025,114,"拔刀剑默认刀模板",17,INK,anchor="lm")
    ys = np.concatenate([obj.xyz[:,:,1].ravel() for objects in (model.objects, template)
                         for obj in (objects["blade"], objects["sheath"])])
    scale = min(2.65, 140 / (ys.max() - ys.min()))
    origin = (1010, 7 + ys.max() * scale)
    for row, (name, title) in enumerate((("blade","刀身"), ("sheath","刀鞘"))):
        y = 142 + row * 188
        panel(im,(36,y,1324,y+171))
        text(im,56,y+16,title,20,INK,bold=True)
        size = (1242,156)
        offset = (60, y+10)
        current = silhouette(model.objects[name], size, origin, scale)
        ref = silhouette(template[name],size,origin,scale)
        im.paste(tint,offset + (offset[0]+size[0],offset[1]+size[1]),current)
        for mask, ink in [(current,color),(ref,REF)]:
            outline = ImageChops.subtract(mask,mask.filter(ImageFilter.MinFilter(3)))
            im.paste(ink,offset + (offset[0]+size[0],offset[1]+size[1]),outline)
        ox,oy = offset[0]+origin[0],offset[1]+origin[1]
        draw.line((ox-7,oy,ox+7,oy),fill=INK,width=2)
        draw.line((ox,oy-7,ox,oy+7),fill=INK,width=2)
        text(im,ox+8,oy+17,"握位原点",15,MUTED)
        if name == "blade":
            # The guard inspection interval (X) and its measured outer Y extent.
            pts = model.objects[name].xyz.reshape(-1,3)
            guard = pts[(pts[:,0] >= -60) & (pts[:,0] <= -30)]
            y1,y2 = oy-scale*guard[:,1].max(), oy-scale*guard[:,1].min()
            x1,x2 = ox-60*scale,ox-30*scale
            box_color = "#687B6A"
            draw.rectangle((x1,y1,x2,y2),outline=box_color)
            text(im,x2+8,y1+2,"刀镡外径框",15,MUTED)
            section_x, cut = section(model.objects[name], .3)
            edge_x = ox+section_x*scale
            section_y = cut[:,:,0].max()
            edge_y = oy-scale*section_y
            draw.line((edge_x,edge_y-8,edge_x,edge_y-30),fill=color,width=2)
            draw.polygon([(edge_x,edge_y-34),(edge_x-4,edge_y-27),(edge_x+4,edge_y-27)],fill=color)
            text(im,edge_x-9,edge_y-26,"+Y  刃口侧",15,color,anchor="rm")

    text(im,37,535,"刀身横截面",25,bold=True)
    text(im,1321,552,"各自刀长的相同位置取截面   ·   +Y 朝上，Z 横向   ·   同比放大",16,MUTED,anchor="rm")
    for i,fraction in enumerate(CUTS):
        left, top = 36 + 260*i, 590
        panel(im,(left,top,left+248,874))
        text(im,left+124,top+25,f"{fraction:.0%}",23,anchor="mt",bold=True)
        for j,(mesh,ink,label) in enumerate(((template["blade"],REF,"模板"),(model.objects["blade"],color,model.title))):
            cx,cy=left+66+115*j, top+146
            _, lines = section(mesh,fraction)
            low,high = lines[:,:,0].min(),lines[:,:,0].max()
            yz = lines.copy()
            yz[:,:,0] -= (low+high)/2
            screen = yz[:,:,[1,0]] * [11.5,-11.5] + [cx,cy]
            draw.line((cx-25,cy,cx+25,cy),fill=LINE)
            for a,b in screen:
                draw.line((tuple(a),tuple(b)),fill=ink,width=2)
            text(im,cx,top+245,label,16,ink,anchor="mm")
    model.save(im,"skeleton")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("template",type=Path,help="External reference .zip or .obj")
    parser.add_argument("blades",nargs="+",choices=tuple(NAMES))
    args = parser.parse_args()
    template = reference_meshes(args.template)
    for kind in args.blades:
        skeleton(Model(kind),template)
