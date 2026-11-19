"""Current-world backup and conservative chunk merge for the native biogas farm tower."""
import sys,json,shutil,zipfile,datetime,msvcrt,io,gzip
from pathlib import Path
import integrated_heat_tower_world as util
nbtlib=util.nbtlib;ROOT=util.ROOT;WORLD=util.WORLD
WORK=ROOT/'build/biogas-steam-tower';GAME=WORK/'game'
ORIGIN=(4479,84,1311);PACK='si_biogas_tower'
LOAD_CHUNKS={(279,81),(279,82),(280,81),(280,82),(281,81),(278,81),(279,83),(279,80)}
def pack(dst):
    def put(p,s):
        f=dst/p;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(s,encoding='utf-8')
    def fun(n,lines):put(f'data/{PACK}/function/{n}.mcfunction','\n'.join(lines)+'\n')
    put('pack.mcmeta',json.dumps({'pack':{'pack_format':48,'description':'32-core biogas superheated-steam tower: native production and chunk loading'}}))
    for t in ['load','tick']:put(f'data/minecraft/tags/function/{t}.json',json.dumps({'values':[PACK+':'+t]}))
    master='4479 88 1327'
    fun('load',['scoreboard objectives add si_bio dummy','execute unless score #running si_bio matches 0..1 run scoreboard players set #running si_bio 0'])
    fun('tick',[f'execute in minecraft:overworld if loaded {master} if block {master} minecraft:lever[powered=true] unless score #running si_bio matches 1 run function {PACK}:start',f'execute in minecraft:overworld if loaded {master} if block {master} minecraft:lever[powered=false] if score #running si_bio matches 1 run function {PACK}:standby'])
    loads=[f'execute in minecraft:overworld run forceload add {x*16} {z*16}' for x,z in sorted(LOAD_CHUNKS)]
    fun('start',['execute in minecraft:overworld run forceload remove 4448 1280 4511 1343']+loads+[f'execute in minecraft:overworld run setblock {master} minecraft:lever[face=floor,facing=north,powered=true]','scoreboard players set #running si_bio 1','tellraw @a {"text":"[二代沼电塔] 32 核常驻运行，加载 4 个主体区块及 4 个输出区块。","color":"green"}'])
    fun('standby',['scoreboard players set #running si_bio 0',f'execute in minecraft:overworld run setblock {master} minecraft:lever[face=floor,facing=north,powered=false]','execute in minecraft:overworld run forceload remove 4448 1280 4511 1343','tellraw @a {"text":"[沼电农场塔] 随区块休眠模式；附近区块加载时仍自然运行。","color":"yellow"}'])
    fun('stop',[f'function {PACK}:standby'])
    fun('visit',['execute in minecraft:overworld run tp @s 4483.5 85 1332.5 180 0'])
    put('README.txt','二代 32 核沼气过热蒸汽塔 + 32 套种植架自动农场\n传送：/function si_biogas_tower:visit\n整塔常驻运行：南侧 (4479,88,1327) 加载开关，或 /function si_biogas_tower:start\n随区块休眠：关闭开关，或 /function si_biogas_tower:standby\n开关管理加载，不模拟关机。作物、机器、配方、热量和电力均由本项目原生机制运行，无持续填液/补电/强制成熟命令。\n四向 EV 输出端：(4496,86,1311)、(4479,86,1328)、(4462,86,1311)、(4479,86,1294)。\n转子正常损耗，入口配有 64 个备用转子；定期更换。主发电为双级蒸汽链路，半流质发电机只供应独立的农场加工电网。\n')
def prepare():
    if (GAME/'world').exists():raise RuntimeError('Existing sandbox; do not overwrite')
    WORK.mkdir(parents=True,exist_ok=True)
    with (WORLD/'session.lock').open('r+b') as lock:
        msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            files={p.relative_to(WORLD).as_posix():util.digest(p) for p in WORLD.rglob('*') if p.is_file() and p.name!='session.lock'}
            backup=ROOT/'backup/worlds'/('新架构_before_biogas_steam_tower_'+datetime.datetime.now().strftime('%Y%m%d_%H%M%S')+'.zip')
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
    if 'file/'+PACK not in [str(x) for x in d['DataPacks']['Enabled']]:d['DataPacks']['Enabled'].append(nbtlib.String('file/'+PACK))
    level.save()
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
    for marker in ['PROBE_VERIFIED','CONSTRUCTION_COMPLETE','RELOAD_VERIFIED']:assert (GAME/marker).exists(),marker
    rs=util.records(source/'region/r.8.2.mca');counts={};bio=[];chunks={}
    for i,r in rs.items():
        c=util.chunk_nbt(r)
        for s in c.get('sections',[]):assert all(not str(p['Name']).startswith('si_core_example:') for p in s.get('block_states',{}).get('palette',[]))
        if i not in affected():continue
        chunks[i]=c
        for e in c.get('block_entities',[]):
            k=str(e['id']);counts[k]=counts.get(k,0)+1;bio.append(e)
    for name,n in {'semifluid_generator':64,'fermenter_elc':32,'fluid_heat_generator':160,'harvest_elc':32,'powder_elc':32,'canner_elc':32,'pump_elc':32,'mfsu':64,'steam_generator':32,'steam_kinetic_generator':64,'kinetic_generator':64,'condenser':32,'fluid_distributor_elc':32}.items():
        assert counts.get('mio_icif:'+name)==n,(name,counts.get('mio_icif:'+name),n)
    crops=[e for e in bio if str(e['id'])=='mio_icif:crop'];assert len(crops)>=1760,counts
    assert all(str(e.get('PlantModId',''))=='mio_icif' and str(e.get('PlantId',''))=='reed' for e in crops),'Missing crop or weed'
    for e in bio:
        if str(e['id'])=='mio_icif:steam_generator':
            assert int(e['pressure'])==221 and int(e['inputMB'])==1 and int(e['calcification'])==0
            assert int(e['waterTank']['Fluid']['amount'])>=9000
            assert str(e['waterTank']['Fluid']['id'])=='mio_icif:distilledwater'
        if str(e['id'])=='mio_icif:steam_kinetic_generator':
            rotor=next(row['Item'] for row in e['Items']['Items'] if int(row['Slot'])==2)
            assert str(rotor['id'])=='mio_icif:normal/item_steam_turbin'
            assert int(rotor.get('components',{}).get('minecraft:damage',0))==0,'Delivery rotor is worn'
    bypos={(int(e['x']),int(e['y']),int(e['z'])):e for e in bio}
    layout=json.loads((ROOT/'docs/design/32_core_biogas_steam_tower_layout.json').read_text('utf-8'))
    for core in layout['cores']:
        cx,cy,cz=core['farm_center']
        returns=[(x,0) for x in range(1,4)]+[(3,1)]+[(x,2) for x in range(-1,4)]+[(-1,1),(-1,0)]
        for dx,dz in returns:
            e=bypos[(cx+dx,cy-4,cz+dz)];assert str(e['id'])=='mio_icif:fluid_pipe'
            if 'buffer' in e:assert str(e['buffer']['id'])=='mio_icif:distilledwater','Contaminated return line'
    for p in [(4496,86,1311),(4479,86,1328),(4462,86,1311),(4479,86,1294)]:
        assert str(util.state_at(chunks,p)['Name'])=='mio_icif:wiring/cable/block_glass_cable',p
    master=util.state_at(chunks,(4479,88,1327))
    assert str(master['Name'])=='minecraft:lever' and str(master['Properties']['powered'])=='false','Delivery is not in standby'
    report={'probe':(GAME/'probe-commissioning.txt').read_text('utf-8'),'body_dimensions':[31,31,85],'loaded_chunks':sorted(LOAD_CHUNKS),'origin':ORIGIN,'arrival':[4483.5,85,1332.5],'master':[4479,88,1327],'counts':counts,'commissioning':(GAME/'commissioning.txt').read_text('utf-8'),'reload':(GAME/'reload-commissioning.txt').read_text('utf-8'),'outputs':(GAME/'output-commissioning.txt').read_text('utf-8')}
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
    assert util.digest(WORLD/'level.dat')==manifest['files']['level.dat']
    assert 'file/'+PACK in [str(v) for v in enabled]
    for name in ['scoreboard.dat','chunks.dat']:
        path=WORLD/'data'/name
        if not path.exists():continue
        nb=nbtlib.load(path)
        if name=='scoreboard.dat':
            for row in nb['data'].get('PlayerScores',[]):
                if str(row.get('Objective',''))=='si_bio' and str(row.get('Name',''))=='#running':row['Score']=nbtlib.Int(0)
        else:
            kept=[]
            for value in nb['data'].get('Forced',[]):
                unsigned=int(value)&((1<<64)-1);x=unsigned&0xffffffff;z=(unsigned>>32)&0xffffffff
                if not(278<=x<=281 and 80<=z<=83):kept.append(int(value))
            nb['data']['Forced']=nbtlib.LongArray(kept)
        out=stage/'data'/name;out.parent.mkdir(exist_ok=True);nb.save(out);transfers.append((out,path))
    pack(stage/'pack')
    dest=WORLD/'datapacks'/PACK;assert dest.exists()
    with (WORLD/'session.lock').open('r+b') as lock:
        msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            current={p.relative_to(WORLD).as_posix():util.digest(p) for p in WORLD.rglob('*') if p.is_file() and p.name!='session.lock'}
            assert current==manifest['files'],'World changed during build; original untouched'
            installed=[]
            try:
                for src,dst in transfers:
                    dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dst);installed.append(dst.relative_to(WORLD).as_posix())
                util.remove_tree(dest,WORLD/'datapacks');shutil.copytree(stage/'pack',dest)
                for name,sha in manifest['files'].items():
                    if name not in installed and not name.startswith('datapacks/'+PACK+'/'):assert util.digest(WORLD/name)==sha,name
                assert util.digest(WORLD/'level.dat')==manifest['files']['level.dat'],'World metadata changed'
                report.update({'installed':installed+[f'datapacks/{PACK}'],'backup':manifest['backup'],'edited_chunks':len(changed)})
                (WORK/'installation-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
            except BaseException:
                with zipfile.ZipFile(manifest['backup']) as z:
                    for n in installed:
                        if n in manifest['files']:(WORLD/n).write_bytes(z.read(n))
                        elif (WORLD/n).exists():(WORLD/n).unlink()
                if dest.exists():util.remove_tree(dest,WORLD/'datapacks')
                with zipfile.ZipFile(manifest['backup']) as z:
                    for name in z.namelist():
                        if name.startswith('datapacks/'+PACK+'/'):
                            p=WORLD/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(name))
                raise
            print('INSTALLED',json.dumps(report['installed']))
        finally:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)
if __name__=='__main__':{'prepare':prepare,'validate':validate,'install':install}[sys.argv[1]]()
