#!/usr/bin/env python3
"""0.1.8.6 language keys (en_us / zh_cn / ja_jp): the 11 phase-2 nano units, the Mk.II items and
their upgrade lines, the station upgrade statuses, the new config entries and sounds.
Idempotent; keeps the line endings of each file.
Usage: lang_186.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
C = 'mio_icif.configuration.'
M = 'module.mio_icif.'

UNITS = {
    'gecko_setae': (('Gecko Setae', '壁虎刚毛', '吸着セタ（ゲッコー）'),
        ('Microscopic setae on the boot soles. Sneak into a wall to climb it; sneak against a wall to slide down slowly. No slow-down on soul sand, honey or powder snow; grip on ice.',
         '靴底上的微观刚毛。潜行时贴墙向前即可攀爬，贴墙潜行时缓慢下滑。在灵魂沙、蜂蜜块与细雪上不减速；在冰上不打滑。',
         '靴底の微細なセタ。スニークで壁に向かって進むと登れ、壁沿いのスニークではゆっくり滑り降りる。ソウルサンド・ハチミツ・粉雪で減速せず、氷でも滑らない。')),
    'shock_gel': (('Shock Gel', '冲击凝胶', '衝撃ゲル'),
        ('Non-Newtonian gel in the boots. The fall protection of the nano boots costs 70 % less EU, and a jump right after a landing rebounds 50 % higher.',
         '靴中的非牛顿凝胶。纳米靴的摔落防护耗电减少 70%，落地后立即起跳可反弹，高度 +50%。',
         'ブーツ内の非ニュートン流体ゲル。ナノブーツの落下防護の電力が 70% 減り、着地直後のジャンプが 50% 高く跳ね返る。')),
    'heat_sink_fins': (('Heat-Sink Fins', '散热鳍片', '放熱フィン'),
        ('Fold-out fins on the chest or legs. Heat cap +50 per fin unit, and an overheat no longer slows you.',
         '胸甲或护腿上的折叠鳍片。每个鳍片单元使热量上限 +50，过热时不再减速。',
         '胸部か脚部の展開式フィン。フィン 1 つにつき熱上限 +50。過熱しても移動が遅くならない。')),
    'thermal_camo': (('Thermal Camouflage', '热迷彩', 'サーマル迷彩'),
        ('The coat cools to the ambient temperature. While sneaking, creatures notice you at half the distance.',
         '涂层冷却到环境温度。潜行时，生物发现你的距离减半。',
         'コートが周囲の温度まで冷える。スニーク中、生物に見つかる距離が半分になる。')),
    'jammer': (('Jammer', '干扰器', 'ジャマー'),
        ('Broadband jamming from the helmet. Hostile creatures beyond 12 blocks lose track of you, and guided weapons cannot lock you.',
         '头盔发出的宽带干扰。12 格外的敌对生物会失去你的踪迹，制导武器也无法锁定你。',
         'ヘルメットからの広帯域妨害。12 ブロックより遠い敵対生物はあなたを見失い、誘導兵器もロックできない。')),
    'recon_cloud': (('Nano Recon Cloud', '纳米侦察云', 'ナノ偵察雲'),
        ('Burst: a swarm of nano sensors spreads 24 blocks ahead. The creatures in it glow for you for 10 s, through walls, without a visor.',
         '爆发：纳米传感器群向前扩散 24 格。其中的生物对你发光 10 秒，可透墙，无需面罩。',
         'バースト：ナノセンサーの群れが前方 24 ブロックに広がる。中の生物があなたにだけ 10 秒間光って見える。壁越し、バイザー不要。')),
    'kinetic_mesh': (('Kinetic Armor Mesh', '动能装甲网', '運動装甲メッシュ'),
        ('Burst: the nano weave locks into hexagonal plates for 6 s and cuts a further 30 % of the damage (total absorption at most 95 %). The weave also syncs with an active nanosaber (HF sync).',
         '爆发：纳米织物锁合成六边形装甲板 6 秒，额外减伤 30%（总吸收上限 95%）。该织物也与开启的纳米剑高频同步。',
         'バースト：ナノの織り目が 6 秒間六角形の装甲板に固まり、ダメージをさらに 30% 減らす（吸収の合計上限 95%）。起動したナノセイバーとも HF 同期する。')),
    'self_repair': (('Self-Repair Coat', '自修复涂层', '自己修復コート'),
        ('After 5 s without damage, the nano coat mends you: 0.5 health every 3 s, 2 000 EU each. Fits nano and quantum pieces.',
         '5 秒未受伤后，纳米涂层为你修复：每 3 秒回复 0.5 生命，每次 2 000 EU。纳米与量子部件均可安装。',
         '5 秒間ダメージを受けないと、ナノコートが修復する：3 秒ごとに 0.5 回復、1 回 2,000 EU。ナノと量子の部位に付く。')),
    'hydrophobic': (('Hydrophobic Coat', '疏水涂层', '撥水コート'),
        ('Water beads off the coat: no water drag, swimming +30 %. Fits nano and quantum pieces.',
         '水珠从涂层上滑落：水中不减速，游泳速度 +30%。纳米与量子部件均可安装。',
         '水をはじくコート：水中で減速せず、泳ぐ速さ +30%。ナノと量子の部位に付く。')),
    'kinetic_generator': (('Kinetic Generator', '动能发电机', '運動発電'),
        ('Harvests the stride: 1 EU per block walked, 2 EU per block sprinted, into the suit. Works with an empty suit. Fits nano and quantum leggings.',
         '收集步伐的能量：每行走 1 格 1 EU，疾跑 2 EU，充入套装。套装没电时也能工作。纳米与量子护腿均可安装。',
         '歩みからエネルギーを得る：歩いて 1 ブロックごとに 1 EU、疾走で 2 EU をスーツに充電する。スーツが空でも動く。ナノと量子のレギンスに付く。')),
    'solar_skin': (('Solar Skin', '太阳能皮肤', 'ソーラースキン'),
        ('Photovoltaic coat: 2 EU/t under open daylight. Works with an empty suit.',
         '光伏涂层：在露天日光下 2 EU/t。套装没电时也能工作。',
         '光発電コート：屋外の日光の下で 2 EU/t。スーツが空でも動く。')),
}

MK2_NAMES = {
    'myofiber': ('Nano Myofiber', '纳米肌纤维', 'ナノ筋繊維'),
    'thermoregulator': ('Thermoregulating Membrane', '热调节膜', '熱調整膜'),
    'emp_emitter': ('EMP Emitter', 'EMP 发射器', 'EMP 放射器'),
    'optical_camo': ('Optical Camouflage', '光学迷彩', '光学迷彩'),
    'toxin_filter': ('Toxin & Radiation Filter', '毒素·辐射过滤器', '毒素・放射線フィルター'),
    'micro_capacitor': ('Micro-Capacitor Lattice', '微电容晶格', 'マイクロキャパシタ格子'),
}
for k, v in UNITS.items():
    MK2_NAMES[k] = v[0]

MK2_LINES = {
    'myofiber': ('Mk.II: sprint +22 %, dash 10 blocks, HF sync counts double.', 'Mk.II：疾跑 +22%，冲刺 10 格，高频同步计两级。', 'Mk.II：疾走 +22%、ダッシュ 10 ブロック、HF 同期は 2 段分。'),
    'thermoregulator': ('Mk.II: fire -70 %, lava -50 %, heat sheds x2.5.', 'Mk.II：火焰 -70%，熔岩 -50%，散热 ×2.5。', 'Mk.II：炎 -70%、溶岩 -50%、放熱 2.5 倍。'),
    'emp_emitter': ('Mk.II: radius 8 blocks, stun 4.5 s, a violet wave.', 'Mk.II：半径 8 格，眩晕 4.5 秒，紫色冲击波。', 'Mk.II：半径 8 ブロック、気絶 4.5 秒、紫の衝撃波。'),
    'optical_camo': ('Mk.II: up to 22 s, 4.5 reserve per second.', 'Mk.II：最长 22 秒，每秒储备 4.5。', 'Mk.II：最大 22 秒、毎秒予備量 4.5。'),
    'toxin_filter': ('Mk.II: radiation -85 %; hunger and nausea filtered too.', 'Mk.II：辐射 -85%；同时过滤饥饿与反胃。', 'Mk.II：放射線 -85%。空腹と吐き気も防ぐ。'),
    'micro_capacitor': ('Mk.II: +400 000 EU.', 'Mk.II：+400 000 EU。', 'Mk.II：+400,000 EU。'),
    'gecko_setae': ('Mk.II: climbs faster and clings to walls without sliding.', 'Mk.II：攀爬更快，贴墙不下滑。', 'Mk.II：速く登り、壁に張り付いて滑らない。'),
    'shock_gel': ('Mk.II: fall protection -85 % EU, rebound +80 %.', 'Mk.II：摔落防护耗电 -85%，反弹 +80%。', 'Mk.II：落下防護の電力 -85%、反発 +80%。'),
    'heat_sink_fins': ('Mk.II: heat cap +75, heat sheds 25 % faster.', 'Mk.II：热量上限 +75，散热加快 25%。', 'Mk.II：熱上限 +75、放熱 25% 速い。'),
    'thermal_camo': ('Mk.II: 30 % detection while sneaking, 75 % while walking.', 'Mk.II：潜行时被发现距离 30%，行走时 75%。', 'Mk.II：スニーク中は 30%、歩行中も 75% の距離でしか見つからない。'),
    'jammer': ('Mk.II: creatures lose track beyond 8 blocks.', 'Mk.II：8 格外的生物即失去踪迹。', 'Mk.II：8 ブロックより遠い生物が見失う。'),
    'recon_cloud': ('Mk.II: 32 blocks all around, glow for 15 s.', 'Mk.II：周围全向 32 格，发光 15 秒。', 'Mk.II：全周 32 ブロック、15 秒光る。'),
    'kinetic_mesh': ('Mk.II: 8 s, a further 40 % cut; HF sync counts double.', 'Mk.II：8 秒，额外减伤 40%；高频同步计两级。', 'Mk.II：8 秒、さらに 40% 軽減。HF 同期は 2 段分。'),
    'self_repair': ('Mk.II: starts after 4 s, 1.0 health every 2 s.', 'Mk.II：4 秒后开始，每 2 秒回复 1.0。', 'Mk.II：4 秒後から、2 秒ごとに 1.0 回復。'),
    'hydrophobic': ('Mk.II: swimming +50 %.', 'Mk.II：游泳速度 +50%。', 'Mk.II：泳ぐ速さ +50%。'),
    'kinetic_generator': ('Mk.II: 2 EU per block walked, 4 sprinted.', 'Mk.II：行走每格 2 EU，疾跑 4 EU。', 'Mk.II：歩行 1 ブロック 2 EU、疾走 4 EU。'),
    'solar_skin': ('Mk.II: 4 EU/t.', 'Mk.II：4 EU/t。', 'Mk.II：4 EU/t。'),
}

OTHER = {
    M + 'tooltip.mk2': ('Mk.II: stronger effect, 20 % less drain', 'Mk.II：效果更强，耗电减少 20%', 'Mk.II：効果が強く、消費電力 -20%'),
    M + 'tooltip.mk2_available': ('Mk.II available (Living Circuit + neutron plates)', '可升级为 Mk.II（生体电路 + 中子板）', 'Mk.II あり（生体回路＋中性子板）'),
    'gui.mio_icif.station.status.upgrading': ('UPGRADING TO MK.II', '正在升级为 MK.II', 'MK.II へ強化中'),
    'gui.mio_icif.station.status.upgrade_one_at_a_time': ('UPGRADE: ONE MK.II UNIT AT A TIME', '升级：每次只放一个 MK.II 单元', '強化：MK.II は 1 個ずつ入れる'),
    C + 'nano.pvp.jammer': ('Jammer blocks player locks', '干扰器阻挡玩家锁定', 'ジャマーがプレイヤーのロックを防ぐ'),
    C + 'nano.pvp.jammer.tooltip': ('Guided weapons of other players cannot lock the wearer.', '其他玩家的制导武器无法锁定穿戴者。', '他のプレイヤーの誘導兵器が着用者をロックできない。'),
    C + 'nano.pvp.recon': ('Recon reveals players', '侦察云显示玩家', '偵察雲がプレイヤーも映す'),
    C + 'nano.pvp.recon.tooltip': ('Other players in the recon cloud glow for the wearer.', '侦察云中的其他玩家对穿戴者发光。', '偵察雲の中の他のプレイヤーも着用者に光って見える。'),
    C + 'nano.jammer_range': ('Jammer range', '干扰器范围', 'ジャマーの範囲'),
    C + 'nano.jammer_range.tooltip': ('Hostile creatures beyond it lose track (Mk.II: two thirds).', '超出该距离的敌对生物失去踪迹（Mk.II：三分之二）。', 'これより遠い敵対生物が見失う（Mk.II は 3 分の 2）。'),
    C + 'nano.recon_range': ('Recon cloud range', '侦察云范围', '偵察雲の範囲'),
    C + 'nano.recon_range.tooltip': ('Blocks (Mk.II: +1/3).', '格（Mk.II：+1/3）。', 'ブロック（Mk.II は +1/3）。'),
    C + 'nano.mesh_reduction': ('Kinetic mesh damage cut', '动能装甲网减伤', '運動装甲メッシュの軽減'),
    C + 'nano.mesh_reduction.tooltip': ('Share of the remaining damage (Mk.II +0.1); total absorption at most 95 %.', '剩余伤害的比例（Mk.II +0.1）；总吸收上限 95%。',
                                        '残りのダメージの割合（Mk.II は +0.1）。吸収の合計上限 95%。'),
    C + 'nano.repair_eu': ('Self-repair EU per step', '自修复每次 EU', '自己修復 1 回の EU'),
    C + 'nano.repair_eu.tooltip': ('EU per repair step.', '每次修复消耗的 EU。', '修復 1 回あたりの EU。'),
    C + 'nano.swim': ('Hydrophobic swim bonus', '疏水游泳加成', '撥水の水泳ボーナス'),
    C + 'nano.swim.tooltip': ('0.3 = +30 % (Mk.II +0.2).', '0.3 = +30%（Mk.II +0.2）。', '0.3 = +30%（Mk.II は +0.2）。'),
    C + 'nano.solar': ('Solar skin EU/t', '太阳能皮肤 EU/t', 'ソーラースキン EU/t'),
    C + 'nano.solar.tooltip': ('Under open daylight (Mk.II: double).', '露天日光下（Mk.II：翻倍）。', '屋外の日光の下（Mk.II は 2 倍）。'),
    C + 'nano.mk2_drain': ('Mk.II drain factor', 'Mk.II 耗电系数', 'Mk.II の消費電力係数'),
    C + 'nano.mk2_drain.tooltip': ('Mk.II units draw this share of the Mk.I drain.', 'Mk.II 单元的耗电为 Mk.I 的该比例。', 'Mk.II は Mk.I の消費電力にこの係数を掛ける。'),
    'mio_icif.subtitle.nano.mesh_on': ('Kinetic mesh locks', '动能装甲网锁合', '運動装甲メッシュ 固化'),
    'mio_icif.subtitle.nano.mesh_hit': ('Kinetic mesh deflects', '动能装甲网偏转', '運動装甲メッシュが弾く'),
    'mio_icif.subtitle.nano.recon': ('Recon swarm spreads', '侦察云扩散', '偵察雲が広がる'),
    'mio_icif.subtitle.nano.rebound': ('Shock gel rebounds', '冲击凝胶反弹', '衝撃ゲルが跳ね返る'),
    'mio_icif.subtitle.nano.repair': ('Nano coat mends', '纳米涂层修复', 'ナノコートが修復'),
}

BURSTS = {
    'reconCloud': ('Nano Recon Cloud', '纳米侦察云', 'ナノ偵察雲'),
    'kineticMesh': ('Kinetic Armor Mesh', '动能装甲网', '運動装甲メッシュ'),
}
BURST_FIELDS = {
    'reserve': (('%s: reserve', '%s：储备', '%s：予備量'), ('Reserve points per use.', '每次使用的储备。', '1 回あたりの予備量。')),
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
        n = 0
        for uid, (name, desc) in UNITS.items():
            data['item.mio_icif.module.item_module_' + uid] = name[i]
            data[M + uid] = name[i]
            data[M + uid + '.desc'] = desc[i]
            data[C + 'nano.unit.' + uid] = name[i]
            data[C + 'nano.unit.' + uid + '.tooltip'] = ('Switch: ', '开关：', 'スイッチ：')[i] + name[i]
            n += 5
        for uid, name in MK2_NAMES.items():
            data['item.mio_icif.module.item_module_' + uid + '_mk2'] = name[i] + ' Mk.II'
            data[M + uid + '.desc.mk2'] = MK2_LINES[uid][i]
            n += 2
        for key, t in OTHER.items():
            data[key] = t[i]
            n += 1
        for bid, bname in BURSTS.items():
            for field, (label, tip) in BURST_FIELDS.items():
                data[C + 'nano.burst.' + bid + '.' + field] = label[i] % bname[i]
                data[C + 'nano.burst.' + bid + '.' + field + '.tooltip'] = tip[i]
                n += 2
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', n)


if __name__ == '__main__':
    main()
