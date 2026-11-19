"""Offline construction sandbox and validated installation into the selected save.

Requires nbtlib in build/heat-complex/python; never edits a loaded world.
"""
from pathlib import Path
import sys, json, hashlib, msvcrt, shutil, zlib, io, struct
sys.path.insert(0, str(Path(__file__).resolve().parents[1]/'build/heat-complex/python'))
import nbtlib

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/heat-complex'
WORLD=ROOT/'run/saves/新架构'
GAME=WORK/'construction-game'
CORES=[(4160+40*c,96,1128+40*r) for r in range(4) for c in range(4)]
PACK='si_heat_complex'

def prepare():
    if (GAME/'world/region').exists():
        raise RuntimeError('Construction sandbox already contains chunks; choose a new sandbox, do not overwrite it.')
    (GAME/'world').mkdir(parents=True,exist_ok=True)
    level=nbtlib.load(WORLD/'level.dat');data=level['Data']
    print('Original world seed:', data['WorldGenSettings']['seed'])
    print('Original datapacks:',data['DataPacks'])
    # Only the isolated sandbox omits dimensions from mods absent in the dev server.
    dims=data['WorldGenSettings']['dimensions']
    for key in list(dims):
        if key not in ['minecraft:overworld','minecraft:the_nether','minecraft:the_end']:del dims[key]
    data.pop('Player',None)
    data['DataPacks']['Enabled']=nbtlib.List[nbtlib.String](['vanilla','mod_data'])
    data['DataPacks']['Disabled']=nbtlib.List[nbtlib.String]([])
    data['GameRules']['doMobSpawning']=nbtlib.String('false')
    data['GameRules']['doFireTick']=nbtlib.String('false')
    data['GameRules']['doWeatherCycle']=nbtlib.String('false')
    data['GameRules']['doDaylightCycle']=nbtlib.String('false')
    data['DayTime']=nbtlib.Long(6000)
    data['SpawnX']=nbtlib.Int(4224);data['SpawnY']=nbtlib.Int(85);data['SpawnZ']=nbtlib.Int(1340)
    level.save(GAME/'world/level.dat')
    (GAME/'eula.txt').write_text('eula=true\n',encoding='utf-8')
    (GAME/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nlevel-name=world\nmax-tick-time=-1\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\nsync-chunk-writes=true\n',encoding='utf-8')
    # The selected world stores Terralith's explicit biome climate table.
    # Load its existing local data jar so newly generated terrain uses the same table.
    terrain=list((ROOT/'run/mods').glob('Terralith*.jar*'))
    if terrain:
        (GAME/'mods').mkdir(exist_ok=True)
        shutil.copy2(terrain[0],GAME/'mods'/terrain[0].name.removesuffix('.DISABLED'))

def chunk_records(path):
    raw=path.read_bytes()
    for i in range(1024):
        loc=int.from_bytes(raw[i*4:i*4+4],'big');offset=loc>>8
        if not offset:continue
        n=int.from_bytes(raw[offset*4096:offset*4096+4],'big')
        codec=raw[offset*4096+4]
        if codec!=2:raise RuntimeError(f'Unsupported compression {codec}')
        yield nbtlib.File.parse(io.BytesIO(zlib.decompress(raw[offset*4096+5:offset*4096+4+n])))

def validate():
    if not (GAME/'CONSTRUCTION_COMPLETE').exists():raise RuntimeError('Construction has not completed')
    if not (GAME/'RELOAD_VERIFIED').exists():raise RuntimeError('Native reload and control tests have not passed')
    region=GAME/'world/region/r.8.2.mca'
    entities=[];chunks=0;palette=set()
    for c in chunk_records(region):
        chunks+=1;entities+=list(c.get('block_entities',[]))
        for s in c.get('sections',[]):
            for p in s.get('block_states',{}).get('palette',[]):palette.add(str(p['Name']))
    assert not any(p.startswith('si_core_example:') for p in palette), 'Example blocks must not be exported'
    from collections import Counter
    counts=Counter(str(e['id']) for e in entities)
    print('Generated chunks',chunks); print('Machine IDs:',json.dumps(counts,ensure_ascii=False))
    # Block entities have specific identifiers; locate core positions independently of naming conventions.
    bypos={(int(e['x']),int(e['y']),int(e['z'])):e for e in entities}
    for p in CORES:
        assert p in bypos,f'Missing reactor {p}'
        assert str(bypos[p]['id']).startswith('mio_icif:'),str(bypos[p]['id'])
        e=bypos[p]
        assert int(e['IsRunning'])==0,f'Reactor still running: {p}'
        assert int(e['HeatStored'])<=32,f'Unsafe heat: {p}'
        rods=[s for s in e['ReactorItems'] if int(s['Slot'])==10]
        assert len(rods)==1 and str(rods[0]['id'])=='mio_icif:reactor/item_reactor_uranium_quad',f'Missing fuel: {p}'
        assert 'mio_icif:fuel_damage' not in rods[0].get('components',{}),f'Used delivery fuel: {p}'
    for e in entities:
        if str(e['id'])=='mio_icif:steam_kinetic_generator':
            rotor=next(s['Item'] for s in e['Items']['Items'] if int(s['Slot'])==2)
            assert str(rotor['id'])=='mio_icif:normal/item_steam_turbin'
            assert int(rotor.get('components',{}).get('minecraft:damage',0))==0,'Used delivery rotor'
    expected={'mfsu':128,'steam_kinetic_generator':96,'condenser':48,'kinetic_generator':96,'reactor_access_hatch':16,'heat_source_fluid':144,'reactor_fluid_port':144,'steam_generator':48,'reactor_chamber':96}
    for suffix,n in expected.items():
        actual=sum(v for k,v in counts.items() if k.rsplit('/',1)[-1].replace('mio_icif:','').replace('_entity','').removeprefix('block_')==suffix)
        if actual!=n:raise RuntimeError(f'Unexpected {suffix} count: {actual}; {counts}')
    result={'world':str(WORLD),'region':'r.8.2.mca','chunks':chunks,'block_entities':dict(counts),'reactors':CORES,'arrival':[4224,85,1340],'backup':json.loads((WORK/'original-world-manifest.json').read_text('utf-8'))['backup'],'commissioning':(GAME/'commissioning.txt').read_text('utf-8')}
    (WORK/'station-manifest.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
    return result

def datapack(destination):
    destination.mkdir(parents=True,exist_ok=False)
    def write(path,text):
        p=destination/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8')
    write('pack.mcmeta',json.dumps({'pack':{'pack_format':48,'description':'16-core thermal station: local master control and chunk-safe operation'}},ensure_ascii=False))
    write('data/minecraft/tags/function/load.json',json.dumps({'values':['si_heat_complex:load']}))
    write('data/minecraft/tags/function/tick.json',json.dumps({'values':['si_heat_complex:tick']}))
    write('data/si_heat_complex/function/load.mcfunction','scoreboard objectives add si_heat dummy\nexecute unless score #running si_heat matches 0..1 run scoreboard players set #running si_heat 0\nschedule function si_heat_complex:safety 1s replace\n')
    write('data/si_heat_complex/function/tick.mcfunction',
          'execute in minecraft:overworld if loaded 4224 85 1310 if block 4224 85 1310 minecraft:lever[powered=true] unless score #running si_heat matches 1 run function si_heat_complex:start\n'
          'execute in minecraft:overworld if loaded 4224 85 1310 if block 4224 85 1310 minecraft:lever[powered=false] if score #running si_heat matches 1 run function si_heat_complex:stop\n')
    # All 16 complete loops and storage racks must tick together while enabled.
    start=['execute in minecraft:overworld run forceload add 4144 1112 4303 1311',
           'execute in minecraft:overworld run setblock 4224 85 1310 minecraft:lever[face=floor,facing=north,powered=true]',
           'scoreboard players set #running si_heat 1']
    stop=['scoreboard players set #running si_heat 0',
          'execute in minecraft:overworld run setblock 4224 85 1310 minecraft:lever[face=floor,facing=north,powered=false]']
    for x,y,z in CORES:
        for commands,power in [(start,'true'),(stop,'false')]:
            commands.append(f'execute in minecraft:overworld run setblock {x-3} {y} {z} minecraft:lever[face=floor,facing=east,powered={power}]')
    start.append('tellraw @a {"text":"[热核基地] 16 堆已启动，完整机组区块保持加载。请定期更换转子与燃料。","color":"aqua"}')
    # Use a short cooldown so residual exchanger heat clears before tickets are removed.
    stop+=['schedule function si_heat_complex:release 10s replace','tellraw @a {"text":"[热核基地] 已全部停堆，10 秒后解除基地的强制加载。","color":"yellow"}']
    write('data/si_heat_complex/function/start.mcfunction','\n'.join(start)+'\n')
    write('data/si_heat_complex/function/stop.mcfunction','\n'.join(stop)+'\n')
    write('data/si_heat_complex/function/release.mcfunction','execute unless score #running si_heat matches 1 in minecraft:overworld run forceload remove 4144 1112 4303 1311\n')
    safety=['execute if score #running si_heat matches 1 in minecraft:overworld run function si_heat_complex:inspect',
            'schedule function si_heat_complex:safety 1s replace']
    inspect=[]
    for x,y,z in CORES:
        inspect += ['scoreboard players set #hull si_heat 0',
                    f'execute store result score #hull si_heat run data get block {x} {y} {z} HeatStored 1',
                    'execute if score #running si_heat matches 1 if score #hull si_heat matches 1000.. run function si_heat_complex:scram',
                    f'execute if score #running si_heat matches 1 unless block {x} {y} {z} mio_icif:generator/block_nuclear_reactor_generator run function si_heat_complex:scram']
        boilers=[((x,y+3,z),(0,1,0)),((x,y-3,z),(0,-1,0)),((x+4,y,z),(1,0,0))]
        for (bx,by,bz),(dx,dy,dz) in boilers:
            for distance in [1,2]:
                tx,ty,tz=bx+distance*dx,by+distance*dy,bz+distance*dz
                inspect += [f'execute if score #running si_heat matches 1 unless data block {tx} {ty} {tz} Items.Items[{{Slot:2}}].Item{{id:"mio_icif:normal/item_steam_turbin"}} run function si_heat_complex:scram',
                            'scoreboard players set #rotor si_heat 0',
                            f'execute store result score #rotor si_heat run data get block {tx} {ty} {tz} Items.Items[{{Slot:2}}].Item.components."minecraft:damage" 1',
                            'execute if score #running si_heat matches 1 if score #rotor si_heat matches 82000.. run function si_heat_complex:scram']
    write('data/si_heat_complex/function/safety.mcfunction','\n'.join(safety)+'\n')
    write('data/si_heat_complex/function/inspect.mcfunction','\n'.join(inspect)+'\n')
    write('data/si_heat_complex/function/scram.mcfunction','function si_heat_complex:stop\ntellraw @a {"text":"[热核基地] 安全联锁停堆：检查堆温、蒸汽转子是否缺失或接近报废。检修后再启动。","color":"red"}\n')
    write('data/si_heat_complex/function/visit.mcfunction','execute in minecraft:overworld run tp @s 4224.5 85 1340.5 180 0\n')
    write('README.txt','主世界 16 堆热核基地\n抵达：/function si_heat_complex:visit\n启动：中央控制室总开关，或 /function si_heat_complex:start\n停堆：总开关，或 /function si_heat_complex:stop\n发电来自真实核燃料、蒸汽与动能机；无周期补液、补电、换燃料命令。\n启动时加载完整机组，防止只加载反应堆而停止冷却设备。\nMFSU 的南面为 EV 输出，禁止直接接回 HV 冷凝机供电线。\n冷却塔是建筑景观；实际冷却在各堆的封闭回路中完成。\n')

def install():
    result=validate();manifest=json.loads((WORK/'original-world-manifest.json').read_text('utf-8'))
    with (WORLD/'session.lock').open('r+b') as lock:
        msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            for name,digest in manifest['original_files'].items():
                assert hashlib.sha256((WORLD/name).read_bytes()).hexdigest()==digest,f'World changed since backup: {name}'
            transfers=[]
            for folder in ['region','entities','poi']:
                src=GAME/f'world/{folder}/r.8.2.mca';dst=WORLD/f'{folder}/r.8.2.mca'
                if src.exists():
                    assert not dst.exists(),f'Existing target region: {dst}'
                    transfers.append((src,dst))
            pack=WORLD/f'datapacks/{PACK}'
            assert not pack.exists(),f'Existing control pack: {pack}'
            # Complete staging before changing the selected world.
            staged=WORK/'install-stage';staged.mkdir(exist_ok=True)
            stagedpack=staged/PACK
            if stagedpack.exists():raise RuntimeError('Previous install staging exists')
            datapack(stagedpack)
            lvl=nbtlib.load(WORLD/'level.dat');enabled=lvl['Data']['DataPacks']['Enabled']
            if 'file/'+PACK not in [str(v) for v in enabled]:enabled.append(nbtlib.String('file/'+PACK))
            lvl.save(staged/'level.dat')
            installed=[]
            try:
                for src,dst in transfers:
                    dst.parent.mkdir(exist_ok=True);shutil.copy2(src,dst);installed.append(dst)
                pack.parent.mkdir(exist_ok=True);shutil.copytree(stagedpack,pack);installed.append(pack)
                shutil.copy2(staged/'level.dat',WORLD/'level.dat')
            except BaseException:
                for path in reversed(installed):
                    if path.is_dir():shutil.rmtree(path)
                    else:path.unlink()
                import zipfile
                with zipfile.ZipFile(manifest['backup']) as z:(WORLD/'level.dat').write_bytes(z.read('level.dat'))
                raise
            # Confirm all original chunk/player/config files remain exactly unchanged.
            for name,digest in manifest['original_files'].items():
                if name=='level.dat':continue
                assert hashlib.sha256((WORLD/name).read_bytes()).hexdigest()==digest,f'Unexpected modification: {name}'
            import zipfile
            with zipfile.ZipFile(manifest['backup']) as z:
                import gzip
                original=nbtlib.File.parse(io.BytesIO(gzip.decompress(z.read('level.dat'))))
            original['Data']['DataPacks']['Enabled'].append(nbtlib.String('file/'+PACK))
            actual=nbtlib.load(WORLD/'level.dat')
            a,b=io.BytesIO(),io.BytesIO();original.write(a);actual.write(b)
            assert a.getvalue()==b.getvalue(),'Unexpected level.dat change beyond DataPacks.Enabled'
            result['installed_files']=[str(p.relative_to(WORLD)) for p in installed]+['level.dat (DataPacks.Enabled only)']
            (WORK/'installation-report.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
            print('INSTALLED',json.dumps(result['installed_files'],ensure_ascii=False))
        finally:
            lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)

if __name__=='__main__':
    {'prepare':prepare,'validate':validate,'install':install}[sys.argv[1]]()
