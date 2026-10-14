#!/usr/bin/env python3
"""0.1.9.1 language keys (en_us / zh_cn / ja_jp): machine contents and the Recovered Parts Case,
the induction furnace heat hold and the Thermal Retention Unit, the new client options and sounds.
Idempotent; keeps the line endings of each file.
Usage: lang_191.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
C = 'mio_icif.configuration.'
G = 'gui.mio_icif.induction.'

KEYS = {
    'item.mio_icif.normal.item_recovered_parts': ('Recovered Parts Case', '回收零件箱', '回収パーツケース'),
    'item.mio_icif.upgrade.thermal_retention_upgrade': ('Thermal Retention Unit', '保温单元', '保温ユニット'),
    'item.mio_icif.upgrade.thermal_retention.tooltip': (
        'Induction furnace: no heat loss while idle, and holding full heat costs no EU',
        '感应炉：空闲时不散热，保持满热量不耗电',
        '誘導炉：待機中に熱が逃げず、最大熱量の保持に EU を使わない'),
    'tooltip.mio_icif.machine_contents': ('Stored inside: %s stacks', '内部存放：%s 组物品', '内部に保管：%s スタック'),
    'tooltip.mio_icif.machine_contents.returned': (
        'Returned when this machine is used as a crafting material',
        '用作合成材料时返还', 'この機械をクラフト素材に使うと返却される'),
    'tooltip.mio_icif.recovered_parts': ('Contents: %s stacks', '内容：%s 组物品', '内容：%s スタック'),
    'tooltip.mio_icif.recovered_parts.use': (
        'Unpacks itself in your inventory (or right-click)', '放入背包时自动拆开（也可右键）', 'インベントリに入ると自動で開く（右クリックでも）'),
    'tooltip.mio_icif.recovered_parts.more': ('... and %s more', '……另外 %s 组', '……ほか %s スタック'),
    G + 'heat': ('Heat: %s%%', '热量：%s%%', '熱量：%s%%'),
    G + 'hold_title': ('Heat Hold', '保温模式', '保温モード'),
    G + 'hold_on': ('Heat hold: ON (heats while idle and holds full heat)', '保温：开（空闲时也加热并保持满热量）',
                    '保温：オン（待機中も加熱し、最大熱量を保つ）'),
    G + 'hold_off': ('Heat hold: OFF', '保温：关', '保温：オフ'),
    G + 'hold_tip': ('Click to switch. Holding full heat costs 1 EU/t, nothing with a Thermal Retention Unit.',
                     '点击切换。保持满热量耗电 1 EU/t，装有保温单元时不耗电。',
                     'クリックで切替。最大熱量の保持は 1 EU/t、保温ユニットがあれば 0。'),
    G + 'eta': ('Full heat in %s:%s', '%s:%s 后达到满热量', '最大熱量まで %s:%s'),
    G + 'insulated': ('Thermal Retention Unit: no heat loss, free hold', '保温单元：不散热，保温免费', '保温ユニット：放熱なし、保温は無料'),
    G + 'not_insulated': ('No Thermal Retention Unit: the heat drops while idle', '无保温单元：空闲时热量下降',
                          '保温ユニットなし：待機中は熱が下がる'),
    C + 'photon_fx': ('Quantum photon effects', '量子光子特效', '量子フォトン演出'),
    C + 'photon_fx.tooltip': ('Photon barrier of the deflector and the photon afterglow of fast Special Maneuver flight.',
                              '偏转器的光子屏障与高速特殊机动的光子残光。',
                              'ディフレクターのフォトンバリアと、高速特殊機動のフォトン残光。'),
    C + 'scan_fx': ('Active scan effect', '主动扫描特效', 'アクティブスキャン演出'),
    C + 'scan_fx.tooltip': ('A scan sphere expands from you every 3 s while an entity sensor unit runs.',
                            '实体传感单元运行时，每 3 秒从你身上扩散出扫描球。',
                            'エンティティ系センサーユニットの稼働中、3 秒ごとにスキャン球が広がる。'),
    C + 'wingman_fx': ('Wingman effects', '僚机特效', 'ウィングマン演出'),
    C + 'wingman_fx.tooltip': ('Lock-on scan laser and hologram marker, muzzle flashes, thruster boosts, attitude jets and slash arcs.',
                               '锁定扫描激光与全息标记、枪口火光、推进器冲刺、姿态喷射与斩击弧光。',
                               'ロックオンのスキャンレーザーとホログラムマーカー、マズルフラッシュ、スラスターブースト、姿勢制御噴射、斬撃の弧。'),
    'mio_icif.subtitle.wingman.boost': ('Wingman thrusters burst', '僚机推进器喷射', 'ウィングマンのスラスターが噴射する'),
    'mio_icif.subtitle.wingman.lock': ('Wingman locks on', '僚机锁定目标', 'ウィングマンがロックオンする'),
    'mio_icif.subtitle.suit.photon_barrier': ('Photon barrier rings', '光子屏障鸣响', 'フォトンバリアが鳴る'),
}


# ja_jp had no upgrade item keys at all (the English text showed); 0.1.9.1 fills them
JA_EXTRA = {
    'item.mio_icif.upgrade.overclocker_upgrade': 'オーバークロッカーアップグレード',
    'item.mio_icif.upgrade.energy_storage_upgrade': 'エネルギーストレージアップグレード',
    'item.mio_icif.upgrade.transformer_upgrade': '変圧器アップグレード',
    'item.mio_icif.upgrade.ejector_upgrade': 'エジェクターアップグレード',
    'item.mio_icif.upgrade.pulling_upgrade': '吸入アップグレード',
    'item.mio_icif.upgrade.fluid_ejector_upgrade': '流体エジェクターアップグレード',
    'item.mio_icif.upgrade.redstone_inv_upgrade': 'レッドストーン信号反転アップグレード',
    'item.mio_icif.upgrade.fluid_pulling_upgrade': '流体吸入アップグレード',
    'item.mio_icif.upgrade.overclocker.tooltip': '1 tick あたりの進行が増え、EU 消費が増え、蓄電量が 1,000 EU 増える',
    'item.mio_icif.upgrade.energy_storage.tooltip': '蓄電量が 10,000 EU 増える',
    'item.mio_icif.upgrade.transformer.tooltip': '入力電圧 Tier を 1 段上げる',
    'item.mio_icif.upgrade.ejector.tooltip': '隣接するコンテナへアイテムを自動で出力する',
    'item.mio_icif.upgrade.pulling.tooltip': '隣接するコンテナからアイテムを自動で吸い込む',
    'item.mio_icif.upgrade.fluid_ejector.tooltip': '隣接するタンクへ流体を自動で出力する',
    'item.mio_icif.upgrade.redstone_inverter.tooltip': '現在のレッドストーン信号を反転する',
    'item.mio_icif.upgrade.fluid_pulling.tooltip': '隣接するタンクから流体を自動で吸い込む',
    'item.mio_icif.upgrade.direction.anyside': '全方向',
    'item.mio_icif.upgrade.direction.set': '方向：%s',
    'item.mio_icif.upgrade.direction.down': '下',
    'item.mio_icif.upgrade.direction.up': '上',
    'item.mio_icif.upgrade.direction.north': '北',
    'item.mio_icif.upgrade.direction.south': '南',
    'item.mio_icif.upgrade.direction.west': '西',
    'item.mio_icif.upgrade.direction.east': '東',
    'item.mio_icif.upgrade.ejector.direction': '出力方向：%s',
    'item.mio_icif.upgrade.pulling.direction': '吸入方向：%s',
    'item.mio_icif.upgrade.fluid_ejector.direction': '流体出力方向：%s',
    'item.mio_icif.upgrade.fluid_pulling.direction': '流体吸入方向：%s',
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
        if lang == 'ja_jp':
            for key, t in JA_EXTRA.items():
                data.setdefault(key, t)
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(KEYS))


if __name__ == '__main__':
    main()
