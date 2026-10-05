#!/usr/bin/env python3
"""0.1.7.33 language keys (en_us / zh_cn / ja_jp). Text follows the ASD-STE100 rules of the manual:
active voice, one instruction per sentence, short sentences, one term per concept."""
import json, os, sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

UNITS = ['ore_scanner', 'grid_telemetry', 'entity_esp', 'ballistic', 'blast_warning', 'behavior_predictor', 'holomap', 'threat_sensor', 'deflector']

NAMES = {
    'en_us': ['Seismic Ore Scanner', 'Grid Telemetry Sensor', 'Biometric Scanner (ESP)', 'Ballistic Computer', 'Blast Warning Timer',
              'Behavior Predictor', 'Tactical 3D Holomap', 'Threat Sensor', 'Quantum Deflector'],
    'zh_cn': ['地震矿物扫描仪', '电网遥测传感器', '生物扫描仪（ESP）', '弹道计算机', '爆炸预警计时器', '行为预测器', '战术3D全息地图', '威胁传感器', '量子偏转器'],
    'ja_jp': ['震探鉱石スキャナー', '電網テレメトリセンサー', '生体スキャナー（ESP）', '弾道計算機', '爆発警告タイマー', '行動予測器', '戦術3Dホロマップ', '脅威センサー', '量子偏向器'],
}
DESCS = {
    'en_us': [
        'Scans the ground in a 16-block radius. Shows ores as colored wireframe cells.',
        'Shows the voltage tier and the EU flow of the block in view and of your chunk.',
        'Marks all creatures in 32 blocks. Shows the type, the distance and the health.',
        'Calculates the path of arrows and thrown items. Shows the impact point and the lead point.',
        'Shows the fuse time of primed TNT and of swelling creepers in 28 blocks.',
        'Shows the path of creatures for the next second. Marks an attack before it starts.',
        'Shows a 3D map of the terrain and of the creatures around you.',
        'Gives a yellow warning when a creature looks at you. Gives a red warning when it targets you.',
        'Uses EU to stop the damage that the armor lets through. Shows each hit on a local field.'],
    'zh_cn': [
        '扫描半径16格的地层。用彩色线框显示矿物。',
        '显示视线方块与所在区块的电压等级和EU流量。',
        '标记32格内的全部生物。显示类型、距离与生命值。',
        '计算箭矢与投掷物的轨迹。显示落点与提前量点。',
        '显示28格内已点燃TNT与膨胀苦力怕的引信时间。',
        '显示生物下一秒的路径。在攻击开始前标记攻击。',
        '显示周围地形与生物的3D地图。',
        '生物注视你时发出黄色警告。生物锁定你时发出红色警告。',
        '消耗EU抵消护甲未挡住的伤害。在局部力场上显示每次命中。'],
    'ja_jp': [
        '半径16ブロックの地層をスキャンする。鉱石を色付きワイヤーフレームで表示する。',
        '視線先のブロックと現在チャンクの電圧Tierと電力流量を表示する。',
        '32ブロック内の全生物をマークする。種類・距離・体力を表示する。',
        '矢と投擲物の軌道を計算する。着弾点と偏差点を表示する。',
        '28ブロック内の点火済みTNTと膨張中クリーパーの起爆時間を表示する。',
        '生物の次の1秒の経路を表示する。攻撃を開始前にマークする。',
        '周囲の地形と生物を3Dマップで表示する。',
        '生物が着用者を注視すると黄色警告を出す。標的にすると赤色警告を出す。',
        'EUを使い、防具が防げなかったダメージを打ち消す。命中を局所力場に表示する。'],
}

COMMON = {
    'en_us': {
        'block.mio_icif.producer.block_quantum_modification_station': 'Quantum Modification Station',
        'module.mio_icif.slot.head': 'Helmet', 'module.mio_icif.slot.chest': 'Chestplate',
        'module.mio_icif.slot.legs': 'Leggings', 'module.mio_icif.slot.feet': 'Boots',
        'module.mio_icif.tooltip.slots': 'Fits: %s',
        'module.mio_icif.tooltip.drain': 'Power: %s EU/t while on',
        'module.mio_icif.tooltip.deflector_cost': 'Power: %s EU per damage point, max %s per hit',
        'module.mio_icif.tooltip.visor': 'Needs a quantum helmet visor to show data',
        'module.mio_icif.tooltip.station': 'Install with the Quantum Modification Station',
        'gui.mio_icif.station.units': 'UNITS %s/%s',
        'gui.mio_icif.station.insert_piece': 'Put a quantum suit piece in the top slot.',
        'gui.mio_icif.station.not_quantum': 'This item does not accept upgrade units.',
        'gui.mio_icif.station.no_units': 'No unit installed. Put a unit in the lower slot.',
        'gui.mio_icif.station.status.idle': 'IDLE',
        'gui.mio_icif.station.status.working': 'INSTALLING',
        'gui.mio_icif.station.status.no_power': 'NO POWER',
        'gui.mio_icif.station.status.not_a_suit_piece': 'NOT A QUANTUM PIECE',
        'gui.mio_icif.station.status.wrong_slot': 'UNIT DOES NOT FIT THIS PIECE',
        'gui.mio_icif.station.status.already_installed': 'UNIT ALREADY INSTALLED',
        'gui.mio_icif.station.status.full': 'NO FREE UNIT SLOT',
        'gui.mio_icif.station.status.no_unit': 'IDLE',
        'gui.mio_icif.station.cost': 'Install: %s EU/t for %s ticks',
        'gui.mio_icif.station.eject_hint': 'Click x to remove the unit',
        'tooltip.mio_icif.armor.feature_viltrum_flight': 'Viltrum flight',
        'key.mio_icif.fcs_hud': 'FCS HUD on/off',
        'key.mio_icif.hud_layout': 'FCS HUD layout editor',
        'hud.mio_icif.fcs.on': 'FCS HUD: ON', 'hud.mio_icif.fcs.off': 'FCS HUD: OFF',
        'screen.mio_icif.hud_layout': 'FCS HUD Layout',
        'screen.mio_icif.hud_layout.hint': 'Drag a panel to move it. Scroll on the holomap to change its size.',
        'screen.mio_icif.hud_layout.reset': 'Reset',
        'screen.mio_icif.hud_layout.status': 'STATUS', 'screen.mio_icif.hud_layout.telemetry': 'GRID TELEMETRY',
        'screen.mio_icif.hud_layout.holomap': 'HOLOMAP', 'screen.mio_icif.hud_layout.ballistic': 'BALLISTIC',
        'screen.mio_icif.hud_layout.blast': 'BLAST WARNING', 'screen.mio_icif.hud_layout.flight': 'VILTRUM AIR DATA',
        'gui.mio_icif.armor_features.hud_layout': 'HUD layout',
        'item.mio_icif.guide': 'Singularity Iteration Manual',
        'mio_icif.configuration.fcs_hud': 'FCS HUD', 'mio_icif.configuration.flight': 'Viltrum flight',
        'mio_icif.configuration.layout': 'HUD layout',
        'mio_icif.configuration.hud_enabled': 'HUD on', 'mio_icif.configuration.palette': 'HUD colour',
        'mio_icif.configuration.esp_outlines': 'ESP outlines', 'mio_icif.configuration.holomap_scale': 'Holomap size',
        'mio_icif.configuration.camera_roll': 'Camera roll in turns', 'mio_icif.configuration.speed_fov': 'Speed FOV',
    },
    'zh_cn': {
        'block.mio_icif.producer.block_quantum_modification_station': '量子改装台',
        'module.mio_icif.slot.head': '头盔', 'module.mio_icif.slot.chest': '胸甲',
        'module.mio_icif.slot.legs': '护腿', 'module.mio_icif.slot.feet': '靴子',
        'module.mio_icif.tooltip.slots': '适用部位：%s',
        'module.mio_icif.tooltip.drain': '功耗：开启时 %s EU/t',
        'module.mio_icif.tooltip.deflector_cost': '功耗：每点伤害 %s EU，每次最多 %s 点',
        'module.mio_icif.tooltip.visor': '需要量子头盔面罩才能显示数据',
        'module.mio_icif.tooltip.station': '用量子改装台安装',
        'gui.mio_icif.station.units': '单元 %s/%s',
        'gui.mio_icif.station.insert_piece': '把量子套装部件放入上方槽位。',
        'gui.mio_icif.station.not_quantum': '该物品不接受升级单元。',
        'gui.mio_icif.station.no_units': '未安装单元。把单元放入下方槽位。',
        'gui.mio_icif.station.status.idle': '待机',
        'gui.mio_icif.station.status.working': '安装中',
        'gui.mio_icif.station.status.no_power': '电力不足',
        'gui.mio_icif.station.status.not_a_suit_piece': '不是量子部件',
        'gui.mio_icif.station.status.wrong_slot': '单元不适用该部件',
        'gui.mio_icif.station.status.already_installed': '单元已安装',
        'gui.mio_icif.station.status.full': '没有空闲单元位',
        'gui.mio_icif.station.status.no_unit': '待机',
        'gui.mio_icif.station.cost': '安装：%s EU/t，%s tick',
        'gui.mio_icif.station.eject_hint': '点击 × 拆下单元',
        'tooltip.mio_icif.armor.feature_viltrum_flight': '维尔特鲁姆飞行',
        'key.mio_icif.fcs_hud': 'FCS HUD 开关',
        'key.mio_icif.hud_layout': 'FCS HUD 布局编辑器',
        'hud.mio_icif.fcs.on': 'FCS HUD：开', 'hud.mio_icif.fcs.off': 'FCS HUD：关',
        'screen.mio_icif.hud_layout': 'FCS HUD 布局',
        'screen.mio_icif.hud_layout.hint': '拖动面板以移动。在全息地图上滚动滚轮以调整大小。',
        'screen.mio_icif.hud_layout.reset': '重置',
        'screen.mio_icif.hud_layout.status': '状态', 'screen.mio_icif.hud_layout.telemetry': '电网遥测',
        'screen.mio_icif.hud_layout.holomap': '全息地图', 'screen.mio_icif.hud_layout.ballistic': '弹道',
        'screen.mio_icif.hud_layout.blast': '爆炸预警', 'screen.mio_icif.hud_layout.flight': '维尔特鲁姆大气数据',
        'gui.mio_icif.armor_features.hud_layout': 'HUD 布局',
        'item.mio_icif.guide': '奇点迭代手册',
        'mio_icif.configuration.fcs_hud': 'FCS HUD', 'mio_icif.configuration.flight': '维尔特鲁姆飞行',
        'mio_icif.configuration.layout': 'HUD 布局',
        'mio_icif.configuration.hud_enabled': '启用 HUD', 'mio_icif.configuration.palette': 'HUD 颜色',
        'mio_icif.configuration.esp_outlines': 'ESP 轮廓', 'mio_icif.configuration.holomap_scale': '全息地图大小',
        'mio_icif.configuration.camera_roll': '转弯时镜头倾斜', 'mio_icif.configuration.speed_fov': '速度视野',
    },
    'ja_jp': {
        'block.mio_icif.producer.block_quantum_modification_station': '量子改造ステーション',
        'module.mio_icif.slot.head': 'ヘルメット', 'module.mio_icif.slot.chest': 'チェストプレート',
        'module.mio_icif.slot.legs': 'レギンス', 'module.mio_icif.slot.feet': 'ブーツ',
        'module.mio_icif.tooltip.slots': '装着部位：%s',
        'module.mio_icif.tooltip.drain': '消費電力：オン時 %s EU/t',
        'module.mio_icif.tooltip.deflector_cost': '消費電力：ダメージ1点あたり %s EU、1回最大 %s 点',
        'module.mio_icif.tooltip.visor': 'データ表示に量子ヘルメットのバイザーが必要',
        'module.mio_icif.tooltip.station': '量子改造ステーションで取り付ける',
        'gui.mio_icif.station.units': 'ユニット %s/%s',
        'gui.mio_icif.station.insert_piece': '量子スーツの部位を上のスロットに入れる。',
        'gui.mio_icif.station.not_quantum': 'このアイテムはアップグレードユニットを受け付けない。',
        'gui.mio_icif.station.no_units': 'ユニット未装着。ユニットを下のスロットに入れる。',
        'gui.mio_icif.station.status.idle': '待機',
        'gui.mio_icif.station.status.working': '取付中',
        'gui.mio_icif.station.status.no_power': '電力不足',
        'gui.mio_icif.station.status.not_a_suit_piece': '量子部位ではない',
        'gui.mio_icif.station.status.wrong_slot': 'この部位に装着できないユニット',
        'gui.mio_icif.station.status.already_installed': '装着済みのユニット',
        'gui.mio_icif.station.status.full': '空きユニット枠なし',
        'gui.mio_icif.station.status.no_unit': '待機',
        'gui.mio_icif.station.cost': '取付：%s EU/t × %s tick',
        'gui.mio_icif.station.eject_hint': '× をクリックしてユニットを外す',
        'tooltip.mio_icif.armor.feature_viltrum_flight': 'ヴィルトラム式飛行',
        'key.mio_icif.fcs_hud': 'FCS HUD オン/オフ',
        'key.mio_icif.hud_layout': 'FCS HUD レイアウト編集',
        'hud.mio_icif.fcs.on': 'FCS HUD：オン', 'hud.mio_icif.fcs.off': 'FCS HUD：オフ',
        'screen.mio_icif.hud_layout': 'FCS HUD レイアウト',
        'screen.mio_icif.hud_layout.hint': 'パネルをドラッグして移動する。ホロマップ上でスクロールしてサイズを変える。',
        'screen.mio_icif.hud_layout.reset': 'リセット',
        'screen.mio_icif.hud_layout.status': 'ステータス', 'screen.mio_icif.hud_layout.telemetry': '電網テレメトリ',
        'screen.mio_icif.hud_layout.holomap': 'ホロマップ', 'screen.mio_icif.hud_layout.ballistic': '弾道',
        'screen.mio_icif.hud_layout.blast': '爆発警告', 'screen.mio_icif.hud_layout.flight': 'ヴィルトラム飛行データ',
        'gui.mio_icif.armor_features.hud_layout': 'HUD配置',
        'item.mio_icif.guide': 'シンギュラリティ・イテレーション マニュアル',
        'mio_icif.configuration.fcs_hud': 'FCS HUD', 'mio_icif.configuration.flight': 'ヴィルトラム式飛行',
        'mio_icif.configuration.layout': 'HUD配置',
        'mio_icif.configuration.hud_enabled': 'HUDを表示', 'mio_icif.configuration.palette': 'HUDの色',
        'mio_icif.configuration.esp_outlines': 'ESP輪郭', 'mio_icif.configuration.holomap_scale': 'ホロマップの大きさ',
        'mio_icif.configuration.camera_roll': '旋回時のカメラ傾斜', 'mio_icif.configuration.speed_fov': '速度に応じた視野',
    },
}


def main():
    for lang in ['en_us', 'zh_cn', 'ja_jp']:
        path = os.path.join(LANG, lang + '.json')
        with open(path, encoding='utf-8') as f:
            data = json.load(f, object_pairs_hook=OrderedDict)
        add = OrderedDict(COMMON[lang])
        for i, uid in enumerate(UNITS):
            add['item.mio_icif.module.item_module_' + uid] = NAMES[lang][i]
            add['module.mio_icif.' + uid] = NAMES[lang][i]
            add['module.mio_icif.' + uid + '.desc'] = DESCS[lang][i]
        for k, v in add.items():
            data[k] = v
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write('\n')
    print('ok')


if __name__ == '__main__':
    main()
