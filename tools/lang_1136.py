#!/usr/bin/env python3
"""0.1.10.36 language values (en_us / zh_cn / ja_jp / ru_ru).
Phase 1: the doubled words of the Chinese weapon and laser tower names (激光镭射 -> 激光, 等离子电浆 -> 等离子), the renamed
materials (Coal Chunk -> Graphite Block, Nano Living Metal -> Nano Superfluid Metal, Neutron Plate -> Degenerate Zero
Matter) and the advanced battery / advanced charging battery name collision.
Phase 2: "Also accepts" of bounty needs; the Miner Retract button and its status lines.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys.
Usage: lang_1136.py <repo root> [--check]
"""
import json
import os
import re
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
LANGS = ('en_us', 'zh_cn', 'ja_jp', 'ru_ru')


def v(en=None, zh=None, ja=None, ru=None):
    return {k: t for k, t in zip(LANGS, (en, zh, ja, ru)) if t is not None}


KEYS = {
    # ---- 1.1 doubled words in Chinese names (the Mining Laser and the CF Sprayer keep theirs)
    'item.mio_icif.item_tool_electric_rifle': v(zh='电力激光步枪'),
    'item.mio_icif.item_tool_advanced_electric_rifle': v(zh='进阶电力激光步枪'),
    'item.mio_icif.item_tool_tactical_laser_rifle': v(zh='铱制刺刀战术激光步枪'),
    'item.mio_icif.item_tool_electric_plasma_gun': v(zh='电力等离子发射器'),
    'block.mio_icif.producer.block_laser_defense_tower': v(zh='激光防御塔'),
    'block.mio_icif.producer.block_sky_patrol_laser_tower': v(zh='巡天激光防御站'),
    'container.mio_icif.laser_defense_tower': v(zh='激光防御塔'),
    'container.mio_icif.sky_patrol_laser_tower': v(zh='巡天激光防御站'),
    'death.attack.laser_tower': v(zh='%1$s被激光防御塔汽化了'),
    'death.attack.laser_tower.player': v(zh='%1$s在与%2$s战斗时被激光防御塔汽化了'),
    # ---- 1.1 renamed materials (registry ids unchanged)
    'item.mio_icif.resource.item_coal_chunk': v('Graphite Block', '石墨块', '黒鉛ブロック', 'Графитовый блок'),
    'item.mio_icif.resource.item_nano_living_metal': v(
        'Nano Superfluid Metal', '纳米超流体金属', 'ナノ超流動金属', 'Нано-сверхтекучий металл'),
    'item.mio_icif.resource.item_neutron_plate': v(
        'Degenerate Zero Matter', '简并态零素', '縮退ゼロ物質', 'Вырожденная нуль-материя'),
    'module.mio_icif.tooltip.mk2_available': v(
        'Mk.II available (Living Circuit + Degenerate Zero Matter)', '可升级为 Mk.II（生体电路 + 简并态零素）',
        'Mk.II あり（生体回路＋縮退ゼロ物質）'),
    # ---- 1.1 advanced battery (advbat) vs advanced charging battery (advchargebat)
    'item.mio_icif.normal.item_advbat_lev0': v('Advanced Battery', '高级电池', '高度バッテリー', 'Продвинутая батарея'),
    'item.mio_icif.normal.item_advchargebat_lev0': v(
        'Advanced Charging Battery', '高级充电电池', '高度充電バッテリー', 'Продвинутая зарядная батарея'),
    # ---- 2.1 bounties and commissions accept the items of the requested item's material tags
    'gui.mio_icif.exchange.bounty_accepts': v('Also accepts: %1$s', '也接受：%1$s', 'こちらも受け付け：%1$s'),
    # ---- 2.4 Miner: Mining Laser in the drill slot, pipe retraction
    'gui.mio_icif.miner.retract': v('Retract', '收回', '回収', 'Поднять'),
    'gui.mio_icif.miner.mine': v('Mine', '开采', '採掘', 'Копать'),
    'gui.mio_icif.miner.retract_help': v(
        'Pulls the mining pipes back up, one every 5 ticks for 5 EU each, into the pipe slot, the storage or an adjacent '
        'inventory. Mining stops until you press the button again. Taking the drill out also retracts the pipes.',
        '把采矿管道逐根收回（每5刻一根，每根耗5EU），放入管道槽、存储栏或相邻的容器。再次按下之前采矿机停止开采。'
        '取出钻头也会收回管道。',
        '採掘パイプを5tickごとに1本（1本5EU）引き上げ、パイプスロット・保管欄・隣接するインベントリに入れます。'
        'もう一度押すまで採掘は止まります。ドリルを取り出してもパイプを回収します。',
        'Поднимает шахтные трубы обратно, по одной каждые 5 тиков за 5 EU, в слот труб, хранилище или соседний '
        'инвентарь. Добыча останавливается до повторного нажатия. Если вынуть бур, трубы тоже поднимаются.'),
    'gui.mio_icif.miner.status.1': v('Retracting pipes', '正在收回管道', 'パイプを回収中', 'Трубы поднимаются'),
    'gui.mio_icif.miner.status.2': v(
        'No room for the pipes: empty the pipe slot or the storage', '没有空间放管道：请清空管道槽或存储栏',
        'パイプを入れる場所がありません：パイプスロットか保管欄を空けてください',
        'Нет места для труб: освободите слот труб или хранилище'),
    'gui.mio_icif.miner.status.3': v(
        'A protection prevents removing the pipe', '保护阻止了移除管道', '保護によりパイプを取り外せません',
        'Защита не даёт убрать трубу'),
    'gui.mio_icif.miner.status.4': v('Not enough energy', '能量不足', 'エネルギーが足りません', 'Недостаточно энергии'),
    'gui.mio_icif.miner.status.5': v(
        'The Mining Laser must be in Mining or Super Heat mode', '采矿镭射枪须处于采矿模式或超级热线模式',
        'マイニングレーザーは採掘モードか超高熱モードにしてください',
        'Шахтёрский лазер должен быть в режиме добычи или перегрева'),
    # ---- 3.2 fuels: proton rods, the tritium neutron source, the lithium breeder
    'item.mio_icif.reactor.item_reactor_proton_simple': v('Fuel Rod (Proton)', '燃料棒(质子)', '燃料棒（陽子）', 'ТВЭЛ (протон)'),
    'item.mio_icif.reactor.item_reactor_proton_dual': v('Dual Fuel Rod (Proton)', '双联燃料棒(质子)', '二連燃料棒（陽子）', 'Двойной ТВЭЛ (протон)'),
    'item.mio_icif.reactor.item_reactor_proton_quad': v('Quad Fuel Rod (Proton)', '四联燃料棒(质子)', '四連燃料棒（陽子）', 'Четверной ТВЭЛ (протон)'),
    'item.mio_icif.reactor.item_reactor_proton_simple_depleted': v(
        'Fuel Rod (Depleted Proton)', '燃料棒(枯竭质子)', '燃料棒（枯渇陽子）', 'ТВЭЛ (отработанный протон)'),
    'item.mio_icif.reactor.item_reactor_proton_dual_depleted': v(
        'Dual Fuel Rod (Depleted Proton)', '双联燃料棒(枯竭质子)', '二連燃料棒（枯渇陽子）', 'Двойной ТВЭЛ (отработанный протон)'),
    'item.mio_icif.reactor.item_reactor_proton_quad_depleted': v(
        'Quad Fuel Rod (Depleted Proton)', '四联燃料棒(枯竭质子)', '四連燃料棒（枯渇陽子）', 'Четверной ТВЭЛ (отработанный протон)'),
    'item.mio_icif.reactor.component.neutron_source': v(
        'Neutron source: each adjacent fuel rod takes one more pulse', '中子源：每根相邻燃料棒多受一次脉冲',
        '中性子源：隣接する燃料棒はそれぞれパルスを 1 回多く受ける', 'Источник нейтронов: каждый соседний ТВЭЛ получает ещё один импульс'),
    'item.mio_icif.reactor.component.decay': v('Decay: %s / %s cycles', '衰变：%s / %s 个周期', '崩壊：%s / %s サイクル',
        'Распад: %s / %s циклов'),
    'reactor_audit.kind.neutron_source': v('neutron source', '中子源', '中性子源', 'источник нейтронов'),
    # ---- 3.1 deepslate iridium ore (made by nuclear blasts) and small iridium dust
    'block.mio_icif.block_ore_iridium_in_deep': v(
        'Deepslate Iridium Ore', '深层铱矿石', '深層岩イリジウム鉱石', 'Глубинносланцевая иридиевая руда'),
    'item.mio_icif.resource.item_iridium_dust_small': v(
        'Small Iridium Dust', '小撮铱粉', '少量のイリジウムの粉', 'Малая кучка иридиевой пыли'),
    # ---- 4.1 the Scanner prices other mods' items itself
    'gui.mio_icif.scanner.excluded': v('Excluded', '不可扫描', '対象外', 'Исключено'),
    'gui.mio_icif.scanner.source.recipe': v('From recipes', '按配方计算', 'レシピから算出', 'По рецептам'),
    'gui.mio_icif.scanner.source.estimate': v('Estimated', '估算', '推定', 'Оценка'),
    'command.mio_icif.uu_scanner.unknown': v('Unknown item %s', '未知物品 %s', '不明なアイテム %s', 'Неизвестный предмет %s'),
    'command.mio_icif.uu_scanner.published': v(
        '%s: published price %s B', '%s：已发布价格 %s B', '%s：公開価格 %s B', '%s: опубликованная цена %s B'),
    'command.mio_icif.uu_scanner.saved': v(
        '%s: Scanner price %s B (%s)', '%s：扫描仪价格 %s B（%s）', '%s：スキャナー価格 %s B（%s）', '%s: цена сканера %s B (%s)'),
    'command.mio_icif.uu_scanner.would': v(
        '%s: not scanned yet; a scan would give %s B (%s)', '%s：尚未扫描；扫描将得到 %s B（%s）',
        '%s：未スキャン。スキャンすると %s B（%s）', '%s: ещё не сканирован; скан даст %s B (%s)'),
    'command.mio_icif.uu_scanner.excluded': v(
        '%s: the Scanner never prices this item (excluded namespace or creative-only)', '%s：扫描仪不为此物品定价（被排除的命名空间或仅限创造）',
        '%s：スキャナーはこのアイテムに価格を付けません（除外された名前空間かクリエイティブ専用）',
        '%s: сканер не оценивает этот предмет (исключённое пространство имён или только для творчества)'),
    'command.mio_icif.uu_scanner.none': v(
        '%s: no price can be worked out', '%s：无法计算价格', '%s：価格を算出できません', '%s: цену вычислить нельзя'),
    'command.mio_icif.uu_scanner.forgot': v(
        '%s: Scanner price removed; the next scan works it out again', '%s：已移除扫描仪价格，下次扫描重新计算',
        '%s：スキャナー価格を削除しました。次のスキャンで再計算します', '%s: цена сканера удалена; следующий скан вычислит её заново'),
    'command.mio_icif.uu_scanner.not_saved': v(
        '%s: no Scanner price saved', '%s：没有已保存的扫描仪价格', '%s：保存されたスキャナー価格はありません', '%s: сохранённой цены сканера нет'),
    # ---- 4.2 the steam reactor, its chamber, the steam vents and the steam turbine generator
    'block.mio_icif.generator.block_steam_reactor': v('Steam Reactor', '蒸汽反应堆', '蒸気原子炉', 'Паровой реактор'),
    'container.mio_icif.steam_reactor': v('Steam Reactor', '蒸汽反应堆', '蒸気原子炉', 'Паровой реактор'),
    'block.mio_icif.reactor.block_steam_reactor_chamber': v(
        'Steam Reactor Chamber', '蒸汽反应仓', '蒸気原子炉チャンバー', 'Камера парового реактора'),
    'block.mio_icif.generator.block_steam_turbine': v(
        'Steam Turbine Generator', '蒸汽涡轮发电机', '蒸気タービン発電機', 'Паротурбинный генератор'),
    'item.mio_icif.reactor.item_reactor_steam_vent': v('Steam Vent', '蒸汽散热片', 'スチームベント', 'Паровой отвод тепла'),
    'item.mio_icif.reactor.item_reactor_steam_vent_core': v(
        'Reactor Steam Vent', '反应堆蒸汽散热片', '原子炉スチームベント', 'Паровой отвод тепла реактора'),
    'item.mio_icif.reactor.item_reactor_steam_vent_overclocked': v(
        'Overclocked Steam Vent', '超频蒸汽散热片', 'オーバークロックスチームベント', 'Разогнанный паровой отвод тепла'),
    'item.mio_icif.reactor.item_reactor_steam_vent_advanced': v(
        'Advanced Steam Vent', '高级蒸汽散热片', '高度スチームベント', 'Продвинутый паровой отвод тепла'),
    'item.mio_icif.reactor.component.steam_vent': v(
        'In a Steam Reactor: up to %s mB of steam / 20 ticks from its heat, more the hotter it is',
        '在蒸汽反应堆中：用自身热量产生蒸汽，最多 %s mB / 20 tick，越热越多',
        '蒸気原子炉内：自身の熱から蒸気を最大 %s mB / 20 tick 生成し、熱いほど多い',
        'В паровом реакторе: до %s мБ пара за 20 тиков из своего тепла, чем горячее, тем больше'),
    'reactor_audit.kind.steam_vent': v('steam vent', '蒸汽散热片', 'スチームベント', 'паровой отвод тепла'),
}


def main():
    bad = 0
    for lang in LANGS:
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        mine = {k: t[lang] for k, t in KEYS.items() if lang in t}
        if CHECK:
            stray = [k for k, t in mine.items() if '%' in re.sub(r'%(?:\d+\$)?([A-Za-z%]|$)', '', t)]
            bad += len(stray)
            if stray:
                print(lang, 'stray %:', stray)
            wrong = [k for k, t in mine.items() if data.get(k) != t]
            bad += len(wrong)
            print(lang, 'missing or different:', len(wrong), wrong[:5])
            continue
        for key, t in mine.items():
            data[key] = t
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(mine))
    if CHECK and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
