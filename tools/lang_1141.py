#!/usr/bin/env python3
"""0.1.10.41 language values (en_us / zh_cn / ja_jp / ru_ru): the Steam Turbine Rotors (block names, the
tooltip with their bonus; the Steam Tunnel's Jade rotor line, status, problem message and structure notes that rotors change).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys.
Usage: lang_1141.py <repo root> [--check]
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
    'block.mio_icif.generator.block_steam_turbine_rotor_bronze': v('Bronze Steam Turbine Rotor', '青铜蒸汽涡轮转子', '青銅の蒸気タービンローター', 'Бронзовый ротор паровой турбины'),
    'block.mio_icif.generator.block_steam_turbine_rotor_iron': v('Iron Steam Turbine Rotor', '铁蒸汽涡轮转子', '鉄の蒸気タービンローター', 'Железный ротор паровой турбины'),
    'block.mio_icif.generator.block_steam_turbine_rotor_adviron': v('Refined Iron Steam Turbine Rotor', '精炼铁蒸汽涡轮转子', '精錬鉄の蒸気タービンローター', 'Ротор паровой турбины из очищенного железа'),
    'block.mio_icif.generator.block_steam_turbine_rotor_titanium': v('Titanium Steam Turbine Rotor', '钛蒸汽涡轮转子', 'チタンの蒸気タービンローター', 'Титановый ротор паровой турбины'),
    'block.mio_icif.generator.block_steam_turbine_rotor_iridium': v('Iridium Steam Turbine Rotor', '铱蒸汽涡轮转子', 'イリジウムの蒸気タービンローター', 'Иридиевый ротор паровой турбины'),
    'block.mio_icif.generator.block_steam_turbine_rotor_netherite': v('Netherite Steam Turbine Rotor', '下界合金蒸汽涡轮转子', 'ネザライトの蒸気タービンローター', 'Незеритовый ротор паровой турбины'),
    'tooltip.mio_icif.steam_turbine_rotor.bonus': v(
        'In a Steam Tunnel: +%1$s EU per 1,024 mB of steam (+%2$s%%)', '放入蒸汽隧道涡轮机内：每 1,024 mB 蒸汽 +%1$s EU（+%2$s%%）',
        'スチームトンネルの中：蒸気 1,024 mB ごとに +%1$s EU（+%2$s%%）', 'В паровом туннеле: +%1$s EU на 1 024 mB пара (+%2$s%%)'),
    # 0.1.10.41: Steam Tunnel texts that rotors change (moved here from lang_1139.py)
    'message.mio_icif.steam_tunnel.problem.air': v(
        'Steam Tunnel: the inside must be air or Steam Turbine Rotors (%s)', '蒸汽隧道涡轮机：内部只能是空气或蒸汽涡轮转子（%s）',
        'スチームトンネル：内部は空気か蒸気タービンローターだけにする（%s）',
        'Паровой туннель: внутри может быть только воздух или роторы паровой турбины (%s)'),
    'structure.mio_icif.steam_tunnel.power_port': v(
        'Power port: up to 3,968 EU/t with rotors, as HV packets (512 EU)', '电力端口：装满转子时最多 3,968 EU/t，HV 数据包（512 EU）',
        '電力ポート：ローター込みで最大 3,968 EU/t、HV パケット（512 EU）',
        'Энергетический порт: до 3 968 EU/t с роторами, пакетами HV (512 EU)'),
    'structure.mio_icif.steam_tunnel.note.inside': v(
        'The 3 blocks inside: air or up to 3 Steam Turbine Rotors; the controller faces out of the front',
        '内部 3 格：空气或最多 3 个蒸汽涡轮转子；控制器朝向前端外侧',
        '内部の 3 ブロックは空気か蒸気タービンローター（最大 3 個）。コントローラーは前端の外を向く',
        '3 блока внутри: воздух или до 3 роторов паровой турбины; контроллер смотрит наружу спереди'),
    'structure.mio_icif.steam_tunnel.note.output': v(
        '1,024 mB of steam make 512 EU (each rotor adds 32 to 160), up to 4 times a tick; 10,000 EU buffer',
        '每 1,024 mB 蒸汽产生 512 EU（每个转子另加 32～160），每 tick 最多 4 次；缓存 10,000 EU',
        '蒸気 1,024 mB で 512 EU（ローター 1 個ごとに 32～160 増える）、1 tick に最大 4 回。バッファ 10,000 EU',
        '1 024 mB пара дают 512 EU (каждый ротор добавляет от 32 до 160), до 4 раз за тик; буфер 10 000 EU'),
    'message.mio_icif.steam_tunnel.running': v(
        'Steam Tunnel: running, %1$s EU/t (%2$s ports, %3$s EU per 1,024 mB)', '蒸汽隧道涡轮机：运行中，%1$s EU/t（%2$s 个端口，每 1,024 mB %3$s EU）',
        'スチームトンネル：稼働中、%1$s EU/t（ポート %2$s 個、1,024 mB で %3$s EU）',
        'Паровой туннель: работает, %1$s EU/t (портов: %2$s, %3$s EU на 1 024 mB)'),
    'jade.mio_icif.steam_tunnel.rotors': v('Rotors: %1$s (%2$s)', '转子：%1$s 个（%2$s）', 'ローター：%1$s 個（%2$s）', 'Роторы: %1$s (%2$s)'),
    'jade.mio_icif.steam_tunnel.output': v('Output: %1$s EU/t (max %2$s)', '输出：%1$s EU/t（最大 %2$s）', '出力：%1$s EU/t（最大 %2$s）',
        'Выход: %1$s EU/t (макс. %2$s)'),
    'jade.mio_icif.steam_tunnel.steam_use': v(
        'Steam use: %1$s mB/t (%2$s mB/t for full output)', '蒸汽用量：%1$s mB/t（满功率需 %2$s mB/t）',
        '蒸気使用量：%1$s mB/t（全出力には %2$s mB/t）', 'Расход пара: %1$s mB/t (%2$s mB/t для полной мощности)'),
    'jade.mio_icif.steam_tunnel.waiting_steam': v(
        'Waiting for steam (1,024 mB per step)', '等待蒸汽（每步 1,024 mB）', '蒸気待ち（1 回に 1,024 mB）', 'Ждёт пар (1 024 mB на шаг)'),
    'jade.mio_icif.steam_tunnel.buffer_full': v(
        'Buffer full: power is not being drawn', '缓存已满：电力未被取走', 'バッファが満杯：電力が使われていない', 'Буфер полон: энергию не забирают'),
    'message.mio_icif.steam_tunnel.waiting_steam': v(
        'Steam Tunnel: formed, waiting for steam (%2$s ports, %3$s EU per 1,024 mB)', '蒸汽隧道涡轮机：已成型，等待蒸汽（%2$s 个端口，每 1,024 mB %3$s EU）',
        'スチームトンネル：完成、蒸気待ち（ポート %2$s 個、1,024 mB で %3$s EU）',
        'Паровой туннель: собран, ждёт пар (портов: %2$s, %3$s EU на 1 024 mB)'),
    'message.mio_icif.steam_tunnel.buffer_full': v(
        'Steam Tunnel: buffer full, power is not being drawn (%2$s ports, %3$s EU per 1,024 mB)',
        '蒸汽隧道涡轮机：缓存已满，电力未被取走（%2$s 个端口，每 1,024 mB %3$s EU）',
        'スチームトンネル：バッファが満杯、電力が使われていない（ポート %2$s 個、1,024 mB で %3$s EU）',
        'Паровой туннель: буфер полон, энергию не забирают (портов: %2$s, %3$s EU на 1 024 mB)'),
    'jade.mio_icif.steam_tunnel.rotor_separator': v(', ', '、', '、', ', '),
    'jade.mio_icif.steam_tunnel.rotor.bronze': v('Bronze', '青铜', '青銅', 'бронза'),
    'jade.mio_icif.steam_tunnel.rotor.iron': v('Iron', '铁', '鉄', 'железо'),
    'jade.mio_icif.steam_tunnel.rotor.adviron': v('Refined Iron', '精炼铁', '精錬鉄', 'очищенное железо'),
    'jade.mio_icif.steam_tunnel.rotor.titanium': v('Titanium', '钛', 'チタン', 'титан'),
    'jade.mio_icif.steam_tunnel.rotor.iridium': v('Iridium', '铱', 'イリジウム', 'иридий'),
    'jade.mio_icif.steam_tunnel.rotor.netherite': v('Netherite', '下界合金', 'ネザライト', 'незерит'),
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
