from PIL import Image
A='/home/claude/work/src/src/main/resources/assets/mio_icif/textures/'
casing=Image.open(A+'block/producer/block_machine.png').convert('RGBA')
tele=Image.open(A+'block/producer/block_teleporter_elc.png').convert('RGBA')
down=tele.crop((0,0,16,16))
def face_front(lit):
    im=casing.copy(); p=im.load()
    dark=(46,50,56,255); frame=(92,97,104,255); glass=(70,108,128,255) if not lit else (88,190,226,255)
    glass2=(58,90,108,255) if not lit else (60,150,200,255)
    hl=(150,200,220,255) if not lit else (210,245,255,255)
    # door frame
    for y in range(1,15):
        for x in range(3,13):
            p[x,y]=frame
    for y in range(2,14):
        for x in range(4,12):
            p[x,y]=dark
    # window
    for y in range(3,10):
        for x in range(5,11):
            p[x,y]=glass if (x+y)%5 else glass2
    # helmet silhouette in window
    helm=(40,44,50,255) if not lit else (230,170,60,255)
    for (x,y) in [(6,4),(7,4),(8,4),(9,4),(6,5),(9,5),(6,6),(7,6),(8,6),(9,6),(7,7),(8,7)]:
        p[x,y]=helm
    eye=(30,30,34,255) if not lit else (255,250,220,255)
    p[7,5]=eye; p[8,5]=eye
    p[5,3]=hl; p[6,3]=hl
    # handle + status leds
    p[11,11]=(170,174,178,255); p[11,12]=(170,174,178,255)
    p[5,12]=(60,190,90,255) if lit else (80,84,88,255)
    p[7,12]=(220,160,40,255) if lit else (80,84,88,255)
    return im
def face_side(lit):
    im=casing.copy(); p=im.load()
    slot=(70,74,80,255); hi=(200,204,208,255)
    for x in (4,7,10):
        for y in range(4,12):
            p[x,y]=slot; p[x+1,y]=hi if y==11 else (110,114,120,255)
    return im
def face_top(lit):
    im=casing.copy(); p=im.load()
    for y in range(4,12):
        for x in range(4,12):
            p[x,y]=(98,102,108,255)
    for x in range(5,11):
        p[x,7]=(150,154,160,255) if not lit else (88,190,226,255)
        p[x,8]=(60,64,70,255)
    return im
strip=Image.new('RGBA',(192,16))
for half,lit in ((0,False),(96,True)):
    faces=[down,face_top(lit),face_side(lit),face_front(lit),face_side(lit),face_side(lit)]
    for i,f in enumerate(faces): strip.paste(f,(half+i*16,0))
strip.save(A+'block/producer/block_armory.png')
strip.resize((192*5,80),Image.NEAREST).save('/tmp/claude-0/-home-claude/74ca4c0f-0cf6-5fad-a66f-c52de96b116f/scratchpad/armory_strip.png')
