"""Model the quantum tachi in the coordinate system of SlashBlade's hand rig."""
from math import hypot

from sb1_nano_model import Mesh, export, lerp
from sb1_quantum_texture import ASSETS, uv, write_textures


RECT = ((0,0),(1,0),(1,1),(0,1))


def panel(mesh, points, tile, coords=RECT, reverse=False):
    mesh.face(points,[uv(tile,s,t) for s,t in coords],reverse=reverse)


# Image-space section data for a shallow, even tachi arc. A small quadratic
# term produces a measurable silhouette without concentrating it at the point.
TIP, ROOT_X, BREAK = -308.0, -32.0, -158.0


def centreline(x):
    t=(ROOT_X-x)/(ROOT_X-TIP)
    return 1.65-7.5*t-14*t*t


PROFILE=tuple((x,centreline(x),width,depth) for x,width,depth in (
    (-32,10.1,.76),(-55,10.02,.75),(-108,9.85,.74),(-125,9.8,.73),
    (BREAK,9.75,.72),(-202,9.6,.70),(-249,9.32,.67),(-286,9.05,.64),
    (-298,8.7,.60),(-305,3.25,.29),(TIP,0,0),
))
# The longest flat is the bright ground up to the shinogi. After the short
# groove ends, the three dormant rail stations lie on the same back plane.
SIDE=((0,0),(.40,.68),(.49,1),(.58,.96),(.70,.91),(.77,.88),(1,.73))
SECTION=SIDE+tuple((t,-d) for t,d in SIDE[:0:-1])
BREAK_SHIFT=(0,-1.15,.7,1.9,-1.3,.3,-.9)


def section_point(r, t, depth):
    x,y,h,z=r
    if t in (.58,.70):
        groove=min(1,max(0,(ROOT_X-x)/12),max(0,(x+125)/17))
        depth-= (.26*groove if depth>0 else -.26*groove)
    if x==BREAK:
        x+=next(offset for (across,_),offset in zip(SIDE,BREAK_SHIFT) if across==t)
    return x,y+(.5-t)*h,z*depth


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
        # These intermediate rings contribute less than a pixel even to
        # the 128px slot. Keep the crest, the fracture and the start of the tip.
        rows=[r for r in rows if r[0] not in (-55,-125,-202,-286,-305)]
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
            if min(ta,tb)==0 or x0<=-298:
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


def tsuba(mesh,light):
    # Section coordinates are (Y,Z). Five strips span the graphite face;
    # the two slit columns intentionally have no surface over their openings.
    centre_y=1.25
    near,far=-31,-34.6
    def point(x,y,z):
        return (x,centre_y+y,z)
    def contour(h,w,c):
        return [(h,-w+c),(h,w-c),(h-c,w),(-h+c,w),(-h,w-c),
                (-h,-w+c),(-h+c,-w),(h-c,-w)]
    outer=contour(14.65,10.15,3.7)
    inner=contour(13.15,8.65,3.0)
    def cap_uv(y,z):
        return ((z+10.15)/20.3,(14.65-y)/29.3)
    def flat(surface,x,yz,tile,rev=False):
        panel(surface,[point(x,y,z) for y,z in yz],tile,[cap_uv(y,z) for y,z in yz],rev)
    holes=((-3.05,-1.55),(1.55,3.05))
    for x,rev in ((near,False),(far,True)):
        for i in range(8):
            j=(i+1)%8
            flat(mesh,x,[outer[i],outer[j],inner[j],inner[i]],"ceramic",rev)
        columns=(-8.65,-5.65,-3.05,-1.55,1.55,3.05,5.65,8.65)
        for a,b in zip(columns,columns[1:]):
            ha=13.15-max(0,abs(a)-5.65)
            hb=13.15-max(0,abs(b)-5.65)
            patches=([(-ha,-10.8,-10.8,-hb),(10.8,ha,hb,10.8)] if
                     (a,b) in holes else [(-ha,ha,hb,-hb)])
            for y0,y1,y2,y3 in patches:
                flat(mesh,x,[(y0,a),(y1,a),(y2,b),(y3,b)],"tsuba",rev)
    for i in range(8):
        j=(i+1)%8
        panel(mesh,[point(x,*v) for x,v in ((near,outer[i]),(far,outer[i]),(far,outer[j]),(near,outer[j]))],"ceramic")
    for a,b in holes:
        wall=[(-10.8,a),(10.8,a),(10.8,b),(-10.8,b)]
        for i in range(4):
            j=(i+1)%4
            panel(mesh,[point(x,*v) for x,v in ((near,wall[i]),(far,wall[i]),(far,wall[j]),(near,wall[j]))],
                  "socket",reverse=True)
        # Light is confined to the inside walls and the thin edges of the
        # apertures. The centre still shows clear through the guard from either face.
        for z,sgn in ((a,1),(b,-1)):
            panel(light,[point(x,y,z+sgn*.045) for x,y in
                        ((near-.15,-10.6),(near-.15,10.6),(far+.15,10.6),(far+.15,-10.6))],
                  "seam_light",reverse=sgn<0)
        for x,rev in ((near+.025,False),(far-.025,True)):
            for lo,hi in ((a,a+.31),(b-.31,b)):
                panel(light,[point(x,y,z) for y,z in
                            ((-10.75,lo),(10.75,lo),(10.75,hi),(-10.75,hi))],"seam_light",reverse=rev)


def fittings(mesh,light):
    hull(mesh,[(36,0,3.8,2.98),(27,.1,4.65,3.3),(-20,.65,5.12,3.4),(-28.7,1.1,4.9,3.15)],
         "grip","frame","frame")
    hull(mesh,[(41.74,.2,.32,.27),(37,.25,4.4,3.3),(33,.15,4.25,3.25)],
         "pommel","ceramic","socket")
    hull(mesh,[(-27.6,1.13,4.95,3.32),(-30.95,1.22,5.25,3.5)],
         "socket","frame","frame")
    tsuba(mesh,light)


SAYA=tuple((x,centreline(x),h,z) for x,h,z in (
    (-34.65,8.03,4.12),(-39,7.9,4.0),(-55,7.82,3.85),
    (-108,7.6,3.75),(-158,7.44,3.7),(-203,7.28,3.52),(-250,7.04,3.37),
    (-292,6.80,3.25),(-306,6.20,3.14),(-309,6.1,3.2),(-320,.28,.25),
))


def at(rows,x):
    for a,b in zip(rows,rows[1:]):
        if x>=b[0]:
            f=(x-a[0])/(b[0]-a[0])
            return (x,*(lerp(v,w,f) for v,w in zip(a[1:],b[1:])))
    return rows[-1]


def make_sheath():
    mesh,light=Mesh(),Mesh()
    first,last=SAYA[0][0],SAYA[-1][0]
    # The graphite koiguchi seats squarely against the steel blade's guard.
    outer=hull(mesh,SAYA[:2],"socket","frame","frame",False,False)[0]
    hull(mesh,SAYA[1:-2],"sheath","ceramic","ceramic",False,False,span=(first,last))
    start,stop=(at(SAYA,x) for x in (-310.6,-312.85))
    hull(mesh,SAYA[-3:-1]+(start,),"ceramic","ceramic","ceramic",False,False)
    hull(mesh,[start,stop],"socket","frame","socket",False,False)
    hull(mesh,[stop,SAYA[-1]],"ceramic","ceramic","ceramic",False,True)
    inner=ring((first,centreline(first),6.55,2.46))
    recess=ring((-48,centreline(-48),6.45,2.34))
    for i in range(8):
        j=(i+1)%8
        panel(mesh,(outer[i],outer[j],inner[j],inner[i]),"socket")
        panel(mesh,(inner[i],inner[j],recess[j],recess[i]),"frame")
    radial_cap(mesh,recess,"socket")
    # Kurikata is a compact graphite lacing bridge on the outward face.
    # Two feet leave the transverse passage open beneath its arched crown.
    upper=[(-117,4.75),(-119,6.05),(-126,6.05),(-128,4.75)]
    lower=[(-117,3.5),(-120,4.60),(-125,4.60),(-128,3.5)]
    for i in range(3):
        front=[(x,centreline(x)+1.9,z) for x,z in (upper[i],upper[i+1],lower[i+1],lower[i])]
        back=[(x,centreline(x)-1.9,z) for x,z in (upper[i],upper[i+1],lower[i+1],lower[i])]
        panel(mesh,front,"socket",reverse=True)
        panel(mesh,back,"socket")
        panel(mesh,(front[0],front[1],back[1],back[0]),"frame")
        panel(mesh,(front[2],front[3],back[3],back[2]),"socket")
        if i==0:
            panel(mesh,(front[3],front[0],back[0],back[3]),"frame")
        if i==2:
            panel(mesh,(front[1],front[2],back[2],back[1]),"frame")
    # Five unevenly spaced graduations share one inset near the mouth.
    for side in (1,-1):
        for layer,tile,lift in ((mesh,"ruler",.035),(light,"ruler_light",.105)):
            ps=[]
            for x,t in ((-63,.29),(-85,.29),(-85,-.27),(-63,-.27)):
                _,cy,h,z=at(SAYA,x)
                ps.append((x,cy+t*h,side*(z+lift)))
            panel(layer,ps,tile,reverse=side<0)
    band=[]
    for x in (-310.95,-312.45):
        x,y,h,z=at(SAYA,x)
        band.append(ring((x,y,h+.095,z+.095)))
    for i in range(8):
        j=(i+1)%8
        panel(light,(band[0][i],band[1][i],band[1][j],band[0][j]),"seam_light")
    return mesh,light


def effect():
    mesh=Mesh()
    shell=[(-33.75,centreline(-34.65),9.55,5.52),(-113,centreline(-113),9.10,7.5),
           (-132,centreline(-132),9.13,7.52),(-160,centreline(-160),8.94,5.0),
           (-248,centreline(-248),8.68,4.78),(-309,centreline(-309),7.66,4.56),
           (-321.2,centreline(-320),1.63,1.63)]
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
        depth=(19.85 if broken else 21.65)+z*.76
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
