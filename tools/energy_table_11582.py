#!/usr/bin/env python3
"""0.1.11.82: the electrical numbers of Singularity Iteration as docs/energy_values.md and docs/SI机器数值说明.xlsx.

0.1.11.88: the workbook is docs/SI机器数值说明.xlsx (the user's name for it; the old docs/energy_values.xlsx was deleted).
The script refuses to overwrite it when a spreadsheet program saved it after the script (hand edits would be lost);
--force overwrites anyway.

Sources:
  * build/addon-gametest/si_energy_dump.json, written by EnergyValuesDumpGameTests on every run of
    `gradlew runAddonGameTestServer` (block entities and electric items measured in the game);
  * the "Power data" tables of the Chinese manual pages (fuel, KU / HU, conditions, rows the dump cannot read);
  * the cable table of the manual page power.md;
  * zh_cn names (English where Chinese is missing).

Each section is built once (columns, rows) and written to both files. Derived columns (EU per operation, uses per
charge, seconds to fill, EU per second / day) are Excel formulas in the workbook and computed values in the Markdown.

Usage: energy_table_11582.py <repo root> [--soffice <path to LibreOffice soffice.exe>] [--force]
  (run the GameTest suite first so the dump is current). With --soffice, LibreOffice recalculates a copy of the
  workbook, every formula result is checked for errors, and the results are stored in the workbook as cached values
  (the formulas and the formatting stay as written), so viewers that do not calculate show the numbers too.
"""
import json
import os
import re
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
DUMP = os.path.join(ROOT, 'build/addon-gametest/si_energy_dump.json')
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
GUIDE = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual/_zh_cn')
OUT_MD = os.path.join(ROOT, 'docs/energy_values.md')
OUT_XLSX = os.path.join(ROOT, 'docs/SI机器数值说明.xlsx')
UNLIMITED = 2 ** 31 - 1

# notes for generators whose output depends on their state (rated 0 or an idle value when measured detached)
NOTES = {
    'generator/block_nuclear_reactor_generator': '按燃料棒布局发电；产出不超过 524,288 EU/t 时发 IV 包（8,192 EU，每 tick 最多 64 个），'
                                                 '超过时发 LuV 包（32,768 EU）；每 tick 最多送出 1,000,000 EU（等于储能）；'
                                                 '氚燃料棒（10,000 周期）和氘燃料棒（5,000 周期，0.1.11.94）是中子源：相邻燃料棒每周期多一次脉冲',
    'generator/block_steam_reactor': '不发 EU：产蒸汽，交给蒸汽涡轮或蒸汽动能发生机',
    'generator/block_rt_generator': '1–32 EU/t，按放入的同位素靶丸数',
    'generator/block_wind_generator': '按高度、遮挡和天气，最多 32 EU/t',
    'generator/block_creative_generator': '创造模式专用',
    'generator/block_unlimit_generator': '创造模式专用',
    'generator/block_steam_tunnel_power_port': '蒸汽隧道多方块的电力端口，发电量由隧道结构决定（见手册「蒸汽隧道」）',
    'generator/block_drop_generator': '烧掉落物：20 EU/t',
    'generator/block_advanced_drop_generator': '烧掉落物：40 EU/t',
    'generator/block_experience_generator': '烧经验球：160 EU/t',
    'generator/block_advanced_experience_generator': '烧经验球：320 EU/t',
    'generator/block_quantum_generator': '8,192 EU/t（有自己的配置项，再乘发电倍率）',
    'fusion/block_fusion_controller': '托卡马克聚变堆：输出不封顶（0.1.11.98 起；示例包层 41,440,000 EU/t，超过 Mek 聚变堆的二倍，更强的包层更多，'
                                      '只在电网最高电压 2,147,483,647 EU/t 处截断），经能量输出端口送出；'
                                      '聚变功率每档加料 612,500 EU/t；储能为输入（LuV），磁体维持 392,000 EU/t，启动 300,000,000 EU；'
                                      '燃料为氘燃料棒和氚燃料棒，满功率每 625 tick 各用一根；破裂缓解用 10k/30k/60k 冷却单元；'
                                      '包层热量到上限即熔毁核爆（默认为核反应堆最强爆炸的 50 倍，配置 fusionExplosionMultiplier）',
    'fusion/block_fusion_power_port': '聚变堆的能量输出端口：多个端口平分输出，每个按分到的量选电压（64 个包送得完的最低等级）',
    'generator/block_kinetic_generator': '输出等级随动能输入：产出不超过 128 EU/t 时 MV 包，超过时（输入超过 512 KU/t）HV 包',
    'generator/block_turbo_kinetic_generator': '输出等级随动能输入：产出不超过 512 EU/t 时 HV 包，超过时 EV 包'
                                               '（风力或水力超过 602 KU/t，其他来源超过 1,462 KU/t）',
    'generator/block_twin_turbo_kinetic_generator': '输出等级随动能输入：产出不超过 2,048 EU/t 时 EV 包，超过时 IV 包'
                                                    '（风力或水力超过 1,706 KU/t；其他来源最多 1,800 EU/t，始终 EV）',
}

# generators whose packet tier follows their output (getSourceTier): (tier text, largest packet EU). The dump reads the
# storage tier, which is only the floor.
DYNAMIC_TIER = {
    'generator/block_kinetic_generator': ('MV→HV', 512),
    'generator/block_turbo_kinetic_generator': ('HV→EV', 2048),
    'generator/block_twin_turbo_kinetic_generator': ('EV→IV', 8192),
    'generator/block_nuclear_reactor_generator': ('IV→LuV', 32768),
    'fusion/block_fusion_controller': ('IV→UIV', 33554432),   # through its power output ports (0.1.11.98: was UHV, no cap now)
    'fusion/block_fusion_power_port': ('IV→UIV', 33554432),
}

# the most a state-dependent generator makes (EU/t, from its manual page or its code), for the planning ranking
MAX_OUT = {
    'fusion/block_fusion_controller': (41440000, '示例包层（质子四联燃料棒＋铱超频散热片）；0.1.11.98 起不封顶，更强的包层更多'),
    'generator/block_geomagnetic_generator': (30720, '手册：扰动为 1 时；平均约 8,300'),
    'generator/block_quantum_generator': (8192, '额定（代码）'),
    'generator/block_twin_turbo_kinetic_generator': (4800, '手册：4,000 KU/t 风力或水力动能'),
    'generator/block_steam_tunnel_power_port': (2048, '手册：蒸汽隧道，每 tick 4 个包'),
    'generator/block_turbo_kinetic_generator': (1700, '手册：2,000 KU/t 风力或水力动能'),
    'generator/block_advanced_experience_generator': (320, '额定（代码）'),
    'generator/block_kinetic_generator': (250, '手册：1,000 KU/t'),
    'generator/block_experience_generator': (160, '额定（代码）'),
    'generator/block_advanced_drop_generator': (40, '额定（代码）'),
    'generator/block_rt_generator': (32, '手册：满靶丸'),
    'generator/block_wind_generator': (32, '手册：高处无遮挡、雷暴'),
    'generator/block_drop_generator': (20, '额定（代码）'),
    'generator/block_water_generator': (8, '手册：水箱加正面水流'),
}


def creative(block_id):
    return 'creative' in block_id or 'unlimit' in block_id


# ------------------------------------------------------------------ sources

def load_lang(code):
    with open(os.path.join(LANG, code + '.json'), 'rb') as f:
        return json.loads(f.read().decode('utf-8-sig'))


ZH, EN = load_lang('zh_cn'), load_lang('en_us')


def name(kind, path):
    key = '%s.mio_icif.%s' % (kind, path.replace('/', '.'))
    other = '%s.mio_icif.%s' % ('item' if kind == 'block' else 'block', path.replace('/', '.'))
    for table_ in (ZH, EN):
        for k in (key, other):
            if k in table_:
                return table_[k]
    return path


def guide_rows(path):
    """Rows of the "电力数据" table of a manual page (machine id with / -> _), as (item, value)."""
    page = os.path.join(GUIDE, 'machines', path.replace('/', '_') + '.md')
    if not os.path.exists(page):
        return []
    text = open(page, encoding='utf-8').read()
    m = re.search(r'## 电力数据\s*\n(.*?)(\n## |\Z)', text, re.S)
    rows = []
    for line in (m.group(1).splitlines() if m else []):
        cells = [c.strip() for c in line.strip().strip('|').split('|')]
        if len(cells) == 2 and cells[0] not in ('项目',) and not set(cells[0]) <= set('-'):
            rows.append((cells[0], cells[1]))
    return rows


def extra(path, skip=('输出电压等级', '输入电压等级', '输出', '储能', '最大输入')):
    return '；'.join('%s：%s' % (k, v) for k, v in guide_rows(path) if k not in skip)


def guide_value(path, key):
    return next((v for k, v in guide_rows(path) if k == key), '')


def cable_rows():
    text = open(os.path.join(GUIDE, 'power.md'), encoding='utf-8').read()
    m = re.search(r'## 导线\s*\n(\|.*?)\n\n', text, re.S)
    rows = []
    for line in (m.group(1).splitlines()[2:] if m else []):
        rows.append([c.strip() for c in line.strip().strip('|').split('|')])
    return rows


# ------------------------------------------------------------------ section model

class Col:
    """One column. get(row) gives the value (also the Markdown value of a formula column). kind: text, code, int, num2.
    formula: an Excel formula with {key} placeholders for other columns of the same row (written as A2, B2...)."""

    def __init__(self, key, header, get, kind='text', formula=None, width=None, note=None):
        self.key, self.header, self.get, self.kind, self.formula, self.width, self.note = key, header, get, kind, formula, width, note


class Section:
    def __init__(self, title, sheet, cols, rows, intro=(), outro=()):
        self.title, self.sheet, self.cols, self.rows, self.intro, self.outro = title, sheet, cols, rows, list(intro), list(outro)


def tier_name(t):
    return t.upper() if t else ''


def tier_eu(eu):
    """The packet limit as a number, or 不限 for the unlimited tiers."""
    if eu is None:
        return ''
    return '不限' if eu > UNLIMITED else eu


def safe_div(a, b):
    return a / b if a and b else ''


# ------------------------------------------------------------------ the sections

def build(dump):
    tiers = sorted(dump['tiers'], key=lambda t: t['index'])
    blocks = [b for b in dump['blocks'] if 'kind' in b]
    items = [i for i in dump['items'] if not i['id'].endswith('_api')]   # test-only items
    S = []

    def tier_cols(label='电压等级'):
        def tier(b):
            return DYNAMIC_TIER[b['id']][0] if b.get('id') in DYNAMIC_TIER else tier_name(b.get('tier'))

        def eu(b):
            return DYNAMIC_TIER[b['id']][1] if b.get('id') in DYNAMIC_TIER else tier_eu(b.get('tierEu'))
        return [Col('tier', label, tier, width=10), Col('tierEu', '单包上限 EU', eu, 'int', width=14)]

    S.append(Section('1. 电压等级', '电压等级',
                     [Col('name', '等级', lambda t: t['name'].upper(), width=10),
                      Col('eu', '单包上限 EU', lambda t: t['eu'] if t['eu'] < 10 ** 15 else '无上限', 'int', width=18)],
                     tiers,
                     intro=['单个电力包的上限，也是导线和机器的耐受等级。超过导线上限会熔毁导线，超过机器输入等级会让机器爆炸。']))

    S.append(Section('2. 导线', '导线',
                     [Col('c%d' % i, h, (lambda i: lambda r: r[i])(i), width=w) for i, (h, w) in
                      enumerate([('导线', 28), ('等级', 8), ('单包上限', 12), ('不电击所需绝缘', 16), ('线损（每格）', 14)])],
                     cable_rows(),
                     outro=['额定值是单包上限，不是通量上限：一根导线每 tick 可以送任意多个不超过上限的包。',
                            '线损默认关闭（服务器选项 lineLossModel = proportional 时才按表扣除）。']))

    gens = sorted((b for b in blocks if b['kind'] == 'generator'), key=lambda b: (-(b.get('rated') or 0), b['id']))

    def gen_note(b):
        note = extra(b['id'])
        if b['id'] in NOTES:
            return '；'.join(x for x in (NOTES[b['id']], note) if x)
        if not b.get('rated'):
            out = guide_value(b['id'], '输出')
            return '；'.join(x for x in ('手册：输出 ' + out if out else '', note) if x)
        return note

    S.append(Section('3. 发电机（EU）', '发电机',
                     [Col('name', '名称', lambda b: name('block', b['id']), width=26),
                      Col('id', 'ID', lambda b: b['id'], 'code', width=44)] + tier_cols('输出等级') +
                     [Col('rated', '额定输出 EU/t', lambda b: b.get('rated', ''), 'int', width=14),
                      Col('cap', '内部储能 EU', lambda b: b.get('capacity', ''), 'int', width=16),
                      Col('ext', '每 tick 最多送出 EU', lambda b: b.get('maxExtract', ''), 'int', width=18),
                      Col('note', '说明（燃料、条件）', gen_note, width=80)],
                     gens,
                     intro=['按额定输出从高到低排列。额定输出为 0 或空的，发电量随状态变化，见「说明」列。',
                            '「输出等级」写成 A→B 的（三种动能发电机、核反应堆和聚变堆），电力包随产出变大：A 是最低等级，产出高时升到 B，'
                            '「单包上限」按 B 计。接线要按 B 选导线和机器。']))

    rows = []
    guide_dir = os.path.join(GUIDE, 'machines')
    for page in sorted(os.listdir(guide_dir)):
        if page.startswith('kugenerator_') or page.startswith('hugenerator_') or page == 'producer_block_steam_kinetic_generator.md':
            pid = page[:-3].replace('_', '/', 1)
            title = re.search(r'^# (.+)$', open(os.path.join(guide_dir, page), encoding='utf-8').read(), re.M)
            rows.append({'name': title.group(1) if title else pid, 'id': pid,
                         'data': '；'.join('%s：%s' % kv for kv in guide_rows(pid))})
    S.append(Section('4. 动能（KU）与热能（HU）来源', '动能与热能',
                     [Col('name', '名称', lambda r: r['name'], width=22), Col('id', 'ID', lambda r: r['id'], 'code', width=44),
                      Col('data', '手册数据', lambda r: r['data'], width=100)],
                     rows,
                     intro=['这些方块不直接发 EU，产出的动能或热能由动能发电机、斯特林发电机等转换成 EU。数值取自手册。']))

    storage = sorted((b for b in blocks if b['kind'] == 'storage' and 'charger' not in b['id']), key=lambda b: b.get('capacity') or 0)
    gesu = [b for b in blocks if 'gesu' in b['id'] and b['kind'] != 'storage']
    io_cols = [Col('cap', '储能 EU', lambda b: b.get('capacity', ''), 'int', width=16),
               Col('rec', '每 tick 最大输入 EU', lambda b: b.get('maxReceive', ''), 'int', width=18),
               Col('ext', '每 tick 最大输出 EU', lambda b: b.get('maxExtract', ''), 'int', width=18)]
    S.append(Section('5. 储电方块', '储电方块',
                     [Col('name', '名称', lambda b: name('block', b['id']), width=30),
                      Col('id', 'ID', lambda b: b['id'], 'code', width=34)] + tier_cols() + io_cols,
                     storage + gesu,
                     outro=['GESU 是多方块结构（核心加 6 个模块）：核心储能，输入和输出模块各自按等级收发。模块的储能是它们的缓冲。']))

    chargers = sorted((b for b in blocks if 'charger' in b['id']), key=lambda b: b.get('capacity') or 0)
    S.append(Section('6. 充电座', '充电座',
                     [Col('name', '名称', lambda b: name('block', b['id']), width=18),
                      Col('id', 'ID', lambda b: b['id'], 'code', width=30)] + tier_cols() + io_cols,
                     chargers))

    by_name = {t['name'].lower(): t for t in tiers}
    rows = []
    for page in sorted(os.listdir(guide_dir)):
        if page.startswith('wiring_transformer_'):
            pid = 'wiring/' + page[len('wiring_'):-3]
            lo, hi = pid.split('transformer_', 1)[1].split('_', 1)
            lo, hi = ('iv' if lo == 'sc' else lo), ('iv' if hi == 'sc' else hi)
            rows.append({'id': pid, 'lo': lo, 'hi': hi})
    rows.sort(key=lambda r: by_name.get(r['lo'], {'eu': 0})['eu'])
    S.append(Section('7. 变压器', '变压器',
                     [Col('name', '名称', lambda r: name('block', r['id']), width=22), Col('id', 'ID', lambda r: r['id'], 'code', width=30),
                      Col('lo', '低压侧', lambda r: r['lo'].upper(), width=10),
                      Col('loEu', '低压侧单包 EU', lambda r: by_name[r['lo']]['eu'] if r['lo'] in by_name else '', 'int', width=14),
                      Col('hi', '高压侧', lambda r: r['hi'].upper(), width=10),
                      Col('hiEu', '高压侧单包 EU', lambda r: by_name[r['hi']]['eu'] if r['hi'] in by_name else '', 'int', width=14)],
                     rows,
                     outro=['变压器在相邻两个等级之间转换：高压侧收进的大包拆成低压侧的小包送出，反过来升压。']))

    machines = sorted((b for b in blocks if b['kind'] == 'machine'), key=lambda b: (-(b.get('energyPerTick') or 0), b['id']))
    S.append(Section('8. 机器耗电', '机器耗电',
                     [Col('name', '名称', lambda b: name('block', b['id']), width=26),
                      Col('id', 'ID', lambda b: b['id'], 'code', width=44)] + tier_cols('输入等级') +
                     [Col('ept', '耗电 EU/t', lambda b: b.get('energyPerTick', ''), 'int', width=12),
                      Col('prog', '工时 tick', lambda b: b.get('maxProgress', ''), 'int', width=12),
                      Col('op', '单次耗电 EU', lambda b: b['energyPerTick'] * b['maxProgress'] if 'energyPerTick' in b and 'maxProgress' in b else '', 'int',
                          formula='=IF(OR({ept}="",{prog}=""),"",{ept}*{prog})', width=14, note='= 耗电 × 工时'),
                      Col('cap', '内部储能 EU', lambda b: b.get('capacity', ''), 'int', width=16)],
                     machines,
                     intro=['「单次耗电」= 耗电 × 工时（空机、没有升级时的默认值）。实际配方可能有自己的工时和耗电（例如精炼机），以 JEI 为准。',
                            '0.1.11.107 起的加工机器大修后，很多加工机器的单次耗电跟着输入走，空机测得的默认值只是起点：打粉机和旋风打粉机按材料的邦德功指数（铁约 600 EU）；压缩机、提取机、方块切割机、洗矿机和两台金属成型机按配方写的工时和功率；电炉和感应炉另有升温；热能离心机按配方的最低热量开工。各机器的计算方式见游戏手册。']))

    others = sorted((b for b in blocks if b['kind'] == 'other' and 'gesu' not in b['id'] and not b['id'].startswith('wiring/cable/')),
                    key=lambda b: b['id'])
    S.append(Section('9. 其他带电方块', '其他带电方块',
                     [Col('name', '名称', lambda b: name('block', b['id']), width=28),
                      Col('id', 'ID', lambda b: b['id'], 'code', width=44)] + tier_cols() + io_cols,
                     others))

    def rate_tier(rate):
        for t in tiers:
            if rate and rate <= t['eu']:
                return t['name'].upper()
        return ''

    batteries = sorted((i for i in items if i.get('kind') == 'battery'), key=lambda i: (i['maxCharge'], i['id']))
    S.append(Section('10. 电池与储能物品', '电池',
                     [Col('name', '名称', lambda i: name('item', i['id']), width=22),
                      Col('id', 'ID', lambda i: i['id'], 'code', width=40),
                      Col('cap', '容量 EU', lambda i: i['maxCharge'], 'int', width=16),
                      Col('rate', '速率 EU/t', lambda i: i.get('chargeRate', ''), 'int', width=12),
                      Col('rtier', '速率对应等级', lambda i: rate_tier(i.get('chargeRate')), width=14),
                      Col('fill', '从空充满秒数', lambda i: safe_div(safe_div(i['maxCharge'], i.get('chargeRate')), 20), 'num2',
                          formula='=IF(OR({rate}="",{rate}=0),"",{cap}/{rate}/20)', width=14, note='= 容量 ÷ 速率 ÷ 20')],
                     batteries,
                     intro=['「速率」是每 tick 充电或放电的最大 EU。「速率对应等级」是能容纳这个速率的最低电压等级（推算值，便于和机器、导线对照）。']))

    tools = sorted((i for i in items if i.get('kind') == 'tool'), key=lambda i: (i['maxCharge'], i['id']))
    S.append(Section('11. 电动工具与武器', '电动工具与武器',
                     [Col('name', '名称', lambda i: name('item', i['id']), width=22),
                      Col('id', 'ID', lambda i: i['id'], 'code', width=40),
                      Col('cap', '容量 EU', lambda i: i['maxCharge'], 'int', width=16),
                      Col('ttier', '工具等级', lambda i: i.get('toolTier', ''), 'int', width=10),
                      Col('rate', '充电速率 EU/t', lambda i: i.get('chargeRate', ''), 'int', width=14),
                      Col('use', '每次耗电 EU', lambda i: i.get('perUse', ''), 'int', width=12),
                      Col('uses', '满电可用次数', lambda i: i['maxCharge'] // i['perUse'] if i.get('perUse') else '', 'int',
                          formula='=IF(OR({use}="",{use}=0),"",INT({cap}/{use}))', width=14, note='= 容量 ÷ 每次耗电（取整）')],
                     tools,
                     outro=['「每次耗电」是工具的基础值。部分工具的实际耗电随模式或目标变化（例如镭射枪各模式、钻头挖硬方块），以物品提示为准。']))

    armor = sorted((i for i in items if i.get('kind') == 'armor'), key=lambda i: (i['maxCharge'], i['id']))
    S.append(Section('12. 电力护甲、背包与喷气背包', '护甲与背包',
                     [Col('name', '名称', lambda i: name('item', i['id']), width=22),
                      Col('id', 'ID', lambda i: i['id'], 'code', width=40),
                      Col('cap', '容量 EU', lambda i: i['maxCharge'], 'int', width=16),
                      Col('atier', '护甲等级', lambda i: i.get('armorTier', ''), 'int', width=10),
                      Col('rate', '充电速率 EU/t', lambda i: i.get('chargeRate', ''), 'int', width=14),
                      Col('idle', '待机耗电 EU/t', lambda i: i.get('energyPerTick', ''), 'int', width=14)],
                     armor,
                     intro=['「待机耗电」是穿戴时每 tick 固定消耗的 EU（0 = 只在受击或使用功能时耗电）。']))

    def best(b):
        return MAX_OUT[b['id']] if b['id'] in MAX_OUT else (b.get('rated') or 0, '额定（实测）')
    ranked = sorted((b for b in gens if not creative(b['id']) and best(b)[0] > 0), key=lambda b: -best(b)[0])[:10]
    S.append(Section('13. 规划参考：可达输出最高的 10 种发电机', '规划·发电',
                     [Col('name', '名称', lambda b: name('block', b['id']), width=26),
                      Col('max', '最大输出 EU/t', lambda b: best(b)[0], 'int', width=14),
                      Col('basis', '依据', lambda b: best(b)[1], width=32)] + tier_cols('输出等级') +
                     [Col('sec', '每秒 EU', lambda b: best(b)[0] * 20, 'int', formula='={max}*20', width=14, note='= 最大输出 × 20 tick'),
                      Col('day', '每天（24000 tick）EU', lambda b: best(b)[0] * 24000, 'int', formula='={max}*24000', width=20,
                          note='= 最大输出 × 24,000 tick')],
                     ranked,
                     intro=['不含创造模式发电机和随布局变化的核反应堆。'],
                     outro=['核反应堆的输出按燃料棒布局，手册「反应堆模拟器」和 JEI「反应堆布局」里的内置示例最高 660 EU/t（6 个反应仓，进料倍率 1x）。']))

    big = sorted((b for b in storage + gesu if 'input' not in b['id'] and 'output' not in b['id']), key=lambda b: -(b.get('capacity') or 0))[:5]
    S.append(Section('14. 规划参考：储能最大的方块', '规划·储能',
                     [Col('name', '名称', lambda b: name('block', b['id']), width=22),
                      Col('cap', '储能 EU', lambda b: b.get('capacity', ''), 'int', width=16)] + tier_cols() +
                     [Col('ext', '最大输出 EU/t', lambda b: '' if 'gesu_core' in b['id'] else b.get('maxExtract', ''), 'int', width=14,
                          note='GESU 核心经输出模块送电：每个 8,192（IV）或 32,768（LuV）'),
                      Col('secs', '满电可放多少秒（按最大输出）', lambda b: '' if 'gesu_core' in b['id'] else safe_div(safe_div(b.get('capacity'), b.get('maxExtract')), 20),
                          'num2', formula='=IF(OR({ext}="",{ext}=0),"",{cap}/{ext}/20)', width=26, note='= 储能 ÷ 最大输出 ÷ 20')],
                     big,
                     outro=['GESU 核心经输出模块送电：每个模块 8,192（IV）或 32,768（LuV）EU/t。',
                            '设计一台新发电机时可以对照：同一等级里，单包不能超过该等级上限；需要更大输出时，每 tick 送多个包（导线没有通量上限），'
                            '或者提高输出等级并配相应的导线和变压器。']))
    return S


PREFACE = [
    '本表汇总电力相关的数值（发电、储电、输电、耗电、电力物品），供规划新机器（例如核聚变多方块结构）时对照。',
    '来源：GameTest EnergyValuesDumpGameTests 在游戏里实测的方块实体和物品（储能、输入输出上限、电压等级、额定输出、耗电和工时、物品容量与速率）；'
    '燃料、动能（KU）、热能（HU）和工作条件取自游戏手册各页的「电力数据」表；导线表取自手册「输电与储电」。',
    '更新：先运行 gradlew runAddonGameTestServer，再运行 python tools/energy_table_11582.py .（同时生成 docs/energy_values.md 和 docs/SI机器数值说明.xlsx）。',
    '方块实体是脱离世界创建的，随状态变化的数值显示为空闲值（例如反应堆按布局发电），这类以说明列为准。',
    '发电量受配置 GeneratorOutput 的倍率影响，本表是默认倍率 1.0 下的值。和其他模组换算：4 FE = 1 EU。',
]


# ------------------------------------------------------------------ Markdown

def fmt_md(col, v):
    if v is None or v == '':
        return '-'
    if col.kind == 'code':
        return '`%s`' % v
    if isinstance(v, (int, float)) and col.kind in ('int', 'num2'):
        if col.kind == 'num2' or (isinstance(v, float) and not v.is_integer()):
            return '{:,.2f}'.format(v)
        return '{:,}'.format(int(v))
    return str(v)


def write_md(sections):
    md = ['# 奇点迭代：电力数值表', '']
    for p in PREFACE:
        md += ['- ' + p]
    md.append('')
    for s in sections:
        md += ['## ' + s.title, '']
        for p in s.intro:
            md += [p, '']
        md.append('| ' + ' | '.join(c.header for c in s.cols) + ' |')
        md.append('|' + '|'.join('---' for _ in s.cols) + '|')
        for r in s.rows:
            md.append('| ' + ' | '.join(fmt_md(c, c.get(r)) for c in s.cols) + ' |')
        md.append('')
        for p in s.outro:
            md += [p, '']
    with open(OUT_MD, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(md))


# ------------------------------------------------------------------ Excel

def write_xlsx(sections):
    from openpyxl import Workbook
    from openpyxl.comments import Comment
    from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
    from openpyxl.utils import get_column_letter

    font = Font(name='Arial', size=10)
    bold = Font(name='Arial', size=10, bold=True, color='FFFFFF')
    title_font = Font(name='Arial', size=14, bold=True)
    head_fill = PatternFill('solid', start_color='305496')
    formula_fill = PatternFill('solid', start_color='EAF1DD')
    thin = Side(style='thin', color='BFBFBF')
    border = Border(left=thin, right=thin, top=thin, bottom=thin)

    wb = Workbook()
    readme = wb.active
    readme.title = '说明'
    readme['A1'] = '奇点迭代：电力数值表'
    readme['A1'].font = title_font
    row = 3
    for p in PREFACE:
        readme.cell(row=row, column=1, value=p).font = font
        readme.cell(row=row, column=1).alignment = Alignment(wrap_text=True, vertical='top')
        row += 1
    row += 1
    readme.cell(row=row, column=1, value='各工作表').font = Font(name='Arial', size=11, bold=True)
    row += 1
    for s in sections:
        readme.cell(row=row, column=1, value='%s（工作表「%s」）' % (s.title, s.sheet)).font = Font(name='Arial', size=10, bold=True)
        row += 1
        for p in s.intro + s.outro:
            c = readme.cell(row=row, column=1, value='    ' + p)
            c.font = font
            c.alignment = Alignment(wrap_text=True, vertical='top')
            row += 1
    readme.cell(row=row + 1, column=1, value='浅绿底色的列是公式（由同一行其他列算出），修改输入列后会自动更新。').font = Font(name='Arial', size=10, italic=True)
    readme.column_dimensions['A'].width = 130

    for s in sections:
        ws = wb.create_sheet(s.sheet)
        letters = {c.key: get_column_letter(i + 1) for i, c in enumerate(s.cols)}
        for i, c in enumerate(s.cols):
            cell = ws.cell(row=1, column=i + 1, value=c.header)
            cell.font, cell.fill, cell.border = bold, head_fill, border
            cell.alignment = Alignment(horizontal='center', vertical='center', wrap_text=True)
            if c.note:
                cell.comment = Comment(c.note, 'energy_table_11582.py')
            ws.column_dimensions[get_column_letter(i + 1)].width = c.width or 14
        for r_index, r in enumerate(s.rows, start=2):
            for i, c in enumerate(s.cols):
                cell = ws.cell(row=r_index, column=i + 1)
                if c.formula:
                    cell.value = c.formula.format(**{k: '%s%d' % (v, r_index) for k, v in letters.items()})
                    cell.fill = formula_fill
                else:
                    v = c.get(r)
                    cell.value = None if v == '' else v
                cell.font, cell.border = font, border
                if c.kind == 'int':
                    cell.number_format = '#,##0'
                    cell.alignment = Alignment(horizontal='right')
                elif c.kind == 'num2':
                    cell.number_format = '#,##0.00'
                    cell.alignment = Alignment(horizontal='right')
                else:
                    cell.alignment = Alignment(wrap_text=c.width is not None and c.width >= 60, vertical='top')
        ws.freeze_panes = 'C2' if len(s.cols) > 3 and s.cols[1].key == 'id' else 'A2'
        if s.rows:
            ws.auto_filter.ref = 'A1:%s%d' % (get_column_letter(len(s.cols)), len(s.rows) + 1)
        ws.row_dimensions[1].height = 30
        foot = len(s.rows) + 3
        for p in s.intro + s.outro:
            c = ws.cell(row=foot, column=1, value=p)
            c.font = Font(name='Arial', size=9, italic=True, color='595959')
            foot += 1
    # Excel computes every formula when the file opens (openpyxl writes formulas without cached values)
    from openpyxl.workbook.properties import CalcProperties
    wb.calculation = CalcProperties(fullCalcOnLoad=True)
    wb.save(OUT_XLSX)


def recalc(soffice):
    """Recalculates a copy with LibreOffice, checks the results and stores them as cached values. Returns (formulas, errors)."""
    import shutil
    import subprocess
    import tempfile
    import zipfile
    from xml.sax.saxutils import escape
    from openpyxl import load_workbook

    tmp = tempfile.mkdtemp(prefix='si_recalc_')
    try:
        profile = 'file:///' + os.path.join(tmp, 'profile').replace(os.sep, '/')
        # LibreOffice works on a copy with an ASCII name (the workbook's own name is Chinese)
        source, out = os.path.join(tmp, 'table.xlsx'), os.path.join(tmp, 'out')
        shutil.copyfile(OUT_XLSX, source)
        subprocess.run([soffice, '-env:UserInstallation=' + profile, '--headless', '--calc', '--convert-to', 'xlsx',
                        '--outdir', out, source], check=True, timeout=300,
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        computed = load_workbook(os.path.join(out, 'table.xlsx'), data_only=True)
        formulas = load_workbook(OUT_XLSX)
        values, errors = {}, []
        for ws in formulas.worksheets:
            for row in ws.iter_rows():
                for c in row:
                    if isinstance(c.value, str) and c.value.startswith('='):
                        v = computed[ws.title][c.coordinate].value
                        if isinstance(v, str) and v.startswith('#'):
                            errors.append('%s!%s %s' % (ws.title, c.coordinate, v))
                        values.setdefault(ws.title, {})[c.coordinate] = v
        # sheet title -> part name
        with zipfile.ZipFile(OUT_XLSX) as z:
            parts = {n: z.read(n) for n in z.namelist()}
        rels = parts['xl/_rels/workbook.xml.rels'].decode('utf-8')
        target = {}
        for rel in re.findall(r'<Relationship\b[^>]*>', rels):   # attribute order differs between writers
            rid, tgt = re.search(r'\bId="([^"]+)"', rel), re.search(r'\bTarget="/?(?:xl/)?([^"]+)"', rel)
            if rid and tgt:
                target[rid.group(1)] = tgt.group(1)
        sheets = re.findall(r'<sheet [^>]*name="([^"]+)"[^>]*r:id="(rId\d+)"', parts['xl/workbook.xml'].decode('utf-8'))
        for title, rid in sheets:
            part = 'xl/' + target[rid]
            xml = parts[part].decode('utf-8')
            for ref, v in values.get(title, {}).items():
                def put(m):
                    attrs = re.sub(r'\s+t="[^"]*"', '', m.group(2))
                    if v is None or v == '':
                        return '<c r="%s"%s t="str">%s<v></v></c>' % (ref, attrs, m.group(3))
                    if isinstance(v, str):
                        return '<c r="%s"%s t="str">%s<v>%s</v></c>' % (ref, attrs, m.group(3), escape(v))
                    return '<c r="%s"%s>%s<v>%s</v></c>' % (ref, attrs, m.group(3), repr(float(v)) if isinstance(v, float) else v)
                xml = re.sub(r'<c r="(%s)"([^>]*)>(<f>.*?</f>)(?:<v>.*?</v>|<v\s*/>)?</c>' % ref, put, xml, count=1)
            parts[part] = xml.encode('utf-8')
        with zipfile.ZipFile(OUT_XLSX + '.tmp', 'w', zipfile.ZIP_DEFLATED) as z:
            for n, data in parts.items():
                z.writestr(n, data)
        os.replace(OUT_XLSX + '.tmp', OUT_XLSX)
        return sum(len(v) for v in values.values()), errors
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def saved_by_hand(path):
    """The program that saved the workbook after this script, or None. openpyxl leaves lastModifiedBy empty and keeps
    itself as the creator; Excel and WPS fill lastModifiedBy, LibreOffice names itself in the application."""
    import zipfile
    if not os.path.exists(path):
        return None
    try:
        with zipfile.ZipFile(path) as z:
            core = z.read('docProps/core.xml').decode('utf-8')
            app = z.read('docProps/app.xml').decode('utf-8') if 'docProps/app.xml' in z.namelist() else ''
    except (KeyError, zipfile.BadZipFile):
        return 'an unknown program'
    creator = re.search(r'<dc:creator>([^<]*)</dc:creator>', core)
    editor = re.search(r'<cp:lastModifiedBy>([^<]*)</cp:lastModifiedBy>', core)
    application = re.search(r'<Application>([^<]*)</Application>', app)
    if editor and editor.group(1).strip():
        return editor.group(1).strip()
    if application and any(k in application.group(1) for k in ('LibreOffice', 'WPS', 'OpenOffice')):
        return application.group(1)
    if not creator or creator.group(1) != 'openpyxl':
        return creator.group(1) if creator else 'an unknown program'
    return None


def main():
    if not os.path.exists(DUMP):
        sys.exit('no dump: run `gradlew runAddonGameTestServer` first (%s)' % DUMP)
    editor = saved_by_hand(OUT_XLSX)
    if editor and '--force' not in sys.argv:
        sys.exit('%s was saved by %s after this script wrote it; regenerating would lose those edits. '
                 'Merge them into the script, or pass --force.' % (OUT_XLSX, editor))
    dump = json.load(open(DUMP, encoding='utf-8'))
    sections = build(dump)
    write_md(sections)
    write_xlsx(sections)
    print('written', OUT_MD, 'and', OUT_XLSX, '|', ', '.join('%s %d' % (s.sheet, len(s.rows)) for s in sections))
    if '--soffice' in sys.argv:
        total, errors = recalc(sys.argv[sys.argv.index('--soffice') + 1])
        print('LibreOffice recalculated %d formulas, errors: %d' % (total, len(errors)))
        for e in errors[:20]:
            print('  ', e)
        if errors:
            sys.exit(1)


if __name__ == '__main__':
    main()
