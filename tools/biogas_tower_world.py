"""Current-world backup and conservative chunk merge for the native biogas farm tower."""
import sys,json,shutil,zipfile,datetime,msvcrt,io,gzip
from pathlib import Path
import integrated_heat_tower_world as util
nbtlib=util.nbtlib;ROOT=util.ROOT;WORLD=util.WORLD
WORK=ROOT/'build/biogas-tower';GAME=WORK/'game'
ORIGIN=(4479,84,1311);PACK='si_biogas_tower'
def pack(dst):
    def put(p,s):
        f=dst/p;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(s,encoding='utf-8')
    def fun(n,lines):put(f'data/{PACK}/function/{n}.mcfunction','\n'.join(lines)+'\n')
    put('pack.mcmeta',json.dumps({'pack':{'pack_format':48,'description':'Biogas vertical farms: native production and whole-tower chunk loading'}}))
    for t in ['load','tick']:put(f'data/minecraft/tags/function/{t}.json',json.dumps({'values':[PACK+':'+t]}))
    master='4479 88 1333'
    fun('load',['scoreboard objectives add si_bio dummy','execute unless score #running si_bio matches 0..1 run scoreboard players set #running si_bio 0'])
    fun('tick',[f'execute in minecraft:overworld if loaded {master} if block {master} minecraft:lever[powered=true] unless score #running si_bio matches 1 run function {PACK}:start',f'execute in minecraft:overworld if loaded {master} if block {master} minecraft:lever[powered=false] if score #running si_bio matches 1 run function {PACK}:standby'])
    fun('start',['execute in minecraft:overworld run forceload add 4448 1280 4511 1343',f'execute in minecraft:overworld run setblock {master} minecraft:lever[face=floor,facing=north,powered=true]','scoreboard players set #running si_bio 1','tellraw @a {"text":"[沼电农场塔] 整塔常驻运行，农场与沼气循环同时加载。","color":"green"}'])
    fun('standby',['scoreboard players set #running si_bio 0',f'execute in minecraft:overworld run setblock {master} minecraft:lever[face=floor,facing=north,powered=false]','execute in minecraft:overworld run forceload remove 4448 1280 4511 1343','tellraw @a {"text":"[沼电农场塔] 随区块休眠模式；附近区块加载时仍自然运行。","color":"yellow"}'])
    fun('stop',[f'function {PACK}:standby'])
    fun('visit',['execute in minecraft:overworld run tp @s 4483.5 85 1340.5 180 0'])
    put('README.txt','八层集成沼电塔 + 32 套种植架自动农场\n传送：/function si_biogas_tower:visit\n整塔常驻运行：南侧 (4479,88,1333) 加载开关，或 /function si_biogas_tower:start\n随区块休眠：关闭开关，或 /function si_biogas_tower:standby\n开关管理加载，不模拟关机。作物、机器、配方、热量和电力均由本项目原生机制运行，无持续填液/补电/强制成熟命令。\n四向 EV 输出端：(4506,86,1311)、(4479,86,1338)、(4452,86,1311)、(4479,86,1284)。\n')
def prepare():
    if (GAME/'world').exists():raise RuntimeError('Existing sandbox; do not overwrite')
    WORK.mkdir(parents=True,exist_ok=True)
    with (WORLD/'session.lock').open('r+b') as lock:
        msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            files={p.relative_to(WORLD).as_posix():util.digest(p) for p in WORLD.rglob('*') if p.is_file() and p.name!='session.lock'}
            backup=ROOT/'backup/worlds'/('新架构_before_biogas_tower_'+datetime.datetime.now().strftime('%Y%m%d_%H%M%S')+'.zip')
            with zipfile.ZipFile(backup,'w',zipfile.ZIP_DEFLATED,compresslevel=3) as z:
                for n in files:z.write(WORLD/n,n)
            with zipfile.ZipFile(backup) as z:assert z.testzip() is None
            (WORK/'source-manifest.json').write_text(json.dumps({'backup':str(backup),'files':files},ensure_ascii=False,indent=2),encoding='utf-8')
            shutil.copytree(WORLD,GAME/'world',ignore=shutil.ignore_patterns('session.lock'))
        finally:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)
    for folder in ['mods','config','defaultconfigs']:
        src=ROOT/'run'/folder
        if src.exists():shutil.copytree(src,GAME/folder)
    for p in (GAME/'mods').glob('*'):
        if p.suffix!='.jar' or 'bettertags' in p.name.lower().replace(' ',''):p.unlink()
    level=nbtlib.load(GAME/'world/level.dat');d=level['Data'];d.pop('Player',None)
    d['SpawnX']=nbtlib.Int(4479);d['SpawnY']=nbtlib.Int(85);d['SpawnZ']=nbtlib.Int(1338)
    d['GameRules']['doMobSpawning']=nbtlib.String('false');d['GameRules']['doDaylightCycle']=nbtlib.String('false');d['DayTime']=nbtlib.Long(6000)
    d['GameRules']['spawnChunkRadius']=nbtlib.String('0')
    d['DataPacks']['Enabled'].append(nbtlib.String('file/'+PACK));level.save()
    pack(GAME/'world/datapacks'/PACK)
    (GAME/'eula.txt').write_text('eula=true\n')
    (GAME/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nlevel-name=world\nmax-tick-time=-1\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\nsync-chunk-writes=true\n')
    print('Prepared current-world copy; backup:',backup)
def affected():
    # Four by four tower footprint and the one-chunk access bridge to the existing campus.
    coords={(x,z) for x in range(278,282) for z in range(80,84)}|{(277,81),(277,82)}
    return {(z%32)*32+x%32 for x,z in coords}
def validate(source=None):
    if source is None:source=GAME/'world'
    for marker in ['CONSTRUCTION_COMPLETE','RELOAD_VERIFIED']:assert (GAME/marker).exists(),marker
    rs=util.records(source/'region/r.8.2.mca');counts={};bio=[];chunks={}
    for i,r in rs.items():
        c=util.chunk_nbt(r)
        for s in c.get('sections',[]):assert all(not str(p['Name']).startswith('si_core_example:') for p in s.get('block_states',{}).get('palette',[]))
        if i not in affected():continue
        chunks[i]=c
        for e in c.get('block_entities',[]):
            k=str(e['id']);counts[k]=counts.get(k,0)+1;bio.append(e)
    for name,n in {'semifluid_generator':192,'fermenter_elc':32,'fluid_heat_generator':32,'harvest_elc':32,'powder_elc':32,'canner_elc':32,'pump_elc':32,'mfsu':64}.items():
        assert counts.get('mio_icif:'+name)==n,(name,counts.get('mio_icif:'+name),n)
    crops=[e for e in bio if str(e['id'])=='mio_icif:crop'];assert len(crops)>=1760,counts
    assert all(str(e.get('PlantModId',''))=='mio_icif' and str(e.get('PlantId',''))=='reed' for e in crops),'Missing crop or weed'
    for p in [(4506,86,1311),(4479,86,1338),(4452,86,1311),(4479,86,1284)]:
        assert str(util.state_at(chunks,p)['Name'])=='mio_icif:wiring/cable/block_glass_cable',p
    master=util.state_at(chunks,(4479,88,1333))
    assert str(master['Name'])=='minecraft:lever' and str(master['Properties']['powered'])=='false','Delivery is not in standby'
    report={'origin':ORIGIN,'arrival':[4483.5,85,1340.5],'master':[4479,88,1333],'counts':counts,'commissioning':(GAME/'commissioning.txt').read_text('utf-8'),'reload':(GAME/'reload-commissioning.txt').read_text('utf-8'),'outputs':(GAME/'output-commissioning.txt').read_text('utf-8')}
    (WORK/'validation-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8');print('VALIDATED native biogas tower:',json.dumps(counts))
    return report
def install():
    report=validate();manifest=json.loads((WORK/'source-manifest.json').read_text('utf-8'));stage=WORK/'install-stage';stage.mkdir(exist_ok=False)
    changed=affected();transfers=[]
    for folder in ['region','entities','poi']:
        src=GAME/f'world/{folder}/r.8.2.mca';dst=WORLD/f'{folder}/r.8.2.mca';before=util.records(dst);after=util.records(src);merged=dict(before)
        for i in changed:
            if i in after:merged[i]=after[i]
            else:merged.pop(i,None)
        if merged==before:continue
        staged=stage/f'{folder}/r.8.2.mca';util.write_region(staged,merged)
        assert all(util.records(staged).get(i)==r for i,r in before.items() if i not in changed)
        transfers.append((staged,dst))
    lvl=nbtlib.load(WORLD/'level.dat');enabled=lvl['Data']['DataPacks']['Enabled']
    if 'file/'+PACK not in [str(v) for v in enabled]:enabled.append(nbtlib.String('file/'+PACK))
    lvl.save(stage/'level.dat');transfers.append((stage/'level.dat',WORLD/'level.dat'));pack(stage/'pack')
    dest=WORLD/'datapacks'/PACK;assert not dest.exists()
    with (WORLD/'session.lock').open('r+b') as lock:
        msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            current={p.relative_to(WORLD).as_posix():util.digest(p) for p in WORLD.rglob('*') if p.is_file() and p.name!='session.lock'}
            assert current==manifest['files'],'World changed during build; original untouched'
            installed=[]
            try:
                for src,dst in transfers:
                    dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dst);installed.append(dst.relative_to(WORLD).as_posix())
                shutil.copytree(stage/'pack',dest)
                for name,sha in manifest['files'].items():
                    if name not in installed:assert util.digest(WORLD/name)==sha,name
                with zipfile.ZipFile(manifest['backup']) as z:original=nbtlib.File.parse(io.BytesIO(gzip.decompress(z.read('level.dat'))))
                original['Data']['DataPacks']['Enabled'].append(nbtlib.String('file/'+PACK));a,b=io.BytesIO(),io.BytesIO();original.write(a);nbtlib.load(WORLD/'level.dat').write(b)
                assert a.getvalue()==b.getvalue(),'Changed world metadata beyond enabling controls'
                report.update({'installed':installed+[f'datapacks/{PACK}'],'backup':manifest['backup'],'edited_chunks':len(changed)})
                (WORK/'installation-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
            except BaseException:
                with zipfile.ZipFile(manifest['backup']) as z:
                    for n in installed:
                        if n in manifest['files']:(WORLD/n).write_bytes(z.read(n))
                        elif (WORLD/n).exists():(WORLD/n).unlink()
                if dest.exists():util.remove_tree(dest,WORLD/'datapacks')
                raise
            print('INSTALLED',json.dumps(report['installed']))
        finally:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)
if __name__=='__main__':{'prepare':prepare,'validate':validate,'install':install}[sys.argv[1]]()
