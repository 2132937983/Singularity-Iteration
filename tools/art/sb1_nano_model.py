"""Build the original Nano blade's game meshes in SlashBlade OBJ space.

X points toward the grip end, Y toward the cutting edge, Z toward the front face.
Dimensions below are model coordinates from the game's brief.
"""
from dataclasses import dataclass
from math import hypot, sqrt
from pathlib import Path

from sb1_nano_texture import atlas_uv


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/assets/mio_icif/model/slashblade/nano/model.obj"


def lerp(a, b, f):
    return a + (b - a) * f


def normal(a, b, c):
    ux, uy, uz = (j-i for i, j in zip(a, b))
    vx, vy, vz = (j-i for i, j in zip(a, c))
    n = (uy*vz-uz*vy, uz*vx-ux*vz, ux*vy-uy*vx)
    length = sqrt(sum(k*k for k in n))
    return tuple(k/length for k in n) if length > 1e-8 else None


class Mesh:
    def __init__(self):
        self.faces = []

    def face(self, points, coords, tile=None, reverse=False):
        if tile:
            coords = [atlas_uv(tile, s, t) for s, t in coords]
        if reverse:
            points, coords = list(reversed(points)), list(reversed(coords))
        for j in range(1, len(points)-1):
            idx = (0, j, j+1)
            tri = tuple(points[k] for k in idx)
            if normal(*tri):
                self.faces.append((tri, tuple(coords[k] for k in idx)))

    def include(self, other, transform=None):
        for points, uv in other.faces:
            pts = tuple(transform(p) for p in points) if transform else points
            self.faces.append((pts, uv))


RECT = ((0, 0), (1, 0), (1, 1), (0, 1))


def cap(mesh, ring, tile, reverse=False):
    ys, zs = [p[1] for p in ring], [p[2] for p in ring]
    top, low, left, right = max(ys), min(ys), min(zs), max(zs)
    coords = [((z-left)/max(right-left,.001), (top-y)/max(top-low,.001)) for _, y, z in ring]
    # The slightly uneven fracture is a piecewise planar fan; use the same
    # centre and triangles on its two complementary end caps.
    centre = tuple(sum(p[k] for p in ring)/len(ring) for k in range(3))
    cuv = tuple(sum(p[k] for p in coords)/len(coords) for k in range(2))
    for i in range(len(ring)):
        j = (i+1) % len(ring)
        mesh.face([centre,ring[i],ring[j]], [cuv,coords[i],coords[j]], tile, reverse)


@dataclass(frozen=True)
class Station:
    x: float
    top: float
    low: float
    width: float


def line(rows, x):
    for a,b in zip(rows,rows[1:]):
        if x >= b.x:
            f = min(1, max(0, (a.x-x)/(a.x-b.x)))
            return Station(x, lerp(a.top,b.top,f), lerp(a.low,b.low,f), lerp(a.width,b.width,f))
    return rows[-1]


BREAK = -110
TIP_X = -300
YOKOTE = -274


def blade_station(x):
    # Original centreline in holder coordinates. The shallow root changes
    # gradually into the sharper fall at the tip in the game's XY plane.
    s = (-32-x)/(-32-TIP_X)
    centre = 3.1 - 2.7*s - 26*s*s
    height = lerp(10.05,9.05,s)
    return Station(x, centre+height/2, centre-height/2, lerp(.78,.63,s))


BODY = [blade_station(x) for x in (-32,-58,BREAK,-157,-204,-245,YOKOTE)]
TIP_Y = blade_station(TIP_X).low + .35
TIP_EDGE = [(YOKOTE,BODY[-1].top),(-282,blade_station(-282).top-.5),
            (-291,blade_station(-291).top-3.2),(-297,blade_station(-297).top-6.8),
            (TIP_X,TIP_Y)]

# t progresses from the dark spine to the +Y edge. Starting on the -Z
# side keeps outward winding on both faces after the change of edge side.
BLADE_RING = ((0,-.64),(.18,-1),(.77,-.90),(1,0),(.77,.90),(.18,1),(0,.64))


def fracture_shift(t):
    offsets = ((0,1.9),(.18,-2.6),(.77,1.0),(1,-1.4))
    for (a,ax),(b,bx) in zip(offsets,offsets[1:]):
        if t <= b:
            return lerp(ax,bx,(t-a)/(b-a))
    return offsets[-1][1]


def blade_point(x, t, w):
    if x > YOKOTE:
        row = line(BODY, x)
        bx = x + (fracture_shift(t) if x == BREAK else 0)
        return bx, lerp(row.low,row.top,t), row.width*w

    # The cold-grey tip plane starts at a diagonal boundary. The single
    # positive-Y orange edge then converges with the spine at one vertex.
    root = BODY[-1]
    s = (YOKOTE-x)/(YOKOTE-TIP_X)
    bx = lerp(YOKOTE-4*(1-t), TIP_X, s)
    by = lerp(lerp(root.low,root.top,t), TIP_Y, s)
    if t > .77:
        for (xa,ya),(xb,yb) in zip(TIP_EDGE,TIP_EDGE[1:]):
            if x >= xb:
                edge = lerp(ya,yb,(xa-x)/(xa-xb))
                ridge = lerp(lerp(root.low,root.top,.77),TIP_Y,s)
                by = lerp(ridge,edge,(t-.77)/.23)
                break
    return bx, by, (1-s)*root.width*w


def clip_plane(poly, axis, boundary, keep_above):
    """Clip one convex face in (x, y, z, distance across blade) space."""
    out = []
    for a,b in zip(poly[-1:]+poly[:-1],poly):
        da,db = a[axis]-boundary,b[axis]-boundary
        if (da < 0) != (db < 0):
            f = da/(da-db)
            out.append(tuple(lerp(aa,bb,f) for aa,bb in zip(a,b)))
        if (db >= 0) == keep_above:
            out.append(b)
    return out


def edge_trace(glow, points, across, side, part, band):
    # Crop the emissive strip out of the same two triangles as the base.
    # At the transverse facet break, an independent flat quad would cut
    # through the bevel and leave a dark notch in the orange line.
    root = BREAK-3.6 if part == 'tip' else -35
    end = BREAK+3.5 if part == 'hilt' else TIP_X+.3
    ta,tb=band
    for indices in ((0,1,2),(0,2,3)):
        polygon=[(*points[i],across[i]) for i in indices]
        for axis,bound,above in ((3,ta,True),(3,tb,False),(0,end,True),(0,root,False)):
            polygon=clip_plane(polygon,axis,bound,above)
            if len(polygon)<3:
                break
        if len(polygon)>=3:
            layer=[(x,y,z+side*.11) for x,y,z,_ in polygon]
            uv=[((BODY[0].x-x)/(BODY[0].x-TIP_X),(t-ta)/(tb-ta)) for x,_,_,t in polygon]
            glow.face(layer,uv,'edge_glow')


def blade_shell(mesh, glow, part, inventory):
    stations = [r.x for r in BODY] + [x for x,_ in TIP_EDGE[1:]]
    stations = [x for x in stations if (part != 'hilt' or x >= BREAK)
                and (part != 'tip' or x <= BREAK)]
    rings = [[blade_point(x,t,w) for t,w in BLADE_RING] for x in stations]
    start, end = BODY[0].x, TIP_X
    # On a 32px GUI render the in-hand line is subpixel. A wider strip
    # follows the same existing bevel faces on the dedicated item meshes.
    band=(.61,.974) if inventory else (.819,.939)
    for xa,xb,aa,bb in zip(stations,stations[1:],rings,rings[1:]):
        for i,(t,_) in enumerate(BLADE_RING):
            j=(i+1)%len(aa)
            pts=(aa[i],bb[i],bb[j],aa[j])
            tj = BLADE_RING[j][0]
            if j == 0:
                tile,uv='dark',RECT
            elif xa <= YOKOTE:
                tile='kissaki'
                a,b=(YOKOTE-xa)/(YOKOTE-TIP_X),(YOKOTE-xb)/(YOKOTE-TIP_X)
                uv=((a,t),(b,t),(b,tj),(a,tj))
            else:
                tile='blade'
                uv=[((start-p[0])/(start-end),v) for p,v in zip(pts,(t,t,tj,tj))]
            mesh.face(pts,uv,tile)
            if min(t,tj)<band[1] and max(t,tj)>band[0]:
                edge_trace(glow,pts,(t,t,tj,tj),-1 if t<tj else 1,part,band)
    if part == 'tip':
        cap(mesh,rings[0],'fracture')
    else:
        cap(mesh,rings[0],'dark')
    if part == 'hilt':
        cap(mesh,rings[-1],'fracture',True)

# Eight vertices form the broad inset face, twin chamfers and narrow spine.
OCT = ((0,.59),(.16,1),(.84,1),(1,.59),(1,-.59),(.84,-1),(.16,-1),(0,-.59))


def oct_ring(row):
    return [(row.x,lerp(row.top,row.low,t),row.width*w) for t,w in OCT]


def oct_body(mesh, rows, face_tile, edge_tile, rim_tile, cap_near=True, cap_far=True, uv_span=None):
    start, end = uv_span or (rows[0].x, rows[-1].x)
    rings = [oct_ring(r) for r in rows]
    for aa,bb in zip(rings,rings[1:]):
        for i in range(8):
            j=(i+1)%8
            pts=(aa[i],bb[i],bb[j],aa[j])
            tile = face_tile if i in (1,5) else (rim_tile if i in (3,7) else edge_tile)
            xa,xb = ((start-p[0])/(start-end) for p in pts[:2])
            va,vb = ((1,0) if i == 5 else (0,1))
            uv=((xa,va),(xb,va),(xb,vb),(xa,vb))
            mesh.face(pts,uv,tile)
    if cap_near:
        cap(mesh,rings[0],rim_tile)
    if cap_far:
        cap(mesh,rings[-1],rim_tile,True)
    return rings


def plate(mesh, outline, width, tile, side_tile, bevel=0):
    """An XY profile with mirrored face plates and visibly chamfered edges."""
    cx,cy = tuple(sum(p[k] for p in outline)/len(outline) for k in (0,1))
    signed_area = sum(x*y2-x2*y for (x,y),(x2,y2) in zip(outline,outline[1:]+outline[:1]))
    if signed_area < 0:
        outline = list(reversed(outline))
    lx,hx=min(p[0] for p in outline),max(p[0] for p in outline)
    ly,hy=min(p[1] for p in outline),max(p[1] for p in outline)
    def uv_xy(p):
        return (hx-p[0])/(hx-lx), (hy-p[1])/(hy-ly)
    front,back=[],[]
    for sign in (1,-1):
        bevel_ring = [(x,y,sign*(width-bevel)) for x,y in outline]
        face_ring = [(x+(cx-x)*min(.14,bevel*.12),y+(cy-y)*min(.20,bevel*.19),sign*width)
                     for x,y in outline]
        centre=(cx,cy,sign*width)
        for i in range(len(outline)):
            j=(i+1)%len(outline)
            pp=(centre,face_ring[i],face_ring[j])
            mesh.face(pp,[uv_xy(p) for p in pp],tile,sign < 0)
            if bevel:
                pp=(bevel_ring[i],bevel_ring[j],face_ring[j],face_ring[i])
                mesh.face(pp, RECT, side_tile, sign < 0)
        if sign == 1:
            front=bevel_ring
        else:
            back=bevel_ring
    for i in range(len(outline)):
        j=(i+1)%len(outline)
        mesh.face((back[i],back[j],front[j],front[i]),RECT,side_tile)


def fittings(mesh, inventory):
    handle=[Station(34,4.1,-3.5,3.55),Station(18,5.0,-4.1,4.05),
            Station(-11,4.8,-4.9,4),Station(-28,4.7,-5.25,3.85)]
    oct_body(mesh,handle,'grip','grip','grip')
    plate(mesh,[(30,4),(34,5.5),(41.74,2.4),(39,-3.9),(34,-5.0),(30,-3.8)],
          4.25,'sheath_cap','metal',0 if inventory else .68)
    oct_body(mesh,[Station(-25.5,5.3,-5.35,4.35),Station(-30.4,5.7,-5.65,4.48)],
             'dark','bevel','metal')
    # The guard is one clipped octagonal disc across the handle. Its taller
    # dorsal corner and slight cant read from the front without a second horn.
    outline=[(16.3,2.2),(10.7,6.35),(-7.5,6.10),(-11.3,3.1),
             (-11.3,-3.3),(-7.3,-6.4),(9.6,-6.4),(15.8,-2.9)]
    rings=[]
    for depth,scale in ((3.05,.91),(2.15,1),(-2.15,1),(-3.05,.91)):
        ring=[]
        for i,(y,z) in enumerate(outline):
            # Two clipped corners come forward from the rim without
            # increasing its y/z outline or filling the sheath opening.
            lip = 4.2 if depth < 0 and i in (0,4) else 0
            ring.append((-31.35-.125*y+.078*z+depth-lip,y*scale,z*scale))
        rings.append(ring)
    cap(mesh,rings[0],'guard')
    cap(mesh,rings[-1],'guard',True)
    for k,(aa,bb) in enumerate(zip(rings,rings[1:])):
        tile='guard' if k==1 else 'bevel'
        for i in range(8):
            j=(i+1)%8
            mesh.face((aa[i],bb[i],bb[j],aa[j]),RECT,tile)


def make_blade(part, inventory=False):
    mesh,glow=Mesh(),Mesh()
    blade_shell(mesh,glow,part,inventory)
    if part != 'tip':
        fittings(mesh,inventory)
    return mesh,glow


def sheath_station(x,width=3.35):
    row=blade_station(x)
    return Station(x,row.top+2.6,row.low-2.9,width)


SAYA = [sheath_station(x,w) for x,w in
        [(-35,3.60),(-49,3.55),(-119,3.49),(-188,3.35),(-246,3.25),(-277,3.12)]]
SAYA += [Station(-299,TIP_Y+5.5,TIP_Y-2.7,2.90),
         Station(-306,TIP_Y+.4,TIP_Y-4.9,2.20),
         Station(-313,TIP_Y-6.2,TIP_Y-7.2,.68)]


def charge_port(mesh, glow):
    # Only the outboard face carries a kurigata. Its dark oblong socket has
    # a real recessed wall; the short lamp is set into the adjoining cheek.
    def point(s,t,height):
        x=lerp(-77,-90,s)
        row=line(SAYA,x)
        return x,(row.top+row.low)/2+(0.5-t)*5.9,row.width+height
    outer=((0,.26),(.13,0),(.82,0),(1,.23),(1,.78),(.84,1),(.12,1),(0,.76))
    hole=((.17,.40),(.23,.27),(.62,.27),(.68,.39),
          (.68,.61),(.62,.72),(.22,.72),(.17,.62))
    face_height=.53
    for i in range(len(outer)):
        j=(i+1)%len(outer)
        a,b,c,d=outer[i],outer[j],hole[j],hole[i]
        mesh.face((point(*a,face_height),point(*b,face_height),point(*c,face_height),point(*d,face_height)),
                  (a,b,c,d),'charge_port')
        mesh.face((point(*a,-.02),point(*b,-.02),point(*b,face_height),point(*a,face_height)),RECT,'bevel')
        mesh.face((point(*d,.08),point(*d,face_height),point(*c,face_height),point(*c,.08)),RECT,'dark')
    mesh.face([point(*p,.08) for p in hole],hole,'charge_port')
    lamp=((.769,.390),(.850,.390),(.850,.595),(.769,.595))
    glow.face([point(*p,face_height+.07) for p in lamp],RECT,'rail_glow')


def make_sheath():
    mesh,glow=Mesh(),Mesh()
    # A single open faceted ferrule meets the transverse guard. Its bevel
    # rolls into the existing armour without extending past the mouth.
    join=line(SAYA,-44.8)
    lip=[]
    for x,margin,width in ((-35,.42,3.95),(-36.5,.72,4.15),(-43.2,.45,3.95)):
        wall=line(SAYA,x)
        lip.append(Station(x,wall.top+margin,wall.low-margin,width))
    lip.append(join)
    outer=oct_body(mesh,lip,'sheath_cap','metal','metal',False,False)[0]
    oct_body(mesh,[join,*SAYA[1:]],'sheath','dark','metal',False,True,
             uv_span=(SAYA[0].x,SAYA[-1].x))
    mouth,end=blade_station(-35),blade_station(-51)
    inner=oct_ring(Station(-35,mouth.top+1.1,mouth.low-1.1,2.18))
    tunnel=oct_ring(Station(-51,end.top+.95,end.low-.95,2.06))
    perimeter=[0.]
    for a,b in zip(inner,inner[1:]+inner[:1]):
        perimeter.append(perimeter[-1]+hypot(b[1]-a[1],b[2]-a[2]))
    for i in range(8):
        j=(i+1)%8
        a,b=perimeter[i]/perimeter[-1],perimeter[i+1]/perimeter[-1]
        mesh.face((outer[i],outer[j],inner[j],inner[i]),((a,0),(b,0),(b,.5),(a,.5)),'repair_comb')
        mesh.face((inner[j],tunnel[j],tunnel[i],inner[i]),((b,.5),(b,1),(a,1),(a,.5)),'repair_comb')
    cap(mesh,tunnel,'dark')
    charge_port(mesh,glow)
    def facepos(s,t,sign,offset=0):
        x=lerp(SAYA[0].x,SAYA[-1].x,s)
        row=line(SAYA,x)
        return x,lerp(row.top,row.low,lerp(.16,.84,t)),sign*(row.width+offset)
    for side in (1,-1):
        for s0 in (.26,.53,.78):
            # These diagonals register with the panel joints drawn in the atlas.
            coords=((s0+.025-.0037,.20),(s0+.025+.0037,.20),
                    (s0-.025+.0037,.82),(s0-.025-.0037,.82))
            pp=[facepos(s,t,side,.105) for s,t in coords]
            glow.face(pp,RECT,'rail_glow',side<0)
    # The dorsal rail tracks the top ridge uninterrupted between the fittings.
    steps=[-51,-119,-188,-246,-277,-288]
    for a,b in zip(steps,steps[1:]):
        pp=[(x,line(SAYA,x).low-.11,z) for x,z in ((a,-.65),(b,-.65),(b,.65),(a,.65))]
        glow.face(pp,RECT,'rail_glow',True)
    return mesh,glow


def make_effect():
    # Seven contour rings include the swept tip and carry a continuous UV belt.
    # Offset the outer ferrule, armour and tail.
    rows=[]
    for x,up,down,out in [(-33.8,2.10,2.13,2.03),(-117,1.65,1.78,1.39),
                          (-187,1.55,1.58,1.36),(-246,1.55,1.55,1.36),
                          (-277,1.59,1.65,1.35),(-300,2.05,1.96,1.55),
                          (-314.2,2.10,1.55,1.30)]:
        row=line(SAYA,x)
        rows.append(Station(x,row.top+up,row.low-down,row.width+out))
    mesh=Mesh()
    rings=[oct_ring(r) for r in rows]
    for aa,bb in zip(rings,rings[1:]):
        for i in range(8):
            j=(i+1)%8
            u0,u1=(rows[0].x-aa[i][0])/64,(rows[0].x-bb[i][0])/64
            mesh.face((aa[i],bb[i],bb[j],aa[j]),((u0,i/8),(u1,i/8),(u1,(i+1)/8),(u0,(i+1)/8)))
    for ring,rev in ((rings[0],False),(rings[-1],True)):
        for i in range(1,7):
            tri=(ring[0],ring[i],ring[i+1])
            mesh.face(tri,[(p[2]/64,p[1]/64) for p in tri],reverse=rev)
    return mesh


def item_pose(p, is_sheath, damaged):
    x,y,z=p
    if is_sheath:
        near,far=SAYA[0],SAYA[-1]
        a=(near.x,(near.top+near.low)/2)
        b=(far.x,(far.top+far.low)/2)
        origin,end= ((63,-71),(-65,45)) if damaged else ((63,-63),(-79,45))
        across,depth=1.23,10.4
    else:
        a,b=(41.74,2.4),(TIP_X,TIP_Y)
        origin,end= ((-54,-64),(82,80)) if damaged else ((-81,-65),(68,76))
        across,depth=1.15,21.4 if damaged else 23.4
    # Stage the original strokes on opposite diagonals. Their curvature
    # and coloured cross-sections survive the final 32px GUI projection.
    dx,dy=b[0]-a[0],b[1]-a[1]
    length=hypot(dx,dy)
    along=((x-a[0])*dx+(y-a[1])*dy)/(length*length)
    cross=((y-a[1])*dx-(x-a[0])*dy)/length
    sx,sy=end[0]-origin[0],end[1]-origin[1]
    width=cross*across/hypot(sx,sy)
    return origin[0]+along*sx-width*sy,origin[1]+along*sy+width*sx,depth+.89*z


def inventory(name):
    body,light=Mesh(),Mesh()
    damaged = name=='item_damaged'
    blade,glow=make_blade('hilt' if damaged else 'all', inventory=True)
    body.include(blade,lambda p:item_pose(p,False,damaged))
    light.include(glow,lambda p:item_pose(p,False,damaged))
    if name!='item_bladens':
        sheath,glow=make_sheath()
        body.include(sheath,lambda p:item_pose(p,True,damaged))
        light.include(glow,lambda p:item_pose(p,True,damaged))
    return body,light


def build():
    meshes={}
    for name,part in (('blade','all'),('blade_damaged','hilt'),('blade_fragment','tip')):
        meshes[name],meshes[name+'_luminous']=make_blade(part)
    meshes['sheath'],meshes['sheath_luminous']=make_sheath()
    meshes['effect']=make_effect()
    for name in ('item_blade','item_damaged','item_bladens'):
        meshes[name],meshes[name+'_luminous']=inventory(name)
    return meshes


def export(meshes, path, title='Nano'):
    # Separate position, UV and flat normal tables keep OBJ hard edges cheap.
    # Indices are shared within an object, while named states remain editable.
    lines=[f'# SB1 | {title} blade | original game mesh']
    offsets=[0,0,0]
    for name,mesh in meshes.items():
        tables=[{}, {}, {}]
        faces=[]
        def index(table, value):
            val=tuple(round(c,6) for c in value)
            if val not in tables[table]:
                tables[table][val]=offsets[table]+len(tables[table])+1
            return tables[table][val]
        for pts,uvs in mesh.faces:
            n=index(2,normal(*pts))
            faces.append('f '+' '.join(f'{index(0,p)}/{index(1,uv)}/{n}' for p,uv in zip(pts,uvs)))
        lines.append('o '+name)
        for i,(prefix,table) in enumerate(zip(('v','vt','vn'),tables)):
            for v in table:
                lines.append(prefix+' '+' '.join(f'{k:.6f}'.rstrip('0').rstrip('.') if k else '0' for k in v))
            offsets[i]+=len(table)
        lines.extend(faces)
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text('\n'.join(lines)+'\n',encoding='ascii',newline='\n')


if __name__=='__main__':
    export(build(),OUTPUT)
