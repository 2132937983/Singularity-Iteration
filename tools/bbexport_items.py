"""Export block model JSONs (classic IC2 cubes) as Blockbench .bbmodel projects with embedded textures."""
import json, os, sys, base64, uuid, glob
from PIL import Image
A = sys.argv[1]; OUT = sys.argv[2]; ids = sys.argv[3:]
def load(p): return json.load(open(p, encoding='utf-8-sig'))
def model_path(ref):
    ns, p = ref.split(':') if ':' in ref else ('minecraft', ref)
    return os.path.join(A, 'models', p + '.json') if ns == 'mio_icif' else None
def resolved(path):
    d = load(path); tex = {}; els = None; chain = [d]
    while True:
        if els is None and 'elements' in d: els = d['elements']
        par = d.get('parent'); mp = model_path(par) if par else None
        if not mp or not os.path.exists(mp): break
        d = load(mp); chain.append(d)
    for d in reversed(chain): tex.update(d.get('textures', {}))
    def res(t, depth=0):
        while t.startswith('#') and depth < 8: t = tex.get(t[1:], t); depth += 1
        return t
    return {k: res(v) for k, v in tex.items()}, els or []
n = 0
for path in sorted(glob.glob(os.path.join(A, 'models/item/3d/*.json'), recursive=True)):
    rel = os.path.relpath(path, os.path.join(A, 'models/item/3d'))[:-5]
    if ids and not any(rel.startswith(i) for i in ids): continue
    tex, els = resolved(path)
    if not els: continue
    keys = [k for k in tex if k != 'particle'] or ['particle']
    textures = []; index = {}
    for k in keys:
        ref = tex[k]
        ns, p = ref.split(':') if ':' in ref else ('minecraft', ref)
        f = os.path.join(A, 'textures', p + '.png')
        if ns != 'mio_icif' or not os.path.exists(f): continue
        im = Image.open(f)
        data = base64.b64encode(open(f, 'rb').read()).decode()
        index[k] = len(textures)
        textures.append({"path": "", "name": os.path.basename(f), "folder": "block", "namespace": "mio_icif",
            "id": str(len(textures)), "width": im.width, "height": im.height, "uv_width": 16, "uv_height": 16,
            "particle": k == 'particle', "render_mode": "default", "visible": True, "internal": True,
            "saved": True, "uuid": str(uuid.uuid4()), "source": "data:image/png;base64," + data})
    elements = []
    for i, e in enumerate(els):
        faces = {}
        for fn, fd in e.get('faces', {}).items():
            t = fd.get('texture', '#all').lstrip('#')
            face = {"uv": fd.get('uv', [0, 0, 16, 16]), "texture": index.get(t, 0)}
            if 'rotation' in fd: face['rotation'] = fd['rotation']
            if 'cullface' in fd: face['cullface'] = fd['cullface']
            faces[fn] = face
        el = {"name": e.get('name', f'cube{i}'), "box_uv": False, "rescale": False, "locked": False,
              "from": e['from'], "to": e['to'], "autouv": 0, "color": i % 8, "origin": [8, 8, 8],
              "faces": faces, "type": "cube", "uuid": str(uuid.uuid4())}
        if 'rotation' in e:
            r = e['rotation']; rot = [0, 0, 0]; rot['xyz'.index(r['axis'])] = r['angle']
            el['rotation'] = rot; el['origin'] = r['origin']
        elements.append(el)
    bb = {"meta": {"format_version": "4.5", "model_format": "java_block", "box_uv": False},
          "name": os.path.basename(rel), "parent": "block/block", "ambientocclusion": True, "front_gui_light": False,
          "visible_box": [1, 1, 0], "resolution": {"width": 16, "height": 16},
          "elements": elements, "outliner": [e['uuid'] for e in elements], "textures": textures}
    out = os.path.join(OUT, rel + '.bbmodel'); os.makedirs(os.path.dirname(out), exist_ok=True)
    json.dump(bb, open(out, 'w')); n += 1
print('exported', n)
