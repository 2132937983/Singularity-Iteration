#!/usr/bin/env python3
"""0.1.7.34 language keys (en_us / zh_cn / ja_jp): Special Maneuver Mode rename, new sound
subtitles, compass switch, reworked FCS unit descriptions. Text follows ASD-STE100 rules."""
import json, os, sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

REMOVE = ['tooltip.mio_icif.armor.feature_viltrum_flight', 'screen.mio_icif.hud_layout.flight']

ADD = {
    'en_us': {
        'tooltip.mio_icif.armor.feature_special_maneuver': 'Special Maneuver Mode',
        'mio_icif.configuration.flight': 'Special Maneuver Mode',
        'mio_icif.configuration.show_compass': 'Heading tape (compass)',
        'screen.mio_icif.hud_layout.compass': 'Heading tape: %s',
        'subtitles.mio_icif.maneuver.boom': 'Shock barrier crack',
        'subtitles.mio_icif.maneuver.wind': 'Air rush',
        'subtitles.mio_icif.maneuver.takeoff': 'Maneuver launch',
        'subtitles.mio_icif.fcs.echo': 'Sonar pulse',
        'subtitles.mio_icif.fcs.lock': 'Ballistic lock',
        'subtitles.mio_icif.fcs.blast_beep': 'Blast warning beep',
        'module.mio_icif.ore_scanner.desc': 'Sends a sonar pulse through the ground in a 16-block radius. Ores light up when the echo reaches them.',
        'module.mio_icif.holomap.desc': 'Shows a 3D box hologram of the blocks and the creatures around you.',
        'module.mio_icif.ballistic.desc': 'Calculates the path of arrows and thrown items. Shows the arc, the impact point and the lead point.',
    },
    'zh_cn': {
        'tooltip.mio_icif.armor.feature_special_maneuver': '特殊机动模式',
        'mio_icif.configuration.flight': '特殊机动模式',
        'mio_icif.configuration.show_compass': '航向带（罗盘）',
        'screen.mio_icif.hud_layout.compass': '航向带：%s',
        'subtitles.mio_icif.maneuver.boom': '音障突破爆鸣',
        'subtitles.mio_icif.maneuver.wind': '气流呼啸',
        'subtitles.mio_icif.maneuver.takeoff': '机动起飞',
        'subtitles.mio_icif.fcs.echo': '声呐脉冲',
        'subtitles.mio_icif.fcs.lock': '弹道锁定',
        'subtitles.mio_icif.fcs.blast_beep': '爆炸预警蜂鸣',
        'module.mio_icif.ore_scanner.desc': '向半径16格的地层发出声呐脉冲。回波到达矿物时矿物高亮。',
        'module.mio_icif.holomap.desc': '以立方体全息影像显示周围的方块与生物。',
        'module.mio_icif.ballistic.desc': '计算箭矢与投掷物的轨迹。显示弹道弧线、落点与提前量点。',
    },
    'ja_jp': {
        'tooltip.mio_icif.armor.feature_special_maneuver': '特殊機動モード',
        'mio_icif.configuration.flight': '特殊機動モード',
        'mio_icif.configuration.show_compass': '方位テープ（コンパス）',
        'screen.mio_icif.hud_layout.compass': '方位テープ：%s',
        'subtitles.mio_icif.maneuver.boom': '音速突破の衝撃音',
        'subtitles.mio_icif.maneuver.wind': '風切り音',
        'subtitles.mio_icif.maneuver.takeoff': '機動発進',
        'subtitles.mio_icif.fcs.echo': 'ソナーパルス',
        'subtitles.mio_icif.fcs.lock': '弾道ロック',
        'subtitles.mio_icif.fcs.blast_beep': '爆発警告ビープ',
        'module.mio_icif.ore_scanner.desc': '半径16ブロックの地層にソナーパルスを送る。エコーが届いた鉱石が光る。',
        'module.mio_icif.holomap.desc': '周囲のブロックと生物を立方体のホログラムで表示する。',
        'module.mio_icif.ballistic.desc': '矢と投擲物の弾道を計算する。弾道の弧、着弾点、偏差点を表示する。',
    },
}


def main():
    for lang, add in ADD.items():
        path = os.path.join(LANG, lang + '.json')
        with open(path, encoding='utf-8') as f:
            data = json.load(f, object_pairs_hook=OrderedDict)
        for k in REMOVE:
            data.pop(k, None)
        for k, v in add.items():
            data[k] = v
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write('\n')
    print('ok')


if __name__ == '__main__':
    main()
