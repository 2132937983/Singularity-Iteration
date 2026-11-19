"""Dimensioned concept for sixteen reactors, shared storage and isolated output.

Writes drawings and a placement manifest only; never opens or modifies a save.
Coordinates are relative to the tower's centre in X/Z and its foundation in Y.
"""
from pathlib import Path
from collections import Counter
import json, math
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/design';OUT.mkdir(parents=True,exist_ok=True)
cells={}
def place(p,kind,**info):
    p=tuple(p)
    if p in cells:
        old=cells[p]['kind']
        if old==kind:return
        if old!='vessel':raise ValueError(f'Collision at {p}: {old} / {kind}')
    cells[p]={'kind':kind,**info}

cores=[]
for layer,y in enumerate([12,28,44,60]):
    for n,(cx,cz,rotation) in enumerate([(11,0,0),(0,11,1),(-11,0,2),(0,-11,3)]):
        label=f'R{layer*4+n+1:02}'
        cores.append({'id':label,'layer':layer+1,'center':[cx,y,cz],'rotation':rotation*90})
        def pos(x,dy,z):
            for _ in range(rotation):x,z=-z,x
            return (cx+x,y+dy,cz+z)
        def put(x,dy,z,kind,**info):place(pos(x,dy,z),kind,reactor=label,**info)
        put(0,0,0,'reactor')
        for d in [(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]:put(*d,'chamber')
        for x in range(-2,3):
            for dy in range(-2,3):
                for z in range(-2,3):
                    if max(abs(x),abs(dy),abs(z))==2:put(x,dy,z,'vessel')
        put(-2,0,0,'redstone_port');put(0,0,2,'access_hatch');put(-3,0,0,'lever')
        for group,(b,axis) in enumerate([((0,3,0),(0,1,0)),((0,-3,0),(0,-1,0)),((4,0,0),(1,0,0))]):
            bx,by,bz=b;ax,ay,az=axis
            put(*b,'boiler',pressure=221,input=4 if group<2 else 1)
            sides=[(1,0,0),(-1,0,0),(0,0,1),(0,0,-1)] if group<2 else [(-1,0,0)]
            for sx,sy,sz in sides:
                ex,ey,ez=bx+sx,by+sy,bz+sz
                put(ex,ey,ez,'exchanger')
                put(ex,ey-1 if group==0 else ey+1,ez,'fluid_port') if group<2 else put(ex-1,ey,ez,'fluid_port')
            for t in [1,2]:
                tx,ty,tz=bx+ax*t,by+ay*t,bz+az*t
                put(tx,ty,tz,'turbine',stage=t)
                put(tx,ty,tz-1,'kinetic_generator')
                put(tx,ty,tz-2,'generation_cable');put(tx,ty,tz-3,'generation_cable')
            co=(bx+ax*3,by+ay*3,bz+az*3);put(*co,'condenser',vents=1 if group<2 else 0)
            for distance in [1,2,3]:put(co[0],co[1],co[2]-distance,'generation_cable')
        for dy in range(-6,7):put(0,dy,-3,'generation_cable')
        for dx in range(8):put(dx,0,-3,'generation_cable')
    # 32 inward-facing MFSUs per deck, no shared corner battery faces.
    for v in range(-4,4):
        for p,front in [((5,y,v),'west'),((-6,y,v),'east'),((v,y,-6),'south'),((v,y,5),'north')]:place(p,'mfsu',facing=front,layer=layer+1)
    for x in range(-7,7):
        for z in [-7,6]:place((x,y,z),'generation_cable')
    for z in range(-7,7):
        for x in [-7,6]:place((x,y,z),'generation_cable')
    for x in range(-5,5):
        for z in [-5,4]:place((x,y,z),'output_cable')
    for z in range(-5,5):
        for x in [-5,4]:place((x,y,z),'output_cable')
    for x in range(6,12):place((x,y,-3),'generation_cable')
    for x in range(-11,-6):place((x,y,3),'generation_cable')
    for z in range(6,12):place((3,y,z),'generation_cable')
    for z in range(-11,-6):place((-3,y,z),'generation_cable')

for y in range(12,61):
    for x,z in [(-7,-7),(-7,6),(6,-7),(6,6)]:place((x,y,z),'generation_cable')
for y in range(2,61):
    for x,z in [(-5,-5),(-5,4),(4,-5),(4,4)]:place((x,y,z),'output_cable')
for x in range(-5,5):
    for z in [-5,4]:place((x,2,z),'output_cable')
for z in range(-5,5):
    for x in [-5,4]:place((x,2,z),'output_cable')
for distance in range(5,28):
    for x,z in [(distance,0),(-distance,0),(0,distance),(0,-distance)]:place((x,2,z),'output_cable')
# Positive-axis arms begin at +4; negative-axis arms begin at -5.
place((4,2,0),'output_cable');place((0,2,4),'output_cable')

generation={p for p,v in cells.items() if v['kind']=='generation_cable'}
output={p for p,v in cells.items() if v['kind']=='output_cable'}
neighbors=[(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]
for x,y,z in generation:
    assert not any((x+dx,y+dy,z+dz) in output for dx,dy,dz in neighbors),'Input/output networks touch'
def connected(points):
    visited=set();todo=[next(iter(points))]
    while todo:
        p=todo.pop()
        if p in visited:continue
        visited.add(p)
        x,y,z=p;todo.extend((x+dx,y+dy,z+dz) for dx,dy,dz in neighbors if (x+dx,y+dy,z+dz) in points and (x+dx,y+dy,z+dz) not in visited)
    return len(visited)==len(points)
assert connected(generation) and connected(output),'Disconnected shared bus'
vectors={'east':(1,0,0),'west':(-1,0,0),'south':(0,0,1),'north':(0,0,-1)}
for (x,y,z),v in cells.items():
    if v['kind']!='mfsu':continue
    dx,dy,dz=vectors[v['facing']];front=(x+dx,y+dy,z+dz)
    assert front in output and front not in generation,'Incorrect battery output wiring'
    for ax,ay,az in neighbors:
        p=(x+ax,y+ay,z+az)
        assert p not in output or p==front,'Output bus touches an input side of storage'
for core in cores:
    cx,cy,cz=core['center']
    for x in range(-2,3):
        for y in range(-2,3):
            for z in range(-2,3):
                if max(abs(x),abs(y),abs(z))==2:
                    assert cells[(cx+x,cy+y,cz+z)]['kind'] in ['vessel','fluid_port','access_hatch','redstone_port'],'Pressure shell incomplete'
counts=Counter(v['kind'] for v in cells.values())
for kind,total in [('reactor',16),('mfsu',128),('exchanger',144),('boiler',48),('turbine',96),('condenser',48),('kinetic_generator',96)]:assert counts[kind]==total
manifest={'status':'design only; native shared-grid operation not yet commissioned','frame_size':[45,72,45],'overall_size':[55,76,55],'layer_core_y':[12,28,44,60],'centres':cores,'counts':dict(counts),'geometry_checks':['No machine overlap','Sixteen complete pressure shells','Two disjoint conductor networks','Generation network connected','Output network connected','All battery outputs face output bus only'],'blocks':[{'position':list(p),**v} for p,v in sorted(cells.items())]}
(OUT/'integrated_16_core_heat_tower_layout.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')

# Native dimensioned drawing, with a reusable vector version and a PNG preview.
W,H=1900,1450
im=Image.new('RGB',(W,H),(12,20,30));d=ImageDraw.Draw(im)
svg=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}"><rect width="100%" height="100%" fill="#0c141e"/>']
from html import escape
font_path='C:/Windows/Fonts/msyh.ttc'
def text(x,y,s,size=23,fill='#cfdae4'):
    d.text((x,y),s,font=ImageFont.truetype(font_path,size),fill=fill)
    svg.append(f'<text x="{x}" y="{y+size}" font-family="Microsoft YaHei,sans-serif" font-size="{size}" fill="{fill}">{escape(s)}</text>')
def line(points,fill,width=2):
    d.line(points,fill=fill,width=width)
    svg.append(f'<polyline points="{" ".join(f"{x:.2f},{y:.2f}" for x,y in points)}" fill="none" stroke="{fill}" stroke-width="{width}"/>')
def polygon(points,fill,outline=None):
    d.polygon(points,fill=fill,outline=outline)
    svg.append(f'<polygon points="{" ".join(f"{x:.2f},{y:.2f}" for x,y in points)}" fill="{fill}" stroke="{outline or fill}"/>')
colors={'reactor':'#dfa331','chamber':'#b6bcc0','vessel':'#83949f','redstone_port':'#f45856','access_hatch':'#d6dde2','fluid_port':'#e8a75b','exchanger':'#e77d37','boiler':'#5d98bb','turbine':'#ccd2da','condenser':'#6385a1','kinetic_generator':'#5cb895','mfsu':'#b4d678','lever':'#fb5353','generation_cable':'#29ccdd','output_cable':'#be8af5'}
def project(x,y,z):return (615+(x-z)*10,1190-(x+z)*4.7-y*10.2)
def cube(x,y,z,color):
    rgb=tuple(int(color[i:i+2],16) for i in [1,3,5])
    shade=lambda k:'#'+''.join(f'{int(v*k):02x}' for v in rgb)
    polygon([project(x+1,y,z),project(x+1,y,z+1),project(x+1,y+1,z+1),project(x+1,y+1,z)],shade(.68))
    polygon([project(x,y,z+1),project(x+1,y,z+1),project(x+1,y+1,z+1),project(x,y+1,z+1)],shade(.82))
    polygon([project(x,y+1,z),project(x+1,y+1,z),project(x+1,y+1,z+1),project(x,y+1,z+1)],color)

text(55,35,'16 核 · 集成式热核能源塔',38,'#edf5fa')
text(55,93,'四层四向堆叠 / 中央储能矩阵 / 共用汇能与输出 / 就地冷却闭环',23,'#8eafc2')
text(55,139,'方案图：已校验设备位置与母线隔离，尚待游戏内验证共享电网与满载运行。',19,'#e8bf72')
for y in [3,20,36,52,68]:
    line([project(-22,y,-22),project(22,y,-22),project(22,y,22),project(-22,y,22),project(-22,y,-22)],'#354652',3)
for x,z in [(-22,-22),(22,-22),(-22,22),(22,22)]:line([project(x,3,z),project(x,71,z)],'#455b69',7)
for (x,y,z),v in sorted(cells.items(),key=lambda kv:(kv[0][1]-(kv[0][0]+kv[0][2])*.47,kv[0][0]+kv[0][2])):
    cube(x,y,z,colors[v['kind']])
for y in [11,27,43,59]:
    # Sparse safety rails illustrate walkways; no blanket floor through machinery.
    line([project(-22,y,22),project(22,y,22),project(22,y,-22)],'#e3ab32',3)
for layer,y in enumerate([12,28,44,60]):
    text(70,1130-y*10.2,f'L{layer+1}  ·  4 核',22,'#b9c7d1')
line([project(-23,0,23),project(-23,72,23)],'#8eafc2',2)
text(55,485,'塔架 72 格',20,'#8eafc2')
text(220,1352,'主体 45 × 45 格；含四向输出伸臂 55 × 55 格',22)

# Single-layer plan uses the actual placement cells at core height.
text(1270,215,'单层俯视 · 四层重复',28,'#edf5fa')
ox,oz,size=1537,504,8
def top(x,z):return (ox+x*size,oz+z*size)
for x in range(-23,24):line([top(x,-23),top(x,23)],'#1b2d3a',1)
for z in range(-23,24):line([top(-23,z),top(23,z)],'#1b2d3a',1)
for (x,y,z),v in cells.items():
    if y!=28:continue
    x0,z0=top(x,z);polygon([(x0,z0),(x0+size,z0),(x0+size,z0+size),(x0,z0+size)],colors[v['kind']])
for label,(x,z) in [('东核',(11,0)),('南核',(0,11)),('西核',(-11,0)),('北核',(0,-11))]:
    px,pz=top(x,z);text(px-25,pz-12,label,16,'#101923')
text(1521,485,'中央',16,'#edf5fa');text(1513,506,'储能塔',16,'#edf5fa')
text(1396,287,'北',22);text(1283,491,'西',22);text(1774,491,'东',22);text(1530,713,'南',22)
text(1265,775,'中央每层 32 台 MFSU，四层共 128 台',21)
text(1265,814,'进电母线在箱体背面，出电母线朝内',21)
text(1265,853,'出电下沉至基座，再分到四向伸臂',21)
legend=[('vessel','5×5×5 独立压力容器'),('exchanger','紧贴容器的热交换机'),('boiler','221 压力蒸汽发生器'),('turbine','两级蒸汽做功与冷凝'),('mfsu','共用中央 MFSU 储能矩阵'),('generation_cable','汇电母线 / 冷凝机供电'),('output_cable','隔离的对外输出母线')]
for i,(kind,label) in enumerate(legend):
    yy=936+i*43;polygon([(1270,yy),(1291,yy),(1291,yy+21),(1270,yy+21)],colors[kind]);text(1307,yy-3,label,20)
text(1265,1263,'功率目标约 10.6 kEU/t',24,'#b4d678')
text(1265,1306,'来自原机组基准；集成版需重新实测',19,'#8eafc2')
text(1265,1344,'储能 51.2 亿 EU · 区块对齐后 4×4 加载',19,'#8eafc2')
im.save(OUT/'integrated_16_core_heat_tower.png')
svg.append('</svg>');(OUT/'integrated_16_core_heat_tower.svg').write_text('\n'.join(svg),encoding='utf-8')
print('Geometry checked:',dict(counts))
print('Design drawings and placement manifest:',OUT)
