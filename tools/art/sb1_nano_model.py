"""Build the original Nano blade's game meshes in SlashBlade OBJ space.

X points toward the grip end, Y toward the spine, Z toward the front face.
Dimensions below are model coordinates from the game's brief.
"""
from dataclasses import dataclass
from math import cos, radians, sin, sqrt
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


BODY = [
    Station(-32, 5.1, -8.7, 1.75), Station(-53, 5.2, -9.6, 1.75),
    Station(-110, 3.9, -10.8, 1.70), Station(-170, 2.3, -12.0, 1.55),
    Station(-221, .7, -13.2, 1.45), Station(-254, -.5, -14.40, 1.70),
]
BREAK = -110
# In front view the back runs almost straight to the point. Only the lower
# outline sweeps up. The transverse seam is seven units farther forward at
# the back, so the two broad facets also separate when seen from the side.
YOKOTE = BODY[-1].x
TIP_X, TIP_Y = -300, -1.8
TIP_EDGE = [(YOKOTE, BODY[-1].low), (-266, -13.55), (-278, -11.10),
            (-289, -7.48), (-296, -4.22), (TIP_X, TIP_Y)]

BLADE_RING = ((0,.60),(.18,1),(.77,.92),(1,0),(.77,-.92),(.18,-1),(0,-.60))


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
        return bx, lerp(row.top,row.low,t), row.width*w

    # All points from the back ridge to the lower arris lie in triangular
    # fans ending at one apex. The curved lower outline opens up the bevel
    # beneath that plane, changing its lighting across the transverse seam.
    root = BODY[-1]
    s = (YOKOTE-x)/(YOKOTE-TIP_X)
    bx = lerp(YOKOTE-7*(1-t), TIP_X, s)
    by = lerp(lerp(root.top,root.low,t), TIP_Y, s)
    if t > .77:
        for (xa,ya),(xb,yb) in zip(TIP_EDGE,TIP_EDGE[1:]):
            if x >= xb:
                edge = lerp(ya,yb,(xa-x)/(xa-xb))
                ridge = lerp(lerp(root.top,root.low,.77),TIP_Y,s)
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


def edge_trace(glow, points, across, side, part):
    # Crop the emissive strip out of the same two triangles as the base.
    # At the transverse facet break, an independent flat quad would cut
    # through the bevel and leave a dark notch in the orange line.
    root = BREAK-3.6 if part == 'tip' else -35
    end = BREAK+3.5 if part == 'hilt' else TIP_X+.3
    ta,tb=.819,.939
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


def blade_shell(mesh, glow, part):
    stations = [r.x for r in BODY] + [x for x,_ in TIP_EDGE[1:]]
    stations = [x for x in stations if (part != 'hilt' or x >= BREAK)
                and (part != 'tip' or x <= BREAK)]
    rings = [[blade_point(x,t,w) for t,w in BLADE_RING] for x in stations]
    start, end = BODY[0].x, TIP_X
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
            if min(t,tj)==.77 and max(t,tj)==1:
                edge_trace(glow,pts,(t,t,tj,tj),1 if t<tj else -1,part)
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
        rings.append([(-31.35-.125*y+.078*z+depth,y*scale,z*scale) for y,z in outline])
    cap(mesh,rings[0],'guard')
    cap(mesh,rings[-1],'guard',True)
    for k,(aa,bb) in enumerate(zip(rings,rings[1:])):
        tile='guard' if k==1 else 'bevel'
        for i in range(8):
            j=(i+1)%8
            mesh.face((aa[i],bb[i],bb[j],aa[j]),RECT,tile)


def make_blade(part, inventory=False):
    mesh,glow=Mesh(),Mesh()
    blade_shell(mesh,glow,part)
    if part != 'tip':
        fittings(mesh,inventory)
    return mesh,glow


SAYA = [Station(-35,11.1,-13.5,5.15),Station(-49,10.8,-13.4,5.35),
        Station(-127,9.5,-14.4,4.95),Station(-212,5.8,-16.1,4.65),
        Station(-266,3.4,-19,4.15),Station(-287,2.0,-12.8,3.75),
        Station(-307,1.3,-4.0,2.45),Station(-313,3.9,1.7,.93)]


def make_sheath():
    mesh,glow=Mesh(),Mesh()
    # A single open faceted ferrule meets the transverse guard. Its bevel
    # rolls into the existing armour without extending past the mouth.
    join=line(SAYA,-44.8)
    lip=[Station(-35,11.55,-13.85,5.46),Station(-36.5,11.90,-14.2,5.70),
         Station(-43.2,11.42,-14.0,5.50),join]
    outer=oct_body(mesh,lip,'sheath_cap','metal','metal',False,False)[0]
    oct_body(mesh,[join,*SAYA[1:]],'sheath','dark','metal',False,True,
             uv_span=(SAYA[0].x,SAYA[-1].x))
    inner=oct_ring(Station(-35,8.8,-10.7,3.40))
    tunnel=oct_ring(Station(-51,8.4,-10.9,3.18))
    for i in range(8):
        j=(i+1)%8
        mesh.face((outer[i],outer[j],inner[j],inner[i]),RECT,'metal')
        mesh.face((inner[j],tunnel[j],tunnel[i],inner[i]),RECT,'dark')
    cap(mesh,tunnel,'dark')
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
    steps=[-51,-127,-212,-266,-285]
    for a,b in zip(steps,steps[1:]):
        pp=[(x,line(SAYA,x).top+.11,z) for x,z in ((a,-.70),(b,-.70),(b,.70),(a,.70))]
        glow.face(pp,RECT,'rail_glow')
    return mesh,glow


def make_effect():
    # Seven contour rings include the swept tip and carry a continuous UV belt.
    # Offset the outer ferrule, armour and tail.
    rows=[Station(-33.8,13.16,-15.50,6.98),Station(-46.5,12.65,-15.12,6.78),
          Station(-128,10.7,-15.7,6.15),Station(-265.8,4.8,-20.35,5.43),
          Station(-287.1,3.35,-14.15,5.05),Station(-306.5,2.7,-5.45,3.85),
          Station(-314.3,5.37,.18,2.15)]
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
        ax,ay,az,angle,ux,uy,uz=-174, -3, 12, -42, .59, .76, .80
        cx,cy=-3.7,2
    else:
        ax,ay,az,angle,ux,uy,uz=-129, -2.9, 22, -133, .55, .79, .89
        cx,cy=-6,4.5
    a,b=(x-ax)*ux,(y-ay)*uy
    theta=radians(angle)
    # The GUI item transform already offsets up and to the right. Keep the
    # asymmetric projection clear of the slot edge and its durability strip.
    x = cx + a*cos(theta) - b*sin(theta) - 15
    y = .77*(cy + a*sin(theta) + b*cos(theta)) - 19
    z = az + z*uz
    if damaged:
        # The broken item's slot has a narrower X frame and more space below.
        return x+15, y-6, z
    # Enlarge the complete item about its low left corner, staying inside
    # both the model frame and the rotated GUI projection. Bare uses it too.
    scale=1.11
    return -75+(x+75)*scale, -72+(y+72)*scale, 18+(z-18)*scale


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


def export(meshes, path):
    # Separate position, UV and flat normal tables keep OBJ hard edges cheap.
    # Indices are shared within an object, while named states remain editable.
    lines=['# SB1 | Nano blade | original game mesh']
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
