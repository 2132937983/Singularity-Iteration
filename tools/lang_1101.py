#!/usr/bin/env python3
"""0.1.10.1 language keys (en_us / zh_cn / ja_jp): the compute core, the six quantum Mk.II items of
phase 2, the nine entanglement synergies, the Entanglement Matrix screen, the panel rows, the unit
tooltips (bandwidth and synergies), the server config (synergy switches and numbers) and the
subtitles. Idempotent; keeps the line endings of each file.
Usage: lang_1101.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
M = 'module.mio_icif.'
IM = 'item.mio_icif.module.'
Q = 'mio_icif.configuration.quantum.'
S = 'quantum.mio_icif.synergy.'
E = 'gui.mio_icif.entangle.'
G = 'gui.mio_icif.equipment_console.'

KEYS = {
    # ---------------------------------------------------------------- compute core and Mk.II items
    IM + 'item_module_compute_core': ('Quantum Compute Core', '量子演算核心', '量子演算コア'),
    M + 'compute_core': ('Quantum Compute Core', '量子演算核心', '量子演算コア'),
    M + 'compute_core.desc': (
        'A second processor for the suit computer: compute bandwidth +8 QB and one more entanglement link on the chestplate. '
        '6 EU/t while on.',
        '套装计算机的第二处理器：演算带宽 +8 QB，胸甲上的纠缠链接 +1。开启时 6 EU/t。',
        'スーツ計算機の第 2 プロセッサ。演算帯域 +8 QB、チェストのもつれリンク +1。オン時 6 EU/t。'),
    IM + 'item_module_ore_scanner_mk2': ('Seismic Ore Scanner Mk.II', '地震矿物扫描仪 Mk.II', '震探鉱石スキャナー Mk.II'),
    M + 'ore_scanner.desc.mk2': (
        'Mk.II: 24-block radius; an ore tag filter in the Singularity Management Panel (all ores or one c:ores tag).',
        'Mk.II：半径 24 格；可在奇点管理面板中按矿石标签筛选（全部矿石或某个 c:ores 标签）。',
        'Mk.II：半径 24 ブロック。特異点管理パネルで鉱石タグの絞り込みができる（全鉱石、または c:ores タグのひとつ）。'),
    IM + 'item_module_grid_telemetry_mk2': ('Grid Telemetry Sensor Mk.II', '电网遥测传感器 Mk.II', '電網テレメトリセンサー Mk.II'),
    M + 'grid_telemetry.desc.mk2': (
        'Mk.II: reads the block in your line of sight up to 36 blocks away (Mk.I: the block in reach).',
        'Mk.II：可读取视线方向 36 格内的方块（Mk.I：仅触及范围内的方块）。',
        'Mk.II：視線の先 36 ブロックまでのブロックを読み取る（Mk.I：手の届く範囲のみ）。'),
    IM + 'item_module_entity_esp_mk2': ('Biometric Scanner (ESP) Mk.II', '生物扫描仪（ESP）Mk.II', '生体スキャナー（ESP）Mk.II'),
    M + 'entity_esp.desc.mk2': (
        'Mk.II: 48 blocks; the target card also shows the exact health and the armor.',
        'Mk.II：48 格；目标卡片还会显示精确生命值与护甲值。',
        'Mk.II：48 ブロック。目標カードに正確な体力と防具値も表示する。'),
    IM + 'item_module_ballistic_mk2': ('Ballistic Computer Mk.II', '弹道计算机 Mk.II', '弾道計算機 Mk.II'),
    M + 'ballistic.desc.mk2': (
        'Mk.II: a multishot weapon shows the paths of all three shots.',
        'Mk.II：多重射击武器会显示全部三发的弹道。',
        'Mk.II：マルチショットの武器では 3 発すべての軌道を表示する。'),
    IM + 'item_module_smart_guidance_mk2': ('Smart Trajectory Guidance Mk.II', '智能弹道诱导 Mk.II', 'スマート弾道誘導 Mk.II'),
    M + 'smart_guidance.desc.mk2': (
        'Mk.II: up to 10 locks; guided shots turn up to 20 degrees per tick (Mk.I: 6 locks, 14 degrees).',
        'Mk.II：最多 10 个锁定；诱导弹每 tick 最多转向 20 度（Mk.I：6 个锁定，14 度）。',
        'Mk.II：ロックは最大 10。誘導弾は 1 tick に最大 20 度旋回する（Mk.I：6 ロック、14 度）。'),
    IM + 'item_module_resonance_field_mk2': ('Resonance Deconstruction Field Mk.II', '力场共振解体 Mk.II', '力場共振解体 Mk.II'),
    M + 'resonance_field.desc.mk2': (
        'Mk.II: radius 3.75 and up to 9 blocks per tick (Mk.I: radius 2.5, 6 blocks).',
        'Mk.II：半径 3.75，每 tick 最多 9 个方块（Mk.I：半径 2.5，6 个方块）。',
        'Mk.II：半径 3.75、1 tick に最大 9 ブロック（Mk.I：半径 2.5、6 ブロック）。'),
    # ---------------------------------------------------------------- unit tooltips
    M + 'tooltip.bandwidth': ('Compute bandwidth: %s QB on a quantum piece', '演算带宽：在量子部件上占用 %s QB', '演算帯域：量子部位で %s QB'),
    M + 'tooltip.compute_core': ('Bandwidth budget +%s QB, entanglement links +1', '带宽预算 +%s QB，纠缠链接 +1', '帯域予算 +%s QB、もつれリンク +1'),
    M + 'tooltip.synergy': ('Synergy: %s (with %s)', '协同：%s（搭配 %s）', 'シナジー：%s（%s と）'),
    # ---------------------------------------------------------------- synergies
    S + 'predictive_guidance': ('Predictive Guidance', '预测诱导', '予測誘導'),
    S + 'predictive_guidance.desc': (
        'Guided shots fly to where the target will be: along the path a creature walks, solved in three passes. '
        'Turn +4 degrees per tick, 2 more locks.',
        '诱导弹飞向目标将要到达的位置：沿生物行走的路径预测，三次迭代求解。每 tick 转向 +4 度，锁定 +2。',
        '誘導弾は目標が来る位置へ飛ぶ。生物が歩く経路に沿って予測し、3 回の反復で解く。旋回 +4 度/tick、ロック +2。'),
    S + 'threat_sight': ('Threat Sight', '威胁透视', '脅威透視'),
    S + 'threat_sight.desc': (
        'Creatures that hunt you glow red through walls, with a ring that counts down to their attack.',
        '正在追猎你的生物会透过墙壁发出红色轮廓，并以圆环倒数其攻击到达的时间。',
        '自分を狙う敵が壁越しに赤く光り、攻撃が届くまでの時間をリングで示す。'),
    S + 'ore_holo': ('Ore Hologram', '地脉全息', '地脈ホロ'),
    S + 'ore_holo.desc': (
        'The ores of the seismic scan appear as 3D voxels in the holomap and the tactical map.',
        '地震扫描到的矿石以 3D 体素显示在全息地图与战术地图中。',
        '震探で見つけた鉱石を、ホロマップと戦術マップに 3D ボクセルで表示する。'),
    S + 'ballistic_sync': ('Ballistic Sync', '弹道同步', '弾道同期'),
    S + 'ballistic_sync.desc': (
        'Locks take 30 % less time, and the planned paths of your guided shots are drawn on the HUD.',
        '锁定时间减少 30%，并在 HUD 上绘制诱导弹的预定轨迹。',
        'ロック時間 -30%。誘導弾の予定軌道を HUD に描く。'),
    S + 'blast_forecast': ('Blast Forecast', '爆心预报', '爆心予報'),
    S + 'blast_forecast.desc': (
        'A coming blast shows its radius as a 3D sphere and an arrow points the way out.',
        '即将发生的爆炸会以 3D 球体显示其半径，并用箭头指示撤离方向。',
        '爆発の半径を 3D の球で示し、退避方向を矢印で示す。'),
    S + 'resonant_deflection': ('Resonant Deflection', '共振偏转', '共鳴偏向'),
    S + 'resonant_deflection.desc': (
        '25 % of the damage the deflector stops charges a resonance; at 8 it bursts and hits the hostile creatures within 4 blocks '
        'with the stored charge.',
        '偏转器挡下的伤害有 25% 会蓄积为共振；达到 8 时爆发，以蓄积的能量伤害 4 格内的敌对生物。',
        '偏向器が止めたダメージの 25% を共鳴として蓄え、8 に達すると放出して半径 4 ブロックの敵に蓄えた分のダメージを与える。'),
    S + 'afterimage_decoy': ('Afterimage Decoy', '残像诱饵', '残像欺瞞'),
    S + 'afterimage_decoy.desc': (
        'A blink leaves a decoy at the start point: for 2 s the creatures that hunted you walk to it and cannot take you back.',
        '跃迁会在出发点留下诱饵：2 秒内原本追猎你的生物会走向诱饵，且无法重新锁定你。',
        '転移すると出発点に残像デコイが残る。2 秒間、自分を狙っていた敵はデコイへ向かい、自分を狙い直せない。'),
    S + 'phase_counter': ('Phase Counter', '相位反击', '位相反撃'),
    S + 'phase_counter.desc': (
        'Damage that passed through you during a phase shift (up to 20) returns as a shock wave when it ends, even with a Mk.I modulator.',
        '相位偏移期间穿透你的伤害（最多 20）会在结束时化为冲击波释放，Mk.I 调制器同样有效。',
        '位相シフト中にすり抜けたダメージ（最大 20）を、解除時に衝撃波として放つ。Mk.I の変調器でも発動する。'),
    S + 'fleet_datalink': ('Fleet Datalink', '舰队数据链', '艦隊データリンク'),
    S + 'fleet_datalink.desc': (
        'The hostile creatures your Wingmen see appear on your HUD, and the Wingmen attack the creature in your sights first.',
        '僚机看到的敌对生物会显示在你的 HUD 上，僚机会优先攻击你瞄准的生物。',
        '僚機が見た敵を HUD に表示し、自分が狙っている敵を僚機が優先して攻撃する。'),
    # ---------------------------------------------------------------- Entanglement Matrix
    E + 'title': ('Entanglement Matrix', '纠缠矩阵', 'もつれマトリクス'),
    E + 'no_units': ('No units on worn quantum pieces.', '穿戴的量子部件上没有单元。', '装着中の量子部位にユニットがない。'),
    E + 'no_hub': ('No link hub: wear a quantum chestplate', '无链接中枢：请穿戴量子胸甲', 'リンクハブなし：量子チェストを装着'),
    E + 'links': ('Links %s/%s', '链接 %s/%s', 'リンク %s/%s'),
    E + 'bandwidth': ('Bandwidth %s/%s QB', '带宽 %s/%s QB', '帯域 %s/%s QB'),
    E + 'units': ('Units: %s + %s', '单元：%s + %s', 'ユニット：%s + %s'),
    E + 'result.2': ('Wear a quantum chestplate: it holds the links.', '请穿戴量子胸甲：链接保存在胸甲上。', '量子チェストを装着してください。リンクはチェストに保存されます。'),
    E + 'result.3': ('All links are in use. Unlink one, or install a compute core.', '链接已用满。请先解除一个，或安装演算核心。',
                     'リンクが満杯です。どれかを解除するか、演算コアを取り付けてください。'),
    E + 'result.4': ('Both units must be installed on worn quantum pieces.', '两个单元都必须安装在穿戴中的量子部件上。', '両方のユニットを装着中の量子部位に取り付けてください。'),
    E + 'result.5': ('Unknown synergy.', '未知的协同。', '不明なシナジー。'),
    E + 'click': ('Click: link / unlink. Or drag one unit onto another.', '点击：链接/解除。也可以把一个单元拖到另一个单元上。',
                  'クリック：リンク／解除。ユニットを別のユニットへドラッグしてもよい。'),
    E + 'no_pair': ('No synergy between these units', '这两个单元之间没有协同', 'この組み合わせにシナジーはない'),
    E + 'release_link': ('Release to link', '松开以链接', '離すとリンク'),
    E + 'release_unlink': ('Release to unlink', '松开以解除链接', '離すとリンク解除'),
    G + 'links': ('Links %s/%s', '链接 %s/%s', 'リンク %s/%s'),
    G + 'links_open': ('Matrix', '矩阵', 'マトリクス'),
    G + 'ore_filter': ('Ore filter', '矿石筛选', '鉱石フィルター'),
    # ---------------------------------------------------------------- server config
    Q + 'section.synergies': ('Synergy switches', '协同开关', 'シナジーのスイッチ'),
    Q + 'section.synergies.tooltip': ('A synergy that is off does nothing; the link stays stored.', '关闭的协同不起作用；链接仍然保存。',
                                      'オフのシナジーは働かない。リンクは保存されたまま。'),
    Q + 'synergy.predictive_guidance': ('Predictive guidance', '预测诱导', '予測誘導'),
    Q + 'synergy.threat_sight': ('Threat sight', '威胁透视', '脅威透視'),
    Q + 'synergy.ore_holo': ('Ore hologram', '地脉全息', '地脈ホロ'),
    Q + 'synergy.ballistic_sync': ('Ballistic sync', '弹道同步', '弾道同期'),
    Q + 'synergy.blast_forecast': ('Blast forecast', '爆心预报', '爆心予報'),
    Q + 'synergy.resonant_deflection': ('Resonant deflection', '共振偏转', '共鳴偏向'),
    Q + 'synergy.afterimage_decoy': ('Afterimage decoy', '残像诱饵', '残像欺瞞'),
    Q + 'synergy.phase_counter': ('Phase counter', '相位反击', '位相反撃'),
    Q + 'synergy.fleet_datalink': ('Fleet datalink', '舰队数据链', '艦隊データリンク'),
    Q + 'pvp.synergy': ('Synergy bursts hit players', '协同爆发命中玩家', 'シナジーの放出がプレイヤーに当たる'),
    Q + 'section.synergy_tuning': ('Synergy and combo numbers', '协同与连锁数值', 'シナジーとコンボの数値'),
    Q + 'section.synergy_tuning.tooltip': ('Shares, thresholds, radii in blocks and times in ticks.', '比例、阈值、半径（格）与时间（tick）。',
                                           '割合、しきい値、半径（ブロック）、時間（tick）。'),
    Q + 'resonance_share': ('Resonance: share of deflected damage', '共振：偏转伤害的蓄积比例', '共鳴：偏向ダメージの蓄積割合'),
    Q + 'resonance_threshold': ('Resonance: burst threshold', '共振：爆发阈值', '共鳴：放出のしきい値'),
    Q + 'resonance_radius': ('Resonance: burst radius', '共振：爆发半径', '共鳴：放出の半径'),
    Q + 'decoy_ticks': ('Decoy length', '诱饵持续时间', 'デコイの持続時間'),
    Q + 'counter_max': ('Phase counter: most damage stored', '相位反击：最大蓄积伤害', '位相反撃：蓄積ダメージの上限'),
    Q + 'tunnel_window': ('Quantum tunnel window', '量子隧穿的时间窗口', '量子トンネルの受付時間'),
    Q + 'frozen_volley': ('Frozen volley damage factor', '冻结齐射的伤害倍率', '凍結斉射のダメージ倍率'),
    # ---------------------------------------------------------------- subtitles
    'mio_icif.subtitle.suit.link_on': ('Entanglement link made', '纠缠链接建立', 'もつれリンク成立'),
    'mio_icif.subtitle.suit.link_off': ('Entanglement link cut', '纠缠链接断开', 'もつれリンク解除'),
    'mio_icif.subtitle.suit.combo': ('Quantum combo', '量子连锁', '量子コンボ'),
    'mio_icif.subtitle.suit.resonance_burst': ('Resonance bursts', '共振爆发', '共鳴が放出される'),
}


def main():
    col = {'en_us': 0, 'zh_cn': 1, 'ja_jp': 2}
    for lang, i in col.items():
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        for key, t in KEYS.items():
            data[key] = t[i]
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(KEYS))


if __name__ == '__main__':
    main()
