#!/usr/bin/env python3
"""0.1.11.78: the advancement tree (docs/advancement_tree_plan.md); 0.1.11.81: one tab, the former Energy, Tools and Gear
and Automation roots hang under Safe Transmission, the root and the Forge Hammer.

Writes data/mio_icif/advancement/<id>.json for every advancement below (the 18 existing ids are rewritten in place with
their new parents; their criteria stay unless noted), the item tag mio_icif:suit_modules, and the titles and
descriptions in en_us / zh_cn / ja_jp / ru_ru. Every item id is checked against assets/mio_icif/models/item first.
Criteria marked IMPOSSIBLE are granted from code (MioAchievements) in 0.1.11.79 / 0.1.11.80.

Usage: data_11578.py <repo root> [--check]
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
RES = os.path.join(ROOT, 'src/main/resources')
ADV = os.path.join(RES, 'data/mio_icif/advancement')
MODELS = os.path.join(RES, 'assets/mio_icif/models/item')
LANG = os.path.join(RES, 'assets/mio_icif/lang')
LANGS = ('en_us', 'zh_cn', 'ja_jp', 'ru_ru')
BACKGROUND = 'mio_icif:textures/achievements/background.png'


def m(path):
    return 'mio_icif:' + path


# ------------------------------------------------------------------ item groups

CABLES = [m('wiring/cable/' + c) for c in (
    'block_tin_cable', 'block_tin_cable_1', 'block_cable_o', 'block_cable', 'block_gold_cable', 'block_gold_cable_1',
    'block_gold_cable_2', 'block_iron_cable', 'block_iron_cable_1', 'block_iron_cable_2', 'block_iron_cable_3',
    'block_glass_cable', 'block_superconducting_cable')]
INSULATED = [m('wiring/cable/' + c) for c in (
    'block_tin_cable_1', 'block_cable', 'block_gold_cable_1', 'block_gold_cable_2', 'block_iron_cable_1',
    'block_iron_cable_2', 'block_iron_cable_3')]
ORES = [m(o) for o in (
    'block_ore_tin', 'block_ore_tin_in_deep', 'block_ore_lead', 'block_ore_lead_in_deep', 'block_ore_uran',
    'block_ore_uran_in_deep', 'block_ore_niobium', 'block_ore_niobium_in_deep', 'block_ore_titanium',
    'block_ore_titanium_in_deep', 'resource/item_raw_tin_ore', 'resource/item_raw_lead_ore', 'resource/item_raw_uran_ore',
    'resource/item_raw_silver_ore', 'resource/item_raw_niobium_ore', 'resource/item_raw_titanium_ore')]
IRIDIUM_ORE = [m('block_ore_iridium_in_deep'), m('resource/item_iridium_dust_small'), m('resource/item_shard_iridium')]
SCAFFOLDS = [m('build/block_scaffold_' + s) for s in ('wood', 'iron', 'steel', 'carbon')]
ADVANCED_MACHINES = [m('producer/' + p) for p in (
    'block_powder_advanced_elc', 'block_compressor_advanced_elc', 'block_metal_former_advanced', 'block_blast_furnace_advanced')]
PACKS = [m(p) for p in ('armor/item_armor_batpack', 'armor/item_armor_advbatpack', 'armor/item_armor_energypack',
                        'armor/item_armor_lappack', 'item_trinket_energy_crystal_belt', 'item_trinket_lapotron_crystal_belt')]
CHARGERS = [m('wiring/' + c) for c in ('block_batbox_charger', 'block_cesu_charger', 'block_mfe_charger', 'block_mfsu_charger',
                                       'block_lesu_charger', 'block_eesu_charger')]
STORAGE_UP = [m('wiring/' + s) for s in ('block_cesu', 'block_mfe', 'block_mfsu', 'block_lesu', 'block_eesu')]
ROTORS = [m('rotors/item_' + r + '_rotor') for r in ('wood', 'bronze', 'iron', 'carbon', 'adviron', 'titanium_iron_alloy', 'super_iridium_alloy')]
RIFLES = [m(r) for r in ('item_tool_electric_rifle', 'item_tool_advanced_electric_rifle', 'item_tool_tactical_laser_rifle')]
NANO = [m('armor/item_armor_nano_' + p) for p in ('helmet', 'chestplate', 'leggings', 'boots')]
QUANTUM = [m('armor/item_armor_quantum_' + p) for p in ('helmet', 'chestplate', 'leggings', 'boots')]
EJECTORS = [m('upgrade/ejector_upgrade'), m('upgrade/fluid_ejector_upgrade')]


# ------------------------------------------------------------------ criteria

def has(*items):
    """Any one of the items in the inventory."""
    return {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': list(items)}]}}


def has_all(*items):
    """Every item in the inventory at once."""
    return {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': [i]} for i in items]}}


def has_tag(tag):
    return {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': '#' + tag}]}}


def code():
    return {'trigger': 'minecraft:impossible'}


def wearing(head, chest, legs, feet):
    eq = {'head': {'items': head}, 'chest': {'items': chest}, 'legs': {'items': legs}, 'feet': {'items': feet}}
    return {'trigger': 'minecraft:tick', 'conditions': {'player': [
        {'condition': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'equipment': eq}}]}}


def killed_with(entity, mainhand):
    return {'trigger': 'minecraft:player_killed_entity', 'conditions': {
        'entity': [{'condition': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'type': entity}}],
        'player': [{'condition': 'minecraft:entity_properties', 'entity': 'this',
                    'predicate': {'equipment': {'mainhand': {'items': mainhand}}}}]}}


def enchanted_with(enchantment):
    return {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [
        {'predicates': {'minecraft:enchantments': [{'enchantments': enchantment}]}}]}}


# ------------------------------------------------------------------ the tree
# (id, parent, icon, frame, hidden, xp, criteria, {lang: (title, description)})
# frame: task / goal / challenge. parent None = the root of a tab.

A = []


def adv(aid, parent, icon, frame, criteria, text=None, hidden=None, xp=0, keep_text=False, curios=False):
    """hidden None: as the existing file (else False). An existing advancement kept as it is (keep_text) also keeps its icon."""
    A.append(dict(id=aid, parent=parent, icon=icon, frame=frame, criteria=criteria, text=text, hidden=hidden, xp=xp,
                  keep_text=keep_text, curios=curios))


def t(en, zh, ja, ru):
    return {'en_us': en, 'zh_cn': zh, 'ja_jp': ja, 'ru_ru': ru}


# ---- tab 1: Singularity Iteration (start, materials, processing machines) ----------------------------------------
adv('root', None, m('armor/item_armor_quantum_helmet'), 'task', {'enter_world': {'trigger': 'minecraft:tick'}}, None, keep_text=True)
adv('singularity_ore', 'root', m('resource/item_raw_tin_ore'), 'task', {'ore': has(*ORES)}, t(
    ('A Singular Start', 'Mine some Singularity Iteration ore'),
    ('奇点启程', '采掘一些奇点迭代的矿石'),
    ('特異点の旅立ち', '特異点イテレーションの鉱石を掘る'),
    ('Сингулярный старт', 'Добудьте руду Singularity Iteration')))
adv('iridium_ore', 'singularity_ore', m('block_ore_iridium_in_deep'), 'goal', {'iridium': has(*IRIDIUM_ORE)}, t(
    ('The Mysterious Iridium Ore', 'One of the rarest resources in this world'),
    ('神秘的铱矿石', '这个世界最稀有的资源之一'),
    ('謎のイリジウム鉱石', 'この世界で最も希少な資源のひとつ'),
    ('Загадочная иридиевая руда', 'Один из самых редких ресурсов этого мира')))
adv('sticky_resin', 'root', m('resource/item_harz'), 'task', {'resin': has(m('resource/item_harz'))}, t(
    ('Sticky Situation', 'Get Sticky Resin'),
    ('胶着场面', '获取黏性树脂'),
    ('ねばねばな状況', '粘着樹脂を手に入れる'),
    ('Липкая ситуация', 'Добудьте липкую смолу')))
adv('scaffold', 'root', m('build/block_scaffold_iron'), 'task', {'scaffold': has(*SCAFFOLDS)}, t(
    ('Quality Scaffolding', 'Sturdier than scaffolding made of bamboo'),
    ('优质脚手架', '比竹子制作的脚手架更坚固'),
    ('上質な足場', '竹の足場より頑丈'),
    ('Качественные леса', 'Прочнее бамбуковых лесов')))
adv('painter', 'root', m('build/item_painter_red'), 'task', {'painted': code()}, t(
    ('I Am a Painter', 'Color a block with a Painter'),
    ('我是一个粉刷匠', '用刷子给方块上色'),
    ('ぼくはペンキ屋さん', 'ブラシでブロックを塗る'),
    ('Я маляр', 'Покрасьте блок кистью')))
adv('hammer_time', 'root', m('item_tool_hammer'), 'task', None, None, keep_text=True)
adv('wire_cutter', 'hammer_time', m('item_tool_cutter'), 'task', None, None, keep_text=True)
adv('any_cable', 'wire_cutter', m('wiring/cable/block_tin_cable'), 'task', {'cable': has(*CABLES)}, t(
    ('Power Flow', 'Make any cable'),
    ('能量流', '制作任意导线'),
    ('エネルギーの流れ', 'ケーブルを作る'),
    ('Поток энергии', 'Сделайте любой кабель')))
adv('cable_burnt', 'any_cable', m('wiring/cable/block_tin_cable'), 'task', {'burnt': code()}, t(
    ('Mind the Current', 'Too much current burns cables'),
    ('小心触电', '电流过高会烧坏电线'),
    ('感電注意', '電流が多すぎるとケーブルが焼ける'),
    ('Осторожно, ток!', 'Слишком большой ток сжигает кабели')), hidden=True)
adv('industrial_prologue', 'any_cable', m('wiring/cable/block_tin_cable_1'), 'task', {'insulated_cable': has(*INSULATED)}, t(
    ('Safe Transmission', 'Insulate a cable'),
    ('安全传输', '将导线附着绝缘层'),
    ('安全な送電', 'ケーブルを絶縁する'),
    ('Безопасная передача', 'Заизолируйте кабель')))
adv('circuit', 'industrial_prologue', m('normal/item_circuit'), 'task', {'circuit': has(m('normal/item_circuit'))}, t(
    ('Human Etching Machine', 'Make an Electronic Circuit'),
    ('人形蚀刻机', '制作电路板'),
    ('人間エッチング機', '電子回路を作る'),
    ('Живой травильный станок', 'Сделайте электронную схему')))
adv('electric_furnace', 'circuit', m('producer/block_furnace_elc'), 'task', {'furnace': has(m('producer/block_furnace_elc'))}, t(
    ('Hot Topic', 'The Electric Furnace smelts faster'),
    ('更热话题', '电炉能烧得更快'),
    ('ホットな話題', '電気炉はもっと速く焼ける'),
    ('Горячая тема', 'Электропечь плавит быстрее')))
adv('induction_furnace', 'electric_furnace', m('producer/block_induction_elc'), 'goal', {'induction': has(m('producer/block_induction_elc'))}, t(
    ('Hotter Topic', 'The Induction Furnace beats the Electric Furnace'),
    ('高热话题', '感应炉比电炉更高效'),
    ('もっとホットな話題', '誘導炉は電気炉より効率がいい'),
    ('Ещё горячее', 'Индукционная печь эффективнее электропечи')))
adv('macerator', 'circuit', m('producer/block_powder_elc'), 'task', {'macerator': has(m('producer/block_powder_elc'))}, t(
    ('Double the Fun', 'Turn on double ore mode: get a Macerator'),
    ('双倍快乐', '开启双倍矿产模式'),
    ('二倍の喜び', '鉱石二倍モード開始：粉砕機を手に入れる'),
    ('Двойная радость', 'Включите режим удвоения руды: добудьте дробитель')))
adv('ore_washer', 'macerator', m('producer/block_washer_elc'), 'task', {'washer': has(m('producer/block_washer_elc'))}, t(
    ('Wash for More', 'Wash your crushed ores'),
    ('「洗洗」更高产', '可以水洗加工好的粉碎矿石了'),
    ('洗えばもっと', '砕いた鉱石を洗えるようになった'),
    ('Помой и получи больше', 'Теперь дроблёную руду можно промыть')))
adv('thermal_centrifuge', 'ore_washer', m('producer/block_centrifuge_elc'), 'task', {'centrifuge': has(m('producer/block_centrifuge_elc'))}, t(
    ("This Carousel Isn't Cold", 'Make a Thermal Centrifuge'),
    ('这个旋转木马不太冷', '制作热能离心机'),
    ('このメリーゴーラウンドは冷たくない', '熱遠心分離機を作る'),
    ('Эта карусель не холодная', 'Сделайте термическую центрифугу')))
adv('extractor', 'circuit', m('producer/block_extractor_elc'), 'task', {'extractor': has(m('producer/block_extractor_elc'))}, t(
    ('Super Extraction', 'Get an Extractor'),
    ('超级提取技术', '获取提取机'),
    ('スーパー抽出技術', '抽出機を手に入れる'),
    ('Супер-экстракция', 'Добудьте экстрактор')))
adv('compressor', 'circuit', m('producer/block_compressor_elc'), 'task', {'compressor': has(m('producer/block_compressor_elc'))}, t(
    ('Advanced Compression', 'Get a Compressor'),
    ('高级压缩技术', '获取压缩机'),
    ('高度圧縮技術', '圧縮機を手に入れる'),
    ('Продвинутое сжатие', 'Добудьте компрессор')))
adv('industrial_diamond', 'compressor', m('normal/item_industrial_diamond'), 'challenge', {'diamond': has(m('normal/item_industrial_diamond'))}, t(
    ('Shine Bright', 'Our industrial diamonds leave old natural diamonds far behind!'),
    ('闪闪发光', '我们先进的工业钻石已经完全超越了老式的天然钻石！'),
    ('キラキラ', '我らの工業用ダイヤは古い天然ダイヤをとうに超えた！'),
    ('Сияние', 'Наши промышленные алмазы оставили природные далеко позади!')), xp=100)
adv('recycler', 'compressor', m('producer/block_recycler_elc'), 'task', {'recycler': has(m('producer/block_recycler_elc'))}, t(
    ('Reduce, Reuse, Recycle', "Useless resources aren't lost yet"),
    ('回收再利用', '手里没有用的资源还有救'),
    ('リサイクル', '使い道のない資源にもまだ救いがある'),
    ('Вторая жизнь', 'Ненужные ресурсы ещё можно спасти')))
adv('scrap_box', 'recycler', m('normal/item_scrapbox'), 'goal', {'opened': code()}, t(
    ('Lucky Draw', 'Open a Scrap Box, there might be a surprise'),
    ('抽个奖', '开启一个废料盒，说不定有意外惊喜'),
    ('ガチャ', 'スクラップボックスを開けよう、何か出るかも'),
    ('Лотерея', 'Откройте ящик утиля, вдруг повезёт')))
adv('metal_former', 'circuit', m('producer/block_metal_former'), 'task', {'former': has(m('producer/block_metal_former'))}, t(
    ('Hands Free', 'Time to put away the hammer and the cutter'),
    ('解放双手', '是时候抛弃锤子和剪刀了'),
    ('両手を解放', 'ハンマーとカッターを置く時が来た'),
    ('Руки свободны', 'Пора отложить молот и ножницы')))
adv('canner', 'circuit', m('producer/block_canner_elc'), 'task', {'canner': has(m('producer/block_canner_elc'))}, t(
    ('Clean and Hygienic', 'Make a Canning Machine'),
    ('干净又卫生', '制作装罐机'),
    ('清潔で衛生的', '缶詰機を作る'),
    ('Чисто и гигиенично', 'Сделайте консервный аппарат')))
adv('canned_food', 'canner', m('normal/item_tin_filled_can'), 'task', {'can': has(m('normal/item_tin_filled_can'))}, t(
    ('Canned Food', 'Can some food in the Canning Machine'),
    ('罐装食品', '使用装罐机装罐食品'),
    ('缶詰食品', '缶詰機で食べ物を缶詰にする'),
    ('Консервы', 'Законсервируйте еду в консервном аппарате')))
adv('lathe', 'circuit', m('producer/block_lathe'), 'task', {'lathe': has(m('producer/block_lathe'))}, t(
    ('Precision Machining', 'Get a Lathe'),
    ('精密加工', '获取车床'),
    ('精密加工', '旋盤を手に入れる'),
    ('Точная обработка', 'Добудьте токарный станок')))
adv('super_circuit', 'circuit', m('normal/item_super_circuit'), 'task', {'circuit': has(m('normal/item_super_circuit'))}, t(
    ('Human Lithography Machine', 'Make a Super Circuit'),
    ('人形光刻机', '制作超级电路板'),
    ('人間露光装置', 'スーパー回路を作る'),
    ('Живой литограф', 'Сделайте суперсхему')))
adv('advanced_alloy', 'hammer_time', m('resource/item_alloy_ingot'), 'task', {'alloy': has(m('resource/item_alloy_ingot'))}, t(
    ('Three in One', 'Make an Advanced Alloy Ingot'),
    ('「三合一」', '制作高级合金锭'),
    ('三位一体の合金', '高級合金インゴットを作る'),
    ('Три в одном', 'Сделайте слиток продвинутого сплава')))
adv('blast_furnace', 'hammer_time', m('producer/block_blast_furnace'), 'task', {'blast_furnace': has(m('producer/block_blast_furnace'))}, t(
    ('Super Hot Topic', 'Make a Blast Furnace'),
    ('超热话题', '制作高炉'),
    ('超ホットな話題', '高炉を作る'),
    ('Сверхгорячая тема', 'Сделайте доменную печь')))
adv('refined_steel', 'blast_furnace', m('resource/item_adviron_ingot'), 'task', None, None, keep_text=True)
adv('advanced_machine', 'refined_steel', m('producer/block_compressor_advanced_elc'), 'goal', {'machine': has(*ADVANCED_MACHINES)}, t(
    ('Industrial Upgrade', 'Advanced machines work faster'),
    ('工业升级', '进阶机器工作更高效'),
    ('産業アップグレード', '上位機械はもっと効率よく動く'),
    ('Промышленный апгрейд', 'Продвинутые машины работают эффективнее')))
adv('nano_blast_furnace', 'blast_furnace', m('producer/block_blast_furnace_elc'), 'task', {'furnace': has(m('producer/block_blast_furnace_elc'))}, t(
    ("It Can't Get Any Hotter", 'Make an Electric Nano Blast Furnace'),
    ('已经不能更热的话题', '制作电力纳米高炉'),
    ('これ以上ホットにはならない', '電気ナノ高炉を作る'),
    ('Горячее некуда', 'Сделайте электрическую нанодоменную печь')))

# ---- tab 2: Energy (generation, storage, nuclear) -----------------------------------------------------------------
# 0.1.11.81: one tab; the Generator needs an RE battery (insulated tin cable), so it hangs under Safe Transmission
adv('industrial_age', 'industrial_prologue', m('generator/block_thermal_generator'), 'task', None, None, keep_text=True)
adv('solar', 'industrial_age', m('generator/block_solar_generator'), 'task', {'solar': has(m('generator/block_solar_generator'))}, t(
    ('Embrace the Sun', 'Make power from sunlight'),
    ('拥抱太阳', '用太阳光发电'),
    ('太陽を抱きしめて', '太陽光で発電する'),
    ('Объятия солнца', 'Получайте энергию от солнечного света')))
adv('quantum_solar', 'solar', m('generator/block_quantum_solar_panel'), 'challenge', {'solar': has(m('generator/block_quantum_solar_panel'))}, t(
    ('Squeeze the Sun Dry', 'Drain the sun with a Quantum Solar Generator'),
    ('榨干太阳', '用量子太阳能发电机榨干太阳'),
    ('太陽を搾り取れ', '量子ソーラー発電機で太陽を搾り取る'),
    ('Выжать солнце досуха', 'Выжмите солнце квантовым солнечным генератором')), xp=500)
adv('clean_ambassador', 'solar', m('generator/block_wind_generator'), 'challenge', {'total': code()}, t(
    ('Clean Energy Ambassador', 'You are our chosen ambassador of clean industry! (2,147,483,647 EU from clean generators)'),
    ('清洁能源推广大使', '你就是我们钦定的清洁工业推广大使！（清洁能源发电机累计发电 2,147,483,647 EU）'),
    ('クリーンエネルギー大使', 'きみこそ我らが選んだクリーン産業の大使だ！（クリーン発電機で累計 2,147,483,647 EU）'),
    ('Посол чистой энергии', 'Вы — наш избранный посол чистой промышленности! (2 147 483 647 EU от чистых генераторов)')), xp=500)
adv('wind', 'industrial_age', m('generator/block_wind_generator'), 'task', {'wind': has(m('generator/block_wind_generator'))}, t(
    ('Cold at the Top', 'Stand a little higher and enjoy the power of the wind'),
    ('高处不胜寒', '站得高一点，享受自然风的能量'),
    ('高きところは寒い', '少し高く立って、自然の風の力を楽しもう'),
    ('Наверху холодно', 'Поднимитесь повыше и насладитесь силой ветра')))
adv('wind_meter', 'wind', m('item_tool_windmeter'), 'task', {'used': code()}, t(
    ('Wind Tester', "Get ready to test the wind, don't get blown away"),
    ('风力测试员', '做好准备测试风力，别被大风吹倒了'),
    ('風力テスター', '風を測る準備をしよう、飛ばされないように'),
    ('Испытатель ветра', 'Приготовьтесь измерить ветер, только не улетите')))
adv('kinetic_generator', 'industrial_age', m('generator/block_kinetic_generator'), 'task', {'generator': has(m('generator/block_kinetic_generator'))}, t(
    ('Right-Hand Rule', 'Make a Kinetic Generator'),
    ('「右手定则」', '制作动能发电机'),
    ('右手の法則', '運動エネルギー発電機を作る'),
    ('Правило правой руки', 'Сделайте кинетический генератор')))
adv('kinetic_running', 'kinetic_generator', m('kugenerator/block_wind_kinetic_generator'), 'goal', {'running': code()}, t(
    ('Full of Kinetics', 'Time to make some kinetic energy: run a Kinetic Generator on a kinetic source'),
    ('动能十足', '是时候生产一些动能了'),
    ('運動エネルギー満タン', '運動エネルギーを作る時だ：運動エネルギー源で発電機を回す'),
    ('Полно кинетики', 'Пора произвести кинетическую энергию: запустите генератор от кинетического источника')))
adv('wind_kinetic', 'kinetic_generator', m('kugenerator/block_wind_kinetic_generator'), 'task', {'wind': has(m('kugenerator/block_wind_kinetic_generator'))}, t(
    ('Unpredictable Winds', 'Make a Wind Kinetic Generator'),
    ('风云难测', '制作风力动能发生机'),
    ('風雲は測りがたし', '風力運動エネルギー発生機を作る'),
    ('Непредсказуемый ветер', 'Сделайте ветряной кинетический генератор')))
adv('rotor', 'wind_kinetic', m('rotors/item_iron_rotor'), 'task', {'rotor': has(*ROTORS)}, t(
    ('Windmill', 'Make any rotor'),
    ('大风车', '制作任意转子'),
    ('大風車', 'ローターを作る'),
    ('Ветряная мельница', 'Сделайте любой ротор')))
adv('super_rotor', 'rotor', m('rotors/item_super_iridium_alloy_rotor'), 'challenge', {'rotor': has(m('rotors/item_super_iridium_alloy_rotor'))}, t(
    ('Unbreakable', 'A Super Iridium Alloy Rotor fears no storm'),
    ('无比坚韧', '超级铱合金转子无惧大风大浪'),
    ('不屈', 'スーパーイリジウム合金ローターは嵐を恐れない'),
    ('Несокрушимый', 'Ротор из суперсплава иридия не боится бурь')), xp=200)
adv('water_kinetic', 'kinetic_generator', m('kugenerator/block_water_kinetic_generator'), 'task', {'water': has(m('kugenerator/block_water_kinetic_generator'))}, t(
    ('River Power', 'Make a Water Kinetic Generator'),
    ('河动力', '制作水力动能发生机'),
    ('川のちから', '水力運動エネルギー発生機を作る'),
    ('Сила реки', 'Сделайте водяной кинетический генератор')))
adv('steam_kinetic', 'kinetic_generator', m('producer/block_steam_kinetic_generator'), 'task', {'steam': has(m('producer/block_steam_kinetic_generator'))}, t(
    ('What Watt Saw', 'Make a Steam Kinetic Generator'),
    ('瓦特之所见', '制作蒸汽动能发生机'),
    ('ワットが見たもの', '蒸気運動エネルギー発生機を作る'),
    ('Что видел Уатт', 'Сделайте паровой кинетический генератор')))
adv('geomagnetic', 'kinetic_generator', m('generator/block_geomagnetic_generator'), 'task', {'geo': has(m('generator/block_geomagnetic_generator'))}, t(
    ('Journey to the Center of the Earth', 'Make a Geomagnetic Generator'),
    ('地心三万里', '制作地磁发电机'),
    ('地底旅行', '地磁気発電機を作る'),
    ('Путешествие к центру Земли', 'Сделайте геомагнитный генератор')))
adv('steam_engine', 'industrial_age', m('producer/block_steam_generator'), 'task', {'steam': has(m('producer/block_steam_generator'))}, t(
    ('Age of Steam', 'Make a Steam Generator'),
    ('蒸汽时代', '制作蒸汽机'),
    ('蒸気の時代', '蒸気機関を作る'),
    ('Эпоха пара', 'Сделайте паровой генератор')))
adv('batbox', 'industrial_age', m('wiring/block_bat_box'), 'task', {'batbox': has(m('wiring/block_bat_box'))}, t(
    ('Energy Storage', 'Get a BatBox'),
    ('能源存储', '获取一个储电盒'),
    ('エネルギー貯蔵', '蓄電箱を手に入れる'),
    ('Хранилище энергии', 'Добудьте энергохранилище')))
adv('portable_storage', 'batbox', m('armor/item_armor_batpack'), 'task', {'pack': has_tag('mio_icif:portable_energy_storage')}, t(
    ('Portable Storage', 'Carry your energy with you'),
    ('便携存储', '把能量随身携带'),
    ('携帯ストレージ', 'エネルギーを持ち歩こう'),
    ('Переносное хранилище', 'Носите энергию с собой')))
adv('charge_pad', 'batbox', m('wiring/block_batbox_charger'), 'task', {'pad': has(*CHARGERS)}, t(
    ('Portable Charging Station', 'Turn your energy storage into a charge pad'),
    ('便携充电站', '将你的能量存储设备改造成一个充电座'),
    ('携帯充電ステーション', 'エネルギー貯蔵装置を充電台に改造する'),
    ('Переносная зарядка', 'Превратите энергохранилище в зарядную площадку')))
adv('storage_upgrade', 'batbox', m('wiring/block_cesu'), 'goal', {'storage': has(*STORAGE_UP)}, t(
    ('Storage Upgrade', 'There are better ways to store energy'),
    ('存储升级', '能量存储也有更好的选择'),
    ('ストレージ強化', 'エネルギー貯蔵にももっといい選択がある'),
    ('Улучшенное хранение', 'Хранить энергию можно и получше')))
adv('gesu', 'storage_upgrade', m('wiring/block_gesu_core'), 'challenge', {'formed': code()}, t(
    ('Mad Energy Hoarder', 'You took energy storage to the limit: build a GESU'),
    ('疯狂电能存储者', '你做到了电能储存的极致'),
    ('狂気の蓄電家', '電力貯蔵を極めた：GESU を組み上げる'),
    ('Безумный накопитель', 'Вы довели хранение энергии до предела: постройте GESU')), xp=500)
adv('eu_meter', 'batbox', m('item_tool_meter'), 'task', {'meter': has(m('item_tool_meter'))}, t(
    ('Into the Grid', 'Make an EU Meter'),
    ('初入电网', '制作EU电表'),
    ('電力網デビュー', 'EU メーターを作る'),
    ('Знакомство с сетью', 'Сделайте измеритель EU')))
adv('energy_terminal', 'eu_meter', m('wiring/block_energy_terminal'), 'task', {'terminal': has(m('wiring/block_energy_terminal'))}, t(
    ('Can This Meter Run Backwards?', 'Make an Energy Management Terminal'),
    ('这个电表可以倒转吗', '制作能源管理终端'),
    ('このメーター、逆回しできる？', 'エネルギー管理端末を作る'),
    ('А счётчик можно открутить?', 'Сделайте терминал управления энергией')))
adv('industrial_accident', 'industrial_age', m('generator/block_thermal_generator'), 'task', None, None, hidden=True, keep_text=True)
adv('clean_energy', 'industrial_age', m('generator/block_nuclear_reactor_generator'), 'task', None, None, keep_text=True)
adv('fluid_reactor', 'clean_energy', m('reactor/block_reactor_fluid_port'), 'goal', {'formed': code()}, t(
    ('A More Advanced Reactor', 'A nuclear reactor is good for more than power: build a Fluid Reactor'),
    ('更先进的反应堆', '核反应堆的用途不单单是发电'),
    ('より先進的な原子炉', '原子炉の使い道は発電だけじゃない：流体原子炉を組む'),
    ('Реактор посовременнее', 'Ядерный реактор годится не только для электричества: постройте жидкостный реактор')))
adv('meltdown', 'clean_energy', m('generator/block_nuclear_reactor_generator'), 'challenge', None, None, hidden=True, keep_text=True)
adv('manhattan_project', 'clean_energy', m('reactor/block_reactor_nuke'), 'task', None, None, keep_text=True)
adv('trinity', 'manhattan_project', m('reactor/block_reactor_nuke'), 'task', None, None, keep_text=True)
adv('big_ivan', 'trinity', m('reactor/block_reactor_nuke'), 'challenge', None, None, keep_text=True)
# 0.1.11.92: the fusion reactor
adv('fusion_formed', 'clean_energy', m('fusion/block_fusion_controller'), 'goal', {'formed': code()}, t(
    ('A Star in a Doughnut', 'Build a fusion reactor: the torus, its coils and its ports'),
    ('甜甜圈里的恒星', '建成聚变堆：环面、线圈和端口'),
    ('ドーナツの中の恒星', '核融合炉を組む：トーラス、コイル、ポート'),
    ('Звезда в бублике', 'Постройте термоядерный реактор: тор, катушки и порты')))
adv('fusion_ignition', 'fusion_formed', m('fusion/block_fusion_toroidal_coil'), 'goal', {'ignited': code()}, t(
    ('Ignition', 'Heat the plasma until it burns on its own heat'),
    ('点火', '把等离子体加热到能靠自身热量燃烧'),
    ('点火', 'プラズマを自分の熱で燃え続けるまで加熱する'),
    ('Зажигание', 'Нагрейте плазму, пока она не будет гореть сама')))
adv('fusion_full_power', 'fusion_ignition', m('fusion/block_fusion_power_port'), 'challenge', {'full': code()}, t(
    ('Twice the Sun in a Jar', 'Run a fusion reactor at its full 39,200,000 EU/t'),
    ('二倍的人造太阳', '让聚变堆以满功率 39,200,000 EU/t 运行'),
    ('瓶の中の太陽二つ分', '核融合炉を最大の39,200,000 EU/tで動かす'),
    ('Два солнца в банке', 'Запустите термоядерный реактор на полные 39 200 000 EU/t')), xp=500)

# ---- tab 3: Tools and Gear -------------------------------------------------------------------------------------------
# 0.1.11.81: the Wrench needs only bronze ingots: straight under the root
adv('wrench', 'root', m('item_tool_wrench'), 'task', {'wrench': has(m('item_tool_wrench'), m('item_tool_wrench_elc'))}, t(
    ('Bad Dismantling, Tears in My Eyes', 'Make a Wrench'),
    ('拆机不规范，眼角两行泪', '制作扳手'),
    ('雑な解体、涙が二筋', 'レンチを作る'),
    ('Неправильный разбор — слёзы из глаз', 'Сделайте гаечный ключ')))
adv('broken_machine', 'wrench', m('producer/block_machine_hull_basic'), 'task', {'broken': code()}, t(
    ('A Cold Shell', 'Remember to take machines apart with a wrench'),
    ('冰冷的外壳', '记得使用扳手拆卸机器'),
    ('冷たい外殻', '機械はレンチで外すのを忘れずに'),
    ('Холодный корпус', 'Не забывайте разбирать машины ключом')), hidden=True)
adv('gold_miner', 'wrench', m('item_tool_iron_driller'), 'task', {'drill': has(m('item_tool_iron_driller'))}, t(
    ('Meet the Dentist', 'Make a Mining Drill'),
    ('会见牙医', '制作铁钻头'),
    ('歯医者さんに会う', '鉄ドリルを作る'),
    ('Визит к дантисту', 'Сделайте железный бур')))
adv('diamond_drill', 'gold_miner', m('item_tool_diamond_driller'), 'task', {'drill': has(m('item_tool_diamond_driller'))}, t(
    ('Worth It', 'Upgrade your drill with diamonds'),
    ('物有所值', '用钻石升级你的铁钻头'),
    ('お値打ち', 'ダイヤでドリルを強化する'),
    ('Оно того стоит', 'Улучшите бур алмазами')))
adv('iridium_drill', 'diamond_drill', m('item_tool_iridium_driller'), 'challenge', {'drill': has(m('item_tool_iridium_driller'))}, t(
    ('Worth Every Penny', 'From now on, drill through the earth with an Iridium Drill'),
    ('物超所值', '现在开始，用铱钻头挖穿地球'),
    ('値段以上', 'これからはイリジウムドリルで地球を掘り抜こう'),
    ('Стоит каждой копейки', 'Теперь пробурите Землю насквозь иридиевым буром')), xp=100)
adv('chainsaw', 'wrench', m('item_tool_iron_chainsaw'), 'task', {'chainsaw': has(m('item_tool_iron_chainsaw'))}, t(
    ('Chainsaw Massacre', 'A chainsaw fells trees, and it is not a bad weapon either'),
    ('电锯惊魂', '电锯不仅是砍树利器，也是一把不错的武器'),
    ('チェーンソーの恐怖', 'チェーンソーは木を切るだけでなく、なかなかの武器にもなる'),
    ('Бензопила', 'Пила валит деревья, да и оружие из неё неплохое')))
adv('chainsaw_creeper', 'chainsaw', m('item_tool_iron_chainsaw'), 'goal', {'creeper': killed_with('minecraft:creeper', m('item_tool_iron_chainsaw'))}, t(
    ('Chainsaw Butcher', 'With a chainsaw, even Creepers are nothing to fear'),
    ('电锯屠夫', '有了电锯，苦力怕也不足为惧'),
    ('チェーンソー屠殺人', 'チェーンソーがあればクリーパーも怖くない'),
    ('Мясник с пилой', 'С пилой и криперы не страшны')))
adv('energy_saving', 'wrench', m('item_tool_iron_driller'), 'task', {'enchanted': enchanted_with('mio_icif:energy_saving')}, t(
    ('Magic in Technology', 'Use magic to make an electric tool use less power'),
    ('科技中的魔法', '用魔法让电动工具更省电'),
    ('テクノロジーの中の魔法', '魔法で電動工具を省電力にする'),
    ('Магия в технологиях', 'С помощью магии заставьте электроинструмент тратить меньше энергии')))
adv('jetpack', 'wrench', m('armor/item_armor_jetpack_electric'), 'task', {'jetpack': has(m('armor/item_armor_jetpack_electric'))}, t(
    ('Escape Gravity', 'Blocky people can fly too!'),
    ('脱离地心引力', '方块人也能飞！'),
    ('重力からの脱出', '四角い人も飛べる！'),
    ('Прочь от гравитации', 'Квадратные люди тоже умеют летать!')))
adv('advanced_jetpack', 'jetpack', m('armor/item_armor_advanced_jetpack'), 'goal', {'jetpack': has(m('armor/item_armor_advanced_jetpack'))}, t(
    ('Fly Higher', 'Make an Advanced Electric Overclocked Jetpack'),
    ('飞得更高', '制作进阶电力超频喷气背包'),
    ('もっと高く', '上位電気オーバークロックジェットパックを作る'),
    ('Выше и выше', 'Сделайте продвинутый разогнанный электрический реактивный ранец')))
adv('flight_ring', 'advanced_jetpack', m('item_trinket_flight_ring'), 'challenge', {'ring': has(m('item_trinket_flight_ring'))}, curios=True, text=t(
    ('Feels Like Flying', 'At last you can fly freely'),
    ('飞一般的感觉', '现在你终于可以自由飞翔了'),
    ('飛ぶような心地', 'ついに自由に飛べるようになった'),
    ('Как будто летишь', 'Наконец-то можно летать свободно')), xp=100)
adv('laser', 'wrench', m('item_tool_laser_miner'), 'task', {'laser': has(m('item_tool_laser_miner'))}, t(
    ('Laser Time', 'Get a Mining Laser'),
    ('镭射时刻', '获得采矿镭射枪'),
    ('レーザータイム', '採掘レーザーを手に入れる'),
    ('Время лазера', 'Добудьте горный лазер')))
adv('like_a_boss', 'laser', m('item_tool_laser_miner'), 'challenge', {
    'dragon': killed_with('minecraft:ender_dragon', m('item_tool_laser_miner')),
    'wither': killed_with('minecraft:wither', m('item_tool_laser_miner'))}, t(
    ('Like a Boss', 'Kill the Ender Dragon and the Wither with a Mining Laser'),
    ('像 Boss 一样', '使用采矿镭射枪击杀末影龙和凋灵'),
    ('ボスのように', '採掘レーザーでエンダードラゴンとウィザーを倒す'),
    ('Как босс', 'Убейте Эндер-дракона и Иссушителя горным лазером')), xp=200)
adv('shield', 'wrench', m('item_tool_steel_shield'), 'task', {'blocked': code()}, t(
    ('Sorry, Not Tomorrow', 'Block a projectile with the Steel Riot Shield'),
    ('对不起，明天不行', '使用钢制防爆盾抵挡一次弹射物攻击'),
    ('ごめん、明日はダメ', '鋼鉄の防爆盾で飛び道具を一度防ぐ'),
    ('Извините, не завтра', 'Отбейте снаряд стальным противоударным щитом')))
adv('electric_rifle', 'wrench', m('item_tool_electric_rifle'), 'task', {'rifle': has(*RIFLES)}, t(
    ("It's High Noon", 'Make an Electric Laser Rifle'),
    ('午时已到', '制作电力步枪'),
    ('正午だ', '電気レーザーライフルを作る'),
    ('Уже полдень', 'Сделайте электрическую лазерную винтовку')))
adv('tachyon', 'electric_rifle', m('item_tool_tachyon_disruptor'), 'task', {'tachyon': has(m('item_tool_tachyon_disruptor'))}, t(
    ('Times Have Changed, My Lord', 'Make an Advanced Tachyon Disruptor'),
    ('大人，时代变了', '制作速子裂解枪'),
    ('殿、時代は変わりました', '上位タキオン分解銃を作る'),
    ('Времена изменились, господин', 'Сделайте продвинутый тахионный дезинтегратор')))
adv('nano_set', 'wrench', m('armor/item_armor_nano_chestplate'), 'challenge', {'set': has_all(*NANO)}, t(
    ('Nanoscale Materials', 'Make a full set of NanoSuit armor'),
    ('纳米级材料', '制作全套纳米防具'),
    ('ナノスケール素材', 'ナノスーツ一式を作る'),
    ('Наноматериалы', 'Сделайте полный комплект наноброни')), xp=100)
adv('skywalker', 'nano_set', m('item_tool_nanosaber'), 'task', None, None, keep_text=True)
adv('quantum_sword', 'skywalker', m('item_tool_quantum_sword'), 'task', {'sword': has(m('item_tool_quantum_sword'))}, t(
    ('Excalibur, Industrial Edition', 'Make a Quantum Sword'),
    ('Excalibur 工业版', '制作量子剑'),
    ('エクスカリバー産業版', '量子剣を作る'),
    ('Экскалибур, промышленная версия', 'Сделайте квантовый меч')))
adv('know_nothing', 'quantum_sword', m('item_tool_quantum_sword'), 'task', {'kill': code()}, t(
    ('You Know Nothing of Power', 'Kill a sword-wielding mob in full netherite with an overclocked Quantum Sword'),
    ('你对力量一无所知', '使用超频模式的量子剑杀死穿着全套下界合金装备的任意持剑生物'),
    ('力を何も知らぬ', 'オーバークロックの量子剣で、ネザライト一式を着て剣を持つモブを倒す'),
    ('Ты ничего не знаешь о силе', 'Убейте разогнанным квантовым мечом моба с мечом в полной незеритовой броне')), hidden=True)
adv('nano_bow', 'nano_set', m('item_tool_nano_bow'), 'task', {'bow': has(m('item_tool_nano_bow'))}, t(
    ('Can This Unleash Imagine Breaker?', 'Make a Nano Compound Bow'),
    ('这个能释放幻想破坏吗', '制作纳米弓'),
    ('これで幻想殺しが撃てる？', 'ナノ複合弓を作る'),
    ('Это выпустит «Убийцу иллюзий»?', 'Сделайте нанокомпозитный лук')))
adv('quantum_set', 'nano_set', m('armor/item_armor_quantum_helmet'), 'task', {'wearing': wearing(*QUANTUM)}, t(
    ('Schrödinger Approves', 'Wear a full set of Quantum armor'),
    ('薛定谔见了都说好', '穿着全套量子盔甲'),
    ('シュレディンガーも絶賛', '量子アーマー一式を着る'),
    ('Шрёдингер одобряет', 'Наденьте полный комплект квантовой брони')))
adv('forgot_charge', 'quantum_set', m('armor/item_armor_quantum_helmet'), 'challenge', {'died': code()}, t(
    ('Forgot to Charge', 'The Quantum Helmet is great, so how did you forget to charge it? (Starve or drown wearing it)'),
    ('忘记充电', '量子头盔虽然好用，但是你怎么忘了给自己的量子头盔充电呢？'),
    ('充電忘れ', '量子ヘルメットは便利なのに、どうして充電を忘れたの？（着けたまま餓死か溺死）'),
    ('Забыл зарядить', 'Квантовый шлем хорош, но как можно было забыть его зарядить? (Умрите от голода или утопления в нём)')),
    hidden=True, xp=100)
adv('armory', 'wrench', m('producer/block_armory'), 'goal', {'armory': has(m('producer/block_armory'))}, t(
    ('Fully Armed', 'Arm yourself with the Armory'),
    ('全副武装', '用武装库来武装自己'),
    ('完全武装', '武装庫で武装する'),
    ('Во всеоружии', 'Вооружитесь с помощью арсенала')))
adv('armory_remote', 'armory', m('normal/item_armory_remote'), 'task', {'remote': has(m('normal/item_armory_remote'))}, t(
    ('You Want to Hen-shin Too?', 'Make an Armory Remote'),
    ('你也要 hen-shin 吗', '制作武装库遥控器'),
    ('きみも変身したいの？', '武装庫リモートコントローラーを作る'),
    ('Тоже хочешь хэн-шин?', 'Сделайте пульт арсенала')))
adv('wingman', 'armory_remote', m('normal/item_armory_remote'), 'task', {'summoned': code()}, t(
    ('Now Face the Gang of Justice', 'Summon a Wingman'),
    ('接下来你将面对正义的群殴', '召唤僚机'),
    ('次は正義の袋叩きだ', '僚機を召喚する'),
    ('Сейчас тебя побьёт банда справедливости', 'Призовите ведомого')))
adv('modification_station', 'armory', m('producer/block_quantum_modification_station'), 'task', {'station': has(m('producer/block_quantum_modification_station'))}, t(
    ('Ever-Changing', 'Make a Universal Suit Modification Station'),
    ('千变万化', '制作通用套装改装台'),
    ('千変万化', '汎用スーツ改造台を作る'),
    ('Тысяча обличий', 'Сделайте универсальный станок модификации костюма')))
adv('suit_module', 'modification_station', m('module/item_module_phase_jump_drive'), 'task', {'module': has_tag('mio_icif:suit_modules')}, t(
    ('The Mask', 'Make any suit module'),
    ('变相怪杰', '制作任意套装单元'),
    ('マスク', 'スーツユニットを作る'),
    ('Маска', 'Сделайте любой модуль костюма')))
adv('quantum_jump', 'suit_module', m('module/item_module_phase_jump_drive'), 'task', {'decoy_jump': code()}, t(
    ('The Silver Flash of Singularity Iteration', 'Use a Quantum Jump while the Afterimage Decoy is active'),
    ('奇点迭代的银色闪光', '残像诱饵生效时使用一次量子跃迁'),
    ('特異点の銀色の閃光', '残像デコイが有効な状態で量子ジャンプを使う'),
    ('Серебряная вспышка Singularity Iteration', 'Совершите квантовый прыжок с активной приманкой-остаточным образом')))
adv('phase_shift', 'suit_module', m('module/item_module_phase_modulator'), 'task', {'phase': code()}, t(
    ('Kamui!', 'Use a Phase Shift'),
    ('神威！', '使用一次相位偏移'),
    ('神威！', '位相シフトを使う'),
    ('Камуи!', 'Используйте фазовый сдвиг')))
adv('time_warp', 'suit_module', m('module/item_module_behavior_predictor_mk2'), 'task', {'warp': code()}, t(
    ('Domain Expansion!', 'Use a Time Warp'),
    ('领域展开！', '使用一次时间扭曲'),
    ('領域展開！', 'タイムワープを使う'),
    ('Расширение территории!', 'Используйте искажение времени')))
adv('force_field', 'suit_module', m('module/item_module_deflector_mk2'), 'task', {'stopped': code()}, t(
    ('Just Kidding, Never Again', 'Stop a projectile with Force Field'),
    ('骗你的，今后都不行', '使用力场展开抵挡一次弹射物攻击'),
    ('うそ、今後もずっとダメ', 'フォースフィールドで飛び道具を一度止める'),
    ('Шучу, теперь никогда', 'Остановите снаряд силовым полем')))

# ---- tab 4: Automation and Technology --------------------------------------------------------------------------------
# 0.1.11.81: the ejector upgrades need tin plates (Forge Hammer)
adv('ejector', 'hammer_time', m('upgrade/ejector_upgrade'), 'task', {'ejector': has(*EJECTORS)}, t(
    ('Simple Automation', 'Make an Ejector Upgrade or a Fluid Ejector Upgrade'),
    ('简单自动化', '制作弹出升级或流体弹出升级'),
    ('かんたん自動化', '排出アップグレードか流体エジェクターアップグレードを作る'),
    ('Простая автоматизация', 'Сделайте улучшение выталкивателя или жидкостного выталкивателя')))
adv('overclocker', 'ejector', m('upgrade/overclocker_upgrade'), 'task', {'overclocker': has(m('upgrade/overclocker_upgrade'))}, t(
    ('Speed Up', 'Make an Overclocker Upgrade'),
    ('加速运行', '制作超频升级'),
    ('高速運転', 'オーバークロックアップグレードを作る'),
    ('Ускорение', 'Сделайте улучшение разгона')))
adv('miner', 'ejector', m('producer/block_miner_elc'), 'task', {'miner': has(m('producer/block_miner_elc'))}, t(
    ('Mechanical Miner', 'Make a Miner'),
    ('机械矿工', '制作采矿机'),
    ('機械の鉱夫', '採掘機を作る'),
    ('Механический шахтёр', 'Сделайте буровую установку')))
adv('advanced_miner', 'miner', m('producer/block_advanced_miner_elc'), 'goal', {'miner': has(m('producer/block_advanced_miner_elc'))}, t(
    ('Super Miner', 'Enjoy the unmatched mining speed of the Advanced Miner'),
    ('超级矿工', '享受高级采矿机无与伦比的挖矿效率'),
    ('スーパー鉱夫', '上位採掘機の比類なき採掘効率を味わう'),
    ('Супершахтёр', 'Оцените непревзойдённую скорость продвинутой буровой')))
adv('harvester', 'ejector', m('producer/block_harvest_elc'), 'task', {'harvester': has(m('producer/block_harvest_elc'))}, t(
    ('Automated Farming', 'Make a Crop Harvester'),
    ('自动化农业', '制作收割机'),
    ('自動化農業', '収穫機を作る'),
    ('Автоматизированная ферма', 'Сделайте комбайн')))
adv('crop_monitor', 'ejector', m('producer/block_matron_elc'), 'task', {'matron': has(m('producer/block_matron_elc'))}, t(
    ('Garden Steward', 'Make a Crop Matron'),
    ('田园管家', '制作作物监管机'),
    ('田園の執事', '作物管理機を作る'),
    ('Садовый управляющий', 'Сделайте смотрителя посевов')))
adv('teleporter', 'ejector', m('producer/block_teleporter_elc'), 'goal', {'teleporter': has(m('producer/block_teleporter_elc'))}, t(
    ('Space Shuttle', 'Time to give your legs a rest: make a Teleporter'),
    ('空间穿梭', '是时候解放双腿了'),
    ('空間シャトル', '脚を休める時だ：テレポーターを作る'),
    ('Челнок', 'Пора дать ногам отдохнуть: сделайте телепорт')))
adv('far_teleport', 'teleporter', m('producer/block_teleporter_elc'), 'challenge', {'far': code()}, t(
    ('A Thousand Miles Away', 'Teleport more than 1000 blocks with a Teleporter'),
    ('千里之外', '使用传送机传送到 1000 米以外的地方'),
    ('千里の彼方', 'テレポーターで 1000 ブロック以上テレポートする'),
    ('За тысячу вёрст', 'Телепортируйтесь дальше чем на 1000 блоков')), xp=100)
adv('terraformer', 'ejector', m('producer/block_terra_elc'), 'goal', {'terraformer': has(m('producer/block_terra_elc'))}, t(
    ('Reshape the World', 'Make a Terraformer'),
    ('重塑世界', '制造地形转换机'),
    ('世界を作り変える', 'テラフォーマーを作る'),
    ('Перекроить мир', 'Сделайте терраформер')))
adv('end_paradise', 'terraformer', m('producer/block_terra_elc'), 'challenge', {'end_terraform': code()}, t(
    ('The End as Paradise', 'Fill the End with life: run a Terraformer in the End for 60 seconds'),
    ('末地如天堂', '让末地充满蓬勃生机'),
    ('天国のようなジ・エンド', 'ジ・エンドに命を満たそう：テラフォーマーを 60 秒動かす'),
    ('Край как рай', 'Наполните Край жизнью: терраформер в Крае 60 секунд')), xp=100)
adv('tesla', 'ejector', m('producer/block_tesla'), 'goal', {'fired': code()}, t(
    ('Tesla Trooper', 'Place and power a Tesla Coil, and be careful once it is on!'),
    ('磁暴步兵', '放置并激活一个特斯拉线圈，激活后务必小心！'),
    ('テスラ兵', 'テスラコイルを置いて起動しよう、起動したらくれぐれも注意！'),
    ('Тесла-пехотинец', 'Поставьте и запустите катушку Теслы, и берегитесь после этого!')))
adv('futures', 'ejector', m('producer/block_future_elc'), 'goal', {'futures': has(m('producer/block_future_elc'))}, t(
    ('Market Trading', 'Make a Futures Machine'),
    ('市场交易', '制作期货机'),
    ('市場取引', '先物取引機を作る'),
    ('Биржевая торговля', 'Сделайте фьючерсный автомат')))
adv('nano_metal', 'ejector', m('resource/item_nano_living_metal'), 'task', {'metal': has(m('resource/item_nano_living_metal'))}, t(
    ("Buddy, It's Wriggling", 'Get Nano Superfluid Metal'),
    ('「伙计，它在蠕动」', '获得纳米超流体金属'),
    ('「おい、うごめいてるぞ」', 'ナノ超流動金属を手に入れる'),
    ('«Приятель, оно шевелится»', 'Добудьте наносверхтекучий металл')))
adv('living_circuit', 'nano_metal', m('normal/item_living_circuit'), 'task', {'circuit': has(m('normal/item_living_circuit'))}, t(
    ("Dude, It's Alive!", 'Get a Living Circuit'),
    ('「哥们儿，它是活的！」', '获得活体电路板'),
    ('「おい、生きてるぞ！」', 'リビング回路を手に入れる'),
    ('«Чувак, оно живое!»', 'Добудьте живую схему')))
adv('ex_nihilo', 'ejector', m('producer/block_matter_elc'), 'task', None, None, keep_text=True)
adv('reality_refactor', 'ex_nihilo', m('producer/block_molecular_transformer'), 'task', None, None, keep_text=True)
adv('rearrange_not_create', 'reality_refactor', m('producer/block_replicator_elc'), 'task', None, None, keep_text=True)
adv('picard_order', 'rearrange_not_create', m('producer/block_replicator_elc'), 'task', None, None, keep_text=True)

TAB_ROOTS = ('root',)   # 0.1.11.81: one tab (Singularity Iteration); the former roots of Energy, Tools and Gear, Automation are branches


# ------------------------------------------------------------------ writing

def items_in(obj, out):
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k in ('items', 'id') and isinstance(v, str) and v.startswith('mio_icif:'):
                out.add(v)
            elif k == 'items' and isinstance(v, list):
                out.update(x for x in v if isinstance(x, str) and x.startswith('mio_icif:'))
            else:
                items_in(v, out)
    elif isinstance(obj, list):
        for v in obj:
            items_in(v, out)


def load(path):
    with open(path, 'rb') as f:
        return json.loads(f.read().decode('utf-8-sig'), object_pairs_hook=OrderedDict)


def build(a):
    path = os.path.join(ADV, a['id'] + '.json')
    old = load(path) if os.path.exists(path) else None
    criteria = a['criteria'] if a['criteria'] is not None else old['criteria']
    display = OrderedDict()
    display['icon'] = old['display']['icon'] if a['keep_text'] and old is not None else {'id': a['icon']}
    display['title'] = {'translate': 'advancements.mio_icif.%s.title' % a['id']}
    display['description'] = {'translate': 'advancements.mio_icif.%s.description' % a['id']}
    if a['parent'] is None:
        display['background'] = BACKGROUND
    display['frame'] = a['frame']
    display['show_toast'] = a['id'] != 'root'
    display['announce_to_chat'] = a['id'] != 'root'
    display['hidden'] = a['hidden'] if a['hidden'] is not None else (bool(old['display'].get('hidden', False)) if old is not None else False)
    out = OrderedDict()
    if a['curios']:   # a Curios item: the advancement exists only with Curios (else its icon and criterion are unknown)
        out['neoforge:conditions'] = [{'type': 'neoforge:mod_loaded', 'modid': 'curios'}]
    if a['parent'] is not None:
        out['parent'] = 'mio_icif:' + a['parent']
    out['display'] = display
    out['criteria'] = criteria
    if old is not None and 'requirements' in old and a['criteria'] is None:
        out['requirements'] = old['requirements']
    if a['xp'] or (old is not None and a['criteria'] is None and 'rewards' in old):
        out['rewards'] = {'experience': a['xp']} if a['xp'] else old['rewards']
    return path, out


def check_tree():
    ids = {a['id'] for a in A}
    assert len(ids) == len(A), 'duplicate id'
    for a in A:
        assert (a['parent'] is None) == (a['id'] in TAB_ROOTS), a['id']
        assert a['parent'] is None or a['parent'] in ids, (a['id'], a['parent'])
        if not a['keep_text']:
            assert a['text'] and set(a['text']) == set(LANGS), a['id']
    missing = set()
    for a in A:
        found = {a['icon']}
        items_in(a['criteria'] or {}, found)
        for i in found:
            if not os.path.exists(os.path.join(MODELS, i.split(':', 1)[1] + '.json')):
                missing.add((a['id'], i))
    return missing


def write_json(path, data):
    text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
    if CHECK:
        return os.path.exists(path) and open(path, encoding='utf-8').read() == text
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)
    return True


def suit_modules():
    names = sorted(n[:-5] for n in os.listdir(os.path.join(MODELS, 'module')) if n.startswith('item_module_') and n.endswith('.json'))
    return {'values': ['mio_icif:module/' + n for n in names]}


def lang():
    bad = 0
    for code_ in LANGS:
        path = os.path.join(LANG, code_ + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        mine = {}
        for a in A:
            if a['keep_text']:
                continue
            title, desc = a['text'][code_]
            mine['advancements.mio_icif.%s.title' % a['id']] = title
            mine['advancements.mio_icif.%s.description' % a['id']] = desc
        if CHECK:
            wrong = [k for k, v in mine.items() if data.get(k) != v]
            bad += len(wrong)
            print(code_, 'missing or different:', len(wrong), wrong[:4])
            continue
        for k, v in mine.items():
            data[k] = v
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(code_, 'keys set:', len(mine))
    for a in A:
        if a['keep_text']:
            data = json.loads(open(os.path.join(LANG, 'en_us.json'), 'rb').read().decode('utf-8-sig'))
            if 'advancements.mio_icif.%s.title' % a['id'] not in data:
                print('kept text missing for', a['id'])
                bad += 1
    return bad


def main():
    missing = check_tree()
    if missing:
        for aid, item in sorted(missing):
            print('missing item model:', aid, item)
        sys.exit(1)
    stale = 0
    for a in A:
        path, data = build(a)
        if not write_json(path, data):
            stale += 1
            print('different:', os.path.relpath(path, ROOT))
    tag = os.path.join(RES, 'data/mio_icif/tags/item/suit_modules.json')
    if not write_json(tag, suit_modules()):
        stale += 1
        print('different: suit_modules tag')
    # batpacks always; the Curios belts only when Curios registers them (optional entries)
    portable = {'values': [p for p in PACKS if 'trinket' not in p] + [{'id': p, 'required': False} for p in PACKS if 'trinket' in p]}
    if not write_json(os.path.join(RES, 'data/mio_icif/tags/item/portable_energy_storage.json'), portable):
        stale += 1
        print('different: portable_energy_storage tag')
    stale += lang()
    roots = {}
    for a in A:
        r = a
        while r['parent'] is not None:
            r = next(x for x in A if x['id'] == r['parent'])
        roots.setdefault(r['id'], 0)
        roots[r['id']] += 1
    print('advancements:', len(A), 'per tab:', roots, 'code-granted:',
          sum(1 for a in A if a['criteria'] and any(c.get('trigger') == 'minecraft:impossible' for c in a['criteria'].values())))
    if CHECK and stale:
        sys.exit(1)


if __name__ == '__main__':
    main()
