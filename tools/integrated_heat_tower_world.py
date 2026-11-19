"""Build in a current-world copy; install only the affected region and controls."""
from pathlib import Path
import sys, json, hashlib, shutil, zipfile, datetime, msvcrt, io, zlib
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'build/heat-complex/python'))
import nbtlib
WORK=ROOT/'build/integrated-heat-tower'
GAME=WORK/'game'
WORLD=ROOT/'run/saves/新架构'
PLAN=json.loads((ROOT/'docs/design/integrated_16_core_heat_tower_layout.json').read_text('utf-8'))
ORIGIN=(4219,84,1179)
def absolute(p):return tuple(a+b for a,b in zip(ORIGIN,p))
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def remove_tree(p,root):
    p=p.resolve();root=root.resolve()
    if p==root or not p.is_relative_to(root):raise RuntimeError(f'Unexpected removal path: {p}')
    shutil.rmtree(p)
def rotate(x,y,z,n):
    for _ in range(n):x,z=-z,x
    return x,y,z
def controlpack(dst):
    def put(p,t):
        f=dst/p;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(t,encoding='utf-8')
    put('pack.mcmeta',json.dumps({'pack':{'pack_format':48,'description':'Integrated sixteen-core thermal tower: controls and safety only'}}))
    for tag,fun in [('load','load'),('tick','tick')]:put(f'data/minecraft/tags/function/{tag}.json',json.dumps({'values':['si_heat_complex:'+fun]}))
    def fun(name,lines):put(f'data/si_heat_complex/function/{name}.mcfunction','\n'.join(lines)+'\n')
    master='4219 88 1202'
    fun('load',['scoreboard objectives add si_heat dummy','execute unless score #running si_heat matches 0..1 run scoreboard players set #running si_heat 0','schedule function si_heat_complex:safety 1s replace'])
    fun('tick',[f'execute in minecraft:overworld if loaded {master} if block {master} minecraft:lever[powered=true] unless score #running si_heat matches 1 run function si_heat_complex:start',f'execute in minecraft:overworld if loaded {master} if block {master} minecraft:lever[powered=false] if score #running si_heat matches 1 run function si_heat_complex:stop'])
    start=['schedule clear si_heat_complex:release','execute in minecraft:overworld run forceload add 4192 1152 4255 1215',f'execute in minecraft:overworld run setblock {master} minecraft:lever[face=floor,facing=north,powered=true]','scoreboard players set #running si_heat 1']
    stop=['scoreboard players set #running si_heat 0',f'execute in minecraft:overworld run setblock {master} minecraft:lever[face=floor,facing=north,powered=false]']
    inspect=[]
    for c in PLAN['centres']:
        center=absolute(c['center']);n=c['rotation']//90
        offset=rotate(-3,0,0,n);p=tuple(a+b for a,b in zip(center,offset));facing=['east','south','west','north'][n]
        for lines,power in [(start,'true'),(stop,'false')]:lines.append(f'execute in minecraft:overworld run setblock {" ".join(map(str,p))} minecraft:lever[face=floor,facing={facing},powered={power}]')
        core=' '.join(map(str,center))
        inspect+=['scoreboard players set #hull si_heat 0',f'execute store result score #hull si_heat run data get block {core} HeatStored 1','execute if score #running si_heat matches 1 if score #hull si_heat matches 1000.. run function si_heat_complex:scram',f'execute if score #running si_heat matches 1 unless block {core} mio_icif:generator/block_nuclear_reactor_generator run function si_heat_complex:scram']
    for b in PLAN['blocks']:
        if b['kind']!='turbine':continue
        p=' '.join(map(str,absolute(b['position'])))
        inspect+=[f'execute if score #running si_heat matches 1 unless data block {p} Items.Items[{{Slot:2}}].Item{{id:"mio_icif:normal/item_steam_turbin"}} run function si_heat_complex:scram','scoreboard players set #rotor si_heat 0',f'execute store result score #rotor si_heat run data get block {p} Items.Items[{{Slot:2}}].Item.components."minecraft:damage" 1','execute if score #running si_heat matches 1 if score #rotor si_heat matches 82000.. run function si_heat_complex:scram']
    start+=['tellraw @a {"text":"[集成热核塔] 16 堆启动，整塔区块加载。请定期更换燃料及转子。","color":"aqua"}']
    stop+=['schedule function si_heat_complex:release 10s replace','tellraw @a {"text":"[集成热核塔] 全部停堆，冷却后释放加载。","color":"yellow"}']
    fun('start',start);fun('stop',stop);fun('inspect',inspect)
    fun('safety',['execute if score #running si_heat matches 1 in minecraft:overworld run function si_heat_complex:inspect','schedule function si_heat_complex:safety 1s replace'])
    fun('release',['execute unless score #running si_heat matches 1 in minecraft:overworld run forceload remove 4192 1152 4255 1215'])
    fun('scram',['function si_heat_complex:stop','tellraw @a {"text":"[集成热核塔] 安全停堆：检查堆温及缺失/磨损转子，修复后重新启动。","color":"red"}'])
    fun('visit',['execute in minecraft:overworld run tp @s 4219.5 85 1213.5 180 0'])
    put('README.txt','四层 × 四堆集成能源塔\n传送：/function si_heat_complex:visit\n启动/停止：塔南面 (4219,88,1202) 总开关，或 /function si_heat_complex:start / stop\n四向 EV 输出端：x=4192/4246,z=1179；x=4219,z=1152/1206；全部 y=86。\n燃料与转子正常消耗；数据包只负责开关、加载与安全检查，不补充流体、能量或燃料。\n')
def prepare():
    if (GAME/'world').exists():raise RuntimeError('Sandbox already exists')
    WORK.mkdir(parents=True,exist_ok=True)
    with (WORLD/'session.lock').open('r+b') as lock:
        msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            files={str(p.relative_to(WORLD)).replace('\\','/'):digest(p) for p in WORLD.rglob('*') if p.is_file() and p.name!='session.lock'}
            backup=ROOT/'backup/worlds'/('新架构_before_integrated_tower_'+datetime.datetime.now().strftime('%Y%m%d_%H%M%S')+'.zip');backup.parent.mkdir(parents=True,exist_ok=True)
            with zipfile.ZipFile(backup,'w',zipfile.ZIP_DEFLATED,compresslevel=3) as z:
                for name in files:z.write(WORLD/name,name)
            with zipfile.ZipFile(backup) as z:assert z.testzip() is None
            (WORK/'source-manifest.json').write_text(json.dumps({'backup':str(backup),'files':files},ensure_ascii=False,indent=2),encoding='utf-8')
            shutil.copytree(WORLD,GAME/'world',ignore=shutil.ignore_patterns('session.lock'))
        finally:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)
    for folder in ['mods','config','defaultconfigs']:
        src=ROOT/'run'/folder
        if src.exists():shutil.copytree(src,GAME/folder)
    # BetterTags has a server constructor defect; omit only from this disposable server.
    for p in (GAME/'mods').glob('*'):
        if p.suffix!='.jar' or 'bettertags' in p.name.lower().replace(' ',''):p.unlink()
    lvl=nbtlib.load(GAME/'world/level.dat');d=lvl['Data'];d.pop('Player',None)
    d['GameRules']['doMobSpawning']=nbtlib.String('false');d['GameRules']['doDaylightCycle']=nbtlib.String('false');d['DayTime']=nbtlib.Long(6000)
    d['SpawnX']=nbtlib.Int(4219);d['SpawnY']=nbtlib.Int(85);d['SpawnZ']=nbtlib.Int(1213);lvl.save()
    scoreboard=GAME/'world/data/scoreboard.dat'
    if scoreboard.exists():
        sb=nbtlib.load(scoreboard)
        for s in sb['data'].get('PlayerScores',[]):
            if str(s.get('Objective',''))=='si_heat' and str(s.get('Name',''))=='#running':s['Score']=nbtlib.Int(0)
        sb.save()
    pack=GAME/'world/datapacks/si_heat_complex'
    if pack.exists():remove_tree(pack,GAME)
    controlpack(pack)
    shutil.copy2(ROOT/'docs/design/integrated_16_core_heat_tower_layout.json',GAME/'layout.json')
    (GAME/'eula.txt').write_text('eula=true\n')
    (GAME/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nlevel-name=world\nmax-tick-time=-1\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\nsync-chunk-writes=true\n')
    print('BACKED UP AND PREPARED',backup)

def records(path):
    if not path.exists():return {}
    raw=path.read_bytes();result={}
    for i in range(1024):
        loc=int.from_bytes(raw[4*i:4*i+4],'big');offset=loc>>8
        if not offset:continue
        start=offset*4096;n=int.from_bytes(raw[start:start+4],'big')
        result[i]=(raw[start:start+4+n],raw[4096+4*i:4096+4*i+4])
    return result
def chunk_nbt(record):
    raw=record[0];assert raw[4]==2,'Expected zlib region data'
    return nbtlib.File.parse(io.BytesIO(zlib.decompress(raw[5:])))
def state_at(chunks,p):
    x,y,z=p;c=chunks[(z//16%32)*32+x//16%32]
    s=next(s for s in c['sections'] if int(s['Y'])==y//16);bs=s['block_states'];pal=bs['palette']
    if len(pal)==1:return pal[0]
    bits=max(4,(len(pal)-1).bit_length());per=64//bits;i=(y%16)*256+(z%16)*16+x%16
    idx=((int(bs['data'][i//per])&((1<<64)-1))>>(i%per*bits))&((1<<bits)-1)
    return pal[idx]
def write_region(path,recs):
    header=bytearray(8192);parts=[];offset=2
    for i,(raw,stamp) in sorted(recs.items()):
        count=(len(raw)+4095)//4096;assert count<256
        header[4*i:4*i+4]=((offset<<8)|count).to_bytes(4,'big');header[4096+4*i:4096+4*i+4]=stamp
        parts.append(raw+bytes(count*4096-len(raw)));offset+=count
    path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(header+b''.join(parts))
def affected():
    chunks={(x,z) for x in range(4144//16,4312//16+1) for z in range(1112//16,1272//16+1)}
    chunks.add((4224//16,1310//16))
    return {(z%32)*32+x%32 for x,z in chunks}
def return_test_energy():
    # Conservation transfer: recover energy from the removed temporary receivers,
    # never create more than the four measured saved charges. Needed for the first run.
    assert (GAME/'RELOAD_VERIFIED').exists()
    if (GAME/'TEST_ENERGY_RETURNED').exists():return
    import re
    report=(GAME/'output-commissioning.txt').read_text('utf-8')
    charges=[int(v) for v in re.findall(r'received (\d+) EU',report)];assert len(charges)==4
    amount=sum(charges);region=GAME/'world/region/r.8.2.mca';rs=records(region)
    chunks={i:chunk_nbt(r) for i,r in rs.items()};batteries=[]
    for i,c in chunks.items():
        for e in c.get('block_entities',[]):
            if str(e['id'])=='mio_icif:mfsu':batteries.append((i,e))
    assert len(batteries)==128
    before=sum(int(e['energy']) for i,e in batteries);share,rest=divmod(amount,128)
    touched=set()
    for j,(i,e) in enumerate(batteries):
        value=int(e['energy'])+share+(j<rest);assert value<=40000000
        e['energy']=nbtlib.Long(value);touched.add(i)
    after=sum(int(e['energy']) for i,e in batteries);assert after-before==amount
    for i in touched:
        buf=io.BytesIO();chunks[i].write(buf);packed=b'\x02'+zlib.compress(buf.getvalue())
        rs[i]=(len(packed).to_bytes(4,'big')+packed,rs[i][1])
    write_region(region,rs);(GAME/'TEST_ENERGY_RETURNED').write_text(str(amount))
    (WORK/'test-energy-transfer.json').write_text(json.dumps({'measured_receiver_charges':charges,'returned':amount,'before':before,'after':after},indent=2))
    print('Returned temporary receiver charge to central storage:',amount,'EU')
def validate():
    for marker in ['CONSTRUCTION_COMPLETE','RELOAD_VERIFIED','TEST_ENERGY_RETURNED']:
        assert (GAME/marker).exists(),f'Missing {marker}'
    recs=records(GAME/'world/region/r.8.2.mca');entities={};counts={};chunks={i:chunk_nbt(r) for i,r in recs.items()}
    for c in chunks.values():
        for s in c.get('sections',[]):
            assert all(not str(p['Name']).startswith('si_core_example:') for p in s.get('block_states',{}).get('palette',[]))
        for e in c.get('block_entities',[]):
            entities[(int(e['x']),int(e['y']),int(e['z']))]=e
            k=str(e['id']);counts[k]=counts.get(k,0)+1
    expected={'nuclear_reactor_generator':16,'mfsu':128,'steam_kinetic_generator':96,'condenser':48,'kinetic_generator':96,'reactor_access_hatch':16,'heat_source_fluid':144,'reactor_fluid_port':144,'steam_generator':48,'reactor_chamber':96}
    for suffix,n in expected.items():assert counts.get('mio_icif:'+suffix)==n,(suffix,counts.get('mio_icif:'+suffix),n)
    for core in PLAN['centres']:
        p=absolute(core['center']);e=entities[p]
        assert int(e['IsRunning'])==0 and int(e['HeatStored'])<=32,p
        rod=next(s for s in e['ReactorItems'] if int(s['Slot'])==10)
        assert str(rod['id'])=='mio_icif:reactor/item_reactor_uranium_quad'
        assert int(rod.get('components',{}).get('mio_icif:fuel_damage',0))==0
    registry={'reactor':'generator/block_nuclear_reactor_generator','chamber':'reactor/block_reactor_chamber','vessel':'reactor/block_reactor_vessel','redstone_port':'reactor/block_reactor_redstone_port','access_hatch':'reactor/block_reactor_access_hatch','fluid_port':'reactor/block_reactor_fluid_port','boiler':'producer/block_steam_generator','exchanger':'hugenerator/block_heat_source_fluid','turbine':'producer/block_steam_kinetic_generator','kinetic_generator':'generator/block_kinetic_generator','condenser':'producer/block_condenser','mfsu':'wiring/block_mfsu','generation_cable':'wiring/cable/block_glass_cable','output_cable':'wiring/cable/block_glass_cable'}
    for b in PLAN['blocks']:
        p=absolute(b['position']);kind=b['kind']
        state=state_at(chunks,p)
        if kind in registry:assert str(state['Name'])=='mio_icif:'+registry[kind],(kind,p,str(state['Name']))
        else:assert str(state['Name'])=='minecraft:lever' and str(state['Properties']['powered'])=='false'
        if kind in registry and kind not in ['redstone_port','vessel']:assert p in entities,(kind,p)
        if kind=='mfsu':assert str(state['Properties']['facing'])==b['facing']
        if kind=='boiler':assert int(entities[p]['pressure'])==221 and int(entities[p]['inputMB'])==b['input']
        if kind=='turbine':
            rotor=next(s['Item'] for s in entities[p]['Items']['Items'] if int(s['Slot'])==2)
            assert str(rotor['id'])=='mio_icif:normal/item_steam_turbin'
            assert int(rotor.get('components',{}).get('minecraft:damage',0))==0,p
    report={'origin':ORIGIN,'arrival':[4219.5,85,1213.5],'master':[4219,88,1202],'core_positions':[absolute(c['center']) for c in PLAN['centres']],'counts':counts,'commissioning':(GAME/'commissioning.txt').read_text('utf-8'),'reload':(GAME/'reload-commissioning.txt').read_text('utf-8'),'outputs':(GAME/'output-commissioning.txt').read_text('utf-8')}
    (WORK/'validated-tower.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print('VALIDATED',json.dumps(expected));return report

def install():
    report=validate();manifest=json.loads((WORK/'source-manifest.json').read_text('utf-8'));stage=WORK/'install-stage';stage.mkdir(exist_ok=False)
    # Stage only changed chunks. Every other chunk retains its original compressed bytes.
    changed=affected();transfer=[]
    for folder in ['region','entities','poi']:
        target=WORLD/f'{folder}/r.8.2.mca';src=GAME/f'world/{folder}/r.8.2.mca'
        before=records(target);after=records(src);merged=dict(before)
        for i in changed:
            if i in after:merged[i]=after[i]
            else:merged.pop(i,None)
        if merged==before:continue
        staged=stage/f'{folder}/r.8.2.mca';write_region(staged,merged)
        check=records(staged)
        assert all(check.get(i)==r for i,r in before.items() if i not in changed),'Unrelated chunk changed'
        transfer.append((staged,target))
    # Preserve world metadata, player, other objectives and foreign forced tickets.
    for name in ['scoreboard.dat','chunks.dat']:
        source=WORLD/'data'/name
        if not source.exists():continue
        nb=nbtlib.load(source)
        if name=='scoreboard.dat':
            for s in nb['data'].get('PlayerScores',[]):
                if str(s.get('Objective',''))=='si_heat' and str(s.get('Name',''))=='#running':s['Score']=nbtlib.Int(0)
        else:
            arr=nb['data'].get('Forced',nbtlib.LongArray([]));kept=[]
            for value in arr:
                v=int(value)&((1<<64)-1);x=v&0xffffffff;z=(v>>32)&0xffffffff
                if not (259<=x<=268 and 69<=z<=81):kept.append(int(value))
            nb['data']['Forced']=nbtlib.LongArray(kept)
        dst=stage/'data'/name;dst.parent.mkdir(exist_ok=True);nb.save(dst);transfer.append((dst,source))
    controlpack(stage/'pack')
    pack=WORLD/'datapacks/si_heat_complex'
    with (WORLD/'session.lock').open('r+b') as lock:
        msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            current={str(p.relative_to(WORLD)).replace('\\','/'):digest(p) for p in WORLD.rglob('*') if p.is_file() and p.name!='session.lock'}
            assert current==manifest['files'],'World changed during construction; original save was not modified'
            installed=[]
            try:
                for src,dst in transfer:
                    dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dst);installed.append(str(dst.relative_to(WORLD)).replace('\\','/'))
                if pack.exists():remove_tree(pack,WORLD/'datapacks')
                shutil.copytree(stage/'pack',pack)
                allowed=set(installed)
                for name,value in manifest['files'].items():
                    if name in allowed or name.startswith('datapacks/si_heat_complex/'):continue
                    assert digest(WORLD/name)==value,'Unexpected modified file '+name
                report.update({'installed':installed+['datapacks/si_heat_complex'],'backup':manifest['backup'],'edited_chunks':len(changed),'unchanged_level_metadata':digest(WORLD/'level.dat')==manifest['files']['level.dat']})
                (WORK/'installation-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
            except BaseException:
                with zipfile.ZipFile(manifest['backup']) as z:
                    for name in installed:(WORLD/name).write_bytes(z.read(name))
                    if pack.exists():remove_tree(pack,WORLD/'datapacks')
                    for name in z.namelist():
                        if name.startswith('datapacks/si_heat_complex/'):
                            p=WORLD/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(name))
                raise
            print('INSTALLED',json.dumps(report['installed'],ensure_ascii=False));print('BACKUP',report['backup'])
        finally:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)
def document():
    report=json.loads((WORK/'installation-report.json').read_text('utf-8'))
    backup=Path(report['backup']).relative_to(ROOT).as_posix()
    text=f'''# “新架构”16 核集成热核能源塔

已建入 `run/saves/新架构` 的主世界，重建原厂房内的 16 座分散机组。四层各四堆，围绕中央储能阵列堆叠；原厂区外侧景观、控制楼和冷却塔保留。旧控制台已停用。

## 抵达与启动

塔基中心为 **(4219, 84, 1179)**，南面抵达点 **(4219.5, 85, 1213.5)**。执行：

```mcfunction
/function si_heat_complex:visit
```

总开关在南侧平台 **(4219, 88, 1202)**，右键启动或停堆。也可以执行：

```mcfunction
/function si_heat_complex:start
/function si_heat_complex:stop
```

交付时全部停堆，配有新的普通四联铀棒和蒸汽转子，闭环已装液。普通燃料和转子会消耗，需要定期更换。原 MFSU 的 **54,325,074 EU** 已迁入中央阵列，另有验收运行产生的电量；没有持续补电命令。

启动后只加载包含完整机组的 **4 × 4 = 16 个区块**（X=4192～4255、Z=1152～1215）。停堆后等待 10 秒冷却，再释放此范围的加载。数据包每秒检查堆温和转子：堆温达到 1000、转子缺失或损伤达到 82000，停止全塔。拆改设备前先停堆。

## 集成结构与电力接入

主体框架宽 45 格，含四向输出臂占地 **55 × 55**，从塔基到最高构件约 **75 格高**。四层核心高度为 Y=96、112、128、144；每层核心位于东 (4230,Y,1179)、南 (4219,Y,1190)、西 (4208,Y,1179)、北 (4219,Y,1168)。外围梯道贯通维护平台，各堆舱口可更换燃料。

| 设备 | 数量 |
| --- | ---: |
| 流体反应堆 / 反应仓 / 访问舱 | 16 / 96 / 16 |
| 液体热交换机（每台 10 个热传导器） | 144 |
| 蒸汽发生器 / 蒸汽动能发生机 | 48 / 96 |
| 普通动能发电机 / 冷凝机 | 96 / 48 |
| 中央 MFSU | 128 |

128 台 MFSU 的总容量为 **5,120,000,000 EU（51.2 亿 EU）**。所有发电机连入同一汇能电网，供给 MFSU 背面及冷凝机；MFSU 正面连接独立的输出总线，再竖直引到底座四臂。两套导线没有直接连接。

四个外接端均为 Y=86 的玻璃导线：

| 方向 | 接线端坐标 |
| --- | --- |
| 东 | (4246, 86, 1179) |
| 南 | (4219, 86, 1206) |
| 西 | (4192, 86, 1179) |
| 北 | (4219, 86, 1152) |

输出包来自 MFSU，为 EV 电压。接入低压设备需配套变压器；输出网络应保持与冷凝机所在的汇能网隔离。临时验收负载已移除，四端可按需接线。

## 热力回路

各堆采用一根四联铀棒的 896 HU/t 布局（进料倍率 1×）：

```text
CYAAJAJAA
YSYJCJCJA
AYJCJCJCJ
AJCJCJCJA
JCJCJCJCJ
AJAJAJAJA
```

C=超频散热片，Y=铱中子反射板，S=四联铀棒，J=元件散热片，A=空。

每堆的九台热交换机按 4＋4＋1 供热，三台蒸汽发生器压力 221、输入分别 4/4/1。每组为过热蒸汽涡轮 → 普通蒸汽涡轮 → 冷凝机，凝结水通过定向流体弹出升级返回蒸汽发生器。每堆初装冷却液 10,000 mB，三台蒸汽发生器各装蒸馏水 10,000 mB。冷却与蒸汽保留各堆紧凑闭环，避免集中流体管线吞吐瓶颈。

总开关及安全联锁数据包只操作开关和区块加载，不生成流体、燃料或电能。南侧新箱子及原控制楼保留普通备件。

## 实测验收与保存

建造在当前存档的独立副本中进行，使用本项目真实方块和当前本地模组。两次运行均以中央储能及发电机缓冲的净增长测量 2,000 tick：

```text
{report['commissioning'].splitlines()[0]}
{report['reload'].splitlines()[0]}
```

16 堆实测堆温均为 16、各堆冷却液保持 10,000 mB，蒸汽发生器水量保持稳定。保存后重新加载，验证总开关启动、缺转子全塔停堆、修复后重启及停堆。四端另外接入临时真实 EV 储电负载，运行 1,000 tick 后：

```text
{report['outputs'].strip()}
```

所有燃料和转子在测试结束后换新，临时负载移除，拆建掉落物清理，交付状态全部停堆。实测功率约 **10.58 kEU/t**，实际表现取决于配置、负载与设备维护。

完整备份：`{backup}`，已通过 ZIP CRC 校验。写回仅合并原厂房及旧控制台的 **{report['edited_chunks']} 个区块**到对应区域文件，更新同名控制数据包及其运行状态/旧加载票。未涉及的区块保留原压缩记录，`level.dat`、玩家数据、其他维度和本地模组配置保持不变。

安装报告：`build/integrated-heat-tower/installation-report.json`；图示：`build/integrated-heat-tower/installed-tower-overview.png`（读取实际存档，简化体素绘制）。

建造及验收代码位于 `src/addonExample/java/example/sicore/IntegratedHeatTowerBuilderGameTests.java`；独立运行配置为 `tools/integrated-heat-tower.init.gradle`；备份、校验与区块合并工具为 `tools/integrated_heat_tower_world.py`。建造辅助代码不进入发布模组 Jar。
'''
    (ROOT/'docs/heat_complex_world_build.md').write_text(text,encoding='utf-8')
    print('Updated operating guide')
if __name__=='__main__':{'prepare':prepare,'validate':validate,'install':install,'document':document,'return-test-energy':return_test_energy}[sys.argv[1]]()
