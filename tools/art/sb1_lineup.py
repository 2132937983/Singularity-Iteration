"""Render the three game models together, at the same camera angle and scale."""
import numpy as np
from PIL import ImageDraw

from sb1_preview import INK, MUTED, PREVIEWS, Model, page, panel, paste, rotation, slot, text


def main():
    im = page(1400, 1000, "奇点迭代   /   联动拔刀", "SlashBlade Resharped    ·    三把刀同尺度")
    draw = ImageDraw.Draw(im)
    cam = rotation(9, -7, 0)
    focus = (cam @ np.array([-153, -9, 0]))[:2]
    variants = [
        ("nano","打刀","压电陶瓷 · 碳纤维","#CB6D2D"),
        ("quantum","浅弯太刀","镜面钢 · 干涉刃纹","#197E8A"),
        ("singularity","大太刀","约束环 · 暗物质","#9F437E"),
    ]
    for row,(kind,style,detail,accent) in enumerate(variants):
        m = Model(kind)
        y = 113+row*281
        panel(im,(36,y,1364,y+259),m.background)
        draw.rectangle((36,y+24,41,y+82),fill=accent)
        text(im,61,y+24,m.title,28,INK,bold=True)
        text(im,62,y+70,style,19,accent)
        text(im,62,y+106,detail,15,MUTED)
        for top,label,scene in [(y+12,"出鞘",m.lit("blade")),(y+129,"入鞘",m.lit("blade","sheath"))]:
            text(im,230,top+17,label,16,MUTED)
            render = m.raster(scene,(900,115),cam,center=focus,pixels=2.13,supersample=2)
            paste(im,render,(258,top))
        draw.line((1152,y+21,1152,y+238),fill="#CBD2CE")
        text(im,1255,y+32,"物品栏",17,MUTED,anchor="mm")
        slot(im,(1191,y+69),m.gui("item_blade",128))
    im.save(PREVIEWS / "SB1-lineup.png",optimize=True)


if __name__ == "__main__":
    main()
