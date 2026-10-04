from PIL import Image
def draw(rows, palette, path, scale_preview=None):
    h=len(rows); w=len(rows[0])
    im=Image.new('RGBA',(w,h),(0,0,0,0))
    for y,r in enumerate(rows):
        assert len(r)==w,(path,y,len(r))
        for x,c in enumerate(r):
            if c!='.': im.putpixel((x,y),palette[c])
    im.save(path)
    if scale_preview: im.resize((w*16,h*16),Image.NEAREST).save(scale_preview)
    return im
