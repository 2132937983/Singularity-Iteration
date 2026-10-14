#!/usr/bin/env python3
"""0.1.10.2 language keys (en_us / zh_cn / ja_jp): the quantum command (wheel, orders, messages,
squad HUD), the 3D holo table, the holomap Mk.II, the server config section "command", the client
options and the subtitles. Idempotent; keeps the line endings of each file.
Usage: lang_1102.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
C = 'gui.mio_icif.command.'
MSG = 'message.mio_icif.command.'
H = 'gui.mio_icif.holo_table.'
Q = 'mio_icif.configuration.quantum.'
CO = 'mio_icif.configuration.'

KEYS = {
    'key.mio_icif.command_wheel': ('Quantum command wheel (hold)', '量子指挥轮盘（按住）', '量子指揮ホイール（長押し）'),
    # ---------------------------------------------------------------- wheel
    C + 'title': ('Quantum Command', '量子指挥', '量子指揮'),
    C + 'entry.move': ('MOVE', '移动', '移動'),
    C + 'entry.hold': ('HOLD', '待机', '待機'),
    C + 'entry.suppress': ('SUPPRESS', '区域压制', '区域制圧'),
    C + 'entry.focus': ('FOCUS', '集中攻击', '集中攻撃'),
    C + 'entry.volley': ('VOLLEY', '齐射', '一斉射撃'),
    C + 'entry.form_up': ('FORM UP', '归队', '編隊復帰'),
    C + 'entry.control': ('MODE', '模式', 'モード'),
    C + 'entry.datalink': ('DATA LINK', '数据链', 'データリンク'),
    C + 'entry.holo': ('HOLO TABLE', '全息台', 'ホロテーブル'),
    C + 'entry.recall': ('Q-RECALL', '量子回收', '量子回収'),
    C + 'sub.squad': ('Follow squad', '跟随全队', '全体に従う'),
    C + 'sub.return': ('Return home', '返回军械库', '武装庫へ帰還'),
    C + 'all': ('ALL (%s)', '全部（%s）', '全機（%s）'),
    C + 'chip_all': ('ALL', '全部', '全機'),
    C + 'cancel': ('cancel', '取消', 'キャンセル'),
    C + 'sky': ('sky point 48m', '空中点 48m', '空中点 48m'),
    C + 'select_hint': ('Wheel / click: choose the Wingmen', '滚轮/点击：选择僚机', 'ホイール／クリック：僚機を選ぶ'),
    C + 'datalink_on': ('DATA LINK ON', '数据链 开', 'データリンク オン'),
    C + 'datalink_off': ('DATA LINK OFF', '数据链 关', 'データリンク オフ'),
    C + 'recall_cd': ('Quantum recall in %s s', '量子回收冷却 %s 秒', '量子回収まで %s 秒'),
    C + 'squad': ('SQUAD', '编队', '編隊'),
    C + 'action.0': ('MOVE', '移动', '移動'),
    C + 'action.1': ('HOLD', '待机', '待機'),
    C + 'action.2': ('SUPPRESS', '区域压制', '区域制圧'),
    C + 'action.3': ('FOCUS', '集中攻击', '集中攻撃'),
    C + 'action.4': ('FORM UP', '归队', '編隊復帰'),
    C + 'action.5': ('MODE', '模式', 'モード'),
    C + 'action.6': ('FOLLOW SQUAD', '跟随全队', '全体に従う'),
    C + 'action.7': ('VOLLEY', '齐射', '一斉射撃'),
    C + 'action.8': ('DATA LINK', '数据链', 'データリンク'),
    C + 'action.9': ('RETURN', '返回', '帰還'),
    C + 'action.10': ('QUANTUM RECALL', '量子回收', '量子回収'),
    # ---------------------------------------------------------------- messages
    MSG + 'no_relay': ('Quantum command needs an active Entanglement Relay on a quantum suit piece.',
                       '量子指挥需要量子套装部件上已启用的量子纠缠中继器。', '量子指揮には量子スーツ部位の有効な量子もつれ中継器が必要です。'),
    MSG + 'no_unit': ('No Wingman can take that order here.', '这里没有能接受该命令的僚机。', 'この命令を受けられる僚機がいません。'),
    MSG + 'bad_point': ('That point is out of the world.', '该地点超出世界范围。', 'その地点はワールドの外です。'),
    MSG + 'bad_target': ('No valid target in range for focus fire.', '范围内没有可集中攻击的有效目标。', '集中攻撃できる目標が範囲内にありません。'),
    MSG + 'done': ('%s: %s Wingmen', '%s：%s 架僚机', '%s：僚機 %s 機'),
    MSG + 'datalink_on': ('Data link on: Wingman sightings go to your HUD and maps.', '数据链已开启：僚机发现的目标会显示在 HUD 与地图上。',
                          'データリンク オン：僚機の発見を HUD とマップに表示します。'),
    MSG + 'datalink_off': ('Data link off.', '数据链已关闭。', 'データリンク オフ。'),
    MSG + 'recall_cooldown': ('Quantum recall is still recharging.', '量子回收仍在冷却。', '量子回収はまだ再充填中です。'),
    MSG + 'recall_coherence': ('Not enough coherence for a quantum recall.', '相干度不足，无法量子回收。', '量子回収に必要なコヒーレンスが足りません。'),
    MSG + 'recall': ('Quantum recall: %s suits fold home (%s from unloaded areas).', '量子回收：%s 套套装返回（其中 %s 套位于未加载区域）。',
                     '量子回収：%s 着が帰還（うち %s 着は未ロード区域）。'),
    MSG + 'recall_lost': ('Quantum recall: a Wingman did not answer.', '量子回收：有一架僚机没有响应。', '量子回収：応答しない僚機がいます。'),
    MSG + 'holo_needs_mk2': ('The holo table needs an active Tactical 3D Holomap Mk.II.', '全息台需要已启用的战术3D全息地图 Mk.II。',
                             'ホロテーブルには有効な戦術3Dホロマップ Mk.II が必要です。'),
    # ---------------------------------------------------------------- holo table
    H + 'title': ('3D HOLO TABLE', '3D 全息台', '3D ホロテーブル'),
    H + 'rotate': ('drag: turn · wheel: range', '拖动：旋转 · 滚轮：范围', 'ドラッグ：回転・ホイール：範囲'),
    H + 'hint': ('Left: MOVE · Right: SUPPRESS · Left on a creature: FOCUS · H: HOLD · F: FORM UP · 0-9: select',
                 '左键：移动 · 右键：区域压制 · 左键点生物：集中攻击 · H：待机 · F：归队 · 0-9：选择',
                 '左：移動・右：区域制圧・生物を左：集中攻撃・H：待機・F：編隊復帰・0-9：選択'),
    H + 'focus': ('FOCUS ▸ %s', '集中攻击 ▸ %s', '集中攻撃 ▸ %s'),
    H + 'point': ('POINT', '地点', '地点'),
    # ---------------------------------------------------------------- holomap Mk.II
    'item.mio_icif.module.item_module_holomap_mk2': ('Tactical 3D Holomap Mk.II', '战术3D全息地图 Mk.II', '戦術3Dホロマップ Mk.II'),
    'module.mio_icif.holomap.desc.mk2': (
        'Mk.II: projects the 3D holo table (hold the tactical map key, or the command wheel): terrain, Wingmen, creatures and ores, '
        'with orders by click.',
        'Mk.II：投射 3D 全息台（按住战术地图键或使用指挥轮盘）：显示地形、僚机、生物与矿石，可点击下达命令。',
        'Mk.II：3D ホロテーブルを投影する（戦術マップキー長押し、またはコマンドホイール）。地形・僚機・生物・鉱石を表示し、クリックで命令できる。'),
    # ---------------------------------------------------------------- server config
    Q + 'section.command': ('Quantum command', '量子指挥', '量子指揮'),
    Q + 'section.command.tooltip': ('Wingman orders, the quantum recall and the order radii.', '僚机命令、量子回收与命令半径。',
                                    '僚機への命令、量子回収、命令の半径。'),
    Q + 'recall_cost': ('Quantum recall cost', '量子回收消耗', '量子回収のコスト'),
    Q + 'recall_cooldown': ('Quantum recall cooldown (ticks)', '量子回收冷却（tick）', '量子回収のクールダウン（tick）'),
    Q + 'recall_unloaded': ('Recall Wingmen in unloaded areas', '回收位于未加载区域的僚机', '未ロード区域の僚機も回収する'),
    Q + 'hold_radius': ('Hold radius', '待机半径', '待機の半径'),
    Q + 'suppress_radius': ('Suppression radius', '压制半径', '制圧の半径'),
    Q + 'focus_range': ('Focus fire range', '集中攻击范围', '集中攻撃の範囲'),
    # ---------------------------------------------------------------- client options
    CO + 'squad_hud': ('Squad HUD', '编队 HUD', '編隊 HUD'),
    CO + 'squad_hud.tooltip': ('Squad panel and edge markers of off-screen Wingmen.', '编队面板与屏幕外僚机的边缘标记。', '編隊パネルと画面外の僚機の端マーカー。'),
    CO + 'order_markers': ('Order markers', '命令标记', '命令マーカー'),
    CO + 'order_markers.tooltip': ('Hold points, suppression rings and focus brackets in the world.', '世界中的待机点、压制圆环与集中攻击框。',
                                   'ワールド内の待機点、制圧リング、集中攻撃の枠。'),
    CO + 'holo_table_cells': ('Holo table cells', '全息台格数', 'ホロテーブルのセル数'),
    CO + 'holo_table_cells.tooltip': ('Terrain cells per side (more = finer, slower).', '每边的地形格数（越多越精细，也越慢）。',
                                      '一辺の地形セル数（多いほど細かく、重い）。'),
    CO + 'thruster_fx': ('Thruster effects', '推进器特效', 'スラスター演出'),
    CO + 'thruster_fx.tooltip': ('Heat haze over thruster flames, shock diamonds at full burn, the landing dust ring.',
                                 '推进器火焰的热浪扭曲、全推力时的激波钻石、着陆时的尘环。',
                                 'スラスター炎の陽炎、全開時のショックダイヤモンド、着地時の塵のリング。'),
    # ---------------------------------------------------------------- subtitles
    'mio_icif.subtitle.command.wheel_open': ('Command wheel opens', '指挥轮盘展开', 'コマンドホイールが開く'),
    'mio_icif.subtitle.command.wheel_select': ('Command wheel ticks', '指挥轮盘选择', 'コマンドホイールの選択音'),
    'mio_icif.subtitle.command.order_ack': ('Wingman acknowledges', '僚机确认', '僚機が応答する'),
    'mio_icif.subtitle.command.quantum_recall': ('Suits fold away', '套装量子回收', 'スーツが量子回収される'),
    'mio_icif.subtitle.command.holo_open': ('Holo table projects', '全息台投射', 'ホロテーブルが投影される'),
    'mio_icif.subtitle.command.holo_close': ('Holo table folds', '全息台收起', 'ホロテーブルが閉じる'),
    'mio_icif.subtitle.suit.thruster_land': ('Thrusters touch down', '推进器着陆', 'スラスターで着地する'),
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
