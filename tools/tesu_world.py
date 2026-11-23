#!/usr/bin/env python3
"""0.1.11.157: the TESU (太级储电罐) structure sample world.

  python tools/tesu_world.py prepare            # a clean server folder: superflat, creative, peaceful, no structures
  ./gradlew --offline -I tools/tesu-world.init.gradle runTesuWorldBuild    # TesuWorldBuilder builds and stops
  python tools/tesu_world.py install [--replace] # copies the world to run/saves/<NAME> as a creative world with commands

0.1.11.177: adding to the installed world. It was played with many other mods, and a server without them must not load it
(saving its chunks would drop their blocks), so the additions go in as a data pack that runs once in the player's own game:
  python tools/tesu_world.py prepare            # a fresh server folder (as for a build)
  ./gradlew --offline -I tools/tesu-world.init.gradle runTesuWorldPack   # TesuWorldBuilder writes showcase-pack and stops
  python tools/tesu_world.py install-pack       # zips the installed world, copies the pack into its datapacks folder

The layout is src/main/resources/data/mio_icif/multiblock/tesu.json (tools/tesu_layout_111157.py).
"""
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'build/heat-complex/python'))   # nbtlib, as the other world tools
WORK = ROOT / 'build/tesu-world'
GAME = WORK / 'game'
NAME = 'TESU结构展示'
DEST = ROOT / 'run/saves' / NAME
LAYOUT = ROOT / 'src/main/resources/data/mio_icif/multiblock/tesu.json'


def prepare():
    if GAME.exists():
        shutil.rmtree(GAME)   # the build's own sandbox, regenerated every time
    GAME.mkdir(parents=True)
    (GAME / 'eula.txt').write_text('eula=true\n', encoding='utf-8')
    shutil.copyfile(LAYOUT, GAME / 'tesu-layout.json')
    (GAME / 'server.properties').write_text('\n'.join([
        'level-name=world', 'level-type=minecraft\\:flat', 'generate-structures=false', 'gamemode=creative', 'difficulty=peaceful',
        'spawn-monsters=false', 'spawn-animals=false', 'spawn-npcs=false', 'online-mode=false', 'server-ip=127.0.0.1', 'server-port=0',
        'view-distance=4', 'simulation-distance=4', 'spawn-protection=0', 'max-tick-time=-1', 'sync-chunk-writes=true', '']), encoding='utf-8')
    print('prepared', GAME)


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
    print('backed up the old world to', out)


def install_pack():
    """0.1.11.177: copies the showcase data pack (TesuWorldBuilder.showcasePack) into the installed world's datapacks folder;
    the world is zipped first. The pack runs once on the world's next load, with all the player's mods present."""
    pack = GAME / 'showcase-pack'
    if not (GAME / 'TESU_SHOWCASE_PACK').exists() or not pack.exists():
        sys.exit('no pack: run the gradle run runTesuWorldPack first')
    if not DEST.exists():
        sys.exit('no installed world at %s' % DEST)
    backup_existing()
    target = DEST / 'datapacks' / 'si_tesu_showcase'
    if target.exists():
        shutil.rmtree(target)
    shutil.copytree(pack, target)
    print('installed the pack into', target, '|', (GAME / 'TESU_SHOWCASE_PACK').read_text(encoding='utf-8').strip())


def install(replace):
    import nbtlib
    marker = GAME / 'TESU_WORLD_BUILT'
    if not marker.exists():
        sys.exit('not built: run the gradle run runTesuWorldBuild first')
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
    d.pop('Player', None)                  # a new player starts at the world spawn, facing both models
    level.save()
    print('installed', DEST, '|', marker.read_text(encoding='utf-8').strip())


if __name__ == '__main__':
    cmd = sys.argv[1] if len(sys.argv) > 1 else ''
    if cmd == 'prepare':
        prepare()
    elif cmd == 'install':
        install('--replace' in sys.argv)
    elif cmd == 'install-pack':
        install_pack()
    else:
        sys.exit(__doc__)
