#!/usr/bin/env python3
"""0.1.9.0 language keys (en_us / zh_cn / ja_jp): the frame cores, their panel switches and
tooltips, the legacy shells, the station core socket and statuses, the core burst key and HUD
line, the config entries (common FrameCores section, client core_attachments) and the sounds.
Idempotent; keeps the line endings of each file.
Usage: lang_190.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
C = 'mio_icif.configuration.'
K = 'core.mio_icif.'

CORES = {
    'solar_array_1': (('Solar Array I', '太阳能阵列 I', 'ソーラーアレイ I'),
        ('A sun panel on a mast. Charges under open sky: the suit first, then your equipment.',
         '桅杆上的太阳能板。在露天下充电：先充套装，再充随身装备。',
         'マストに載ったソーラーパネル。空の下で充電する：まずスーツ、次に携行装備。')),
    'solar_array_2': (('Solar Array II', '太阳能阵列 II', 'ソーラーアレイ II'),
        ('A solar panel with two folding wings. Charges under open sky: the suit first, then your equipment.',
         '带两片折叠翼的太阳能板。在露天下充电：先充套装，再充随身装备。',
         '折りたたみ翼 2 枚のソーラーパネル。空の下で充電する：まずスーツ、次に携行装備。')),
    'solar_array_3': (('Solar Array III', '太阳能阵列 III', 'ソーラーアレイ III'),
        ('A sun crown: wings, a sun core on the brow and a solar fan. Charges under open sky: the suit first, then your equipment.',
         '太阳冠：双翼、额前的太阳核心与太阳能扇。在露天下充电：先充套装，再充随身装备。',
         'サンクラウン：翼、額のサンコア、ソーラーファン。空の下で充電する：まずスーツ、次に携行装備。')),
    'graviton': (('Graviton Bulwark', '重力子壁垒', 'グラビトン・バルワーク'),
        ('Gravity anchor: no fall damage, no knockback and no blast push. Armour +1. Emergency repair below 20 % health: 1 health per tick for 5,000 EU.',
         '重力锚：免疫摔落伤害，不受击退与爆炸冲击。护甲 +1。生命低于 20% 时紧急修复：每刻回复 1 点，消耗 5,000 EU。',
         '重力アンカー：落下ダメージなし、ノックバックと爆風の押し出しなし。防御力 +1。体力 20% 未満で緊急修復：1 tick ごとに 1 回復、5,000 EU。')),
    'propulsion': (('Advanced Propulsion', '高级推进', 'アドバンスド・プロパルジョン'),
        ('Twin boosters and a reactor: +90,000,000 EU, no flight ceiling, thrust +15 %, sharper maneuver vectoring, armour +1, knockback resistance +0.5, repair 2.0 health/s (30,000 EU/s).',
         '双推进器与反应堆：+90,000,000 EU，无飞行高度上限，推力 +15%，特殊机动矢量加速增强，护甲 +1，击退抗性 +0.5，修复 2.0 生命/秒（30,000 EU/秒）。',
         'ツインブースターとリアクター：+90,000,000 EU、飛行高度上限なし、推力 +15%、特殊機動のベクトル加速強化、防御力 +1、ノックバック耐性 +0.5、修復 2.0 体力/秒（30,000 EU/秒）。')),
}
CORE_ITEM = ('%s Core', '%s核心', '%s・コア')

LEGACY = ['item.mio_icif.normal.item_solar_helmet', 'item.mio_icif.armor.item_armor_advanced_solar_helmet',
          'item.mio_icif.armor.item_armor_hybrid_solar_helmet', 'item.mio_icif.armor.item_armor_ultimate_solar_helmet',
          'item.mio_icif.armor.item_armor_advanced_quantum_chestplate', 'item.mio_icif.armor.item_armor_heavy_quantum_chestplate']
LEGACY_SUFFIX = (' (Legacy)', '（旧式）', '（レガシー）')
# ja_jp had no names for the retired items
LEGACY_JA = ['ソーラーヘルメット', 'アドバンスド・ソーラーヘルメット', 'ハイブリッド・ソーラーヘルメット',
             'アルティメット・ハイブリッド・ソーラーヘルメット', 'アドバンスド量子チェストプレート', 'ヘビー量子チェストプレート']

OTHER = {
    K + 'tooltip.socket': ('Socket: %s', '插槽：%s', 'ソケット：%s'),
    K + 'socket.head': ('nano or quantum helmet', '纳米或量子头盔', 'ナノまたは量子ヘルメット'),
    K + 'socket.chest': ('nano or quantum chestplate', '纳米或量子胸甲', 'ナノまたは量子チェストプレート'),
    K + 'socket.quantum_head': ('quantum helmet', '量子头盔', '量子ヘルメット'),
    K + 'socket.quantum_chest': ('quantum chestplate', '量子胸甲', '量子チェストプレート'),
    K + 'tooltip.solar': ('Day %s EU/t · night %s EU/t · transfer up to %s EU/t', '白天 %s EU/t · 夜间 %s EU/t · 传输上限 %s EU/t',
                          '昼 %s EU/t · 夜 %s EU/t · 転送上限 %s EU/t'),
    K + 'tooltip.burst': ('Core burst key: high-voltage arc discharge and a knockback shock wave', '核心爆发键：高压电弧放电与击退冲击波',
                          'コアバーストキー：高電圧アーク放電とノックバック衝撃波'),
    K + 'tooltip.station': ('Install it with the Universal Suit Modification Station.', '用通用套装改装台安装。',
                            '汎用スーツ改造台で取り付ける。'),
    K + 'feature.solar': ('Solar array', '太阳能阵列', 'ソーラーアレイ'),
    K + 'feature.solar_all': ('Solar: charge carried equipment', '太阳能：为随身装备充电', 'ソーラー：携行装備も充電'),
    K + 'feature.emergency_repair': ('Emergency repair', '紧急修复', '緊急修復'),
    K + 'feature.anchor': ('Gravity anchor', '重力锚', '重力アンカー'),
    K + 'feature.repair': ('Nano repair', '纳米修复', 'ナノ修復'),
    K + 'feature.knockback': ('Knockback resistance', '击退抗性', 'ノックバック耐性'),
    K + 'feature.visual': ('Core exterior (3D)', '核心外装（3D）', 'コア外装（3D）'),
    'tooltip.mio_icif.legacy_armor': ('Retired item (0.1.9.0): it turns into a standard suit piece with a frame core',
                                      '已退役物品（0.1.9.0）：将转换为装有框架核心的标准套装部件',
                                      '退役アイテム（0.1.9.0）：フレームコア付きの標準スーツ部位に変わる'),
    'tooltip.mio_icif.legacy_armor.becomes': ('Becomes %s: when you carry it, in the modification station, or crafted alone',
                                              '转换为 %s：携带时、放入改装台或单独合成时',
                                              '変換先 %s：所持したとき、改造台、または単体でクラフトしたとき'),
    'tooltip.mio_icif.legacy_armor.into': ('%s + %s', '%s + %s', '%s + %s'),
    'gui.mio_icif.station.core_socket': ('Core socket', '核心插槽', 'コアソケット'),
    'gui.mio_icif.station.core_empty': ('Empty. Put a frame core into the unit slot.', '空。将框架核心放入单元槽。',
                                        '空き。フレームコアをユニットスロットに入れる。'),
    'gui.mio_icif.station.core_eject': ('Click to remove the core (it goes to you).', '点击取出核心（返还给你）。',
                                        'クリックでコアを取り外す（手元に戻る）。'),
    'gui.mio_icif.station.status.core_install': ('INSTALLING CORE', '正在安装核心', 'コア取付中'),
    'gui.mio_icif.station.status.core_swap': ('SWAPPING CORE (THE OLD ONE COMES BACK)', '正在更换核心（旧核心会返还）', 'コア交換中（旧コアは戻る）'),
    'gui.mio_icif.station.status.core_returned': ('REPLACED CORE: TAKE IT OUT', '已替换的核心：请取出', '交換したコア：取り出してください'),
    'gui.mio_icif.equipment_console.appearance.core_attachments': ('Frame core exterior (3D attachments)', '框架核心外装（3D 附件）',
                                                                   'フレームコア外装（3D アタッチメント）'),
    'key.mio_icif.core_burst': ('Core burst (arc discharge)', '核心爆发（电弧放电）', 'コアバースト（アーク放電）'),
    'hud.mio_icif.core.no_burst': ('No core burst: wear a quantum chestplate with an Advanced Propulsion core',
                                   '没有核心爆发：请穿戴装有高级推进核心的量子胸甲',
                                   'コアバーストなし：アドバンスド・プロパルジョン・コア付きの量子チェストプレートを着用する'),
    'mio_icif.subtitle.core.discharge': ('Arc discharge cracks', '电弧放电炸响', 'アーク放電が炸裂する'),
    'mio_icif.subtitle.core.install': ('Frame core seats', '框架核心就位', 'フレームコアが装着される'),
    # common config, section FrameCores
    C + 'FrameCores': ('Frame Cores', '框架核心', 'フレームコア'),
    C + 'core.section.cores': ('Core switches', '核心开关', 'コアのスイッチ'),
    C + 'core.section.cores.tooltip': ('A core that is off does nothing; it stays installed.', '关闭的核心不起作用，但仍保持安装。',
                                       'オフのコアは何もしない。取り付けたままになる。'),
    C + 'core.section.discharge': ('Arc discharge', '电弧放电', 'アーク放電'),
    C + 'core.section.discharge.tooltip': ('Core burst of the Advanced Propulsion core.', '高级推进核心的核心爆发。',
                                           'アドバンスド・プロパルジョン・コアのコアバースト。'),
    C + 'core.discharge_players': ('Discharge hits players', '放电可命中玩家', '放電がプレイヤーに当たる'),
    C + 'core.discharge_players.tooltip': ('The arc discharge also hits and pushes other players (only where PvP is on).',
                                           '电弧放电也会命中并推开其他玩家（仅在开启 PvP 时）。',
                                           'アーク放電がほかのプレイヤーにも当たり押し出す（PvP が有効なときだけ）。'),
    C + 'core.discharge_radius': ('Discharge radius', '放电半径', '放電半径'),
    C + 'core.discharge_radius.tooltip': ('Radius in blocks.', '半径（格）。', '半径（ブロック）。'),
    C + 'core.discharge_damage': ('Arc damage', '电弧伤害', 'アークのダメージ'),
    C + 'core.discharge_damage.tooltip': ('Damage of each arc (less at the edge of the radius).', '每道电弧的伤害（半径边缘较低）。',
                                          'アーク 1 本のダメージ（半径の端では少ない）。'),
    C + 'core.discharge_push': ('Shock wave push', '冲击波推力', '衝撃波の押し出し'),
    C + 'core.discharge_push.tooltip': ('Horizontal push of the shock wave at the centre.', '冲击波在中心处的水平推力。',
                                        '中心での衝撃波の水平の押し出し。'),
    C + 'core.discharge_eu': ('EU per discharge', '每次放电 EU', '放電 1 回の EU'),
    C + 'core.discharge_eu.tooltip': ('Taken from the suit energy pool.', '从套装能量池中扣除。', 'スーツのエネルギーから払う。'),
    C + 'core.discharge_cooldown': ('Discharge cooldown', '放电冷却', '放電のクールダウン'),
    C + 'core.discharge_cooldown.tooltip': ('Cooldown in ticks (20 ticks = 1 s).', '冷却时间（刻，20 刻 = 1 秒）。',
                                            'クールダウン（tick、20 tick = 1 秒）。'),
    # client config
    C + 'core_attachments': ('Frame core attachments', '框架核心外装', 'フレームコア外装'),
    C + 'core_attachments.tooltip': ('Frame cores show their exterior (solar wings, graviton ring, boosters) on worn helmets and chestplates.',
                                     '框架核心在穿戴的头盔与胸甲上显示外装（太阳能翼、重力环、推进器）。',
                                     'フレームコアが着用中のヘルメットとチェストプレートに外装（ソーラー翼、グラビトンリング、ブースター）を表示する。'),
}


def main():
    col = {'en_us': 0, 'zh_cn': 1, 'ja_jp': 2}
    for lang, i in col.items():
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        n = 0
        for cid, (name, desc) in CORES.items():
            pattern = CORE_ITEM[i] if not cid.startswith('solar') or i == 0 else ('%s 核心', '%s 核心', '%s コア')[i]
            data['item.mio_icif.module.item_core_' + cid] = pattern % name[i]
            data[K + cid] = name[i]
            data[K + cid + '.desc'] = desc[i]
            data[C + 'core.enabled.' + cid] = name[i]
            data[C + 'core.enabled.' + cid + '.tooltip'] = ('Switch: ', '开关：', 'スイッチ：')[i] + name[i]
            n += 5
        for j, key in enumerate(LEGACY):
            if key not in data and i == 2:
                data[key] = LEGACY_JA[j]
            if key in data and not data[key].endswith(LEGACY_SUFFIX[i]):
                data[key] = data[key] + LEGACY_SUFFIX[i]
                n += 1
        for key, t in OTHER.items():
            data[key] = t[i]
            n += 1
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', n)


if __name__ == '__main__':
    main()
