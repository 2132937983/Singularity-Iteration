"""Render an isometric overview from the actual saved block palettes (no mockup)."""
from pathlib import Path
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import heat_complex_world as h

X0,Z0,Y0=4128,1088,84
NX,NZ,NY=304,288,103
grid=np.zeros((NY,NZ,NX),dtype=np.uint16)
names=['minecraft:air']; ids={'minecraft:air':0}
for chunk in h.chunk_records(h.GAME/'world/region/r.8.2.mca'):
    cx,cz=int(chunk['xPos'])*16-X0,int(chunk['zPos'])*16-Z0
    if cx<0 or cz<0 or cx>=NX or cz>=NZ:continue
    for section in chunk.get('sections',[]):
        sy=int(section['Y'])*16-Y0
        if sy+16<=0 or sy>=NY:continue
        bs=section.get('block_states',{});palette=bs.get('palette',[])
        if not palette:continue
        mapped=[]
        for p in palette:
            name=str(p['Name'])
            if name in ['minecraft:cave_air','minecraft:void_air']:name='minecraft:air'
            if name not in ids:ids[name]=len(names);names.append(name)
            mapped.append(ids[name])
        if len(mapped)==1:voxels=np.full((16,16,16),mapped[0],dtype=np.uint16)
        else:
            bits=max(4,(len(mapped)-1).bit_length());per=64//bits
            data=np.array(bs['data'],dtype=np.int64).view(np.uint64)
            indices=np.arange(4096);decoded=(data[indices//per]>>(indices%per*bits).astype(np.uint64))&np.uint64((1<<bits)-1)
            voxels=np.array(mapped,dtype=np.uint16)[decoded.astype(np.int32)].reshape(16,16,16)
        lo=max(0,-sy);hi=min(16,NY-sy)
        grid[max(0,sy):min(NY,sy+16),cz:cz+16,cx:cx+16]=voxels[lo:hi]

def color(name):
    p=name.split(':',1)[-1]
    if name.startswith('mio_icif:'):
        if 'cable' in p:return (57,180,220)
        return (116,151,164)
    if 'sea_lantern' in p:return (211,255,241)
    if 'yellow' in p:return (245,187,35)
    if 'cyan' in p:return (43,146,166)
    if 'light_blue' in p:return (101,173,199)
    if 'water' in p:return (43,122,175)
    if 'white_concrete' in p:return (215,224,227)
    if 'light_gray' in p:return (151,164,175)
    if 'glass' in p:return (104,176,192)
    if 'black' in p:return (26,37,51)
    if 'deepslate' in p:return (48,61,77)
    if 'campfire' in p:return (215,137,49)
    if 'iron' in p:return (176,193,204)
    if 'gray' in p:return (92,109,123)
    if 'stone' in p:return (135,150,160)
    return (126,143,149)

scale=3.5
W,H=2400,1800
canvas=Image.new('RGB',(W,H),(12,22,36));draw=ImageDraw.Draw(canvas)
# Looking north-east from the gate: x increases toward right, south faces are visible.
def project(x,z,y):return (W/2+(x-z)*scale, H-240-(x+z)*scale*.5-y*scale)

occupied=grid!=0
faces=[]
for kind in range(3):
    visible=np.zeros_like(occupied)
    if kind==0:
        visible[:-1]=occupied[:-1]&~occupied[1:];visible[-1]=occupied[-1]
    elif kind==1:
        visible[:,:,:-1]=occupied[:,:,:-1]&~occupied[:,:,1:];visible[:,:,-1]=occupied[:,:,-1]
    else:
        visible[:,:-1,:]=occupied[:,:-1,:]&~occupied[:,1:,:];visible[:,-1,:]=occupied[:,-1,:]
    ys,zs,xs=np.nonzero(visible)
    for y,z,x in zip(ys.tolist(),zs.tolist(),xs.tolist()):
        # Farther cubes paint first; adjacent top/side tie handled with a small face offset.
        faces.append((-(x+z)+y*.01,kind,x,z,y,int(grid[y,z,x])))
faces.sort(key=lambda t:(t[0],t[1]))
for _,kind,x,z,y,i in faces:
    if kind==0:points=[(x,z,y+1),(x+1,z,y+1),(x+1,z+1,y+1),(x,z+1,y+1)];shade=1.0
    elif kind==1:points=[(x+1,z,y),(x+1,z+1,y),(x+1,z+1,y+1),(x+1,z,y+1)];shade=.7
    else:points=[(x,z+1,y),(x+1,z+1,y),(x+1,z+1,y+1),(x,z+1,y+1)];shade=.83
    c=tuple(int(v*shade) for v in color(names[i]));draw.polygon([project(*p) for p in points],fill=c)
font=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',30)
small=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',21)
draw.text((70,50),'新架构 · 16 堆热核基地',font=font,fill=(233,244,250))
draw.text((70,100),'从实际存档方块生成的等轴测预览 · 主世界 (4224, 85, 1340)',font=small,fill=(146,186,205))
draw.text((70,H-80),'16 堆 / 144 热交换机 / 96 蒸汽涡轮 / 128 MFSU / 四座冷却塔',font=small,fill=(180,216,229))
out=h.WORK/'station-overview.png';canvas.save(out)
print('Actual-world preview:',out,'faces:',len(faces))
