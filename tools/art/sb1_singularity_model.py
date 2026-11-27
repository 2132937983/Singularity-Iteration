"""Original SlashBlade game model for the Singularity odachi. Edge points +Y."""
from math import cos, pi, radians, sin

from sb1_nano_model import Mesh, export, lerp
from sb1_singularity_texture import ASSETS, uv


RECT = ((0,0), (1,0), (1,1), (0,1))
ROOT_X, BREAK_X, TIP_X = -32.0, -133.0, -332.0


def surface(mesh, points, tile, coords=RECT, reverse=False):
    mesh.face(points, [uv(tile, s, t) for s,t in coords], reverse=reverse)


def butt(mesh, ring, tile, reverse=False, offset=0):
    """Cap shaped sections without flattening their jagged fracture."""
    lo, hi = min(p[1] for p in ring), max(p[1] for p in ring)
    left, right = min(p[2] for p in ring), max(p[2] for p in ring)
    def coord(p):
        return ((p[2]-left)/max(.001,right-left), (hi-p[1])/max(.001,hi-lo))
    if all(p[0]==ring[0][0] for p in ring):
        # Convex planar caps have affine UVs. Triangulating from the rim
        # preserves the same surface without a spare centre in every LOD.
        surface(mesh,[(x+offset,y,z) for x,y,z in ring],tile,[coord(p) for p in ring],reverse)
        return
    center = tuple(sum(p[i] for p in ring)/len(ring) for i in range(3))
    for i in range(len(ring)):
        pts = [center,ring[i],ring[(i+1)%len(ring)]]
        surface(mesh, [(x+offset,y,z) for x,y,z in pts], tile, [coord(p) for p in pts], reverse)


def profile(x):
    s = (ROOT_X-x)/(ROOT_X-TIP_X)
    mid = .9 + 1.7*s - 33*s*s
    height = 12.1 - 2.55*s
    width = .82 - .16*s
    if x < -311:
        # The last few units change direction gently towards the +Y edge.
        for (xa,ya,ha,wa),(xb,yb,hb,wb) in zip(TIP_ROWS,TIP_ROWS[1:]):
            if x >= xb:
                t = (xa-x)/(xa-xb)
                return (lerp(ya,yb,t), lerp(ha,hb,t), lerp(wa,wb,t))
        return TIP_ROWS[-1][1:]
    return mid, height, width


TIP_ROWS = [(-311, *profile(-311)), (-319,-28.32,8.7,.53),
            (-325,-28.90,5.9,.37), (-329,-28.48,2.9,.18), (TIP_X,-27.30,0,0)]
STATIONS = [ROOT_X,-72,-103,BREAK_X,-170,-205,-243,-279,-299] + [row[0] for row in TIP_ROWS]
SECTION = [(0,0),(.22,.42),(.71,1),(.95,.88),(1,.66),
           (1,-.66),(.95,-.88),(.71,-1),(.22,-.42)]
ITEM_SECTION = [(t if t != .22 else .43,w) for t,w in SECTION]


def sword_ring(x, section):
    mid, h, width = profile(x)
    # The two broken pieces split on the very same ring used by the intact
    # blade. Its jogs read on both broad faces, and the caps match exactly.
    offsets = (1.3,-1.7,1.8,-2.1,.9,.9,-2.1,1.8,-1.7)
    return [(x+(offsets[i] if x == BREAK_X else 0), mid+(.5-t)*h, w*width)
            for i,(t,w) in enumerate(section)]


def sword_body(body, glow, part, item):
    xs = [x for x in STATIONS if (part != 'tip' or x <= BREAK_X)
          and (part != 'hilt' or x >= BREAK_X)]
    if item:
        xs = [x for x in xs if x not in (-72,-170,-243,-299,-329)]
    section = ITEM_SECTION if item else SECTION
    edge_span = section[1][0]
    for xa,xb in zip(xs,xs[1:]):
        a,b = sword_ring(xa,section),sword_ring(xb,section)
        for i,(t,_) in enumerate(section):
            j = (i+1)%len(section)
            tj = section[j][0]
            points = (a[i],b[i],b[j],a[j])
            def along(p):
                return min(1,max(0,(ROOT_X-p[0])/(ROOT_X-TIP_X)))
            s = [along(p) for p in points]
            tt = (t,t,tj,tj)
            if i in (0,len(section)-1):
                face_uv = [(u,v/edge_span) for u,v in zip(s,tt)]
                surface(body, points, 'edge_lip', face_uv)
                side = 1 if i==0 else -1
                overlay = [(x,y,z+side*.09) for x,y,z in points]
                surface(glow, overlay, 'sear', face_uv)
            elif t == tj:
                surface(body, points, 'dark')
            else:
                surface(body, points, 'blade', list(zip(s,tt)))
            if i in (1,len(section)-2) and -325 <= xa <= -319:
                # A short separate trace follows the inside of the upturned
                # hook. Transparent ends keep it apart from the main edge.
                side = 1 if i==1 else -1
                overlay = [(x,y,z+side*.09) for x,y,z in points]
                coords = [((-319-x)/13,(v-edge_span)/(.71-edge_span)) for (x,_,_),v in zip(points,tt)]
                surface(glow,overlay,'hook',coords)
    if part != 'tip':
        butt(body, sword_ring(ROOT_X,section), 'dark')
    if part in ('hilt','tip'):
        rim = sword_ring(BREAK_X,section)
        butt(body, rim, 'impact', part=='hilt')
        butt(glow, rim, 'hot_break', part=='hilt', -.09 if part=='hilt' else .09)


OCT = [(1,.58),(.64,1),(-.64,1),(-1,.58),(-1,-.58),(-.64,-1),(.64,-1),(1,-.58)]
HEX = [(1,0),(.64,1),(-.64,1),(-1,0),(-.64,-1),(.64,-1)]


def oval(x, y, h, w, section=OCT):
    return [(x,y+h*dy,w*dz) for dy,dz in section]


def sleeve(mesh, rows, broad, trim, close=True):
    start,end = rows[0][0],rows[-1][0]
    rings = [oval(*r) for r in rows]
    for a,b in zip(rings,rings[1:]):
        u,v = (a[0][0]-start)/(end-start), (b[0][0]-start)/(end-start)
        for i in range(8):
            j = (i+1)%8
            tile = broad if i in (1,5) else trim
            tv = (1,0) if i == 5 else (0,1)
            surface(mesh,(a[i],b[i],b[j],a[j]), tile,
                    ((u,tv[0]),(v,tv[0]),(v,tv[1]),(u,tv[1])))
    if close:
        butt(mesh,rings[0],trim)
        butt(mesh,rings[-1],trim,True)
    return rings


def recessed_port(body,glow,face,item):
    # Face winding points towards the nose (-X). The floor lies 1.6 units
    # inside the housing; neither opening changes the guard's outer contour.
    if item:
        surface(body,face,'vent_back')
        surface(glow,[(x-.09,y,z) for x,y,z in face],'vent_light')
        return
    cx,cy,cz=(sum(p[i] for p in face)/4 for i in range(3))
    opening=[(cx,lerp(cy,y,.75),lerp(cz,z,.75)) for _,y,z in face]
    floor=[(cx+1.6,lerp(cy,y,.59),lerp(cz,z,.59)) for _,y,z in face]
    for i in range(4):
        j=(i+1)%4
        surface(body,(face[i],face[j],opening[j],opening[i]),'metal')
        surface(body,(opening[i],opening[j],floor[j],floor[i]),'port')
    surface(body,floor,'vent_back')
    surface(glow,[(x-.09,y,z) for x,y,z in floor],'vent_light')


def restraint(body, glow, item):
    # Three different surfaces remain visible in a diagonal view: faceted
    # field housing, its open bore and the non-emitting core inside the bore.
    n = 8 if item else 12
    def ring(x,ry,rz):
        return [(x, .6+ry*cos(2*pi*(i+.5)/n), rz*sin(2*pi*(i+.5)/n)) for i in range(n)]
    outer_a,outer_b = ring(-28.5,14.45,9.8),ring(-37.6,14.45,9.8)
    inner_a,inner_b = ring(-28.5,9.6,6.6),ring(-37.6,9.6,6.6)
    for i in range(n):
        j = (i+1)%n
        for a,b,tile,rev in ((outer_a,outer_b,'metal',False), (inner_a,inner_b,'port',True),
                              (inner_a,outer_a,'metal',False)):
            surface(body,(a[i],b[i],b[j],a[j]),tile,reverse=rev)
        nose=(outer_b[i],inner_b[i],inner_b[j],outer_b[j])
        if i in (n-1,n//2-1):
            recessed_port(body,glow,nose,item)
        else:
            surface(body,nose,'metal')
        def inner_light(p):
            x,y,z = p
            return (x,.6+(y-.6)*.987,z*.987)
        a,b = ring(-31.4,9.6,6.6),ring(-33.6,9.6,6.6)
        pts = [inner_light(p) for p in (a[i],b[i],b[j],a[j])]
        surface(glow,pts,'bronze' if i == (n//2)-1 else 'light',reverse=True)
        if i%(n//4) == 0:
            # Four physical coil shoes wrap the outer facets. A narrow
            # stripe runs across each crown and is visible in the GUI.
            a,b = ring(-27.4,16,10.9),ring(-38.7,16,10.9)
            for points,tile in [((a[i],b[i],b[j],a[j]),'coil'),
                                ((outer_a[i],a[i],a[j],outer_a[j]),'metal'),
                                ((b[i],outer_b[i],outer_b[j],b[j]),'metal'),
                                ((a[i],outer_a[i],outer_b[i],b[i]),'dark'),
                                ((b[j],outer_b[j],outer_a[j],a[j]),'dark')]:
                surface(body,points,tile)
            half_width = 2.5 if item else 1.35
            a,b = ring(-32.95+half_width,16.10,11.01),ring(-32.95-half_width,16.10,11.01)
            surface(glow,(a[i],b[i],b[j],a[j]),'light')
    # Low-poly closed orb, inset in the guard bore (axis X).
    sphere = ((-24.7,0),(-28.3,4.4),(-32.1,6.1),(-36.4,4.3),(-39.3,0))
    if item:
        sphere = sphere[::2]
    rings = [ring(x,r,r) for x,r in sphere]
    for row,(a,b) in enumerate(zip(rings,rings[1:])):
        for i in range(n):
            j=(i+1)%n
            surface(body,(a[i],b[i],b[j],a[j]),'core',
                    [(s,t/(len(rings)-1)) for s,t in
                     ((i/n,row),(i/n,row+1),(j/n,row+1),(j/n,row))])


def handle(body,glow,item):
    sleeve(body,[(31,0,4.3,3.45),(22,.1,5.1,3.8),(-15,0,5.3,3.85),
                 (-27,0,4.4,3.6)],'grip','dark')
    restraint(body,glow,item)
    # The small terminal ring faces sideways; the opening survives even
    # when the large guard is seen edge-on.
    def rim(z,rx,ry):
        return [(34.84+rx*cos(i*pi/4),ry*sin(i*pi/4),z) for i in range(8)]
    front,back,hole_f,hole_b=rim(2.65,6.9,5.1),rim(-2.65,6.9,5.1),rim(2.65,3.7,2.4),rim(-2.65,3.7,2.4)
    for a,b,tile,rev in ((front,back,'metal',False),(hole_b,hole_f,'port',False),
                          (hole_f,front,'metal',False),(back,hole_b,'dark',False)):
        for i in range(8):
            j=(i+1)%8
            surface(body,(a[i],b[i],b[j],a[j]),tile,reverse=rev)


def sword(part, item=False):
    body,glow = Mesh(),Mesh()
    sword_body(body,glow,part,item)
    if part != 'tip':
        handle(body,glow,item)
    return body,glow


MOUTH = -38.6
COLLARS = ((MOUTH,-50),(-96,-102),(-160,-166),(-226,-232),(-291,-297))


def case_row(x, relief=0):
    if x <= -311:
        tip_profile = [(-311, -26, 7.8,4),(-325,-28.25,6.7,3.5),(-335,-27.7,4.6,3)]
        for a,b in zip(tip_profile,tip_profile[1:]):
            if x >= b[0]:
                f=(a[0]-x)/(a[0]-b[0])
                return (x,lerp(a[1],b[1],f),lerp(a[2],b[2],f)+relief,lerp(a[3],b[3],f)+relief)
        return (x,-27.7,4.6+relief,3+relief)
    mid,h,_=profile(x)
    return (x,mid,h*.5+2.85+relief,4.7-.6*((MOUTH-x)/295)+relief)


def talons(mesh):
    # Three tapered silver-purple jaws have open space between their tips.
    def point(x,cy,theta,ry,rz):
        return (x,cy+ry*cos(theta),rz*sin(theta))
    for k, ang in enumerate((.12,2.28,4.15)):
        rings=[]
        for x,cy,ry,rz,spread,width in [(-332.2,-27.7,4.4,3.6,.57,1.7),
                                        (-340,-28.3,5.6,3.85,.34,1.25)]:
            rings.append([point(x,cy,ang-spread,ry,rz),point(x,cy,ang+spread,ry,rz),
                          point(x,cy,ang+spread,ry-width,rz-width),
                          point(x,cy,ang-spread,ry-width,rz-width)])
        a,b=rings
        apex=point(-349+1.3*k,-28.6,ang,3.1,2.6)
        for i in range(4):
            j=(i+1)%4
            surface(mesh,(a[i],b[i],b[j],a[j]),'claw' if i==0 else 'metal')
            surface(mesh,(b[i],apex,b[j]),'claw',RECT[:3])


def case(item=False):
    body,glow=Mesh(),Mesh()
    # At GUI scale a single facet replaces the two narrow top/bottom bevels.
    section=HEX if item else OCT
    sides=len(section)
    flank=(1,4) if item else (1,5)
    # Incorporate the five field coils in the casing profile. There is no
    # second closed cylinder underneath any of the five raised bands.
    rows=[]
    for i,(a,b) in enumerate(COLLARS):
        lift=1.5 if i==0 else 1.2
        if i:
            rows.append((a+1.5,0))
        rows.extend(((a,lift),(b,lift),(b-1.5,0)))
    rows.extend(((-311,0),(-325,0),(-335,0)))
    sections=[oval(*case_row(x,r),section) for x,r in rows]
    for k,(a,b) in enumerate(zip(sections,sections[1:])):
        xa,ra=rows[k]
        xb,rb=rows[k+1]
        in_coil=ra>0 and rb>0
        for i in range(sides):
            j=(i+1)%sides
            meter_face=in_coil and i==flank[0]
            if in_coil or ra!=rb:
                tile='coil_meter' if meter_face else 'coil' if in_coil else 'metal'
                surface(body,(a[i],b[i],b[j],a[j]),tile)
            else:
                u,v=(MOUTH-xa)/(MOUTH+335),(MOUTH-xb)/(MOUTH+335)
                t=(1,0) if i==flank[1] else (0,1)
                tex='case' if i in flank else 'dark'
                surface(body,(a[i],b[i],b[j],a[j]),tex,((u,t[0]),(v,t[0]),(v,t[1]),(u,t[1])))
            if in_coil and (i in flank or k==0):
                # A magenta status seam crosses the coil surface. The mouth
                # seam continues around its rim; the other coils vent sideways.
                left,right=(xa,xb) if meter_face else (lerp(xa,xb,.22),lerp(xa,xb,.48))
                ca,cb=case_row(left,ra+.09),case_row(right,ra+.09)
                p,q=oval(*ca,section),oval(*cb,section)
                tile=f'meter_{COLLARS.index((xa,xb))+1}' if meter_face else 'light'
                surface(glow,(p[i],q[i],q[j],p[j]),tile)
    x,cy,h,w=case_row(MOUTH)
    inner=oval(x,cy,h-1.15,w-1.25,section)
    throat=oval(x-11,cy-.1,h-1.25,w-1.32,section)
    for i in range(sides):
        j=(i+1)%sides
        surface(body,(inner[i],sections[0][i],sections[0][j],inner[j]),'metal')
        surface(body,(inner[j],throat[j],throat[i],inner[i]),'dark')
    butt(body,throat,'dark')
    butt(body,sections[-1],'dark',True)
    talons(body)
    return body,glow


def effect():
    mesh=Mesh()
    rows=[(-37.4,case_row(MOUTH,2.85)[1:]),(-106,case_row(-106,2.65)[1:]),
          (-185,case_row(-185,2.65)[1:]),(-252,case_row(-252,2.65)[1:]),
          (-303,case_row(-303,2.65)[1:]),(-332,(-28.15,8.3,5.6)),
          (-350.3,(-28.4,4.6,3.9))]
    rings=[oval(x,*rest) for x,rest in rows]
    for a,b in zip(rings,rings[1:]):
        u,v = ((MOUTH-r[0][0])/64 for r in (a,b))
        for i in range(8):
            j=(i+1)%8
            mesh.face((a[i],b[i],b[j],a[j]),((u,i/8),(v,i/8),(v,(i+1)/8),(u,(i+1)/8)))
    for ring, rev in ((rings[0],False),(rings[-1],True)):
        for i in range(1,7):
            tri=(ring[0],ring[i],ring[i+1])
            mesh.face(tri,[(p[1]/64,p[2]/64) for p in tri],reverse=rev)
    return mesh


def item_pose(p,sheath,damaged):
    x,y,z=p
    if sheath:
        theta=radians(-48)
        dx,dy=(x+189)*.65,(y+11)*1.1
        cx,cy,cz=-13,-10,11.4
    else:
        theta=radians(-141)
        dx,dy=(x+145)*.55,(y+9)*1.25
        cx,cy,cz=-7,-5,20
    px,py=cx+dx*cos(theta)-dy*sin(theta),cy+dx*sin(theta)+dy*cos(theta)
    if sheath:
        px,py=.916*px+4,.809*py-3.84
    else:
        px,py=1.10*px+4.45,.894*py+2.4
    if damaged:
        return (px*.89+6.6,py*1.04-4,cz+z*.62)
    return (px,py,cz+z*.72)


def inventory(name):
    damaged=name=='item_damaged'
    body,glow=Mesh(),Mesh()
    pieces=[(sword('hilt' if damaged else 'all',True),False)]
    if name!='item_bladens':
        pieces.append((case(True),True))
    for (part,light),sheath in pieces:
        def place(p):
            return item_pose(p,sheath,damaged)
        body.include(part,place)
        glow.include(light,place)
    return body,glow


def build():
    meshes={}
    for part,name in (('all','blade'),('hilt','blade_damaged'),('tip','blade_fragment')):
        meshes[name],meshes[name+'_luminous']=sword(part)
    meshes['sheath'],meshes['sheath_luminous']=case()
    meshes['effect']=effect()
    for name in ('item_blade','item_damaged','item_bladens'):
        meshes[name],meshes[name+'_luminous']=inventory(name)
    return meshes


if __name__=='__main__':
    export(build(), ASSETS/'model.obj', title='Singularity')
