#!/usr/bin/env python3
"""
0.1.7.33: generates the GuideME manual (en_us default, _zh_cn, _ja_jp) from
  tools/guide_data/*.json   - function / procedure / notes text (ASD-STE100 rules, see ste_lint.py)
  si_specs.json             - measured power data of every energy block (Round33GameTests#dumpMachineSpecs)
  lang files                - block names

Usage: gen_guide.py <repo root> <si_specs.json>
"""
import glob, json, os, re, shutil, sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
SPECS = json.load(open(sys.argv[2], encoding='utf-8')) if len(sys.argv) > 2 else {}
RES = os.path.join(ROOT, 'src/main/resources/assets/mio_icif')
OUT = os.path.join(RES, 'guides/mio_icif/manual')
LANGS = {'en': ('', 'en_us'), 'zh': ('_zh_cn', 'zh_cn'), 'ja': ('_ja_jp', 'ja_jp')}

NAMES = {l: json.load(open(os.path.join(RES, 'lang', f + '.json'), encoding='utf-8')) for l, (_, f) in LANGS.items()}

DATA = {}
for path in sorted(glob.glob(os.path.join(ROOT, 'tools/guide_data/*.json'))):
    if os.path.basename(path).startswith('_') or os.path.basename(path) in ('suit.json', 'si_specs.json'):
        continue
    for k, v in json.load(open(path, encoding='utf-8')).items():
        if not k.startswith('_'):
            DATA[k] = v
UNITS = {k: v for k, v in json.load(open(os.path.join(ROOT, 'tools/guide_data/suit.json'), encoding='utf-8')).items() if not k.startswith('_')}

T = {
    'function': {'en': 'Function', 'zh': '功能', 'ja': '機能'},
    'power': {'en': 'Power data', 'zh': '电力数据', 'ja': '電力データ'},
    'procedure': {'en': 'Procedure', 'zh': '使用步骤', 'ja': '使用手順'},
    'notes': {'en': 'Notes', 'zh': '注意', 'ja': '注意'},
    'recipe': {'en': 'Recipe', 'zh': '配方', 'ja': 'レシピ'},
    'item': {'en': 'Item', 'zh': '项目', 'ja': '項目'},
    'value': {'en': 'Value', 'zh': '数值', 'ja': '値'},
    'rated': {'en': 'Rated voltage tier', 'zh': '额定电压等级', 'ja': '定格電圧Tier'},
    'input_tier': {'en': 'Input voltage tier', 'zh': '输入电压等级', 'ja': '入力電圧Tier'},
    'max_input': {'en': 'Maximum input', 'zh': '最大输入', 'ja': '最大入力'},
    'storage': {'en': 'Energy storage', 'zh': '储能', 'ja': '蓄電容量'},
    'use': {'en': 'Use while working', 'zh': '工作耗电', 'ja': '稼働時消費'},
    'time': {'en': 'Operation time', 'zh': '单次工作时间', 'ja': '1回の処理時間'},
    'per_op': {'en': 'Energy per operation', 'zh': '单次耗电', 'ja': '1回あたりの電力'},
    'output_tier': {'en': 'Output voltage tier', 'zh': '输出电压等级', 'ja': '出力電圧Tier'},
    'output': {'en': 'Output', 'zh': '输出', 'ja': '出力'},
    'no_limit': {'en': 'No limit', 'zh': '无上限', 'ja': '上限なし'},
    'no_power': {'en': 'This block uses no EU.', 'zh': '该方块不使用EU。', 'ja': 'このブロックはEUを使わない。'},
    'measured': {'en': 'The game measured these values from the block entity of this version.',
                 'zh': '这些数值由本版本的方块实体实测得出。',
                 'ja': 'この値は本バージョンのブロックエンティティから計測した。'},
    'slots': {'en': 'Fits', 'zh': '适用部位', 'ja': '装着部位'},
    'drain': {'en': 'Power', 'zh': '功耗', 'ja': '消費電力'},
    'visor': {'en': 'Display', 'zh': '显示', 'ja': '表示'},
    'visor_yes': {'en': 'Needs a quantum helmet visor', 'zh': '需要量子头盔面罩', 'ja': '量子ヘルメットのバイザーが必要'},
    'visor_no': {'en': 'No visor needed', 'zh': '不需要面罩', 'ja': 'バイザー不要'},
    'per_hit': {'en': '4000 EU per damage point, max 12 points per hit', 'zh': '每点伤害4000 EU，每次最多12点', 'ja': 'ダメージ1点につき4000 EU、1回最大12点'},
    'while_on': {'en': 'EU/t while on', 'zh': 'EU/t（开启时）', 'ja': 'EU/t（オン時）'},
    'warning': {'en': 'WARNING:', 'zh': '警告：', 'ja': '警告：'},
}
SLOT = {'head': {'en': 'Helmet', 'zh': '头盔', 'ja': 'ヘルメット'}, 'chest': {'en': 'Chestplate', 'zh': '胸甲', 'ja': 'チェストプレート'},
        'legs': {'en': 'Leggings', 'zh': '护腿', 'ja': 'レギンス'}, 'feet': {'en': 'Boots', 'zh': '靴子', 'ja': 'ブーツ'}}
TIERS = ['ULV', 'LV', 'MV', 'HV', 'EV', 'IV', 'LuV', 'ZPM', 'UV', 'UHV', 'UEV', 'UIV', 'UMV', 'UXV', 'MAX']

CATEGORIES = [
    ('generators', {'en': 'Generators', 'zh': '发电机', 'ja': '発電機'},
     {'en': ['Generators make EU from fuel, sunlight, wind, water, heat or kinetic energy.', 'Each page gives the output voltage tier and the output in EU/t.'],
      'zh': ['发电机用燃料、阳光、风、水、热能或动能产生EU。', '每页给出输出电压等级与输出EU/t。'],
      'ja': ['発電機は燃料・日光・風・水・熱・運動エネルギーからEUを作る。', '各ページに出力電圧Tierと出力EU/tを示す。']},
     ('generator/', 'hugenerator/', 'kugenerator/'), 'mio_icif:generator/block_geo_generator'),
    ('machines', {'en': 'Machines', 'zh': '机器', 'ja': '機械'},
     {'en': ['Machines use EU to process items, fluids and blocks.', 'Each page gives the input voltage tier, the use while working and the operation time.'],
      'zh': ['机器消耗EU加工物品、流体与方块。', '每页给出输入电压等级、工作耗电与单次工作时间。'],
      'ja': ['機械はEUを使ってアイテム・流体・ブロックを加工する。', '各ページに入力電圧Tier・稼働時消費・処理時間を示す。']},
     ('producer/', 'produce/', 'checker/', 'energy_converter/'), 'mio_icif:producer/block_powder_elc'),
    ('power', {'en': 'Power transmission and storage', 'zh': '输电与储电', 'ja': '送電と蓄電'},
     {'en': ['Cables move EU between blocks. Transformers change the voltage tier.', 'Storage blocks keep EU and charge items.'],
      'zh': ['导线在方块之间传送EU。变压器改变电压等级。', '储电方块储存EU并给物品充电。'],
      'ja': ['ケーブルはブロック間でEUを運ぶ。変圧器は電圧Tierを変える。', '蓄電ブロックはEUを貯め、アイテムを充電する。']},
     ('wiring/',), 'mio_icif:wiring/block_mfe'),
    ('heavy', {'en': 'Reactors, oil rigs and pipes', 'zh': '反应堆、油井与管道', 'ja': '原子炉・油井・パイプ'},
     {'en': ['These pages describe large structures and the pipes that feed them.', 'Read every WARNING line before you build a reactor.'],
      'zh': ['这些页面说明大型结构及为其供料的管道。', '建造反应堆前阅读每条警告。'],
      'ja': ['これらのページは大型構造物と、それに供給するパイプを説明する。', '原子炉を作る前に全ての警告を読む。']},
     ('reactor/', 'oilrig/', 'pipe/'), 'mio_icif:reactor/block_reactor_vessel'),
]


def name(lang, block_id):
    key = 'block.mio_icif.' + block_id.replace('/', '.')
    for l in (lang, 'en'):
        if key in NAMES[l]:
            return NAMES[l][key]
    return block_id.split('/')[-1]


def fmt(n):
    return '{:,}'.format(int(n))


def tier_name(t):
    if t is None or t < 0:
        return '-'
    if t >= len(TIERS) - 1:
        return 'MAX'
    return TIERS[t]


def packet(t):
    return 8 * 4 ** t


CABLE = {'ulv': 0, 'lv': 1, 'mv': 2, 'hv': 3, 'ev': 4, 'iv': 5, 'luv': 6, 'zpm': 7, 'uv': 8}


def spec_rows(lang, spec, kind=None):
    rows = []
    big = 2 ** 31 - 2
    if kind == 'cable':
        c = spec.get('cable', '')
        if c in CABLE:
            rows.append((T['rated'][lang], '%s (%s EU)' % (TIERS[CABLE[c]], fmt(packet(CABLE[c])))))
        else:
            rows.append((T['rated'][lang], T['no_limit'][lang]))
        return rows
    if spec.get('source'):
        st = spec.get('sourceTier', -1)
        if st >= 0:
            rows.append((T['output_tier'][lang], '%s (%s EU)' % (tier_name(st), fmt(packet(min(st, 14))))))
        out = spec.get('powerOutput', 0) or spec.get('maxExtract', 0)
        if out > 0:
            rows.append((T['output'][lang], fmt(out) + ' EU/t'))
    else:
        st = spec.get('sinkTier', -1)
        if st >= big or spec.get('maxReceive', 0) >= 2 ** 62:
            rows.append((T['input_tier'][lang], T['no_limit'][lang]))
        elif st >= 0 and spec.get('maxReceive', 0) > 0:
            rows.append((T['input_tier'][lang], '%s (%s EU)' % (tier_name(st), fmt(packet(min(st, 14))))))
        mr = spec.get('maxReceive', 0)
        if 0 < mr < 2 ** 62:
            rows.append((T['max_input'][lang], fmt(mr) + ' EU/t'))
    cap = spec.get('capacity', 0)
    if cap > 0:
        rows.append((T['storage'][lang], fmt(cap) + ' EU'))
    pt, ticks = spec.get('perTick', -1), spec.get('ticks', -1)
    if pt > 0 and not spec.get('source') and spec.get('maxReceive', 0) < 2 ** 62:
        rows.append((T['use'][lang], fmt(pt) + ' EU/t'))
        if ticks > 1:
            secs = ticks / 20
            rows.append((T['time'][lang], '%s tick (%s s)' % (fmt(ticks), ('%.1f' % secs).rstrip('0').rstrip('.'))))
            rows.append((T['per_op'][lang], fmt(pt * ticks) + ' EU'))
    if spec.get('source') and spec.get('maxReceive', 0) > 0 and spec.get('sinkTier', -1) >= 0:
        st = spec['sinkTier']
        if st < big:
            rows.append((T['input_tier'][lang], '%s (%s EU)' % (tier_name(st), fmt(packet(min(st, 14))))))
    return rows


def table(lang, rows):
    out = ['| %s | %s |' % (T['item'][lang], T['value'][lang]), '|---|---|']
    out += ['| %s | %s |' % r for r in rows]
    return out


def numbered(lines):
    return ['%d. %s' % (i + 1, l) for i, l in enumerate(lines)]


def notes(lang, lines):
    out = []
    for l in lines:
        if l.startswith(('WARNING:', '警告：', '警告:')):
            body = re.sub(r'^(WARNING:|警告：|警告:)\s*', '', l)
            out += ['', '> **%s** %s' % (T['warning'][lang], body), '']
        else:
            out.append('- ' + l)
    return out


def frontmatter(title, parent=None, position=None, icon=None, item_ids=None):
    fm = ['---', 'navigation:', '  title: "%s"' % title.replace('"', "'")]
    if icon:
        fm.append('  icon: %s' % icon)
    if parent:
        fm.append('  parent: %s' % parent)
    if position is not None:
        fm.append('  position: %d' % position)
    if item_ids:
        fm.append('item_ids:')
        fm += ['  - %s' % i for i in item_ids]
    fm.append('---')
    return fm


def write(lang, rel, lines):
    sub = LANGS[lang][0]
    path = os.path.join(OUT, sub, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines).rstrip() + '\n')


def category_of(block_id):
    for cat, _, _, prefixes, _ in CATEGORIES:
        if block_id.startswith(prefixes):
            return cat
    return 'machines'


def machine_page(lang, block_id, parent, position):
    title = name(lang, block_id)
    entry = DATA.get(block_id, {})
    spec = SPECS.get(block_id)
    lines = frontmatter(title, parent, position, 'mio_icif:' + block_id, ['mio_icif:' + block_id])
    lines += ['', '# ' + title, '', '<Row>', '  <BlockImage id="mio_icif:%s" scale="3" />' % block_id, '</Row>', '']
    fn = entry.get('function', {}).get(lang)
    if fn:
        lines += ['## ' + T['function'][lang], ''] + fn + ['']
    lines += ['## ' + T['power'][lang], '']
    rows = spec_rows(lang, spec, entry.get('kind')) if spec else []
    if rows:
        lines += table(lang, rows) + ['', T['measured'][lang], '']
    else:
        lines += [T['no_power'][lang], '']
    usage = entry.get('usage', {}).get(lang)
    if usage:
        lines += ['## ' + T['procedure'][lang], ''] + numbered(usage) + ['']
    nt = entry.get('notes', {}).get(lang)
    if nt:
        lines += ['## ' + T['notes'][lang], ''] + notes(lang, nt) + ['']
    lines += ['## ' + T['recipe'][lang], '', '<RecipesFor id="mio_icif:%s" fallbackText="-" />' % block_id]
    return lines


# ------------------------------------------------------------------ hand-written pages (STE)

INDEX = {
    'en': ['Singularity Iteration Manual', [
        'This manual describes the machines, the power system and the quantum suit of Singularity Iteration.',
        'Each machine page gives the function, the measured power data, a procedure and the recipe.',
        'The text follows the ASD-STE100 rules: short sentences, active voice and one word for one meaning.']],
    'zh': ['奇点迭代手册', [
        '本手册说明奇点迭代的机器、电力系统与量子套装。',
        '每个机器页面给出功能、实测电力数据、使用步骤与配方。',
        '本文遵循ASD-STE100规则：短句、主动语态、一词一义。']],
    'ja': ['シンギュラリティ・イテレーション マニュアル', [
        'このマニュアルはシンギュラリティ・イテレーションの機械・電力システム・量子スーツを説明する。',
        '各機械のページに機能・実測電力データ・使用手順・レシピを示す。',
        '本文はASD-STE100の規則に従う。短い文、能動態、一語一義を使う。']],
}

CONVENTIONS = {
    'en': ['Units and voltage tiers', [
        'EU is the unit of electrical energy. EU/t is EU per game tick. One second has 20 ticks.',
        'A cable carries EU in packets. The voltage tier limits the size of one packet.',
        'A machine explodes when it receives a packet above its input voltage tier.',
        'Use a transformer to change the voltage tier between a source and a machine.'],
        ['Voltage tier', 'Maximum packet']],
    'zh': ['单位与电压等级', [
        'EU是电能单位。EU/t表示每游戏tick的EU。一秒有20 tick。',
        '导线以电力包传送EU。电压等级限制单个电力包的大小。',
        '机器收到超过其输入电压等级的电力包时会爆炸。',
        '用变压器在电源与机器之间改变电压等级。'],
        ['电压等级', '最大电力包']],
    'ja': ['単位と電圧Tier', [
        'EUは電気エネルギーの単位だ。EU/tはゲーム1 tickあたりのEUを示す。1秒は20 tickだ。',
        'ケーブルはEUをパケット単位で運ぶ。電圧Tierは1パケットの大きさを制限する。',
        '機械は入力電圧Tierを超えるパケットを受けると爆発する。',
        '電源と機械の間の電圧Tierを変えるには変圧器を使う。'],
        ['電圧Tier', '最大パケット']],
}

SUIT_INDEX = {
    'en': ['Quantum suit upgrades', [
        'The quantum suit accepts upgrade units. Each unit adds one sensor or one defense function.',
        'The quantum modification station installs and removes the units.',
        'Each piece has a fixed number of unit slots: helmet 4, chestplate 3, leggings 2, boots 2.',
        'A HUD unit shows its data only when you wear a quantum helmet. The helmet is the visor.',
        'Each installed unit adds a switch to the equipment console. A unit uses power only while its switch is on.'],
        ['Unit', 'Fits', 'Power']],
    'zh': ['量子套装升级', [
        '量子套装可以安装升级单元。每个单元增加一项传感或防御功能。',
        '量子改装台负责安装和拆卸单元。',
        '每个部件的单元位数量固定：头盔4，胸甲3，护腿2，靴子2。',
        'HUD单元只在穿戴量子头盔时显示数据。头盔就是面罩。',
        '每个已安装单元在装备控制台中增加一个开关。开关打开时单元才耗电。'],
        ['单元', '适用部位', '功耗']],
    'ja': ['量子スーツのアップグレード', [
        '量子スーツはアップグレードユニットを受け付ける。各ユニットがセンサーか防御の機能を1つ加える。',
        '量子改造ステーションがユニットを取り付け、取り外す。',
        '各部位のユニットスロット数は固定だ。ヘルメット4、チェストプレート3、レギンス2、ブーツ2。',
        'HUDユニットは量子ヘルメット着用時だけデータを表示する。ヘルメットがバイザーになる。',
        '取り付けた各ユニットは装備コンソールにスイッチを加える。スイッチがオンの間だけ電力を使う。'],
        ['ユニット', '装着部位', '消費電力']],
}

HUD_PAGE = {
    'en': ['FCS HUD', [
        ('Function', ['The FCS HUD shows the data of the active units in the quantum helmet visor.',
                      'The layout follows a tank gunner sight: a framed view, a heading tape and a range finder.',
                      'Data panels sit at the screen edges. Threat markers sit on the outer ring.']),
        ('Procedure: move the panels', ['Open the equipment console.', 'Click HUD layout.', 'Drag a panel to a new position.',
                                        'Scroll on the holomap to change its size.', 'Click Done.']),
        ('Controls', ['Press H to switch the HUD on or off.', 'Set the colors and the outlines in the mod configuration screen.',
                      'Bind a key to the layout editor in the controls screen.']),
        ('Read-outs', ['RNG gives the distance to the block or creature at the aim point.', 'EL gives the pitch of the view.',
                       'PWR gives the total EU/t of all active units. The bar shows the helmet charge.'])]],
    'zh': ['FCS HUD', [
        ('功能', ['FCS HUD在量子头盔面罩上显示已启用单元的数据。',
                  '布局仿照坦克炮手瞄准镜：带框视野、航向带与测距仪。',
                  '数据面板位于屏幕边缘。威胁标记位于外环。']),
        ('步骤：移动面板', ['打开装备控制台。', '点击HUD布局。', '把面板拖到新位置。', '在全息地图上滚动滚轮调整大小。', '点击完成。']),
        ('操作', ['按H开关HUD。', '在模组配置界面设定颜色与轮廓。', '在按键设置中给布局编辑器绑定按键。']),
        ('读数', ['RNG显示到瞄准点方块或生物的距离。', 'EL显示视线俯仰角。', 'PWR显示全部已启用单元的EU/t。进度条显示头盔电量。'])]],
    'ja': ['FCS HUD', [
        ('機能', ['FCS HUDは量子ヘルメットのバイザーに有効ユニットのデータを表示する。',
                  '配置は戦車砲手照準器にならう。フレーム付き視野・方位テープ・測距計を持つ。',
                  'データパネルは画面の端に並ぶ。脅威マーカーは外周に並ぶ。']),
        ('手順：パネルを動かす', ['装備コンソールを開く。', 'HUD配置をクリックする。', 'パネルを新しい位置へドラッグする。',
                                 'ホロマップ上でスクロールしてサイズを変える。', '完了をクリックする。']),
        ('操作', ['Hキーを押してHUDをオン・オフする。', '色と輪郭はMod設定画面で設定する。', 'レイアウト編集のキーは操作設定画面で割り当てる。']),
        ('表示の読み方', ['RNGは照準点のブロックか生物までの距離を示す。', 'ELは視線の仰角を示す。',
                          'PWRは有効ユニット全体のEU/tを示す。バーはヘルメットの残量を示す。'])]],
}

FLIGHT_PAGE = {
    'en': ['Viltrum flight', [
        ('Function', ['Viltrum flight is a free-flight mode of the quantum chestplate and the advanced quantum chestplate.',
                      'Thrust acts along the view axis. Speed builds up, and the flight path turns towards the view axis.',
                      'When you release the forward key, the suit glides and slows down.',
                      'Above 48 m/s the suit breaks the shock barrier with a boom and a vapor ring.']),
        ('Procedure', ['Open the equipment console.', 'Select the chestplate.', 'Switch on Viltrum flight.',
                       'Press jump two times to start the flight.', 'Hold forward to fly along the view axis.',
                       'Hold the boost key or the sprint key to boost.', 'Land on a block to stop the flight.']),
        ('Power data', ['The flight uses 6 EU/t plus 30 x speed x speed EU/t. Speed is in blocks per tick.',
                        'Cruise uses about 42 EU/t. Full boost uses about 313 EU/t.',
                        'A full quantum chestplate gives about 4 hours of cruise or 26 minutes of full boost.']),
        ('Notes', ['The switch starts off. The jetpack keeps its normal controls while the switch is off.',
                   'Set the camera roll and the speed field of view in the mod configuration screen.'])]],
    'zh': ['维尔特鲁姆飞行', [
        ('功能', ['维尔特鲁姆飞行是量子胸甲与高级量子胸甲的自由飞行模式。',
                  '推力沿视线方向作用。速度逐步增加，飞行路径转向视线方向。',
                  '松开前进键后，套装滑翔并减速。',
                  '速度超过48 m/s时，套装以音爆与蒸汽环突破激波屏障。']),
        ('使用步骤', ['打开装备控制台。', '选择胸甲。', '打开维尔特鲁姆飞行。', '连按两次跳跃开始飞行。',
                      '按住前进键沿视线方向飞行。', '按住加速键或疾跑键加速。', '落到方块上停止飞行。']),
        ('电力数据', ['飞行耗电为6 EU/t加30乘速度平方EU/t。速度单位为格每tick。',
                      '巡航约耗42 EU/t。全速加速约耗313 EU/t。',
                      '满电量子胸甲可巡航约4小时，或全速加速约26分钟。']),
        ('注意', ['开关默认关闭。开关关闭时喷气背包保持原有操作。', '在模组配置界面设定镜头倾斜与速度视野。'])]],
    'ja': ['ヴィルトラム式飛行', [
        ('機能', ['ヴィルトラム式飛行は量子チェストプレートと高度量子チェストプレートの自由飛行モードだ。',
                  '推力は視線方向に働く。速度は徐々に上がり、飛行経路は視線方向へ曲がる。',
                  '前進キーを離すと、スーツは滑空して減速する。',
                  '48 m/sを超えると、スーツは衝撃音と蒸気リングを伴って衝撃波の壁を越える。']),
        ('使用手順', ['装備コンソールを開く。', 'チェストプレートを選ぶ。', 'ヴィルトラム式飛行をオンにする。',
                      'ジャンプを2回押して飛行を始める。', '前進キーを押し続けて視線方向へ飛ぶ。',
                      'ブーストキーかダッシュキーを押し続けて加速する。', 'ブロックに着地して飛行を止める。']),
        ('電力データ', ['消費は6 EU/tに、30×速度×速度 EU/tを加えた値だ。速度の単位はブロック毎tickだ。',
                        '巡航は約42 EU/tを使う。最大ブーストは約313 EU/tを使う。',
                        '満充電の量子チェストプレートで、巡航は約4時間、最大ブーストは約26分続く。']),
        ('注意', ['スイッチの初期値はオフだ。オフの間、ジェットパックは通常の操作のまま動く。',
                  'カメラの傾きと速度視野はMod設定画面で設定する。'])]],
}


def unit_name(lang, uid):
    return NAMES[lang].get('module.mio_icif.' + uid) or NAMES['en'].get('module.mio_icif.' + uid, uid)


def main():
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    ids = sorted(set(list(SPECS.keys()) + list(DATA.keys())))
    station = 'producer/block_quantum_modification_station'
    for lang in LANGS:
        # index
        title, intro = INDEX[lang]
        lines = frontmatter(title, None, 0, 'mio_icif:producer/block_machine_hull_advanced') + ['', '# ' + title, ''] + intro + ['']
        lines += ['<SubPages id="index.md" icons={true} />']
        write(lang, 'index.md', lines)
        # conventions
        title, body, head = CONVENTIONS[lang]
        lines = frontmatter(title, 'index.md', 1, 'mio_icif:item_tool_meter') + ['', '# ' + title, ''] + body + ['']
        lines += ['| %s | %s |' % tuple(head), '|---|---|']
        for t in range(1, 8):
            lines.append('| %s | %s EU |' % (TIERS[t], fmt(packet(t))))
        write(lang, 'conventions.md', lines)
        # categories and machines
        for pos, (cat, ctitle, cintro, prefixes, icon) in enumerate(CATEGORIES):
            members = [i for i in ids if category_of(i) == cat and i != station]
            members.sort(key=lambda i: name('en', i))
            lines = frontmatter(ctitle[lang], 'index.md', 10 + pos, icon) + ['', '# ' + ctitle[lang], ''] + cintro[lang] + ['']
            lines += ['<SubPages id="%s.md" icons={true} alphabetical={true} />' % cat]
            write(lang, cat + '.md', lines)
            for n, i in enumerate(members):
                write(lang, 'machines/%s.md' % i.replace('/', '_'), machine_page(lang, i, cat + '.md', n))
        # suit
        title, body, head = SUIT_INDEX[lang]
        lines = frontmatter(title, 'index.md', 5, 'mio_icif:armor/item_armor_quantum_helmet') + ['', '# ' + title, ''] + body + ['']
        lines += ['| %s | %s | %s |' % tuple(head), '|---|---|---|']
        for uid, u in UNITS.items():
            slots = ' / '.join(SLOT[s][lang] for s in u['slots'])
            drain = T['per_hit'][lang] if uid == 'deflector' else '%d %s' % (u['drain'], T['while_on'][lang])
            lines.append('| <ItemLink id="mio_icif:module/item_module_%s" /> | %s | %s |' % (uid, slots, drain))
        lines += ['', '<SubPages id="suit/index.md" icons={true} />']
        write(lang, 'suit/index.md', lines)
        write(lang, 'machines/%s.md' % station.replace('/', '_'), machine_page(lang, station, 'suit/index.md', 0))
        for n, (uid, u) in enumerate(UNITS.items()):
            t = unit_name(lang, uid)
            item = 'mio_icif:module/item_module_' + uid
            lines = frontmatter(t, 'suit/index.md', 10 + n, item, [item]) + ['', '# ' + t, '', '<ItemImage id="%s" scale="3" />' % item, '']
            lines += ['## ' + T['function'][lang], ''] + u['function'][lang] + ['']
            slots = ' / '.join(SLOT[s][lang] for s in u['slots'])
            drain = T['per_hit'][lang] if uid == 'deflector' else '%d %s' % (u['drain'], T['while_on'][lang])
            vis = T['visor_no'][lang] if uid == 'deflector' else T['visor_yes'][lang]
            lines += ['## ' + T['power'][lang], ''] + table(lang, [(T['slots'][lang], slots), (T['drain'][lang], drain), (T['visor'][lang], vis)]) + ['']
            lines += ['## ' + T['procedure'][lang], ''] + numbered(u['usage'][lang]) + ['']
            if u.get('notes'):
                lines += ['## ' + T['notes'][lang], ''] + notes(lang, u['notes'][lang]) + ['']
            lines += ['## ' + T['recipe'][lang], '', '<RecipesFor id="%s" fallbackText="-" />' % item]
            write(lang, 'suit/units/%s.md' % uid, lines)
        for rel, page, pos, icon in [('suit/fcs_hud.md', HUD_PAGE, 2, 'mio_icif:module/item_module_entity_esp'),
                                     ('suit/viltrum_flight.md', FLIGHT_PAGE, 3, 'mio_icif:armor/item_armor_quantum_chestplate')]:
            title, sections = page[lang]
            lines = frontmatter(title, 'suit/index.md', pos, icon) + ['', '# ' + title, '']
            for head, body in sections:
                lines += ['## ' + head, '']
                lines += numbered(body) if head.startswith(('Procedure', '使用步骤', '使用手順', '步骤', '手順')) else body
                lines += ['']
            write(lang, rel, lines)
    count = sum(len(files) for _, _, files in os.walk(OUT))
    print('pages:', count, 'machines with data:', sum(1 for i in ids if i in DATA), 'of', len(ids))


if __name__ == '__main__':
    main()
