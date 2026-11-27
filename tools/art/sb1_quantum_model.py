"""Model the quantum tachi in the coordinate system of SlashBlade's hand rig."""
from math import hypot

from sb1_nano_model import Mesh, export, lerp
from sb1_quantum_texture import ASSETS, uv, write_textures


RECT = ((0,0),(1,0),(1,1),(0,1))


def panel(mesh, points, tile, coords=RECT, reverse=False):
    mesh.face(points,[uv(tile,s,t) for s,t in coords],reverse=reverse)


# Position along the tachi, centre Y, full blade height and half depth. The
# curve turns earliest behind the broken-blade station, then approaches a long
# sloping tangent. At the last two stations the edge and spine meet together.
PROFILE = (
    (-32,1.7,10.9,.81), (-62,3.55,10.8,.80), (-91,4.1,10.65,.78),
    (-127,2.85,10.45,.75), (-163,.35,10.15,.72), (-211,-6.85,9.85,.67),
    (-261,-16.7,9.25,.62), (-298,-24.85,8.4,.57), (-307,-26.85,7.8,.49),
    (-316,-29.0,0,0),
)
BREAK = -127
TIP = PROFILE[-1][0]
# t=0 is the +Y edge. The two shoulders either side of the narrow floor
# give the luminous fuller real relief in the 3D cross-sections.
SIDE = ((0,0),(.21,.44),(.40,1),(.49,.68),(.61,.68),(.71,.95),(1,.74))
SECTION = SIDE + tuple((t,-d) for t,d in SIDE[:0:-1])
BREAK_SHIFT = (0, -2.3, .4, 2.1, -.6, -1.6, 1.8)


def section_point(r, t, depth):
    x,y,h,z = r
    if x == BREAK:
        dx = next(offset for (across,_),offset in zip(SIDE,BREAK_SHIFT) if across == t)
        x += dx
    return x, y + (.5-t)*h, z*depth


def radial_cap(mesh, ring, tile, reverse=False):
    ys = [p[1] for p in ring]
    zs = [p[2] for p in ring]
    centre = tuple(sum(p[k] for p in ring)/len(ring) for k in range(3))
    coords = [((z-min(zs))/max(.001,max(zs)-min(zs)), (max(ys)-y)/max(.001,max(ys)-min(ys)))
              for _,y,z in ring]
    for i in range(len(ring)):
        j=(i+1)%len(ring)
        panel(mesh,(centre,ring[i],ring[j]),tile,((.5,.5),coords[i],coords[j]),reverse)


def surface_light(light, points, coords, side, near=0, far=1):
    a,b,c,d = points
    aa,bb,cc,dd = coords
    def mix(first,second,s):
        return tuple(lerp(v,w,s) for v,w in zip(first,second))
    ps = (mix(a,b,near),mix(a,b,far),mix(d,c,far),mix(d,c,near))
    st = (mix(aa,bb,near),mix(aa,bb,far),mix(dd,cc,far),mix(dd,cc,near))
    panel(light,[(x,y,z+side*.075) for x,y,z in ps],"blade_light",st)


def make_blade(part, inventory=False):
    mesh,light = Mesh(),Mesh()
    rows = [r for r in PROFILE if part == "full" or
            (r[0] >= BREAK if part=="hilt" else r[0] <= BREAK)]
    if inventory:
        # These two intermediate rings contribute less than a pixel even to
        # the 128px slot. Keep the crest, the fracture and the start of the tip.
        rows=[r for r in rows if r[0] not in (-62,-307)]
    rings = [[section_point(r,t,d) for t,d in SECTION] for r in rows]
    for k,(a,b) in enumerate(zip(rings,rings[1:])):
        x0,x1 = rows[k][0],rows[k+1][0]
        for i,(ta,_) in enumerate(SECTION):
            j=(i+1)%len(SECTION)
            tb=SECTION[j][0]
            pts=(a[i],b[i],b[j],a[j])
            if ta == tb:
                panel(mesh,pts,"frame")
                continue
            st=tuple(((p[0]-PROFILE[0][0])/(TIP-PROFILE[0][0]),t)
                     for p,t in zip(pts,(ta,ta,tb,tb)))
            panel(mesh,pts,"blade",st)
            if {ta,tb} in ({0,.21},{.49,.61}):
                near = 2.7/(x0-x1) if part=="tip" and x0==BREAK else 0
                far = 1-2.7/(x0-x1) if part=="hilt" and x1==BREAK else 1
                surface_light(light,pts,st,1 if ta<tb else -1,near,far)
    if part=="hilt":
        radial_cap(mesh,rings[-1],"fracture",True)
    if part=="tip":
        radial_cap(mesh,rings[0],"fracture")
    else:
        radial_cap(mesh,rings[0],"socket")
        fittings(mesh,light)
    return mesh,light


# An eight-sided section with a wide flat side facing each cheek. Starting
# at the dorsal lip keeps the two long material islands facing the same way.
OCT = ((1,.55),(.67,1),(-.67,1),(-1,.55),(-1,-.55),(-.67,-1),(.67,-1),(1,-.55))


def ring(row):
    x,y,h,z=row
    return [(x,y+h*v,z*w) for v,w in OCT]


def hull(mesh, rows, sides, shoulders, rim, cap_near=True, cap_far=True, span=None):
    rings = [ring(r) for r in rows]
    start,end = span or (rows[0][0],rows[-1][0])
    for a,b in zip(rings,rings[1:]):
        sa,sb=(a[0][0]-start)/(end-start),(b[0][0]-start)/(end-start)
        for i in range(8):
            j=(i+1)%8
            pts=(a[i],b[i],b[j],a[j])
            if i in (1,5):
                ta,tb=(0,1) if i==1 else (1,0)
                panel(mesh,pts,sides,((sa,ta),(sb,ta),(sb,tb),(sa,tb)))
            else:
                panel(mesh,pts,rim if i in (3,7) else shoulders)
    if cap_near:
        radial_cap(mesh,rings[0],rim)
    if cap_far:
        radial_cap(mesh,rings[-1],rim,True)
    return rings


def prong(mesh, light, upper):
    # The two prongs sweep forward along X. The lattice sits in a dark
    # ceramic bezel on both sides and remains visible in a reverse display.
    sign=1 if upper else -1
    bias=1.2
    outline=[(-30,4),(-30.8,9),(-35.3,13),(-47.5,14.0),(-45.8,10.2),(-36.1,5.5)]
    roots=[(x,sign*y+bias) for x,y in outline]
    if not upper:
        roots=[(x+1.2,y) for x,y in roots]
    xs,ys=zip(*roots)
    front=[(x,y,4.55) for x,y in roots]
    back=[(x,y,-4.55) for x,y in roots]
    coords=[((max(xs)-x)/(max(xs)-min(xs)),(max(ys)-y)/(max(ys)-min(ys))) for x,y in roots]
    # The authored front outline is counter-clockwise for the lower prong.
    ccw=sum(x*y1-x1*y for (x,y),(x1,y1) in zip(roots,roots[1:]+roots[:1]))>0
    panel(mesh,front,"prong",coords,not ccw)
    panel(mesh,back,"prong",coords,ccw)
    for i in range(len(front)):
        j=(i+1)%len(front)
        panel(mesh,(front[i],back[i],back[j],front[j]),"frame",reverse=not ccw)
    # Place the square screens slightly forward of the roots and align them
    # to the prongs' cant; all four grille faces are flush with their mounts.
    cx,cy=(-39.7 if upper else -38.5),bias+sign*10.5
    face=[(cx+3.0,cy-sign*2.65),(cx-2.85,cy-sign*1.7),
          (cx-2.85,cy+sign*2.65),(cx+3.0,cy+sign*1.7)]
    for side in (1,-1):
        panel(mesh,[(x,y,side*4.58) for x,y in face],"grille",reverse=side*sign>0)
        panel(light,[(x,y,side*4.68) for x,y in face],"emitter",reverse=side*sign>0)


def fittings(mesh,light):
    hull(mesh,[(36,0,3.9,3.0),(27,0,4.8,3.4),(-20,.7,5.3,3.6),(-29,1.2,4.9,3.25)],
         "grip","ceramic","frame")
    hull(mesh,[(41.74,.2,.32,.27),(37,.25,4.5,3.35),(33,.15,4.35,3.35)],
         "pommel","ceramic","socket")
    hull(mesh,[(-28.5,1.15,5.4,3.8),(-30.8,1.4,8.3,5.6),(-34.9,1.6,8.5,5.65)],
         "frame","ceramic","frame")
    for upper in (True,False):
        prong(mesh,light,upper)


# Mouth, broad sweep, tail emitter and faceted spear. Give the ceramic housing
# clearance for the tip even in the section just ahead of the emitter.
SAYA = (
    (-35.2,1.9,8.4,4.5),(-62,3.5,8.35,4.25),(-91,4.0,8.05,4.12),
    (-127,2.85,7.98,4.0),(-163,.35,7.85,3.9),(-211,-6.85,7.7,3.75),
    (-261,-16.7,7.4,3.55),(-299,-25,6.75,3.5),(-308,-27,6.2,3.45),
    (-315,-28.6,6.7,3.85),(-327,-31.8,.25,.25),
)


def at(rows,x):
    for a,b in zip(rows,rows[1:]):
        if x>=b[0]:
            f=(x-a[0])/(b[0]-a[0])
            return (x,*(lerp(v,w,f) for v,w in zip(a[1:],b[1:])))
    return rows[-1]


def make_sheath():
    mesh,light=Mesh(),Mesh()
    first,last=SAYA[0][0],SAYA[-1][0]
    outer = hull(mesh,SAYA[:-2],"sheath","ceramic","frame",False,False,span=(first,last))[0]
    # A graphite seat beneath the luminous ring keeps it cyan when the game
    # adds the emitter colour to the glazed shell.
    left,right=(at(SAYA,x) for x in (-310.5,-313.3))
    hull(mesh,[SAYA[-3],left],"prong","ceramic","frame",False,False)
    hull(mesh,[left,right],"socket","socket","frame",False,False)
    hull(mesh,[right,*SAYA[-2:]],"prong","ceramic","frame",False,True)
    # A real open throat, visible even when the blade and sheath are viewed apart.
    inner=ring((first,1.9,6.9,2.7))
    recess=ring((-47,2.5,6.65,2.5))
    for i in range(8):
        j=(i+1)%8
        panel(mesh,(outer[i],outer[j],inner[j],inner[i]),"socket")
        panel(mesh,(inner[i],inner[j],recess[j],recess[i]),"frame")
    radial_cap(mesh,recess,"socket")
    # The two white cheek guides sit over the mouth sidewalls. The diagonal
    # roof follows the short fore-swept prongs without filling their gap.
    for side in (1,-1):
        outline=[(-35.3,7.3),(-40.2,7.75),(-52.1,4.05),(-47.5,.8),(-35.3,1.6)]
        face=[(x,y,side*5.13) for x,y in outline]
        low=[(x,y,side*3.95) for x,y in outline]
        coords=[((first-x)/17,(7.75-y)/7) for x,y in outline]
        panel(mesh,face,"prong",coords,side<0)
        for i in range(len(face)):
            j=(i+1)%len(face)
            panel(mesh,(face[i],low[i],low[j],face[j]),"frame",reverse=side<0)
    # Interference windows on both sides of the shell align to the dark
    # registration seams in the atlas. Only the middle of each seam is lit.
    def point(s,t,side):
        x=lerp(first,last,s)
        _,y,h,z=at(SAYA,x)
        return x,y+lerp(.67,-.67,t)*h,side*(z+.092)
    for s in (.22,.47,.73):
        for side in (1,-1):
            st=[(s+(t-.5)*4/251+offset,t) for t,offset in
                ((.05,-.003),(.05,.003),(.95,.003),(.95,-.003))]
            panel(light,[point(u,v,side) for u,v in st],"seam_light",reverse=side<0)
    # The emitter band is a closed octagonal ring over the pointed end cap.
    band=[]
    for x in (-311.0,-312.8):
        x,y,h,z=at(SAYA,x)
        band.append(ring((x,y,h+.1,z+.1)))
    for i in range(8):
        j=(i+1)%8
        panel(light,(band[0][i],band[1][i],band[1][j],band[0][j]),"seam_light")
    return mesh,light


def effect():
    mesh=Mesh()
    shell=[(-34.1,1.95,9.98,6.53),(-90,4.15,9.76,5.42),(-160,.66,9.5,5.37),
           (-211,-6.85,9.07,5.05),(-262,-16.95,8.7,4.91),(-315,-28.6,8.10,5.23),
           (-328.3,-32.1,1.5,1.6)]
    rings=[ring(r) for r in shell]
    for a,b in zip(rings,rings[1:]):
        for i in range(8):
            j=(i+1)%8
            pts=(a[i],b[i],b[j],a[j])
            mesh.face(pts,[((shell[0][0]-p[0])/64,t/8) for p,t in zip(pts,(i,i,i+1,i+1))])
    for end,rev in ((rings[0],False),(rings[-1],True)):
        for j in range(1,7):
            tri=(end[0],end[j],end[j+1])
            mesh.face(tri,[(p[2]/32,p[1]/32) for p in tri],reverse=rev)
    return mesh


def inventory_transform(p, is_sheath, broken):
    x,y,z=p
    if is_sheath:
        start=(59,-63) if not broken else (60,-73)
        end=(-84,42) if not broken else (-68,45)
        s=(x-SAYA[0][0])/(SAYA[-1][0]-SAYA[0][0])
        offset=(y-lerp(SAYA[0][1],SAYA[-1][1],s))*1.02
        depth=12+z*.87
    else:
        start=(-80,-66) if not broken else (-66,-75)
        end=(66,74)
        s=(x-41.74)/(TIP-41.74)
        offset=(y-lerp(.2,PROFILE[-1][1],s))*1.19
        depth=22+z*.90
    dx,dy=end[0]-start[0],end[1]-start[1]
    length=hypot(dx,dy)
    return start[0]+dx*s+dy/length*offset,start[1]+dy*s-dx/length*offset,depth


def build():
    models={}
    for name,part in (("blade","full"),("blade_damaged","hilt"),("blade_fragment","tip")):
        models[name],models[name+"_luminous"]=make_blade(part)
    models["sheath"],models["sheath_luminous"]=make_sheath()
    models["effect"]=effect()
    for name in ("item_blade","item_damaged","item_bladens"):
        broken=name=="item_damaged"
        parts=make_blade("hilt" if broken else "full",inventory=True)
        for source,suffix in zip(parts,("","_luminous")):
            model=Mesh()
            model.include(source,lambda p:inventory_transform(p,False,broken))
            if name!="item_bladens":
                model.include(models["sheath"+suffix],lambda p:inventory_transform(p,True,broken))
            models[name+suffix]=model
    return models


if __name__ == "__main__":
    write_textures()
    export(build(),ASSETS/"model.obj",title="Quantum")
