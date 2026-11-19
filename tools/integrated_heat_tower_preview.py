"""Simplified voxel overview read from the installed save, not a concept drawing."""
import numpy as np
from PIL import Image,ImageDraw,ImageFont
import integrated_heat_tower_world as h
X0,Y0,Z0=4192,84,1152
NX,NY,NZ=64,80,64
grid=np.zeros((NY,NZ,NX),dtype=np.uint16);names=['minecraft:air'];ids={names[0]:0}
for rec in h.records(h.WORLD/'region/r.8.2.mca').values():
    c=h.chunk_nbt(rec);cx,cz=int(c['xPos'])*16-X0,int(c['zPos'])*16-Z0
    if not(0<=cx<NX and 0<=cz<NZ):continue
    for s in c.get('sections',[]):
        sy=int(s['Y'])*16-Y0
        if sy+16<=0 or sy>=NY:continue
        bs=s.get('block_states',{});pal=bs.get('palette',[])
        if not pal:continue
        mapped=[]
        for p in pal:
            name=str(p['Name'])
            if name.endswith(':cave_air') or name.endswith(':void_air'):name=names[0]
            if name not in ids:ids[name]=len(names);names.append(name)
            mapped.append(ids[name])
        if len(mapped)==1:v=np.full((16,16,16),mapped[0],dtype=np.uint16)
        else:
            bits=max(4,(len(mapped)-1).bit_length());per=64//bits
            raw=np.array(bs['data'],dtype=np.int64).view(np.uint64);i=np.arange(4096)
            decoded=(raw[i//per]>>(i%per*bits).astype(np.uint64))&np.uint64((1<<bits)-1)
            v=np.array(mapped,dtype=np.uint16)[decoded.astype(np.int32)].reshape(16,16,16)
        lo=max(0,-sy);hi=min(16,NY-sy);grid[max(0,sy):min(NY,sy+16),cz:cz+16,cx:cx+16]=v[lo:hi]
def color(n):
    if 'mfsu' in n:return (157,197,83)
    if 'glass_cable' in n:return (43,203,215)
    if 'heat_source_fluid' in n:return (228,134,56)
    if 'steam_generator' in n:return (78,150,182)
    if 'condenser' in n:return (88,151,170)
    if 'kinetic_generator' in n:return (156,188,191)
    if n.startswith('mio_icif:'):return (133,154,172)
    if 'sea_lantern' in n:return (208,255,243)
    if 'cyan' in n:return (31,156,169)
    if 'orange' in n:return (236,140,49)
    if 'yellow' in n:return (230,177,46)
    if 'black' in n:return (25,38,49)
    if 'iron' in n:return (171,188,198)
    if 'deepslate' in n:return (40,55,67)
    if 'gray' in n:return (78,93,104)
    if 'ladder' in n or 'sign' in n or 'chest' in n:return (174,142,88)
    return (128,146,155)
canvas=Image.new('RGB',(1800,1450),(11,20,29));d=ImageDraw.Draw(canvas)
def project(x,z,y):return (900+(x-z)*10,1310-(x+z)*5-y*10)
occupied=grid!=0;faces=[]
for kind in range(3):
    vis=np.zeros_like(occupied)
    if kind==0:vis[:-1]=occupied[:-1]&~occupied[1:];vis[-1]=occupied[-1]
    elif kind==1:vis[:,:,:-1]=occupied[:,:,:-1]&~occupied[:,:,1:];vis[:,:,-1]=occupied[:,:,-1]
    else:vis[:,:-1,:]=occupied[:,:-1,:]&~occupied[:,1:,:];vis[:,-1,:]=occupied[:,-1,:]
    ys,zs,xs=np.nonzero(vis)
    for y,z,x in zip(ys.tolist(),zs.tolist(),xs.tolist()):faces.append((-(x+z)+y*.01,kind,x,z,y,int(grid[y,z,x])))
for _,kind,x,z,y,i in sorted(faces):
    if kind==0:p=[(x,z,y+1),(x+1,z,y+1),(x+1,z+1,y+1),(x,z+1,y+1)];shade=1
    elif kind==1:p=[(x+1,z,y),(x+1,z+1,y),(x+1,z+1,y+1),(x+1,z,y+1)];shade=.68
    else:p=[(x,z+1,y),(x+1,z+1,y),(x+1,z+1,y+1),(x,z+1,y+1)];shade=.83
    d.polygon([project(*v) for v in p],fill=tuple(int(v*shade) for v in color(names[i])))
font='C:/Windows/Fonts/msyh.ttc'
d.text((65,40),'新架构 · 16 核集成能源塔',font=ImageFont.truetype(font,34),fill='#e5eff4')
d.text((65,96),'从已安装存档读取的方块概览（简化体素绘制，非游戏截图）',font=ImageFont.truetype(font,22),fill='#93b3c5')
d.text((65,1367),'四层四向 × 16 堆  /  128 MFSU  /  共用汇能与隔离输出  /  主世界 4219, 84, 1179',font=ImageFont.truetype(font,22),fill='#b4d3e0')
out=h.WORK/'installed-tower-overview.png';canvas.save(out);print(out)
