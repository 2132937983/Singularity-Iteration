#!/usr/bin/env python3
"""0.1.8.5 language keys (en_us / zh_cn / ja_jp): nano suit units, Universal Suit Modification
Station, nano core HUD and panel, burst keys, sounds, effect and the NanoSuit common config.

The script is idempotent and keeps the line endings of each file.
Usage: lang_185.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
C = 'mio_icif.configuration.'
M = 'module.mio_icif.'
S = 'gui.mio_icif.station.'
E = 'gui.mio_icif.equipment_console.'

UNITS = {
    'myofiber': (
        ('Nano Myofiber', '纳米肌纤维', 'ナノ筋繊維'),
        ('Artificial muscle fibre woven into the leggings. Sprint +15 %, steps up one block. Burst: Kinetic Dash, an 8-block horizontal dash (no fall damage). '
         'With an active nanosaber in hand: HF sync, attack speed +12 % and light armour penetration.',
         '织入护腿的人造肌纤维。疾跑 +15%，可直接迈上一格台阶。爆发：动能冲刺，约 8 格的水平冲刺（冲刺中无摔落伤害）。'
         '主手持开启的纳米剑时：高频同步，攻击速度 +12%，并带轻度护甲穿透。',
         'レギンスに織り込んだ人工筋繊維。疾走 +15%、段差 1 ブロックをそのまま登る。バースト：キネティックダッシュ（約 8 ブロックの水平突進、突進中は落下ダメージなし）。'
         '起動したナノセイバーを手に持つと HF 同期：攻撃速度 +12%、軽い防具貫通。')),
    'thermoregulator': (
        ('Thermoregulating Membrane', '热调节膜', '熱調整膜'),
        ('A heat-pump membrane under the chest plates. No freezing (powder snow, cold), fire damage -50 %, lava -30 %. The nano core sheds heat twice as fast.',
         '胸甲下的热泵膜。不会冻结（细雪、寒冷），火焰伤害 -50%，熔岩 -30%。纳米核心散热速度翻倍。',
         '胸部装甲の下の熱ポンプ膜。凍結しない（粉雪・寒冷）。炎のダメージ -50%、溶岩 -30%。ナノコアの放熱が 2 倍になる。')),
    'emp_emitter': (
        ('EMP Emitter', 'EMP 发射器', 'EMP 放射器'),
        ('Burst: an electromagnetic pulse of 6 blocks. Erases hostile projectiles in flight, lets creeper fuses fall back, stuns mechanical creatures for 3 s '
         'and breaks the guidance locks on you. Ends your optical camouflage. Breaks no blocks.',
         '爆发：半径 6 格的电磁脉冲。抹除飞行中的敌方弹体，让苦力怕的引信回落，使机械类生物眩晕 3 秒，并解除锁定你的制导。会结束你自己的光学迷彩。不破坏方块。',
         'バースト：半径 6 ブロックの電磁パルス。飛行中の敵の弾を消し、クリーパーの導火を戻し、機械系の生物を 3 秒気絶させ、あなたへの誘導ロックを外す。'
         '自分の光学迷彩は解ける。ブロックは壊さない。')),
    'optical_camo': (
        ('Optical Camouflage', '光学迷彩', '光学迷彩'),
        ('Sustained burst: the nano coat bends light around you for up to 15 s (6 reserve per second). Creatures lose sight of you beyond 2.5 blocks. '
         'Attacking, being hit or an EMP ends it. Press the key again to switch it off.',
         '持续爆发：纳米涂层让光线绕过你，最长 15 秒（每秒消耗 6 储备）。2.5 格外的生物会失去你的踪迹。攻击、受击或 EMP 会使其结束。再按一次按键即可关闭。',
         '持続バースト：ナノコートが光を曲げ、最大 15 秒姿を消す（毎秒予備量 6）。2.5 ブロックより遠い生物はあなたを見失う。攻撃・被弾・EMP で解除。もう一度キーを押すとオフ。')),
    'toxin_filter': (
        ('Toxin & Radiation Filter', '毒素·辐射过滤器', '毒素・放射線フィルター'),
        ('Nano filter in the helmet intake. Poison and wither do not take hold; radiation -60 %. Fits nano and quantum helmets.',
         '头盔进气口中的纳米过滤器。中毒与凋零不会生效；辐射 -60%。纳米与量子头盔均可安装。',
         'ヘルメット吸気口のナノフィルター。毒とウィザーを受けない。放射線 -60%。ナノとクォンタムのヘルメットに付く。')),
    'micro_capacitor': (
        ('Micro-Capacitor Lattice', '微电容晶格', 'マイクロキャパシタ格子'),
        ('Capacitor cells grown into the nano lattice: +250 000 EU storage in this piece. Passive, no switch.',
         '生长在纳米晶格中的电容单元：本部件储能 +250 000 EU。被动生效，没有开关。',
         'ナノ格子に成長させたキャパシタセル：この部位の蓄電 +250,000 EU。常時有効でスイッチはない。')),
}

OTHER = {
    # station
    'block.mio_icif.producer.block_quantum_modification_station': ('Universal Suit Modification Station', '通用套装改装台', '汎用スーツ改造台'),
    M + 'tooltip.station': ('Install with the Universal Suit Modification Station', '在通用套装改装台中安装', '汎用スーツ改造台で取り付ける'),
    M + 'tooltip.visor': ('Needs a suit visor to show data (quantum helmet; the nano helmet for nano-ready units)',
                          '需要套装面罩才能显示数据（量子头盔；可装于纳米的单元也可用纳米头盔）',
                          'データ表示にはスーツのバイザーが必要（量子ヘルメット。ナノ対応ユニットはナノヘルメットでも可）'),
    M + 'tooltip.class.quantum': ('Quantum suit only', '仅限量子套装', '量子スーツ専用'),
    M + 'tooltip.class.nano': ('Nano suit only: %s LP', '仅限纳米套装：%s LP', 'ナノスーツ専用：%s LP'),
    M + 'tooltip.class.shared': ('Quantum suit (1 slot) or nano suit (%s LP)', '量子套装（1 槽）或纳米套装（%s LP）', '量子スーツ（1 枠）またはナノスーツ（%s LP）'),
    M + 'tooltip.burst.single': ('Burst: %s reserve, +%s heat, cooldown %s s', '爆发：储备 %s，热量 +%s，冷却 %s 秒', 'バースト：予備量 %s、熱 +%s、クールダウン %s 秒'),
    M + 'tooltip.burst.sustained': ('Sustained burst: %s reserve per second, +%s heat, cooldown %s s', '持续爆发：每秒储备 %s，热量 +%s，冷却 %s 秒',
                                    '持続バースト：毎秒予備量 %s、熱 +%s、クールダウン %s 秒'),
    M + 'tooltip.hf_sync': ('HF sync with an active nanosaber: attack speed and armour penetration', '与开启的纳米剑高频同步：攻击速度与护甲穿透',
                            '起動したナノセイバーと HF 同期：攻撃速度と防具貫通'),
    'item.mio_icif.module.item_nano_lattice_densifier': ('Nano Lattice Densifier', '纳米晶格致密剂', 'ナノ格子高密度化材'),
    'item.mio_icif.module.item_nano_lattice_densifier.desc': ('Packs the nano lattice of a nano suit piece tighter: +%s LP, at most %s per piece.',
                                                             '使纳米套装部件的纳米晶格更致密：+%s LP，每件最多 %s 次。',
                                                             'ナノスーツ部位のナノ格子を高密度化する：+%s LP、1 部位につき最大 %s 回。'),
    S + 'lattice': ('LATTICE %s/%s LP', '晶格 %s/%s LP', '格子 %s/%s LP'),
    S + 'lp_cost': ('Lattice: %s LP', '晶格：%s LP', '格子：%s LP'),
    S + 'insert_piece': ('Put a quantum or nano suit piece in the top slot.', '在上方槽位放入量子或纳米套装部件。', '上のスロットに量子またはナノスーツの部位を入れる。'),
    S + 'no_units_nano': ('No unit installed. Put a nano unit or a lattice densifier in the lower slot.', '尚未安装单元。在下方槽位放入纳米单元或晶格致密剂。',
                          'ユニットなし。下のスロットにナノユニットか格子高密度化材を入れる。'),
    S + 'status.not_a_suit_piece': ('NOT A SUIT PIECE', '不是套装部件', 'スーツ部位ではない'),
    S + 'status.wrong_class': ('UNIT DOES NOT FIT THIS SUIT CLASS', '该单元不适用于此套装类别', 'このスーツ種別には付かない'),
    S + 'status.no_lattice': ('NOT ENOUGH LATTICE POINTS', '晶格点数不足', '格子ポイント不足'),
    S + 'status.lattice_max': ('LATTICE FULLY DENSIFIED', '晶格已完全致密化', '格子は最大まで高密度化済み'),
    # effect, keys, HUD
    'effect.mio_icif.emp_stun': ('EMP Stun', 'EMP 眩晕', 'EMP 気絶'),
    'key.mio_icif.nano_burst_primary': ('Nano burst (primary)', '纳米爆发（主）', 'ナノバースト（メイン）'),
    'key.mio_icif.nano_burst_secondary': ('Nano burst (secondary)', '纳米爆发（副）', 'ナノバースト（サブ）'),
    'hud.mio_icif.nano.no_core': ('No nano core: wear a nano suit piece with a burst unit', '没有纳米核心：请穿戴装有爆发单元的纳米套装部件',
                                  'ナノコアなし：バーストユニット付きのナノスーツ部位を着用する'),
    # panel
    E + 'nano': ('Nano lattice', '纳米晶格', 'ナノ格子'),
    E + 'lattice': ('Lattice %s/%s LP', '晶格 %s/%s LP', '格子 %s/%s LP'),
    E + 'nano_offline': ('Nano core offline', '纳米核心离线', 'ナノコア停止中'),
    E + 'reserve': ('Reserve', '储备', '予備量'),
    E + 'heat': ('Heat', '热量', '熱'),
    E + 'burst_primary': ('Burst [%s]', '爆发 [%s]', 'バースト [%s]'),
    E + 'burst_secondary': ('Burst [%s]', '爆发 [%s]', 'バースト [%s]'),
    E + 'burst_none': ('none', '无', 'なし'),
    E + 'burst_auto': ('(auto)', '（自动）', '（自動）'),
    # panel strings that had no Japanese text yet (the panel now shows the nano rows next to them)
    E + 'nothing': ('Select an equipped item', '请选择一件装备', '装備を選択する'),
    E + 'allocation': ('Energy allocation', '能量分配', 'エネルギー配分'),
    E + 'no_features': ('No configurable features', '没有可配置的特性', '設定できる機能はない'),
    E + 'modes': ('Operating modes', '工作模式', '動作モード'),
    E + 'click_select': ('Click to select', '点击选择', 'クリックで選択'),
    # sounds
    'mio_icif.subtitle.nano.dash': ('Kinetic dash', '动能冲刺', 'キネティックダッシュ'),
    'mio_icif.subtitle.nano.emp': ('EMP pulse', 'EMP 脉冲', 'EMP パルス'),
    'mio_icif.subtitle.nano.camo_on': ('Optical camouflage engages', '光学迷彩启动', '光学迷彩 起動'),
    'mio_icif.subtitle.nano.camo_off': ('Optical camouflage drops', '光学迷彩解除', '光学迷彩 解除'),
    'mio_icif.subtitle.nano.overheat': ('Nano suit overheats', '纳米套装过热', 'ナノスーツ過熱'),
    'mio_icif.subtitle.nano.denied': ('Burst refused', '爆发被拒绝', 'バースト不可'),
    'mio_icif.subtitle.nano.hf_sync': ('Blade HF sync', '刀刃高频同步', 'ブレード HF 同期'),
    # config sections
    C + 'NanoSuit': ('Nano Suit Units', '纳米套装单元', 'ナノスーツユニット'),
    C + 'nano.section.units': ('Unit switches', '单元开关', 'ユニットのスイッチ'),
    C + 'nano.section.pvp': ('PvP effects', 'PvP 效果', 'PvP 効果'),
    C + 'nano.section.core': ('Nano core', '纳米核心', 'ナノコア'),
    C + 'nano.section.tuning': ('Unit numbers', '单元数值', 'ユニットの数値'),
    C + 'nano.section.bursts': ('Burst costs', '爆发消耗', 'バーストのコスト'),
    C + 'nano.pvp.emp': ('EMP affects players', 'EMP 影响玩家', 'EMP がプレイヤーに効く'),
    C + 'nano.pvp.emp.tooltip': ('Erases projectiles of players, cancels their camouflage and scrambles their guidance.',
                                 '抹除玩家的弹体，解除其迷彩并干扰其制导。', 'プレイヤーの弾を消し、迷彩を解除し、誘導を乱す。'),
    C + 'nano.pvp.camo': ('Camouflage hides from players', '迷彩对玩家隐身', '迷彩がプレイヤーから隠す'),
    C + 'nano.pvp.camo.tooltip': ('Off: only creatures lose sight of the wearer; players see a shimmer.', '关闭时：只有生物失去目标；玩家看到微光。',
                                  'オフ：生物だけが見失い、プレイヤーには揺らぎが見える。'),
    C + 'nano.pvp.hf': ('HF sync pierces players', '高频同步穿透玩家护甲', 'HF 同期がプレイヤーの防具を貫通'),
    C + 'nano.pvp.hf.tooltip': ('Armour penetration of the HF sync also applies to players.', '高频同步的护甲穿透也作用于玩家。', 'HF 同期の防具貫通をプレイヤーにも適用する。'),
    C + 'nano.reserve_regen': ('Reserve regeneration (per second)', '储备回复（每秒）', '予備量の回復（毎秒）'),
    C + 'nano.reserve_regen.tooltip': ('Reserve points regained per second while a nano piece is worn.', '穿戴纳米部件时每秒回复的储备点数。',
                                       'ナノ部位の着用中に毎秒回復する予備量。'),
    C + 'nano.eu_per_reserve': ('EU per reserve point', '每点储备的 EU', '予備量 1 あたりの EU'),
    C + 'nano.eu_per_reserve.tooltip': ('Paid from the suit energy pool.', '从套装能量池支付。', 'スーツの共有電力から払う。'),
    C + 'nano.heat_dissipation': ('Heat dissipation (per second)', '散热（每秒）', '放熱（毎秒）'),
    C + 'nano.heat_dissipation.tooltip': ('Doubled by the thermoregulating membrane.', '热调节膜使其翻倍。', '熱調整膜で 2 倍になる。'),
    C + 'nano.overheat_resume': ('Overheat unlock below', '过热解锁阈值', '過熱解除のしきい値'),
    C + 'nano.overheat_resume.tooltip': ('After an overheat, bursts unlock below this heat.', '过热后热量低于此值时爆发解锁。', '過熱後、熱がこの値を下回るとバーストが使える。'),
    C + 'nano.capacitor': ('Micro-capacitor bonus (EU)', '微电容加成（EU）', 'マイクロキャパシタの追加容量（EU）'),
    C + 'nano.capacitor.tooltip': ('EU storage that the micro-capacitor lattice adds to its piece.', '微电容晶格为所在部件增加的储能。', 'マイクロキャパシタ格子が部位に足す蓄電量。'),
    C + 'nano.sprint': ('Myofiber sprint bonus', '肌纤维疾跑加成', '筋繊維の疾走ボーナス'),
    C + 'nano.sprint.tooltip': ('0.15 = +15 %.', '0.15 = +15%。', '0.15 = +15%。'),
    C + 'nano.dash_distance': ('Kinetic Dash distance', '动能冲刺距离', 'キネティックダッシュの距離'),
    C + 'nano.dash_distance.tooltip': ('Blocks.', '格。', 'ブロック。'),
    C + 'nano.hf_speed': ('HF sync attack speed', '高频同步攻击速度', 'HF 同期の攻撃速度'),
    C + 'nano.hf_speed.tooltip': ('0.12 = +12 %.', '0.12 = +12%。', '0.12 = +12%。'),
    C + 'nano.hf_pen': ('HF sync armour penetration', '高频同步护甲穿透', 'HF 同期の防具貫通'),
    C + 'nano.hf_pen.tooltip': ('Share of the armour reduction that is ignored (0.15 = 15 %).', '被忽略的护甲减伤比例（0.15 = 15%）。', '無視する防具軽減の割合（0.15 = 15%）。'),
    C + 'nano.emp_radius': ('EMP radius', 'EMP 半径', 'EMP 半径'),
    C + 'nano.emp_radius.tooltip': ('Blocks.', '格。', 'ブロック。'),
    C + 'nano.emp_stun': ('EMP stun (ticks)', 'EMP 眩晕（tick）', 'EMP 気絶（tick）'),
    C + 'nano.emp_stun.tooltip': ('Stun of mechanical creatures and creepers.', '机械类生物与苦力怕的眩晕时间。', '機械系の生物とクリーパーの気絶時間。'),
    C + 'nano.camo_max': ('Camouflage limit (s)', '迷彩上限（秒）', '迷彩の上限（秒）'),
    C + 'nano.camo_max.tooltip': ('Longest optical camouflage run.', '光学迷彩单次最长持续时间。', '光学迷彩 1 回の最長時間。'),
}

BURSTS = {
    'kineticDash': ('Kinetic Dash', '动能冲刺', 'キネティックダッシュ'),
    'emp': ('EMP', 'EMP', 'EMP'),
    'opticalCamo': ('Optical Camouflage', '光学迷彩', '光学迷彩'),
}
BURST_FIELDS = {
    'reserve': (('%s: reserve', '%s：储备', '%s：予備量'), ('Reserve points per use (camouflage: per second).', '每次使用的储备（迷彩为每秒）。', '1 回あたりの予備量（迷彩は毎秒）。')),
    'heat': (('%s: heat', '%s：热量', '%s：熱'), ('Heat added per use.', '每次使用增加的热量。', '1 回あたりに加わる熱。')),
    'cooldown': (('%s: cooldown (ticks)', '%s：冷却（tick）', '%s：クールダウン（tick）'), ('Ticks before the next use.', '下次使用前的 tick 数。', '次に使えるまでの tick。')),
}


def main():
    col = {'en_us': 0, 'zh_cn': 1, 'ja_jp': 2}
    for lang, i in col.items():
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        count = 0
        for uid, (name, desc) in UNITS.items():
            data['item.mio_icif.module.item_module_' + uid] = name[i]
            data[M + uid] = name[i]
            data[M + uid + '.desc'] = desc[i]
            data[C + 'nano.unit.' + uid] = name[i]
            data[C + 'nano.unit.' + uid + '.tooltip'] = ('Switch: ', '开关：', 'スイッチ：')[i] + name[i]
            count += 5
        for key, t in OTHER.items():
            data[key] = t[i]
            count += 1
        for bid, bname in BURSTS.items():
            for field, (label, tip) in BURST_FIELDS.items():
                data[C + 'nano.burst.' + bid + '.' + field] = label[i] % bname[i]
                data[C + 'nano.burst.' + bid + '.' + field + '.tooltip'] = tip[i]
                count += 2
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', count)


if __name__ == '__main__':
    main()
