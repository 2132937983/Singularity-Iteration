#!/usr/bin/env python3
"""0.1.10.0 language keys (en_us / zh_cn / ja_jp): the quantum action units, the quantum bay
expander, the quantum Mk.II items, the action names and tooltips, the station status, the panel
rows, the server config section QuantumSuit, the client options and the subtitles.
Idempotent; keeps the line endings of each file.
Usage: lang_1100.py <repo root>
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
C = 'mio_icif.configuration.'
A = 'quantum.mio_icif.action.'
G = 'gui.mio_icif.equipment_console.'

KEYS = {
    # ---------------------------------------------------------------- units and items
    IM + 'item_module_phase_jump_drive': ('Phase Jump Drive', '相位跃迁驱动器', '位相跳躍ドライブ'),
    M + 'phase_jump_drive': ('Phase Jump Drive', '相位跃迁驱动器', '位相跳躍ドライブ'),
    M + 'phase_jump_drive.desc': (
        'Folds the space ahead of the suit. Action: Quantum Blink, a jump to the point you look at (12 blocks), never through walls; '
        'no fall damage right after it.',
        '折叠套装前方的空间。行动：量子跃迁，瞬移到视线所指的位置（12 格），不会穿墙；跃迁后短时间内不受摔落伤害。',
        'スーツ前方の空間を折りたたむ。アクション：量子転移。視線の先へ瞬間移動する（12 ブロック）。壁は抜けられない。転移直後は落下ダメージを受けない。'),
    M + 'phase_jump_drive.desc.mk2': ('Mk.II: 20 blocks and two charges.', 'Mk.II：20 格，可连续使用两次。', 'Mk.II：20 ブロック、2 回分チャージ。'),
    IM + 'item_module_phase_jump_drive_mk2': ('Phase Jump Drive Mk.II', '相位跃迁驱动器 Mk.II', '位相跳躍ドライブ Mk.II'),
    IM + 'item_module_phase_modulator': ('Phase Modulator', '相位调制器', '位相変調器'),
    M + 'phase_modulator': ('Phase Modulator', '相位调制器', '位相変調器'),
    M + 'phase_modulator.desc': (
        'Shifts the suit out of phase with matter. Action: Phase Shift, 1 s in which creature, projectile and blast damage passes '
        'through you; no attacks or mining meanwhile. Lava, fire, drowning and the void still hurt.',
        '使套装与物质的相位错开。行动：相位偏移，1 秒内生物、投射物与爆炸伤害穿透你而过；期间不能攻击或挖掘。岩浆、火焰、溺水与虚空仍会造成伤害。',
        'スーツを物質と位相のずれた状態にする。アクション：位相シフト。1 秒間、生物・投射物・爆発のダメージがすり抜ける。その間は攻撃も採掘もできない。'
        '溶岩・火・溺水・奈落のダメージは受ける。'),
    M + 'phase_modulator.desc.mk2': ('Mk.II: 1.5 s, and a shock wave when it ends.', 'Mk.II：1.5 秒，结束时释放冲击波。', 'Mk.II：1.5 秒、解除時に衝撃波を放つ。'),
    IM + 'item_module_phase_modulator_mk2': ('Phase Modulator Mk.II', '相位调制器 Mk.II', '位相変調器 Mk.II'),
    IM + 'item_module_coherence_capacitor': ('Coherence Capacitor', '相干电容器', 'コヒーレンスキャパシタ'),
    M + 'coherence_capacitor': ('Coherence Capacitor', '相干电容器', 'コヒーレンスキャパシタ'),
    M + 'coherence_capacitor.desc': (
        'Stores quantum coherence for the actions: maximum +50, regeneration +25 %. Passive.',
        '为行动储存量子相干度：上限 +50，回复 +25%。被动生效。',
        'アクション用の量子コヒーレンスを蓄える：上限 +50、回復 +25%。常時有効。'),
    IM + 'item_module_entanglement_relay': ('Entanglement Relay', '量子纠缠中继器', '量子もつれ中継器'),
    M + 'entanglement_relay': ('Entanglement Relay', '量子纠缠中继器', '量子もつれ中継器'),
    M + 'entanglement_relay.desc': (
        'Entangles the suit with your Wingmen. Action: Entangled Volley, every Wingman in this dimension fires at your Smart Guidance '
        'locks (or the creature you look at) and all bolts strike at the same moment.',
        '使套装与你的僚机量子纠缠。行动：纠缠齐射，本维度内的所有僚机向你的智能制导锁定目标（或你注视的生物）开火，所有弹体同时命中。',
        'スーツとウィングマンを量子もつれで結ぶ。アクション：一斉射撃。同じディメンションの全ウィングマンが、スマート誘導のロック目標（なければ視線の先の生物）へ撃ち、'
        '全弾が同時に着弾する。'),
    IM + 'item_module_deflector_mk2': ('Quantum Deflector Mk.II', '量子偏转器 Mk.II', '量子偏向器 Mk.II'),
    M + 'deflector.desc.mk2': (
        'Mk.II: 3,000 EU per damage point. Action: Force Field, a dome that stops hostile projectiles in the air and pushes hostile '
        'creatures out (toggle).',
        'Mk.II：每点伤害 3,000 EU。行动：力场展开，展开一个让敌方投射物停在空中并把敌对生物推出去的力场罩（开关式）。',
        'Mk.II：1 ダメージあたり 3,000 EU。アクション：力場展開。敵の投射物を空中で止め、敵対生物を外へ押し出すドームを張る（トグル）。'),
    IM + 'item_module_behavior_predictor_mk2': ('Behavior Predictor Mk.II', '行为预测器 Mk.II', '行動予測器 Mk.II'),
    M + 'behavior_predictor.desc.mk2': (
        'Mk.II: Action: Time Dilation, a field that slows hostile creatures and projectiles around you for 4 s while you move faster.',
        'Mk.II：行动：时间扭曲，在你周围形成 4 秒的力场，使敌对生物与投射物减速，同时你的移动加快。',
        'Mk.II：アクション：時間歪曲。周囲の敵対生物と投射物を 4 秒間減速させ、自分は速く動ける。'),
    IM + 'item_quantum_bay_expander': ('Quantum Bay Expander', '量子扩展槽', '量子拡張ベイ'),
    IM + 'item_quantum_bay_expander.desc': (
        'Adds %s unit slot to a quantum suit piece, at most %s per piece. Permanent.',
        '为量子套装部件增加 %s 个单元槽，每个部件最多 %s 个。永久生效。',
        '量子スーツの部位にユニット枠を %s 追加する。1 部位につき %s 個まで。取り外し不可。'),
    IM + 'item_quantum_bay_expander.slots': ('Helmet 5, chestplate 4, leggings 3, boots 3', '头盔 5、胸甲 4、护腿 3、靴子 3', 'ヘルメット 5、チェスト 4、レギンス 3、ブーツ 3'),
    # ---------------------------------------------------------------- actions and tooltips
    A + 'blink': ('Quantum Blink', '量子跃迁', '量子転移'),
    A + 'phase_shift': ('Phase Shift', '相位偏移', '位相シフト'),
    A + 'force_field': ('Force Field', '力场展开', '力場展開'),
    A + 'time_dilation': ('Time Dilation', '时间扭曲', '時間歪曲'),
    A + 'volley': ('Entangled Volley', '纠缠齐射', '一斉射撃'),
    M + 'tooltip.action.single': ('Action (V/B): %s, %s coherence, cooldown %s s', '行动（V/B）：%s，%s 相干度，冷却 %s 秒',
                                  'アクション（V/B）：%s、コヒーレンス %s、クールダウン %s 秒'),
    M + 'tooltip.action.sustained': ('Toggle action (V/B): %s, %s coherence to start, %s per second, cooldown %s s after',
                                     '开关行动（V/B）：%s，启动 %s 相干度，每秒 %s，结束后冷却 %s 秒',
                                     'トグルアクション（V/B）：%s、開始にコヒーレンス %s、毎秒 %s、終了後クールダウン %s 秒'),
    M + 'tooltip.action_mk2': ('Mk.II unlocks the action: %s', 'Mk.II 解锁行动：%s', 'Mk.II でアクション解放：%s'),
    # ---------------------------------------------------------------- station and panel
    'gui.mio_icif.station.status.bay_max': ('EXPANSION BAY ALREADY FITTED', '已装有扩展槽', '拡張ベイは装着済み'),
    G + 'quantum': ('Quantum core', '量子核心', '量子コア'),
    G + 'quantum_slots': ('Units %s/%s', '单元 %s/%s', 'ユニット %s/%s'),
    G + 'quantum_offline': ('Quantum core offline (no action unit)', '量子核心离线（无行动单元）', '量子コア停止中（アクションユニットなし）'),
    G + 'coherence': ('Coherence', '相干度', 'コヒーレンス'),
    G + 'decoherent': ('DECOHERENT', '退相干', 'デコヒーレンス'),
    # ---------------------------------------------------------------- server config
    C + 'QuantumSuit': ('Quantum Suit', '量子套装', '量子スーツ'),
    Q + 'section.actions': ('Action switches', '行动开关', 'アクションのスイッチ'),
    Q + 'section.actions.tooltip': ('An action that is off does nothing; the unit stays installed.', '关闭的行动不起作用；单元保持安装。',
                                    'オフのアクションは動かない。ユニットは取り付けたまま。'),
    Q + 'section.pvp': ('Effects on players (PvP)', '对玩家的效果（PvP）', 'プレイヤーへの効果（PvP）'),
    Q + 'section.pvp.tooltip': ('All on by default; the server PvP setting applies too.', '默认全部开启；服务器的 PvP 设置同样适用。',
                                '初期値はすべてオン。サーバーの PvP 設定も適用される。'),
    Q + 'section.coherence': ('Coherence', '相干度', 'コヒーレンス'),
    Q + 'section.coherence.tooltip': ('The resource of the quantum actions.', '量子行动的资源。', '量子アクションの資源。'),
    Q + 'section.tuning': ('Action numbers', '行动数值', 'アクションの数値'),
    Q + 'section.tuning.tooltip': ('Coherence costs, cooldowns in ticks, ranges in blocks.', '相干度消耗、冷却（tick）、范围（格）。',
                                   'コヒーレンスのコスト、クールダウン（tick）、範囲（ブロック）。'),
    Q + 'section.slowmo': ('Single-player slow motion', '单人慢动作', 'シングル専用スローモーション'),
    Q + 'section.slowmo.tooltip': ('Real slow motion during your time dilation: single player with nobody else online only. Off by default.',
                                   '时间扭曲期间的真实慢动作：仅限单人且无其他玩家在线。默认关闭。',
                                   '時間歪曲中の本物のスローモーション。シングルプレイで他に誰もいないときだけ。初期値はオフ。'),
    Q + 'action.blink': ('Quantum blink', '量子跃迁', '量子転移'),
    Q + 'action.phase_shift': ('Phase shift', '相位偏移', '位相シフト'),
    Q + 'action.force_field': ('Force field (deflector Mk.II)', '力场展开（偏转器 Mk.II）', '力場展開（量子偏向器 Mk.II）'),
    Q + 'action.time_dilation': ('Time dilation (behavior predictor Mk.II)', '时间扭曲（行为预测器 Mk.II）', '時間歪曲（行動予測器 Mk.II）'),
    Q + 'action.volley': ('Entangled volley', '纠缠齐射', '一斉射撃'),
    Q + 'action.coherence_capacitor': ('Coherence capacitor', '相干电容器', 'コヒーレンスキャパシタ'),
    Q + 'pvp.phase': ('Phase shock wave hits players', '相位冲击波命中玩家', '位相シフトの衝撃波がプレイヤーに当たる'),
    Q + 'pvp.field': ('Force field acts on players', '力场作用于玩家', '力場がプレイヤーに作用する'),
    Q + 'pvp.dilation': ('Time dilation slows players', '时间扭曲使玩家减速', '時間歪曲がプレイヤーを減速させる'),
    Q + 'pvp.volley': ('Volley may target players', '齐射可以瞄准玩家', '一斉射撃がプレイヤーを狙える'),
    Q + 'regen': ('Coherence regeneration per second', '每秒相干度回复', '毎秒のコヒーレンス回復'),
    Q + 'regen_delay': ('Regeneration delay (ticks)', '回复延迟（tick）', '回復までの遅延（tick）'),
    Q + 'eu_per_point': ('EU per coherence point', '每点相干度的 EU', 'コヒーレンス 1 点あたりの EU'),
    Q + 'decoherence': ('Decoherence lock (ticks)', '退相干锁定（tick）', 'デコヒーレンスのロック（tick）'),
    Q + 'capacitor': ('Capacitor bonus', '电容器加成', 'キャパシタの上限加算'),
    Q + 'blink_range': ('Blink range', '跃迁距离', '転移の距離'),
    Q + 'blink_range_mk2': ('Blink range (Mk.II)', '跃迁距离（Mk.II）', '転移の距離（Mk.II）'),
    Q + 'blink_cost': ('Blink cost', '跃迁消耗', '転移のコスト'),
    Q + 'blink_cooldown': ('Blink cooldown per charge', '每次跃迁的冷却', '転移のチャージ時間'),
    Q + 'phase_ticks': ('Phase shift length', '相位偏移时长', '位相シフトの長さ'),
    Q + 'phase_ticks_mk2': ('Phase shift length (Mk.II)', '相位偏移时长（Mk.II）', '位相シフトの長さ（Mk.II）'),
    Q + 'phase_cost': ('Phase shift cost', '相位偏移消耗', '位相シフトのコスト'),
    Q + 'phase_cooldown': ('Phase shift cooldown', '相位偏移冷却', '位相シフトのクールダウン'),
    Q + 'phase_shock_radius': ('Phase shock wave radius', '相位冲击波半径', '衝撃波の半径'),
    Q + 'phase_shock_damage': ('Phase shock wave damage', '相位冲击波伤害', '衝撃波のダメージ'),
    Q + 'field_radius': ('Force field radius', '力场半径', '力場の半径'),
    Q + 'field_cost': ('Force field start cost', '力场启动消耗', '力場の開始コスト'),
    Q + 'field_drain': ('Force field drain per second', '力场每秒消耗', '力場の毎秒コスト'),
    Q + 'field_cooldown': ('Force field cooldown', '力场冷却', '力場のクールダウン'),
    Q + 'dilation_radius': ('Time dilation radius', '时间扭曲半径', '時間歪曲の半径'),
    Q + 'dilation_ticks': ('Time dilation length', '时间扭曲时长', '時間歪曲の長さ'),
    Q + 'dilation_cost': ('Time dilation cost', '时间扭曲消耗', '時間歪曲のコスト'),
    Q + 'dilation_cooldown': ('Time dilation cooldown', '时间扭曲冷却', '時間歪曲のクールダウン'),
    Q + 'dilation_slow': ('Creature slow', '生物减速', '生物の減速'),
    Q + 'dilation_projectile': ('Projectile speed factor', '投射物速度系数', '投射物の速度係数'),
    Q + 'volley_cost': ('Volley cost', '齐射消耗', '一斉射撃のコスト'),
    Q + 'volley_cooldown': ('Volley cooldown', '齐射冷却', '一斉射撃のクールダウン'),
    Q + 'volley_damage': ('Volley damage per Wingman', '每架僚机的齐射伤害', 'ウィングマン 1 機あたりのダメージ'),
    Q + 'volley_range': ('Volley range', '齐射范围', '一斉射撃の範囲'),
    Q + 'slowmo': ('Real slow motion', '真实慢动作', '本物のスローモーション'),
    Q + 'slowmo_rate': ('Slow motion tick rate', '慢动作的 tick 速率', 'スロー中のティックレート'),
    # ---------------------------------------------------------------- client options
    C + 'quantum_suit': ('Quantum suit', '量子套装', '量子スーツ'),
    C + 'quantum_hud': ('Quantum core HUD', '量子核心 HUD', '量子コア HUD'),
    C + 'quantum_hud.tooltip': ('Coherence ring, action keys and action timers around the crosshair.', '准星周围的相干度环、行动键与行动计时。',
                                '照準まわりのコヒーレンスリング、アクションキー、アクションのタイマー。'),
    C + 'visor_boot': ('Visor boot sequence', '面罩启动动画', 'バイザー起動シーケンス'),
    C + 'visor_boot.tooltip': ('The FCS visor boots when the HUD comes on: scan line, panels, aim calibration.',
                               'HUD 开启时 FCS 面罩执行启动动画：扫描线、面板展开、准星校准。',
                               'HUD が点くと FCS バイザーが起動演出を行う：走査線、パネル展開、照準較正。'),
    C + 'quantum_fx_intensity': ('Quantum action effects', '量子行动特效', '量子アクション演出の強さ'),
    C + 'quantum_fx_intensity.tooltip': ('Strength of the blink, phase, field, dilation and volley effects.', '跃迁、相位、力场、时间扭曲与齐射特效的强度。',
                                         '転移・位相・力場・時間歪曲・一斉射撃の演出の強さ。'),
    C + 'decoherence_glitch': ('Decoherence glitch', '退相干故障效果', 'デコヒーレンスのグリッチ'),
    C + 'decoherence_glitch.tooltip': ('The HUD tears and splits colours while the quantum core decoheres. Off: a calm warning.',
                                       '量子核心退相干时 HUD 撕裂并分色。关闭：仅平静的警告。',
                                       'デコヒーレンス中に HUD が乱れて色ずれする。オフ：控えめな警告のみ。'),
    # ---------------------------------------------------------------- subtitles
    'mio_icif.subtitle.suit.quantum_blink': ('Quantum blink', '量子跃迁', '量子転移'),
    'mio_icif.subtitle.suit.phase_on': ('Suit shifts out of phase', '套装相位偏移', 'スーツが位相シフトする'),
    'mio_icif.subtitle.suit.phase_off': ('Suit phases back', '套装相位恢复', 'スーツの位相が戻る'),
    'mio_icif.subtitle.suit.field_on': ('Force field rises', '力场展开', '力場が展開する'),
    'mio_icif.subtitle.suit.field_off': ('Force field falls', '力场消失', '力場が消える'),
    'mio_icif.subtitle.suit.field_catch': ('Force field stops a shot', '力场挡住弹体', '力場が弾を止める'),
    'mio_icif.subtitle.suit.dilation_on': ('Time slows down', '时间变慢', '時間が遅くなる'),
    'mio_icif.subtitle.suit.dilation_off': ('Time catches up', '时间恢复', '時間が戻る'),
    'mio_icif.subtitle.suit.volley_order': ('Volley ordered', '下令齐射', '一斉射撃の指令'),
    'mio_icif.subtitle.suit.volley_impact': ('Volley strikes', '齐射命中', '一斉射撃が着弾する'),
    'mio_icif.subtitle.suit.decoherence': ('Quantum core decoheres', '量子核心退相干', '量子コアがデコヒーレンスする'),
    'mio_icif.subtitle.suit.visor_boot': ('Visor boots', '面罩启动', 'バイザーが起動する'),
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
