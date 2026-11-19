#!/usr/bin/env python3
"""0.1.11.89: the fusion reactor (tokamak) sample world.

  python tools/fusion_world.py prepare            # a clean server folder: superflat, creative, peaceful, no structures
  ./gradlew --offline -I tools/fusion-world.init.gradle runFusionWorldBuild    # FusionTokamakWorldBuilder builds and stops
  python tools/fusion_world.py install [--replace] # copies the world to run/saves/<NAME> as a creative world with commands
  python tools/fusion_world.py preview            # isometric picture of the full and the cutaway model (real textures)
  python tools/fusion_world.py prepare --verify   # 0.1.11.93: a throwaway folder for runFusionWorldVerify (it pulls the lever
                                                  # and logs the controller for 3,000 ticks to FUSION_VERIFY.json)

The layout is src/main/resources/data/mio_icif/multiblock/fusion_tokamak.json (tools/fusion_layout_11589.py): prepare copies it into the server folder,
the builder places it and the preview draws it.
"""
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'build/heat-complex/python'))   # nbtlib, as the other world tools
WORK = ROOT / 'build/fusion-world'
GAME = WORK / 'game'
NAME = '聚变堆结构展示'
DEST = ROOT / 'run/saves' / NAME
TEX = ROOT / 'src/main/resources/assets/mio_icif/textures/block/fusion'
LAYOUT = ROOT / 'src/main/resources/data/mio_icif/multiblock/fusion_tokamak.json'


def prepare(game=GAME):
    if game.exists():
        shutil.rmtree(game)   # the build's own sandbox, regenerated every time
    game.mkdir(parents=True)
    (game / 'eula.txt').write_text('eula=true\n', encoding='utf-8')
    shutil.copyfile(LAYOUT, game / 'fusion-layout.json')
    (game / 'server.properties').write_text('\n'.join([
        'level-name=world', 'level-type=minecraft\\:flat', 'generate-structures=false', 'gamemode=creative', 'difficulty=peaceful',
        'spawn-monsters=false', 'spawn-animals=false', 'spawn-npcs=false', 'online-mode=false', 'server-ip=127.0.0.1', 'server-port=0',
        'view-distance=4', 'simulation-distance=4', 'spawn-protection=0', 'max-tick-time=-1', 'sync-chunk-writes=true', '']), encoding='utf-8')
    print('prepared', game)


def backup_existing():
    """Zips the installed world into backup/worlds (it may have been played in) after making sure the game has not got it open."""
    import datetime
    import msvcrt
    import zipfile
    lock = DEST / 'session.lock'
    if lock.exists():
        with lock.open('r+b') as f:
            try:
                msvcrt.locking(f.fileno(), msvcrt.LK_NBLCK, 1)
                msvcrt.locking(f.fileno(), msvcrt.LK_UNLCK, 1)
            except OSError:
                sys.exit('%s is open in the game; leave the world first' % DEST)
    out = ROOT / 'backup/worlds' / ('%s_%s.zip' % (NAME, datetime.datetime.now().strftime('%Y%m%d_%H%M%S')))
    out.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED, compresslevel=3) as z:
        for p in DEST.rglob('*'):
            if p.is_file() and p.name != 'session.lock':
                z.write(p, p.relative_to(DEST).as_posix())
    with zipfile.ZipFile(out) as z:
        assert z.testzip() is None
    print('backed up the old world to', out)


def install(replace):
    import nbtlib
    marker = GAME / 'FUSION_WORLD_BUILT'
    if not marker.exists():
        sys.exit('not built: run the gradle run runFusionWorldBuild first')
    if DEST.exists():
        if not replace:
            sys.exit('%s exists; pass --replace to overwrite it' % DEST)
        backup_existing()
        shutil.rmtree(DEST)
    shutil.copytree(GAME / 'world', DEST, ignore=shutil.ignore_patterns('session.lock'))
    level = nbtlib.load(DEST / 'level.dat')
    d = level['Data']
    d['LevelName'] = nbtlib.String(NAME)
    d['GameType'] = nbtlib.Int(1)          # creative
    d['allowCommands'] = nbtlib.Byte(1)
    d['Difficulty'] = nbtlib.Byte(0)       # peaceful
    d['hardcore'] = nbtlib.Byte(0)
    d.pop('Player', None)                  # a new player starts at the world spawn, in front of both models
    level.save()
    print('installed', DEST, '|', marker.read_text(encoding='utf-8').strip())


# ------------------------------------------------------------------ preview

FACES = {   # name -> (top, south, east) textures; a facing block shows its front on the face it looks at
    'central_solenoid': ('central_solenoid_end', 'central_solenoid_side', 'central_solenoid_side'),
    'poloidal_coil': ('poloidal_coil_end', 'poloidal_coil_side', 'poloidal_coil_side'),
}
FRONTS = {'controller': 'controller_front'}
ORIENTABLE = {'controller', 'nbi_port', 'ecrh_port', 'fuel_port', 'exhaust_port', 'cryo_port', 'power_port', 'mitigation_port'}


def faces(name, facing):
    if name in FACES:
        return FACES[name]
    if name == 'toroidal_coil':   # 0.1.11.94: its narrow edge faces along the coil's plane, its copper turns to the sides
        edge, side = 'toroidal_coil_edge', 'toroidal_coil'
        return ('toroidal_coil_end', edge if facing in ('north', 'south') else side, edge if facing in ('east', 'west') else side)
    if name in ORIENTABLE:
        front = FRONTS.get(name, name)
        return ('cryostat', front if facing == 'south' else 'cryostat', front if facing == 'east' else 'cryostat')
    return (name, name, name)


def preview():
    from PIL import Image, ImageDraw, ImageEnhance
    s = 9
    cache = {}

    def tex(name):
        if name not in cache:
            path = TEX / (name + '.png')
            if name == 'glass':
                cache[name] = Image.new('RGBA', (16, 16), (214, 64, 214, 150))
            elif path.exists():
                cache[name] = Image.open(path).convert('RGBA')
            else:   # a texture not drawn yet
                cache[name] = Image.new('RGBA', (16, 16), (255, 0, 255, 255))
        return cache[name]

    def face_images(top, south, east):
        key = (top, south, east)
        if key not in cache:
            t = tex(top).transform((2 * s, s), Image.AFFINE, (8 / s, 16 / s, -8, -8 / s, 16 / s, 8), Image.NEAREST, fillcolor=(0, 0, 0, 0))
            l = tex(south).transform((s, 2 * s), Image.AFFINE, (16 / s, 0, 0, -8 / s, 16 / s, -8), Image.NEAREST, fillcolor=(0, 0, 0, 0))
            r = tex(east).transform((s, 2 * s), Image.AFFINE, (16 / s, 0, 0, 8 / s, 16 / s, -16), Image.NEAREST, fillcolor=(0, 0, 0, 0))
            l = ImageEnhance.Brightness(l).enhance(0.78)
            r = ImageEnhance.Brightness(r).enhance(0.6)
            cube = Image.new('RGBA', (2 * s, 2 * s), (0, 0, 0, 0))
            cube.alpha_composite(l, (0, 0))
            cube.alpha_composite(r, (s, 0))
            cube.alpha_composite(t, (0, 0))
            cache[key] = cube
        return cache[key]

    layout = json.loads(LAYOUT.read_text(encoding='utf-8'))
    full = [tuple(b) for b in layout['blocks']]
    cut = [b for b in full if not (b[0] >= 1 and b[2] >= 1)]
    cut += [(x, y, z, 'glass', '') for x, y, z in layout['plasma'] if not (x >= 1 and z >= 1)]   # as in the world

    def render(blocks, title):
        w, h = 54 * s, 44 * s
        img = Image.new('RGBA', (w, h), (58, 62, 70, 255))
        ox, oy = w // 2 - s, 20 * s
        for x, y, z, n, f in sorted(blocks, key=lambda b: (b[0] + b[2], b[1])):
            top, south, east = ('glass',) * 3 if n == 'glass' else faces(n, f)
            img.alpha_composite(face_images(top, south, east), (ox + (x - z) * s, oy + (x + z) * s // 2 - y * s))
        ImageDraw.Draw(img).text((8, 8), title, fill=(240, 240, 240))
        return img

    size = 'x'.join(str(v) for v in layout['size'])
    a, b = render(full, 'full ' + size), render(cut, 'cutaway (SE quarter removed, magenta = plasma ring)')
    sheet = Image.new('RGBA', (a.width + b.width, a.height), (58, 62, 70, 255))
    sheet.alpha_composite(a, (0, 0))
    sheet.alpha_composite(b, (a.width, 0))
    out = WORK / 'fusion-preview.png'
    sheet.save(out)
    print('preview', out)


if __name__ == '__main__':
    cmd = sys.argv[1] if len(sys.argv) > 1 else ''
    if cmd == 'prepare':
        prepare(WORK / 'verify' if '--verify' in sys.argv else GAME)
    elif cmd == 'install':
        install('--replace' in sys.argv)
    elif cmd == 'preview':
        preview()
    else:
        sys.exit(__doc__)
