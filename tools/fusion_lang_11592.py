#!/usr/bin/env python3
"""0.1.11.92: the fusion reactor's texts (deuterium cell, controller screen, messages, multiblock plan, JEI) in en_us, zh_cn,
ja_jp and ru_ru, inserted after the fusion block names (keeps each file's CRLF lines and order). Idempotent.

Usage: fusion_lang_11592.py <repo root>
"""
import json
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
ANCHOR = '"block.mio_icif.fusion.block_fusion_mitigation_port"'
G = 'gui.mio_icif.fusion.'
S = 'structure.mio_icif.fusion_reactor'

# key, en, zh, ja, ru
TEXTS = [
    ('item.mio_icif.normal.item_deuterium_cell', 'Deuterium Cell', '氘单元', '重水素セル', 'Дейтериевая капсула'),
    (G + 'state.off', 'Off', '停机', '停止', 'Выключен'),
    (G + 'state.cooling', 'Cooling the magnets', '磁体冷却中', '磁石を冷却中', 'Охлаждение магнитов'),
    (G + 'state.charging', 'Charging the solenoid', '中心螺线管充能', 'ソレノイド充電中', 'Зарядка соленоида'),
    (G + 'state.heating', 'Heating the plasma', '加热等离子体', 'プラズマ加熱中', 'Нагрев плазмы'),
    (G + 'state.burning', 'Burning', '燃烧中', '燃焼中', 'Горение'),
    (G + 'state.disrupted', 'Disrupted', '已破裂', 'ディスラプション', 'Срыв'),
    (G + 'state.quenched', 'Magnets quenched', '磁体失超', '磁石クエンチ', 'Срыв сверхпроводимости'),
    (G + 'progress.cooling', '%s%%', '%s%%', '%s%%', '%s%%'),
    (G + 'progress.charging', '%s%%', '%s%%', '%s%%', '%s%%'),
    (G + 'progress.burn', '%s s', '%s 秒', '%s 秒', '%s с'),
    (G + 'progress.steady', '(steady)', '（稳态）', '（定常）', '(стационарно)'),   # 0.1.11.93: two ECRH heaters, no pulse limit
    (G + 'progress.timer', '%s s', '%s 秒', '%s 秒', '%s с'),
    (G + 'temperature', '%s keV (%s x10^8 K)', '温度 %s keV（%s 亿K）', '温度 %s keV（%s 億K）', '%s кэВ (%s x10^8 K)'),
    (G + 'q', 'Q %s', 'Q %s', 'Q %s', 'Q %s'),
    (G + 'fusion', 'Fusion %s EU/t', '聚变 %s EU/t', '核融合 %s EU/t', 'Синтез %s EU/t'),
    (G + 'output', 'Output %s EU/t', '输出 %s EU/t', '出力 %s EU/t', 'Выход %s EU/t'),
    (G + 'feed', 'Feed %s / %s', '加料 %s / %s', '燃料供給 %s / %s', 'Подача %s / %s'),
    (G + 'fuel', 'D %s  T %s', '氘 %s  氚 %s', 'D %s  T %s', 'D %s  T %s'),
    (G + 'ports', 'Out %s Heat %s Cryo %s Exh %s Fuel %s Mit %s', '输出%s 加热%s 低温%s 排气%s 加料%s 缓解%s',
     '出力%s 加熱%s 冷却%s 排気%s 燃料%s 緩和%s', 'Вых%s Нагр%s Крио%s Выхл%s Топл%s Срыв%s'),
    (G + 'stores', 'In %s  Heat %s/%s', '输入 %s  包层热 %s/%s', '入力 %s  熱 %s/%s', 'Вход %s  Тепло %s/%s'),
    (G + 'slot.deuterium', 'D', '氘', 'D', 'D'),
    (G + 'slot.tritium', 'T', '氚', 'T', 'T'),
    (G + 'slot.empty', 'Out', '空', '空', 'Пуст'),
    (G + 'slot.ice', 'Ice', '冰', '氷', 'Лёд'),
    (G + 'status.ok', 'Ready to run', '运行条件齐全', '運転できる', 'Готов к работе'),
    (G + 'status.unformed', 'The structure is incomplete', '结构未成型', '構造が未完成', 'Структура не собрана'),
    (G + 'status.no_signal', 'Needs a redstone signal', '需要红石信号', 'レッドストーン信号が必要', 'Нужен сигнал красного камня'),
    (G + 'status.no_cryo', 'Needs a cryogenic port', '缺少低温端口', '極低温ポートが必要', 'Нужен криогенный порт'),
    (G + 'status.no_heater', 'Needs a heating port', '缺少加热端口（中性束或电子回旋）', '加熱ポートが必要', 'Нужен порт нагрева'),
    (G + 'status.no_exhaust', 'No exhaust port: helium ash builds up', '缺少排气端口：氦灰会累积', '排気ポートなし：ヘリウム灰がたまる',
     'Нет выхлопного порта: копится гелий'),
    (G + 'status.no_power_port', 'No power output port', '缺少能量输出端口', '電力出力ポートがない', 'Нет порта вывода энергии'),
    (G + 'status.no_fuel', 'Needs deuterium and tritium', '需要氘和氚', '重水素と三重水素が必要', 'Нужны дейтерий и тритий'),
    (G + 'status.blanket_invalid', 'The blanket holds something that is not a reactor component', '包层里有不是反应堆元件的物品',
     'ブランケットに原子炉部品でない物がある', 'В бланкете не компонент реактора'),
    (G + 'unformed_at', 'Wrong block at %s, %s, %s: needs %s', '%s, %s, %s 处方块不对：应为%s', '%s, %s, %s のブロックが違う：%sが必要',
     'Неверный блок %s, %s, %s: нужен %s'),
    (G + 'part.central_solenoid', 'Central Solenoid', '中心螺线管', '中心ソレノイド', 'Центральный соленоид'),
    (G + 'part.toroidal_coil', 'Toroidal Field Coil', '环向场线圈', 'トロイダル磁場コイル', 'Катушка тороидального поля'),
    (G + 'part.poloidal_coil', 'Poloidal Field Coil', '极向场线圈', 'ポロイダル磁場コイル', 'Катушка полоидального поля'),
    (G + 'part.vacuum_vessel', 'Vacuum Vessel Wall', '真空室壁', '真空容器壁', 'Стенка вакуумной камеры'),
    (G + 'part.blanket', 'Blanket Module', '包层模块', 'ブランケットモジュール', 'Модуль бланкета'),
    (G + 'part.divertor', 'Divertor', '偏滤器', 'ダイバータ', 'Дивертор'),
    (G + 'part.port_slot', 'a port or a vacuum vessel wall', '端口或真空室壁', 'ポートか真空容器壁', 'порт или стенка камеры'),
    (G + 'part.controller', 'the console', '控制台', 'コントローラー', 'контроллер'),
    (G + 'part.plasma', 'air (the plasma channel)', '空气（等离子体通道）', '空気（プラズマの通路）', 'воздух (канал плазмы)'),
    ('message.mio_icif.fusion.disruption', 'Plasma disruption! The components of the blanket\'s first column were destroyed.',
     '等离子体破裂！包层第一列的元件被摧毁。', 'プラズマ・ディスラプション！ブランケット第1列の部品が壊れた。',
     'Срыв плазмы! Компоненты первого столбца бланкета разрушены.'),
    ('message.mio_icif.fusion.disruption_mitigated', 'Plasma disruption, mitigated by the pellet injection (one ice used).',
     '等离子体破裂，已由碎丸注入缓解（用掉一块冰）。', 'プラズマ・ディスラプション。ペレット入射で緩和した（氷を1個使用）。',
     'Срыв плазмы, смягчён инжекцией пеллет (израсходован лёд).'),
    (S, 'Fusion Reactor (Tokamak)', '聚变堆（托卡马克）', '核融合炉（トカマク）', 'Термоядерный реактор (токамак)'),
    (S + '.solenoid', 'Central solenoid', '中心螺线管', '中心ソレノイド', 'Центральный соленоид'),
    (S + '.toroidal', 'Toroidal field coils (eight D-shaped coils)', '环向场线圈（8 个 D 形线圈）', 'トロイダル磁場コイル（D形8基）',
     'Катушки тороидального поля (восемь D-образных)'),
    (S + '.poloidal', 'Outer poloidal field coils', '外极向场线圈', '外側ポロイダル磁場コイル', 'Внешние катушки полоидального поля'),
    (S + '.vessel', 'Vacuum vessel (with the port ducts)', '真空室（含端口管道）', '真空容器（ポートダクトを含む）', 'Вакуумная камера (с патрубками портов)'),
    (S + '.blanket', 'Blanket modules', '包层模块', 'ブランケットモジュール', 'Модули бланкета'),
    (S + '.divertor', 'Divertor (the floor of the plasma channel)', '偏滤器（等离子体通道的底）', 'ダイバータ（プラズマ通路の床）',
     'Дивертор (дно канала плазмы)'),
    (S + '.port', 'Port slot: any fusion port, or a vacuum vessel wall to close it', '端口位置：任意聚变端口，或用真空室壁封住',
     'ポート位置：任意の核融合ポート、または真空容器壁でふさぐ', 'Место порта: любой порт реактора или стенка камеры'),
    (S + '.note.plasma', 'The plasma channel inside the blanket must stay empty (air).', '包层内的等离子体通道必须保持为空（空气）。',
     'ブランケット内のプラズマ通路は空（空気）のままにする。', 'Канал плазмы внутри бланкета должен оставаться пустым (воздух).'),
    (S + '.note.ports', 'To run it needs a cryogenic port, a heating port (neutral beam or electron cyclotron), an exhaust port and a power output port.',
     '运行需要：低温端口、加热端口（中性束或电子回旋）、排气端口和能量输出端口。',
     '運転には極低温ポート、加熱ポート（中性粒子ビームか電子サイクロトロン）、排気ポート、電力出力ポートが必要。',
     'Для работы нужны криогенный порт, порт нагрева, выхлопной порт и порт вывода энергии.'),
    (S + '.note.turn', 'The whole structure turns with the console.', '整个结构随控制台的朝向转动。', '構造全体がコントローラーの向きに合わせて回る。',
     'Вся структура поворачивается вместе с контроллером.'),
    ('jei.mio_icif.fusion_info.1', 'The fusion reactor (a tokamak): a torus 25 blocks across with eight toroidal field coils, two outer poloidal field coils and a central solenoid. See the multiblock plan for the layout.',
     '聚变堆（托卡马克）：宽 25 格的环面，带 8 个环向场线圈、2 圈外极向场线圈和中心螺线管。布局见多方块结构图。',
     '核融合炉（トカマク）：幅25ブロックのトーラスに、トロイダル磁場コイル8基、外側ポロイダル磁場コイル2本、中心ソレノイド。配置はマルチブロック図を参照。',
     'Термоядерный реактор (токамак): тор шириной 25 блоков, восемь катушек тороидального поля, две внешние полоидальные и центральный соленоид.'),
    ('jei.mio_icif.fusion_info.2', 'Its blanket takes the fission reactor\'s components: rods fission from the plasma\'s neutrons, lithium cells breed tritium, vents and heat exchangers keep the blanket cool.',
     '包层可放入核反应堆的元件：燃料棒在聚变中子下裂变，锂单元增殖氚，散热片和热交换器给包层降温。',
     'ブランケットには原子炉の部品を入れられる：燃料棒はプラズマの中性子で核分裂し、リチウムセルは三重水素を増殖し、放熱板と熱交換器が冷やす。',
     'Бланкет принимает компоненты ядерного реактора: стержни делятся от нейтронов плазмы, литий нарабатывает тритий.'),
    ('jei.mio_icif.fusion_info.3', 'Up to 39,200,000 EU/t with the best blanket (twice the Mekanism fusion reactor). Deuterium comes from the centrifuge (water cells), tritium from lithium cells.',
     '最优包层最高 39,200,000 EU/t（Mek 聚变堆的二倍）。氘由离心机从水单元分离，氚由锂单元增殖。',
     '最良のブランケットで最大39,200,000 EU/t。重水素は遠心分離機で水セルから、三重水素はリチウムセルから得る。',
     'До 39 200 000 EU/t с лучшим бланкетом. Дейтерий — из центрифуги (водяные капсулы), тритий — из литиевых капсул.'),
]


def main():
    added = 0
    for code, col in (('en_us', 1), ('zh_cn', 2), ('ja_jp', 3), ('ru_ru', 4)):
        path = os.path.join(LANG, code + '.json')
        raw = open(path, 'rb').read().decode('utf-8')
        eol = '\r\n' if '\r\n' in raw else '\n'
        s = raw.replace('\r\n', '\n')
        anchor = ANCHOR
        for entry in TEXTS:
            key = '"%s"' % entry[0]
            if key not in s:
                j = s.index('\n', s.index(anchor)) + 1
                s = s[:j] + '  %s: %s,\n' % (key, json.dumps(entry[col], ensure_ascii=False)) + s[j:]
                added += 1
            anchor = key
        json.loads(s)
        open(path, 'wb').write(s.replace('\n', eol).encode('utf-8'))
    print('texts added:', added)


if __name__ == '__main__':
    main()
