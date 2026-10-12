#!/usr/bin/env python3
"""0.1.8.1 language keys (en_us / zh_cn / ja_jp) and sound definitions. ASD-STE100 style for English.

* Subtitles for the nine new sounds (jetpack thrusters, machine loops).
* Energy Management Terminal: sub-network rows and tooltip; the Japanese terminal keys that
  were missing (the terminal showed English text in Japanese).
* Harvester: storage-full status.
* sounds.json: the nine new sound events.

The script is idempotent: run it again after a change of the tables below.
Usage: lang_181.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/mio_icif')
LANG = os.path.join(ASSETS, 'lang')

_T = 'gui.mio_icif.energy_terminal.'
_S = 'mio_icif.subtitle.'

ADD = {
    'en_us': {
        _S + 'jetpack.electric.loop': 'Electric jetpack thrusts',
        _S + 'jetpack.advanced.loop': 'Advanced jetpack roars',
        _S + 'jetpack.ionic.loop': 'Ion thrusters hum',
        _S + 'jetpack.ignite': 'Jetpack ignites',
        _S + 'jetpack.shutdown': 'Jetpack shuts down',
        _S + 'machine.loop.compressor': 'Compressor pumps',
        _S + 'machine.loop.servo': 'Servos move',
        _S + 'machine.loop.discharge': 'Electricity crackles',
        _S + 'machine.loop.bubbler': 'Liquid bubbles',
        _T + 'subnet_devices': '%s dev',
        _T + 'tip.subnet.packet': 'Packet: %s EU (cable rating %s EU)',
        _T + 'tip.subnet.idle': 'No energy flow measured',
        _T + 'tip.subnet.scroll': 'Scroll to see more sub-networks',
        'gui.mio_icif.harvest.storage_full': 'Storage full: paused',
    },
    'zh_cn': {
        _S + 'jetpack.electric.loop': '电力喷气背包：推进',
        _S + 'jetpack.advanced.loop': '高级喷气背包：轰鸣',
        _S + 'jetpack.ionic.loop': '离子推进器：嗡鸣',
        _S + 'jetpack.ignite': '喷气背包：点火',
        _S + 'jetpack.shutdown': '喷气背包：熄火',
        _S + 'machine.loop.compressor': '压缩机：泵送',
        _S + 'machine.loop.servo': '伺服机构：运动',
        _S + 'machine.loop.discharge': '电流：噼啪作响',
        _S + 'machine.loop.bubbler': '液体：冒泡',
        _T + 'subnet_devices': '%s 台',
        _T + 'tip.subnet.packet': '电压包：%s EU（线缆额定 %s EU）',
        _T + 'tip.subnet.idle': '未测到能量流动',
        _T + 'tip.subnet.scroll': '滚动鼠标查看更多子网',
        'gui.mio_icif.harvest.storage_full': '存储已满：暂停',
    },
    'ja_jp': {
        _S + 'jetpack.electric.loop': '電気ジェットパック：噴射',
        _S + 'jetpack.advanced.loop': '高度ジェットパック：轟音',
        _S + 'jetpack.ionic.loop': 'イオンスラスター：作動音',
        _S + 'jetpack.ignite': 'ジェットパック：点火',
        _S + 'jetpack.shutdown': 'ジェットパック：停止',
        _S + 'machine.loop.compressor': 'コンプレッサー：圧送',
        _S + 'machine.loop.servo': 'サーボ：作動',
        _S + 'machine.loop.discharge': '放電音',
        _S + 'machine.loop.bubbler': '液体：泡立ち',
        _T + 'subnet_devices': '%s 台',
        _T + 'tip.subnet.packet': '電圧パケット：%s EU（電線定格 %s EU）',
        _T + 'tip.subnet.idle': 'エネルギーの流れなし',
        _T + 'tip.subnet.scroll': 'スクロールで他のサブネットを表示',
        'gui.mio_icif.harvest.storage_full': '格納庫満杯：停止中',
        # Energy Management Terminal keys that ja_jp did not have (English was shown)
        'block.mio_icif.wiring.block_energy_terminal': 'エネルギー管理端末',
        'container.mio_icif.energy_terminal': 'エネルギー管理端末',
        _T + 'summary': '機器 %s · 電線 %s',
        _T + 'filter.all': '全部',
        _T + 'filter.generators': '発電',
        _T + 'filter.consumers': '消費',
        _T + 'filter.storage': '蓄電',
        _T + 'no_network': '電線網に接続されていません',
        _T + 'no_match': 'この分類の機器はありません',
        _T + 'storage_flow': '入力 %s / 出力 %s EU/t',
        _T + 'ago': '%s 秒前',
        _T + 'category.generator': '発電機',
        _T + 'category.consumer': '消費機器',
        _T + 'category.storage': '蓄電機器',
        _T + 'category.transformer': '変圧器',
        _T + 'switch_hint': 'スイッチをクリックすると、この機器を電力網から切り離します',
        _T + 'switched_off': '停止中：電力網から切り離されています',
        'tooltip.mio_icif.energy_terminal': '電線網に取り付けると、発電・消費・蓄電を監視し、機器を遠隔で入切できます',
    },
}

# sound event -> (file, attenuation distance)
SOUNDS = OrderedDict([
    ('jetpack.electric.loop', ('sfx_jetpack_electric_loop', 24)),
    ('jetpack.advanced.loop', ('sfx_jetpack_advanced_loop', 32)),
    ('jetpack.ionic.loop', ('sfx_jetpack_ionic_loop', 28)),
    ('jetpack.ignite', ('sfx_jetpack_ignite', 24)),
    ('jetpack.shutdown', ('sfx_jetpack_shutdown', 20)),
    ('machine.loop.compressor', ('sfx_loop_compressor', 16)),
    ('machine.loop.servo', ('sfx_loop_servo', 16)),
    ('machine.loop.discharge', ('sfx_loop_discharge', 16)),
    ('machine.loop.bubbler', ('sfx_loop_bubbler', 16)),
])


def main():
    for lang, table in ADD.items():
        path = os.path.join(LANG, lang + '.json')
        with open(path, encoding='utf-8') as f:
            data = json.load(f, object_pairs_hook=OrderedDict)
        for k, v in table.items():
            data[k] = v
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write('\n')
        print(lang, 'keys set:', len(table))

    path = os.path.join(ASSETS, 'sounds.json')
    with open(path, encoding='utf-8') as f:
        sounds = json.load(f, object_pairs_hook=OrderedDict)
    for event, (name, distance) in SOUNDS.items():
        sounds[event] = OrderedDict([
            ('subtitle', _S + event),
            ('sounds', [OrderedDict([('name', 'mio_icif:' + name), ('attenuation_distance', distance)])]),
        ])
        if not os.path.exists(os.path.join(ASSETS, 'sounds', name + '.ogg')):
            raise SystemExit('missing sound file: ' + name + '.ogg')
    with open(path, 'w', encoding='utf-8', newline='\r\n') as f:
        json.dump(sounds, f, indent=2, ensure_ascii=False)
        f.write('\n')
    print('sounds.json events set:', len(SOUNDS))


if __name__ == '__main__':
    main()
