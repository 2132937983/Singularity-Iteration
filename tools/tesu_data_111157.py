#!/usr/bin/env python3
"""0.1.11.157: the TESU (太级储电罐) data: blockstates, block and item models, loot tables, recipes, block tags and the four
languages. Running it again writes the same files (lang keys and tag entries are replaced in place, not added twice).

0.1.11.166: the top electrode is gone (its id is an alias of the casing); running this removes its files, lang keys and tag entry.

0.1.11.167: the relay is a pylon three blocks tall; its models and blockstates (and the column's) are written by
tools/tesu_relay_pillar_111167.py, not here. The column (no item, no drop) gets its name and its tool tags here."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
A = RES / 'assets/mio_icif'
D = RES / 'data'
NS = 'mio_icif'
W = 'wiring/'
TEX = NS + ':block/wiring/'

BLOCKS = ['block_tesu_casing', 'block_tesu_cell', 'block_tesu_controller', 'block_tesu_input', 'block_tesu_output',
          'block_tesu_wireless_port', 'block_tesu_relay']   # 0.1.11.158: the wireless field's blocks
MACHINES = ['block_tesu_controller', 'block_tesu_input', 'block_tesu_output', 'block_tesu_wireless_port', 'block_tesu_relay']
FACING = ['block_tesu_controller', 'block_tesu_input', 'block_tesu_output']   # the entity blocks carry a horizontal facing
PYLON = {'block_tesu_relay'}   # 0.1.11.167: modelled by tools/tesu_relay_pillar_111167.py
COLUMN = 'block_tesu_relay_column'   # 0.1.11.167: the pylon's shaft and head (no item, no loot)


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')


def models():
    cube = {'block_tesu_casing': 'block_tesu_casing', 'block_tesu_cell': 'block_tesu_cell',
            'block_tesu_input': 'block_tesu_input', 'block_tesu_output': 'block_tesu_output',
            'block_tesu_wireless_port': 'block_tesu_wireless_port'}
    for name, tex in cube.items():
        write(A / 'models/block' / W / (name + '.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': TEX + tex}})
    write(A / 'models/block' / W / 'block_tesu_controller.json', {'parent': 'minecraft:block/orientable', 'textures': {
        'front': TEX + 'block_tesu_controller_front', 'side': TEX + 'block_tesu_casing', 'top': TEX + 'block_tesu_casing'}})
    for name in BLOCKS:
        if name not in PYLON:
            write(A / 'models/item' / W / (name + '.json'), {'parent': NS + ':block/' + W + name})
    # 0.1.11.158: the wireless upgrade and the connector
    write(A / 'models/item/upgrade/wireless_upgrade.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': NS + ':item/upgrade/wireless_upgrade'}})
    write(A / 'models/item/normal/item_tesu_connector.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': NS + ':item/normal/item_tesu_connector'}})


def blockstates():
    for name in BLOCKS:
        if name in PYLON:
            continue
        model = NS + ':block/' + W + name
        if name in FACING:
            turn = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
            variants = {}
            for f, y in turn.items():
                v = {'model': model}
                if name == 'block_tesu_controller' and y:   # only the controller has a front
                    v['y'] = y
                variants['facing=' + f] = v
        else:
            variants = {'': {'model': model}}
        write(A / 'blockstates' / W / (name + '.json'), {'variants': variants})


def loot():
    for name in BLOCKS:
        write(D / NS / 'loot_table/blocks' / W / (name + '.json'), {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1.0, 'bonus_rolls': 0.0, 'entries': [{'type': 'minecraft:item', 'name': NS + ':' + W + name}],
                       'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
            'random_sequence': NS + ':blocks/' + W + name})


def ingredient(v):
    """'#tag', '!item' (an electric item or machine block: matches whatever its charge or data, mio_icif:electric) or 'item'."""
    if v.startswith('#'):
        return {'tag': v[1:]}
    if v.startswith('!'):
        return {'type': 'mio_icif:electric', 'item': v[1:]}
    return {'item': v}


def shaped(name, pattern, key, result, count=1):
    keys = {k: ingredient(v) for k, v in key.items()}
    write(D / NS / 'recipe' / ('shaped_tesu_' + name + '.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': keys,
        'result': {'id': result if ':' in result else NS + ':' + W + result, 'count': count}})


def recipes():
    shaped('controller', ['DFD', 'QGQ', 'DFD'], {'D': '#c:dense_plates/super_iridium', 'F': 'mio_icif:normal/item_field_generator',
        'Q': '!mio_icif:normal/item_super_lapotron_crystal', 'G': '!mio_icif:wiring/block_gesu_core'}, 'block_tesu_controller')
    shaped('input', ['NFN', 'CGC', 'NTN'], {'N': '#c:plates/neutron', 'F': 'mio_icif:normal/item_field_generator', 'C': '#c:circuits/ultimate',
        'G': '!mio_icif:wiring/block_gesu_input_iv', 'T': '!mio_icif:wiring/transformer_luv_zpmv'}, 'block_tesu_input')
    shaped('output', ['NFN', 'CGC', 'NTN'], {'N': '#c:plates/neutron', 'F': 'mio_icif:normal/item_field_generator', 'C': '#c:circuits/ultimate',
        'G': '!mio_icif:wiring/block_gesu_output_luv', 'T': '!mio_icif:wiring/transformer_luv_zpmv'}, 'block_tesu_output')
    shaped('casing', ['TPT', 'PHP', 'TPT'], {'T': '#c:plates/titanium', 'P': '#c:plates/alloy',
        'H': 'mio_icif:producer/block_machine_hull_advanced'}, 'block_tesu_casing', 8)
    # 0.1.11.158: about 1.2 billion EU of storage in its parts (4 super lapotron crystals, 2 EESU), as the 189 cells hold 227 billion
    shaped('cell', ['SIS', 'ECE', 'SIS'], {'S': '!mio_icif:normal/item_super_lapotron_crystal', 'I': '#c:plates/iridium',
        'E': '!mio_icif:wiring/block_eesu', 'C': 'mio_icif:wiring/block_tesu_casing'}, 'block_tesu_cell')
    # 0.1.11.158: the wireless field
    node = '!mio_icif:wiring/block_wireless_power_transmission_node'
    shaped('wireless_port', ['NWN', 'CTC', 'NWN'], {'N': '#c:plates/neutron', 'W': node, 'C': '#c:circuits/ultimate',
        'T': '!mio_icif:wiring/block_tesu_output'}, 'block_tesu_wireless_port')
    shaped('relay', ['PWP', 'CHC', 'PFP'], {'P': '#c:plates/titanium', 'W': node, 'C': '#c:circuits/elite',
        'H': 'mio_icif:wiring/block_tesu_casing', 'F': 'mio_icif:normal/item_field_generator'}, 'block_tesu_relay')
    shaped('wireless_upgrade', [' W ', 'CUC', ' P '], {'W': node, 'C': '#c:circuits/advanced', 'U': 'mio_icif:upgrade/transformer_upgrade',
        'P': '#c:plates/alloy'}, 'mio_icif:upgrade/wireless_upgrade')
    shaped('connector', [' M ', 'CHC'], {'M': '!mio_icif:item_electric_wireless_manager', 'C': '#c:circuits/advanced',
        'H': 'mio_icif:wiring/block_tesu_casing'}, 'mio_icif:normal/item_tesu_connector')


def add_to_tag(path, ids):
    """Adds ids to a block tag; in a shared tag (the c and minecraft namespaces) as {"id": ..., "required": false}."""
    data = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace': False, 'values': []}
    shared = path.relative_to(D).parts[0] in ('c', 'minecraft')
    for i in ids:
        entry = {'id': i, 'required': False} if shared else i
        data['values'] = [v for v in data['values'] if v != i and not (isinstance(v, dict) and v.get('id') == i)]
        data['values'].append(entry)
    write(path, data)


def tags():
    ids = [NS + ':' + W + n for n in BLOCKS + [COLUMN]]
    add_to_tag(D / 'minecraft/tags/block/mineable/pickaxe.json', ids)
    add_to_tag(D / 'c/tags/block/needs_diamond_tool.json', [NS + ':' + W + n for n in MACHINES + [COLUMN]])
    add_to_tag(D / 'c/tags/block/machine.json', [NS + ':' + W + n for n in MACHINES])
    add_to_tag(D / NS / 'tags/block/energy_container.json', [NS + ':' + W + 'block_tesu_controller'])
    # 0.1.11.170: the wrench takes the casing and the cells apart (mio_icif:wrenchable); 0.1.11.172: and the machines, since the
    # wrenches read only that tag
    add_to_tag(D / NS / 'tags/block/wrenchable.json', [NS + ':' + W + n for n in ['block_tesu_casing', 'block_tesu_cell'] + MACHINES])


LANG = {
    'block.mio_icif.wiring.block_tesu_casing': ('TESU 外壳', 'TESU Casing', 'TESU外装', 'Корпус TESU'),
    'block.mio_icif.wiring.block_tesu_cell': ('TESU 储能单元', 'TESU Energy Cell', 'TESU蓄電セル', 'Энергоячейка TESU'),
    'block.mio_icif.wiring.block_tesu_controller': ('TESU 控制器', 'TESU Controller', 'TESUコントローラー', 'Контроллер TESU'),
    'block.mio_icif.wiring.block_tesu_input': ('TESU 输入端口', 'TESU Input Port', 'TESU入力ポート', 'Входной порт TESU'),
    'block.mio_icif.wiring.block_tesu_output': ('TESU 输出端口', 'TESU Output Port', 'TESU出力ポート', 'Выходной порт TESU'),
    'container.mio_icif.tesu_controller': ('太级储电罐（TESU）', 'TESU (Tera Storage)', 'テラ級蓄電タンク（TESU）', 'Теранакопитель TESU'),
    'container.mio_icif.tesu_input': ('TESU 输入端口', 'TESU Input Port', 'TESU入力ポート', 'Входной порт TESU'),
    'container.mio_icif.tesu_output': ('TESU 输出端口', 'TESU Output Port', 'TESU出力ポート', 'Выходной порт TESU'),
    'tooltip.mio_icif.tesu.controller': ('7×7×13 长方体储电罐的核心', 'Core of the 7×7×13 storage box', '7×7×13 直方体蓄電タンクの中核',
                                         'Ядро накопителя-параллелепипеда 7×7×13'),   # 0.1.11.168: a box
    'tooltip.mio_icif.tesu.multiblock_hint': ('放在底圈正面正中，结构随它的朝向转动', 'Goes in the middle of the front of the base ring; the structure turns with it',
                                              '底部リングの正面中央に置く。構造はその向きに合わせて回る', 'Ставится в центр передней стороны нижнего кольца; структура поворачивается вместе с ним'),
    'tooltip.mio_icif.tesu.input': ('接受任何电压（最高 MAX），转交给控制器', 'Takes any voltage (up to MAX) and hands it to the controller',
                                    'あらゆる電圧（最大 MAX）を受け取り、コントローラーへ渡す', 'Принимает любое напряжение (до MAX) и передаёт контроллеру'),
    'tooltip.mio_icif.tesu.output': ('右键提高电压，潜行右键降低（IV～MAX），每 tick 送出一包', 'Right-click raises the voltage, sneak-right-click lowers it (IV–MAX); one packet a tick',
                                     '右クリックで電圧を上げ、スニーク右クリックで下げる（IV～MAX）。1 tick に 1 パケット',
                                     'ПКМ повышает напряжение, Shift+ПКМ понижает (IV–MAX); один пакет за тик'),
    'tooltip.mio_icif.tesu.port_slot': ('装在 TESU 底圈的端口位上', 'Fits a port slot of the TESU base ring', 'TESU 底部リングのポート位置に設置',
                                        'Ставится в слот порта нижнего кольца TESU'),
    'message.mio_icif.tesu.output_tier': ('TESU 输出端口电压：%s（单包 %s EU）', 'TESU output port voltage: %s (%s EU a packet)',
                                          'TESU出力ポートの電圧：%s（1 パケット %s EU）', 'Напряжение выходного порта TESU: %s (%s EU за пакет)'),
    'gui.mio_icif.tesu.ports': ('输入 %s · 输出 %s · 无线 %s', 'In %s · Out %s · Wireless %s', '入力 %s · 出力 %s · 無線 %s',
                                'Вход %s · Выход %s · Беспров. %s'),
    'gui.mio_icif.tesu.output_rate': ('输出 %s EU/t', 'Output %s EU/t', '出力 %s EU/t', 'Выход %s EU/t'),
    'gui.mio_icif.tesu.no_structure': ('结构未完成', 'Structure incomplete', '構造が未完成', 'Структура не собрана'),
    'gui.mio_icif.tesu.wrong_at': ('%s, %s, %s 应为%s', '%s, %s, %s should be %s', '%s, %s, %s は%sが必要', '%s, %s, %s: нужен %s'),
    'gui.mio_icif.tesu.part.casing': ('外壳', 'casing', '外装', 'корпус'),
    'gui.mio_icif.tesu.part.glass': ('防爆玻璃', 'reinforced glass', '防爆ガラス', 'бронестекло'),
    'gui.mio_icif.tesu.part.cell': ('储能单元', 'an energy cell', '蓄電セル', 'энергоячейка'),
    'gui.mio_icif.tesu.part.controller': ('控制器', 'the controller', 'コントローラー', 'контроллер'),
    'gui.mio_icif.tesu.part.port_slot': ('端口或外壳', 'a port or casing', 'ポートか外装', 'порт или корпус'),
    'gui.mio_icif.tesu.part.air': ('空气', 'air', '空気', 'воздух'),
    # 0.1.11.158: the wireless field
    'block.mio_icif.wiring.block_tesu_wireless_port': ('TESU 无线输出端口', 'TESU Wireless Output Port', 'TESU無線出力ポート', 'Беспроводной выходной порт TESU'),
    'block.mio_icif.wiring.block_tesu_relay': ('TESU 中继器', 'TESU Relay', 'TESU中継器', 'Ретранслятор TESU'),
    'item.mio_icif.normal.item_tesu_connector': ('TESU 无线连接器', 'TESU Wireless Connector', 'TESU無線コネクター', 'Беспроводной соединитель TESU'),
    'item.mio_icif.upgrade.wireless_upgrade': ('无线升级', 'Wireless Upgrade', '無線アップグレード', 'Беспроводное улучшение'),
    'item.mio_icif.upgrade.wireless.tooltip': ('在 TESU 无线范围内，每 tick 把机器充满，无视电压等级',
                                               'Inside a TESU wireless field the machine is filled every tick, whatever the voltage',
                                               'TESU の無線範囲内で毎 tick 機械を満充電する。電圧は問わない',
                                               'В беспроводной зоне TESU машина заряжается полностью каждый тик, независимо от напряжения'),
    'tooltip.mio_icif.tesu.wireless_port': ('装在 TESU 端口位上：给 ±%s 格内装了无线升级的机器充电，本身不存电',
                                            'In a TESU port slot: charges machines with a wireless upgrade within ±%s blocks; holds no energy itself',
                                            'TESU のポート位置に置く：±%s マス内の無線アップグレード付き機械を充電する。自身は蓄電しない',
                                            'В слоте порта TESU: заряжает машины с беспроводным улучшением в пределах ±%s блоков; сам энергию не хранит'),
    'tooltip.mio_icif.tesu.relay': ('把无线范围扩展到自身周围 ±%s 格，没有损耗', 'Spreads the wireless field ±%s blocks round itself, without loss',
                                    '周囲 ±%s マスに無線範囲を広げる。損失なし', 'Расширяет беспроводную зону на ±%s блоков вокруг себя, без потерь'),
    # 0.1.11.167: the relay is a pylon three blocks tall
    'block.mio_icif.wiring.block_tesu_relay_column': ('TESU 中继器', 'TESU Relay', 'TESU中継器', 'Ретранслятор TESU'),
    'tooltip.mio_icif.tesu.relay.height': ('三格高的立柱：上方需要留出两格空间', 'A pylon three blocks tall: needs two free blocks above',
                                           '高さ 3 ブロックの柱：上に 2 ブロックの空きが必要', 'Мачта высотой 3 блока: над ней нужно 2 свободных блока'),
    'tooltip.mio_icif.tesu.relay.link': ('用 TESU 无线连接器连到覆盖它的端口或中继器', 'Link it with the TESU Wireless Connector to a port or relay that covers it',
                                         '自分を範囲に含むポートか中継器へ TESU無線コネクターでつなぐ', 'Свяжите его соединителем TESU с портом или ретранслятором, в зоне которого он стоит'),
    # 0.1.11.159: the connector works like the wireless power transmission node's remote: a target, then the relays
    'tooltip.mio_icif.tesu.connector.target': ('目标：%s, %s, %s', 'Target: %s, %s, %s', 'ターゲット：%s, %s, %s', 'Цель: %s, %s, %s'),
    'tooltip.mio_icif.tesu.connector.use': ('潜行右键无线输出端口或中继器设为目标，再右键目标 ±%s 格内的中继器连接到目标；潜行右键空气清除目标',
                                            'Sneak-right-click a wireless output port or relay to set the target, then right-click a relay within ±%s blocks of it to link it. Sneak-right-click the air to clear',
                                            '無線出力ポートか中継器をスニーク右クリックでターゲットに設定し、そこから ±%s マス内の中継器を右クリックしてつなぐ。空中でスニーク右クリックで解除',
                                            'Shift+ПКМ по беспроводному порту или ретранслятору задаёт цель, затем ПКМ по ретранслятору в пределах ±%s блоков от неё связывает его. Shift+ПКМ в воздух сбрасывает'),
    'message.mio_icif.tesu.connector.target_set': ('已将 %s, %s, %s 设为连接目标，右键其范围内的中继器即可连接', 'Target set to %s, %s, %s; right-click a relay in its field to link it',
                                                   '%s, %s, %s をターゲットに設定。範囲内の中継器を右クリックでつなぐ', 'Цель: %s, %s, %s; ПКМ по ретранслятору в её зоне связывает его'),
    'message.mio_icif.tesu.connector.not_node': ('只能把 TESU 无线输出端口或 TESU 中继器设为目标', 'Only a TESU wireless output port or a TESU relay can be the target',
                                                 'ターゲットにできるのは TESU無線出力ポートか TESU中継器だけ', 'Целью может быть только беспроводной порт или ретранслятор TESU'),
    'message.mio_icif.tesu.connector.no_target': ('没有设置目标：先潜行右键无线输出端口或中继器', 'No target set: sneak-right-click a wireless output port or relay first',
                                                  'ターゲット未設定：先に無線出力ポートか中継器をスニーク右クリック', 'Цель не задана: сначала Shift+ПКМ по беспроводному порту или ретранслятору'),
    'message.mio_icif.tesu.connector.target_info': ('当前目标：%s, %s, %s', 'Current target: %s, %s, %s', '現在のターゲット：%s, %s, %s', 'Текущая цель: %s, %s, %s'),
    'message.mio_icif.tesu.connector.other_dim': ('目标在另一个维度，重新设置', 'The target is in another dimension; set a new one',
                                                  'ターゲットは別のディメンションにある。設定し直す', 'Цель в другом измерении; задайте новую'),
    'message.mio_icif.tesu.connector.self': ('不能把中继器连到它自己', 'A relay cannot be linked to itself', '中継器を自分自身にはつなげない', 'Ретранслятор нельзя связать с самим собой'),
    'message.mio_icif.tesu.connector.port_hint': ('无线输出端口不用连接：潜行右键把它设为目标，再右键中继器', 'A wireless output port needs no link: sneak-right-click it to set it as the target, then right-click relays',
                                                  '無線出力ポートは接続不要：スニーク右クリックでターゲットにして、中継器を右クリック',
                                                  'Беспроводному порту связь не нужна: Shift+ПКМ делает его целью, затем ПКМ по ретрансляторам'),
    'message.mio_icif.tesu.connector.cleared': ('已清除目标', 'Target cleared', 'ターゲットを解除', 'Цель сброшена'),
    'message.mio_icif.tesu.connector.gone': ('目标 %s, %s, %s 已不是无线输出端口或中继器（或所在区块未加载），重新设置',
                                             'The target %s, %s, %s is no longer a wireless output port or relay (or not loaded); set a new one',
                                             'ターゲット %s, %s, %s はもう無線出力ポートでも中継器でもない（または未ロード）。設定し直す',
                                             'Цель %s, %s, %s больше не порт и не ретранслятор (или не загружена); задайте новую'),
    'message.mio_icif.tesu.connector.too_far': ('中继器不在目标 %s, %s, %s 的覆盖范围内（±%s 格）', 'The relay is outside the field of the target %s, %s, %s (±%s blocks)',
                                                '中継器がターゲット %s, %s, %s の範囲（±%s マス）の外にある', 'Ретранслятор вне зоны цели %s, %s, %s (±%s блоков)'),
    'message.mio_icif.tesu.connector.loop': ('不能连成环', 'That link would make a loop', 'ループになるのでつなげない', 'Эта связь замкнёт кольцо'),
    'message.mio_icif.tesu.connector.linked': ('中继器 %s, %s, %s 已连接到 %s, %s, %s', 'Relay %s, %s, %s linked to %s, %s, %s',
                                               '中継器 %s, %s, %s を %s, %s, %s につないだ', 'Ретранслятор %s, %s, %s связан с %s, %s, %s'),
    'message.mio_icif.tesu.wireless.port_on': ('TESU 无线输出端口：已接入 TESU，覆盖 ±%s 格', 'TESU wireless output port: on a TESU, covering ±%s blocks',
                                               'TESU無線出力ポート：TESU に接続、±%s マスを範囲に', 'Беспроводной порт TESU: подключён, зона ±%s блоков'),
    'message.mio_icif.tesu.wireless.port_off': ('TESU 无线输出端口：没有接入成型的 TESU（覆盖 ±%s 格）', 'TESU wireless output port: not on a formed TESU (would cover ±%s blocks)',
                                                'TESU無線出力ポート：完成した TESU に未接続（範囲 ±%s マス）', 'Беспроводной порт TESU: не в собранном TESU (зона ±%s блоков)'),
    'message.mio_icif.tesu.wireless.relay_on': ('TESU 中继器：连接到 %s, %s, %s，有电，覆盖 ±%s 格', 'TESU relay: linked to %s, %s, %s, powered, covering ±%s blocks',
                                                'TESU中継器：%s, %s, %s に接続、給電中、±%s マスを範囲に', 'Ретранслятор TESU: связан с %s, %s, %s, питание есть, зона ±%s блоков'),
    'message.mio_icif.tesu.wireless.relay_dead': ('TESU 中继器：连接到 %s, %s, %s，但连接没有通到成型的 TESU（覆盖 ±%s 格）',
                                                  'TESU relay: linked to %s, %s, %s, but the links reach no formed TESU (±%s blocks)',
                                                  'TESU中継器：%s, %s, %s に接続、だが完成した TESU に届いていない（±%s マス）',
                                                  'Ретранслятор TESU: связан с %s, %s, %s, но связи не доходят до собранного TESU (±%s блоков)'),
    'message.mio_icif.tesu.wireless.relay_unlinked': ('TESU 中继器：未连接，用 TESU 无线连接器连接', 'TESU relay: not linked; link it with the TESU Wireless Connector',
                                                      'TESU中継器：未接続。TESU無線コネクターでつなぐ', 'Ретранслятор TESU: не связан; свяжите соединителем TESU'),
    # the JEI "Multiblock Structures" plan and the manual's structure view (BuiltinStructureBlueprints.tesu)
    'structure.mio_icif.tesu': ('太级储电罐（TESU）', 'TESU (Tera Storage)', 'テラ級蓄電タンク（TESU）', 'Теранакопитель TESU'),
    'structure.mio_icif.tesu.casing': ('TESU 外壳', 'TESU Casing', 'TESU外装', 'Корпус TESU'),
    'structure.mio_icif.tesu.glass': ('防爆玻璃（四面中间三列的观察窗）', 'Reinforced Glass (a window three blocks wide in the middle of each side)',
                                      '防爆ガラス（各面中央の幅 3 列の窓）', 'Бронестекло (окно шириной в 3 блока в центре каждой стороны)'),   # 0.1.11.169
    'structure.mio_icif.tesu.cell': ('TESU 储能单元（罐内填满）', 'TESU Energy Cell (filling the inside)',
                                     'TESU蓄電セル（内部をすべて埋める）', 'Энергоячейка TESU (заполняет всё внутри)'),
    'structure.mio_icif.tesu.port': ('端口位：TESU 输入、输出或无线输出端口，或用外壳封住', 'Port slot: a TESU input, output or wireless output port, or casing to close it',
                                     'ポート位置：TESU入力・出力・無線出力ポート、または外装でふさぐ', 'Слот порта: входной, выходной или беспроводной порт TESU либо корпус'),
    'structure.mio_icif.tesu.note.inside': ('罐内 9 层，每层中间 3-5-5-5-3 的 21 格是储能单元，共 189 块；四个内角是外壳。',
                                            'Inside, 9 layers of energy cells in a 3-5-5-5-3 disc, 21 a layer, 189 in all; the four inner corners are casing.',
                                            '内部は 9 層、各層中央の 3-5-5-5-3 の 21 マスが蓄電セル、計 189 個。四隅の内側は外装。',
                                            'Внутри 9 слоёв энергоячеек диском 3-5-5-5-3, по 21 в слое, всего 189; четыре внутренних угла — корпус.'),
    'structure.mio_icif.tesu.note.turn': ('控制器放在底圈正面正中，朝外；整个结构随它的朝向转动。',
                                          'The controller sits in the middle of the front of the base ring, facing out; the structure turns with it.',
                                          'コントローラーは底部リングの正面中央に外向きに置く。構造はその向きに合わせて回る。',
                                          'Контроллер стоит в центре передней стороны нижнего кольца лицом наружу; структура поворачивается вместе с ним.'),
}
ANCHOR = 'block.mio_icif.wiring.block_gesu_output_luv'
REMOVED = ['tooltip.mio_icif.tesu.connector.source', 'message.mio_icif.tesu.connector.selected',   # 0.1.11.159
           'block.mio_icif.wiring.block_tesu_electrode', 'gui.mio_icif.tesu.part.electrode', 'structure.mio_icif.tesu.electrode']   # 0.1.11.166
# 0.1.11.166: the top electrode's files and tag entry
GONE_FILES = [A / 'blockstates' / W / 'block_tesu_electrode.json', A / 'models/block' / W / 'block_tesu_electrode.json',
              A / 'models/item' / W / 'block_tesu_electrode.json', A / 'textures/block' / W / 'block_tesu_electrode.png',
              D / NS / 'loot_table/blocks' / W / 'block_tesu_electrode.json', D / NS / 'recipe/shaped_tesu_electrode.json',
              A / 'textures/block' / W / 'block_tesu_relay.png']   # 0.1.11.167: the relay's old cube texture
GONE_BLOCKS = [NS + ':' + W + 'block_tesu_electrode']


def removed():
    for path in GONE_FILES:
        if path.exists():
            path.unlink()
            print('removed', path.relative_to(ROOT))
    for tag in D.glob('*/tags/block/**/*.json'):
        data = json.loads(tag.read_text(encoding='utf-8'))
        kept = [v for v in data.get('values', []) if (v.get('id') if isinstance(v, dict) else v) not in GONE_BLOCKS]
        if len(kept) != len(data.get('values', [])):
            data['values'] = kept
            write(tag, data)
            print('untagged', tag.relative_to(ROOT))


def lang():
    for i, code in enumerate(('zh_cn', 'en_us', 'ja_jp', 'ru_ru')):
        path = A / 'lang' / (code + '.json')
        raw = path.read_bytes().decode('utf-8')
        nl = '\r\n' if '\r\n' in raw else '\n'
        lines = raw.replace('\r\n', '\n').split('\n')
        lines = [l for l in lines if not any(l.lstrip().startswith(json.dumps(k) + ':') for k in REMOVED)]
        at = next(n for n, l in enumerate(lines) if l.lstrip().startswith('"' + ANCHOR + '"'))
        for key in reversed(list(LANG)):
            text = '  ' + json.dumps(key, ensure_ascii=False) + ': ' + json.dumps(LANG[key][i], ensure_ascii=False) + ','
            existing = next((n for n, l in enumerate(lines) if l.lstrip().startswith(json.dumps(key) + ':')), None)
            if existing is not None:
                lines[existing] = text
            else:
                if not lines[at].rstrip().endswith(','):
                    lines[at] = lines[at].rstrip() + ','
                lines.insert(at + 1, text)
        out = '\n'.join(lines)
        json.loads(out)
        path.write_bytes(out.replace('\n', nl).encode('utf-8'))
        print('lang', code)


if __name__ == '__main__':
    models()
    blockstates()
    loot()
    recipes()
    tags()
    removed()
    lang()
    print('done')
