import si_gui2 as g
OUT='/home/claude/work/src/src/main/resources/assets/mio_icif/textures/gui/'
# Armory console 176x232
img=g.new_canvas()
g.panel(img,0,0,176,232)
g.groove(img,4,15,168)
for r in range(6):
    g.well(img,7,18+r*18,56,18,grid=False)
    for c in range(6):
        g.slot(img,65+c*18,18+r*18)
g.groove(img,4,126,168)
g.slot(img,7,129)
g.groove(img,4,145,168)
g.inventory(img,7,149)
img.save(OUT+'gui_armory.png')
# Remote console 256x200
img=g.new_canvas()
g.panel(img,0,0,256,200)
g.groove(img,4,15,248)
g.well(img,7,19,158,122,grid=True)
g.well(img,169,19,80,122,grid=True)
g.groove(img,4,145,248)
g.well(img,7,149,242,44,grid=True)
img.save(OUT+'gui_armory_remote.png')
print('ok')
