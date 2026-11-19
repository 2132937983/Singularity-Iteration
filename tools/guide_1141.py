"""0.1.10.41: machine input and output rates in the "Power data" tables of 25 guide pages (en / zh / ja): fuel, steam, water and
heat use, HU, KU and EU production. The Kinetic, Turbo Kinetic, Twin Turbo Kinetic and Water Mill pages showed their maximum EU
per tick to cables as "Output"; they now show what the machine makes. The numbers come from the block entities (constants and
the semifluid fuel table); Round1141FixGameTests checks the English pages against them. Idempotent; --check reports pages that
miss the new text.
Usage: guide_1141.py <tree> [--check]"""
import os
import sys

TREE = sys.argv[1]
CHECK = '--check' in sys.argv[2:]
BASE = os.path.join(TREE, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual')
LANGS = ('', '_zh_cn/', '_ja_jp/')
HEADING = {'': '## Power data', '_zh_cn/': '## 电力数据', '_ja_jp/': '## 電力データ'}
TABLE_HEAD = {'': '| Item | Value |\n|---|---|', '_zh_cn/': '| 项目 | 数值 |\n|---|---|', '_ja_jp/': '| 項目 | 値 |\n|---|---|'}
NO_EU = {'': 'This block uses no EU.', '_zh_cn/': '该方块不使用EU。', '_ja_jp/': 'このブロックはEUを使わない。'}
MEASURED = {'': 'The game measured these values from the block entity of this version.',
            '_zh_cn/': '这些数值由本版本的方块实体实测得出。', '_ja_jp/': 'この値は本バージョンのブロックエンティティから計測した。'}
# The config multiplier changes the EU of every EU generator; fuel generators burn their fuel at the same speed.
EU_NOTE = ('With a generator output multiplier other than 1.0 (config GeneratorOutput), the EU values change.',
           '发电倍率（配置 GeneratorOutput）不为 1.0 时，EU 数值会改变。',
           '発電倍率（設定 GeneratorOutput）が 1.0 以外のとき、EU の値が変わる。')
FUEL_NOTE = (EU_NOTE[0] + ' The fuel use stays the same.', EU_NOTE[1] + '燃料消耗不变。', EU_NOTE[2] + '燃料消費は変わらない。')

BIOGAS, BIOMASS, CRUDE, DIESEL = ('Biogas', '沼气', 'バイオガス'), ('Biomass', '生物质', 'バイオマス'), ('Crude Oil', '原油', '原油'), \
    ('Diesel Oil', '柴油', 'ディーゼル燃料')


def fuel(name, value):
    """A fuel row: the value (numbers and units) is the same in every language."""
    return (f'Fuel: {name[0]}', value, f'燃料：{name[1]}', value, f'燃料：{name[2]}', value)


def same(en, zh, ja, value):
    return (en, value, zh, value, ja, value)


# page: mode, rows (en label, en value, zh label, zh value, ja label, ja value), note, replaced rows {lang index: (old, new)}
# modes: "append" adds rows at the end of the table; "no_eu" puts a table above "This block uses no EU."; "generator" replaces
# that sentence with a table (the Stirling generators make EU).
PAGES = {
    'generator_block_semifluid_generator': ('append', [
        fuel(BIOGAS, '0.5 mB/t → 16 EU/t (32 EU/mB)'),
        fuel(BIOMASS, '1 mB/t → 8 EU/t (8 EU/mB)'),
        fuel(CRUDE, '0.5 mB/t → 8 EU/t (16 EU/mB)'),
        fuel(DIESEL, '0.25 mB/t → 32 EU/t (128 EU/mB)'),
    ], FUEL_NOTE, {0: ('| Output | 16 EU/t |', '| Output | 8 to 32 EU/t, by fuel |'),
                   1: ('| 输出 | 16 EU/t |', '| 输出 | 8 至 32 EU/t，按燃料 |'),
                   2: ('| 出力 | 16 EU/t |', '| 出力 | 8〜32 EU/t（燃料による） |')}),
    'generator_block_advanced_semifluid_generator': ('append', [
        fuel(BIOGAS, '1.5 mB/t → 36 EU/t (24 EU/mB)'),
    ], FUEL_NOTE, {}),
    'generator_block_geo_generator': ('append', [
        fuel(('Lava', '熔岩', '溶岩'), '2 mB/t → 20 EU/t (10 EU/mB)'),
        ('Adjacent lava blocks', '+1 EU/t each, up to 6', '相邻熔岩方块', '每个 +1 EU/t，最多 6 个', '隣接する溶岩ブロック', '1 個につき +1 EU/t、最大 6 個'),
    ], FUEL_NOTE, {}),
    'generator_block_diesel_generator': ('append', [
        fuel(DIESEL, '16 mB/t → 120 EU/t (7.5 EU/mB)'),
    ], FUEL_NOTE, {}),
    'generator_block_water_generator': ('append', [
        same('Water in the tank', '水箱中的水', 'タンクの水', '1 mB/t → 4 EU/t (4 EU/mB)'),
        same('Water at the front face', '正面的水', '正面の水', '+2 EU/t'),
    ], FUEL_NOTE, {0: ('| Output | 32 EU/t |', '| Output | up to 6 EU/t |'),
                   1: ('| 输出 | 32 EU/t |', '| 输出 | 最多 6 EU/t |'),
                   2: ('| 出力 | 32 EU/t |', '| 出力 | 最大 6 EU/t |')}),
    'generator_block_steam_turbine': ('append', [
        ('Steam use', '128 mB/t → 64 EU/t (2 mB per EU)', '蒸汽消耗', '128 mB/t → 64 EU/t (每 EU 2 mB)',
         '蒸気消費', '128 mB/t → 64 EU/t (1 EU あたり 2 mB)'),
    ], None, {}),
    'hugenerator_block_fluid_heat_generator': ('no_eu', [
        fuel(BIOGAS, '0.5 mB/t → 32 HU/t (64 HU/mB)'),
        fuel(BIOMASS, '1 mB/t → 16 HU/t (16 HU/mB)'),
        fuel(CRUDE, '0.5 mB/t → 32 HU/t (64 HU/mB)'),
        fuel(DIESEL, '0.25 mB/t → 768 HU/t (3,072 HU/mB)'),
        same('Fuel tank', '燃料箱', '燃料タンク', '10,000 mB'),
    ], None, {}),
    'hugenerator_block_heat_source_fluid': ('no_eu', [
        ('Lava → Pahoehoe Lava', '1 mB → 1 mB, 20 HU per mB', '熔岩 → 熔岩岩浆', '1 mB → 1 mB，每 mB 20 HU',
         '溶岩 → パホイホイ溶岩', '1 mB → 1 mB、1 mB あたり 20 HU'),
        ('Hot Coolant → Coolant', '1 mB → 1 mB, 20 HU per mB', '热冷却液 → 冷却液', '1 mB → 1 mB，每 mB 20 HU',
         '高温冷却液 → 冷却液', '1 mB → 1 mB、1 mB あたり 20 HU'),
        ('Heat output', '10 HU/t per Heat Conductor, up to 100 HU/t', '热量输出', '每个热传导器 10 HU/t，最多 100 HU/t',
         '熱出力', '熱伝導体 1 個につき 10 HU/t、最大 100 HU/t'),
        ('Fluid use', '0.5 mB/t per Heat Conductor, up to 5 mB/t', '流体消耗', '每个热传导器 0.5 mB/t，最多 5 mB/t',
         '流体消費', '熱伝導体 1 個につき 0.5 mB/t、最大 5 mB/t'),
    ], None, {}),
    'hugenerator_block_solid_heat_generator': ('no_eu', [
        ('Heat output', '20 HU/t while the fuel burns', '热量输出', '燃烧时 20 HU/t', '熱出力', '燃焼中 20 HU/t'),
        ('Burn time', 'furnace burn time ÷ 4 (Coal: 400 t, 8,000 HU)', '燃烧时间', '熔炉燃烧时间 ÷ 4 (煤炭：400 t，8,000 HU)',
         '燃焼時間', 'かまどの燃焼時間 ÷ 4 (石炭：400 t、8,000 HU)'),
    ], None, {}),
    'hugenerator_block_heat_generator_elc': ('append', [
        same('Conversion', '转换', '変換', '1 EU → 1 HU'),
        ('Heat output', '10 HU/t per Coil, up to 100 HU/t (10 coils)', '热量输出', '每个线圈 10 HU/t，最多 100 HU/t (10 个线圈)',
         '熱出力', 'コイル 1 個につき 10 HU/t、最大 100 HU/t (コイル 10 個)'),
    ], None, {}),
    'hugenerator_block_rt_heat_generator': ('no_eu', [
        ('Heat output', '2, 4, 8, 16, 32 or 64 HU/t for 1 to 6 pellets', '热量输出', '1 至 6 个靶丸：2、4、8、16、32、64 HU/t',
         '熱出力', 'ペレット 1〜6 個：2、4、8、16、32、64 HU/t'),
        ('Pellet use', 'none, the pellets do not wear out', '靶丸消耗', '无，靶丸不会耗尽', 'ペレット消費', 'なし、ペレットは消耗しない'),
    ], None, {}),
    'generator_block_stirling_generator': ('generator', [
        same('Output voltage tier', '输出电压等级', '出力電圧Tier', 'MV (128 EU)'),
        same('Energy storage', '储能', '蓄電容量', '200,000 EU'),
        same('Conversion', '转换', '変換', '2 HU → 1 EU'),
    ], EU_NOTE, {}),
    'generator_block_advanced_stirling_generator': ('generator', [
        same('Output voltage tier', '输出电压等级', '出力電圧Tier', 'MV (128 EU)'),
        same('Energy storage', '储能', '蓄電容量', '1,000,000 EU'),
        same('Conversion', '转换', '変換', '100 HU → 85 EU'),
        ('Heat input', 'up to 2,000 HU/t', '热量输入', '最多 2,000 HU/t', '熱入力', '最大 2,000 HU/t'),
    ], EU_NOTE, {}),
    'kugenerator_block_stirling_kinetic_generator': ('no_eu', [
        ('Conversion', '4 HU + 1 mB Water → 12 KU + 1 mB Hot Water', '转换', '4 HU + 1 mB 水 → 12 KU + 1 mB 热水',
         '変換', '4 HU + 水 1 mB → 12 KU + 熱水 1 mB'),
        same('KU buffer', '动能缓冲', 'KUバッファ', '2,000 KU'),
        same('Heat buffer', '热量缓冲', '熱バッファ', '1,000 HU'),
    ], None, {}),
    'producer_block_steam_kinetic_generator': ('no_eu', [
        ('Steam', '2 KU per mB, per 1,000 mB: 900 mB of exhaust steam and 1 mB of Distilled Water', '蒸汽',
         '每 mB 2 KU，每 1,000 mB 产生 900 mB 废蒸汽和 1 mB 蒸馏水', '蒸気', '1 mB あたり 2 KU、1,000 mB ごとに排蒸気 900 mB と蒸留水 1 mB'),
        ('Superheated Steam', '4 KU per mB, all of it leaves as exhaust steam', '过热蒸汽', '每 mB 4 KU，全部作为废蒸汽排出',
         '過熱蒸気', '1 mB あたり 4 KU、すべて排蒸気として出る'),
        ('Throttle', 'KU × (1 − Distilled Water ÷ 1,000 mB)', '节流', 'KU × (1 − 蒸馏水 ÷ 1,000 mB)',
         '出力制限', 'KU × (1 − 蒸留水 ÷ 1,000 mB)'),
    ], None, {}),
    'generator_block_kinetic_generator': ('append', [], EU_NOTE, {
        0: ('| Output | 4,096 EU/t |', '| Production | 1 EU per 4 KU, up to 250 EU/t |\n| KU input | up to 1,000 KU/t |'),
        1: ('| 输出 | 4,096 EU/t |', '| 产出 | 每 4 KU 产 1 EU，最多 250 EU/t |\n| 动能输入 | 最多 1,000 KU/t |'),
        2: ('| 出力 | 4,096 EU/t |', '| 生産 | 4 KU ごとに 1 EU、最大 250 EU/t |\n| KU入力 | 最大 1,000 KU/t |')}),
    'generator_block_turbo_kinetic_generator': ('append', [], EU_NOTE, {
        0: ('| Output | 2,048 EU/t |', '| Production | 0.85 EU per KU from wind or water kinetic generators, 0.085 EU per KU from other '
            'sources |\n| KU input | up to 2,000 KU/t (up to 1,700 EU/t) |'),
        1: ('| 输出 | 2,048 EU/t |', '| 产出 | 风力或水力动能发生机每 KU 0.85 EU，其他来源每 KU 0.085 EU |\n| 动能输入 | 最多 2,000 KU/t (最多 1,700 EU/t) |'),
        2: ('| 出力 | 2,048 EU/t |', '| 生産 | 風力または水力の運動エネルギー発生機は 1 KU あたり 0.85 EU、ほかの発生源は 0.085 EU |\n'
            '| KU入力 | 最大 2,000 KU/t (最大 1,700 EU/t) |')}),
    'generator_block_twin_turbo_kinetic_generator': ('append', [], EU_NOTE, {
        0: ('| Output | 8,192 EU/t |', '| Production | 1.2 EU per KU from wind or water kinetic generators, 0.12 EU per KU from other '
            'sources |\n| KU input | up to 4,000 KU/t (up to 4,800 EU/t) |'),
        1: ('| 输出 | 8,192 EU/t |', '| 产出 | 风力或水力动能发生机每 KU 1.2 EU，其他来源每 KU 0.12 EU |\n| 动能输入 | 最多 4,000 KU/t (最多 4,800 EU/t) |'),
        2: ('| 出力 | 8,192 EU/t |', '| 生産 | 風力または水力の運動エネルギー発生機は 1 KU あたり 1.2 EU、ほかの発生源は 0.12 EU |\n'
            '| KU入力 | 最大 4,000 KU/t (最大 4,800 EU/t) |')}),
    'producer_block_steam_generator': ('no_eu', [
        same('Water → Steam', '水 → 蒸汽', '水 → 蒸気', '1 mB → 100 mB'),
        ('Heat input', 'up to 1,200 HU/t', '热量输入', '最多 1,200 HU/t', '熱入力', '最大 1,200 HU/t'),
        ('Superheated Steam', 'from 220 bar (valve up to 300 bar)', '过热蒸汽', '220 bar 起 (压力阀最高 300 bar)',
         '過熱蒸気', '220 bar から (圧力弁は最大 300 bar)'),
    ], None, {}),
    'producer_block_steam_repressurizer': ('no_eu', [
        same('Steam', '蒸汽', '蒸気', '10 mB + 1 HU → 16 mB'),
        same('Superheated Steam', '过热蒸汽', '過熱蒸気', '10 mB + 1 HU → 32 mB'),
    ], None, {}),
    'producer_block_condenser': ('append', [
        same('Steam or Superheated Steam → Distilled Water', '蒸汽或过热蒸汽 → 蒸馏水', '蒸気または過熱蒸気 → 蒸留水', '10,000 mB → 100 mB'),
        ('Speed', '100 mB/t of steam, each Heat Vent +100 mB/t for 2 EU/t (4 vents: 500 mB/t, 8 EU/t)', '速度',
         '蒸汽 100 mB/t，每个散热片 +100 mB/t，耗 2 EU/t (4 个：500 mB/t，8 EU/t)', '速度',
         '蒸気 100 mB/t、ヒートベント 1 個につき +100 mB/t、2 EU/t (4 個：500 mB/t、8 EU/t)'),
    ], None, {}),
    'producer_block_washer_elc': ('append', [
        ('Water use', '200 mB per operation (0.5 mB/t)', '用水', '每次操作 200 mB (0.5 mB/t)', '水消費', '1 回 200 mB (0.5 mB/t)'),
    ], None, {}),
    'producer_block_pump_elc': ('append', [
        ('Fluid per operation', '1,000 mB (one source block)', '每次抽取', '1,000 mB (一个流体源方块)', '1 回の汲み上げ', '1,000 mB (流体源ブロック 1 個)'),
    ], None, {}),
    'producer_block_electrolyzer_elc': ('append', [
        ('Water Cell → Empty Cell', '1 per operation', '水单元 → 空单元', '每次操作 1 个', '水セル → 空のセル', '1 回 1 個'),
        ('Stored energy', '200 EU per Water Cell, sent to adjacent blocks at up to 32 EU/t', '储存能量',
         '每个水单元 200 EU，以最多 32 EU/t 送给相邻方块', '蓄えたエネルギー', '水セル 1 個につき 200 EU、隣接ブロックへ最大 32 EU/t で送る'),
    ], None, {}),
    # the cycle time and EU per tick come from each recipe (processingtime, energypertick) since 0.1.10.41
    'producer_block_oil_refinery_elc': ('append', [
        ('Crude Oil → Diesel Oil', '1 mB → 1 mB per cycle (10 t, 8 EU/t)', '原油 → 柴油', '每周期 1 mB → 1 mB (10 t，8 EU/t)',
         '原油 → ディーゼル燃料', '1 サイクル 1 mB → 1 mB (10 t、8 EU/t)'),
        ('Water → Distilled Water', '1 mB → 1 mB per cycle (10 t, 8 EU/t)', '水 → 蒸馏水', '每周期 1 mB → 1 mB (10 t，8 EU/t)',
         '水 → 蒸留水', '1 サイクル 1 mB → 1 mB (10 t、8 EU/t)'),
        ('Steam → Distilled Water', '1 mB → 1 mB per cycle (10 t, 8 EU/t)', '蒸汽 → 蒸馏水', '每周期 1 mB → 1 mB (10 t，8 EU/t)',
         '蒸気 → 蒸留水', '1 サイクル 1 mB → 1 mB (10 t、8 EU/t)'),
        ('Overclocker Upgrade', '+1 mB per cycle each, the EU/t rises in step', '超频升级', '每个 +1 mB/周期，EU/t 按比例增加',
         'オーバークロッカーアップグレード', '1 個につき 1 サイクル +1 mB、EU/t も比例して増える'),
        ('Recipe data', 'datapacks set the cycle time and the EU/t of each recipe (processingtime, energypertick)', '配方数据',
         '数据包设置每个配方的周期时间与 EU/t (processingtime、energypertick)', 'レシピデータ',
         'データパックが各レシピのサイクル時間と EU/t を設定する (processingtime、energypertick)'),
    ], None, {}),
}


def rows_text(rows, i):
    return '\n'.join(f'| {r[2 * i]} | {r[2 * i + 1]} |' for r in rows)


def section_bounds(text, lang):
    start = text.index(HEADING[lang] + '\n')
    end = text.find('\n## ', start + 3)
    return start, len(text) if end < 0 else end + 1


def apply(text, lang, i, mode, rows, note, replaced):
    """The page text with the rates in; unchanged when they are there already."""
    start, end = section_bounds(text, lang)
    sec = text[start:end]
    if lang_index_old := replaced.get(i):
        old, new = lang_index_old
        if new not in sec:
            assert sec.count(old) == 1, (lang, old)
            sec = sec.replace(old, new)
    if rows:
        table = rows_text(rows, i)
        if table not in sec:
            if mode == 'append':
                lines = sec.split('\n')
                last = max(n for n, line in enumerate(lines) if line.startswith('|'))
                lines.insert(last + 1, table)
                sec = '\n'.join(lines)
            else:
                assert sec.count(NO_EU[lang]) == 1, (lang, mode)
                full = TABLE_HEAD[lang] + '\n' + table + '\n\n'
                sec = sec.replace(NO_EU[lang], full + (NO_EU[lang] if mode == 'no_eu' else note[i]))
    if note and note[i] not in sec:
        anchor = MEASURED[lang]
        assert sec.count(anchor) == 1, (lang, anchor)
        sec = sec.replace(anchor, anchor + '\n\n' + note[i])
    return text[:start] + sec + text[end:]


def main():
    bad = 0
    for page, (mode, rows, note, replaced) in PAGES.items():
        for i, lang in enumerate(LANGS):
            path = os.path.join(BASE, lang + 'machines', page + '.md')
            raw = open(path, 'rb').read()
            eol = b'\r\n' if b'\r\n' in raw else b'\n'
            text = raw.decode('utf-8').replace('\r\n', '\n')
            new = apply(text, lang, i, mode, rows, note, replaced)
            if new == text:
                continue
            if CHECK:
                bad += 1
                print(f'{lang or "en/"}{page}: missing the rates')
                continue
            open(path, 'wb').write(new.encode('utf-8').replace(b'\n', eol))
            print(f'{lang or "en/"}{page}: done')
    if CHECK:
        print('guide_1141 check:', 'OK' if not bad else f'{bad} missing')
        sys.exit(1 if bad else 0)


if __name__ == '__main__':
    main()
