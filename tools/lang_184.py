#!/usr/bin/env python3
"""0.1.8.4 language keys (en_us / zh_cn / ja_jp).

* Common config: the Blueprint Builder UU supply and the output multiplier of every EU generator
  (mio_icif.configuration.<key> and <key>.tooltip, as the NeoForge config screen shows them).
* Blueprint Builder screen: the "UU" line of materials that replicators make for the build.

The script is idempotent and keeps the line endings of each file.
Usage: lang_184.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
C = 'mio_icif.configuration.'

# generator id: (en, zh, ja)
GENERATORS = {
    'solar_generator': ('Solar Generator', '太阳能发电机', 'ソーラー発電機'),
    'advanced_solar_panel': ('Advanced Solar Panel', '高级太阳能板', 'アドバンスド・ソーラーパネル'),
    'hybrid_solar_panel': ('Hybrid Solar Panel', '混合太阳能板', 'ハイブリッド・ソーラーパネル'),
    'ultimate_hybrid_solar_panel': ('Ultimate Hybrid Solar Panel', '终极混合太阳能板', 'アルティメット・ハイブリッド・ソーラーパネル'),
    'quantum_solar_panel': ('Quantum Solar Panel', '量子太阳能板', '量子ソーラーパネル'),
    'mets_advanced_solar_generator': ('Advanced Solar Generator (METS)', '高级太阳能发电机（METS）', 'アドバンスド・ソーラー発電機（METS）'),
    'photon_resonance_solar_generator': ('Photon Resonance Solar Generator', '光子共振太阳能发电机', '光子共振ソーラー発電機'),
    'ultimate_photon_resonance_solar_generator': ('Ultimate Photon Resonance Solar Generator', '终极光子共振太阳能发电机', 'アルティメット光子共振ソーラー発電機'),
    'wind_generator': ('Wind Mill', '风力发电机', '風力発電機'),
    'water_generator': ('Water Mill', '水力发电机', '水力発電機'),
    'geo_generator': ('Geothermal Generator', '地热发电机', '地熱発電機'),
    'geomagnetic_generator': ('Geomagnetic Generator', '地磁发电机', '地磁気発電機'),
    'nuclear_reactor_generator': ('Nuclear Reactor (EU mode)', '核反应堆（EU 模式）', '原子炉（EU モード）'),
    'rt_generator': ('Radioisotope Thermoelectric Generator', '放射性同位素温差发电机', '放射性同位体熱電発電機'),
    'kinetic_generator': ('Kinetic Generator', '动能发电机', '運動エネルギー発電機'),
    'turbo_kinetic_generator': ('Turbo Kinetic Generator', '涡轮动能发电机', 'ターボ運動エネルギー発電機'),
    'twin_turbo_kinetic_generator': ('Twin Turbo Kinetic Generator', '双涡轮动能发电机', 'ツインターボ運動エネルギー発電機'),
    'semifluid_generator': ('Semifluid Generator', '半流质发电机', '半流体発電機'),
    'advanced_semifluid_generator': ('Advanced Semifluid Generator', '高级半流质发电机', 'アドバンスド半流体発電機'),
    'diesel_generator': ('Diesel Generator', '柴油发电机', 'ディーゼル発電機'),
    'stirling_generator': ('Stirling Generator', '斯特林发电机', 'スターリング発電機'),
    'advanced_stirling_generator': ('Advanced Stirling Generator', '高级斯特林发电机', 'アドバンスド・スターリング発電機'),
    'thermal_generator': ('Generator', '火力发电机', '火力発電機'),
    'drop_generator': ('Drop Generator', '掉落物发电机', 'ドロップ発電機'),
    'advanced_drop_generator': ('Advanced Drop Generator', '高级掉落物发电机', 'アドバンスド・ドロップ発電機'),
    'experience_generator': ('Experience Generator', '经验发电机', '経験値発電機'),
    'advanced_experience_generator': ('Advanced Experience Generator', '高级经验发电机', 'アドバンスド経験値発電機'),
    'quantum_generator': ('Quantum Generator', '量子发电机', '量子発電機'),
}

GEN_TIP = ('Output multiplier: 1.0 = stock EU/t, 2.0 = double, 0 = off. Fuel burns at the same speed.',
           '发电量倍率：1.0 = 原始 EU/t，2.0 = 两倍，0 = 停止。燃料消耗速度不变。',
           '発電量の倍率：1.0 = 既定の EU/t、2.0 = 2 倍、0 = 停止。燃料の消費速度は変わらない。')
GEN_NAME = ('%s output', '%s 发电量', '%s の発電量')

OTHER = {
    C + 'builder_replicator_supply': ('Builder: UU supply from replicators', '建筑机：复制机 UU 供料', '建築機：複製機からの UU 供給'),
    C + 'builder_replicator_supply.tooltip': (
        'When no Material Interface has a material, the Blueprint Builder orders it from an idle Replicator in range that has its pattern. The Replicator pays with its own UU matter and energy.',
        '材料接口中没有某种材料时，蓝图建筑机向范围内拥有其模板的空闲复制机下单，复制机用自身的 UU 物质与能量支付。',
        '資材インターフェースにない資材を、範囲内でそのパターンを持つ待機中の複製機に発注する。複製機は自分の UU 物質と電力で支払う。'),
    C + 'builder_replicator_range': ('Builder: replicator range', '建筑机：复制机范围', '建築機：複製機の範囲'),
    C + 'builder_replicator_range.tooltip': ('Distance in blocks in which the Blueprint Builder finds Replicators.',
                                              '蓝图建筑机搜索复制机的距离（格）。', '設計図建築機が複製機を探す距離（ブロック）。'),
    'gui.mio_icif.builder.replicating': ('UU:', 'UU 复制:', 'UU 複製:'),
}


def main():
    col = {'en_us': 0, 'zh_cn': 1, 'ja_jp': 2}
    for lang, i in col.items():
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8'), object_pairs_hook=OrderedDict)
        n = 0
        for gid, names in GENERATORS.items():
            key = C + 'generator_output.' + gid
            data[key] = GEN_NAME[i] % names[i]
            data[key + '.tooltip'] = GEN_TIP[i]
            n += 2
        for key, t in OTHER.items():
            data[key] = t[i]
            n += 1
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', n)


if __name__ == '__main__':
    main()
