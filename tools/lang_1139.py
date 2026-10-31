#!/usr/bin/env python3
"""0.1.10.39 language values (en_us / zh_cn / ja_jp / ru_ru): the Steam Tunnel (block names, its state line, Jade, the JEI
structure plan; the structure keys have no ru_ru, like their siblings).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys.
Usage: lang_1139.py <repo root> [--check]
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
    # C1: the Steam Tunnel
    'block.mio_icif.generator.block_steam_tunnel': v('Steam Tunnel', '蒸汽隧道涡轮机', 'スチームトンネル', 'Паровой туннель'),
    'block.mio_icif.generator.block_steam_tunnel_casing': v(
        'Steam Tunnel Casing', '蒸汽隧道外壳', 'スチームトンネル外装', 'Корпус парового туннеля'),
    'block.mio_icif.generator.block_steam_tunnel_fluid_port': v(
        'Steam Tunnel Fluid Port', '蒸汽隧道流体端口', 'スチームトンネル流体ポート', 'Жидкостный порт парового туннеля'),
    'block.mio_icif.generator.block_steam_tunnel_power_port': v(
        'Steam Tunnel Power Port', '蒸汽隧道电力端口', 'スチームトンネル電力ポート', 'Энергетический порт парового туннеля'),
    'message.mio_icif.steam_tunnel.running': v(
        'Steam Tunnel: running, %1$s EU/t (%2$s ports)', '蒸汽隧道涡轮机：运行中，%1$s EU/t（%2$s 个端口）',
        'スチームトンネル：稼働中、%1$s EU/t（ポート %2$s 個）', 'Паровой туннель: работает, %1$s EU/t (портов: %2$s)'),
    'message.mio_icif.steam_tunnel.idle': v(
        'Steam Tunnel: formed, idle (%2$s ports): it needs 1,024 mB of steam and room in its buffer',
        '蒸汽隧道涡轮机：已成型，待机（%2$s 个端口）：需要 1,024 mB 蒸汽且缓存有空间',
        'スチームトンネル：完成、待機中（ポート %2$s 個）：蒸気 1,024 mB とバッファの空きが必要',
        'Паровой туннель: собран, простаивает (портов: %2$s): нужно 1 024 mB пара и место в буфере'),
    'message.mio_icif.steam_tunnel.checking': v(
        'Steam Tunnel: checking the structure...', '蒸汽隧道涡轮机：正在检查结构……', 'スチームトンネル：構造を確認中…',
        'Паровой туннель: проверка конструкции...'),
    'message.mio_icif.steam_tunnel.problem.unloaded': v(
        'Steam Tunnel: part of the structure is not loaded (%s)', '蒸汽隧道涡轮机：部分结构未加载（%s）',
        'スチームトンネル：構造の一部が読み込まれていない（%s）', 'Паровой туннель: часть конструкции не загружена (%s)'),
    'message.mio_icif.steam_tunnel.problem.air': v(
        'Steam Tunnel: the inside must be air (%s)', '蒸汽隧道涡轮机：内部必须为空气（%s）', 'スチームトンネル：内部は空気でなければならない（%s）',
        'Паровой туннель: внутри должен быть воздух (%s)'),
    'message.mio_icif.steam_tunnel.problem.port_on_edge': v(
        'Steam Tunnel: a port on an edge (%s); ports go in the middle of a side or the back',
        '蒸汽隧道涡轮机：端口在棱上（%s）；端口只能放在侧面中间或后端中心',
        'スチームトンネル：ポートが辺にある（%s）。ポートは側面の中央か後端の中心に置く',
        'Паровой туннель: порт на ребре (%s); порты ставятся в середину стороны или в центр задней стенки'),
    'message.mio_icif.steam_tunnel.problem.casing': v(
        'Steam Tunnel: casing missing (%s)', '蒸汽隧道涡轮机：缺少外壳（%s）', 'スチームトンネル：外装がない（%s）',
        'Паровой туннель: нет корпуса (%s)'),
    'message.mio_icif.steam_tunnel.problem.casing_or_port': v(
        'Steam Tunnel: casing or port missing (%s)', '蒸汽隧道涡轮机：缺少外壳或端口（%s）', 'スチームトンネル：外装かポートがない（%s）',
        'Паровой туннель: нет корпуса или порта (%s)'),
    'jade.mio_icif.steam_tunnel.fluids': v(
        'Steam %1$s / %2$s mB, water %3$s / %4$s mB', '蒸汽 %1$s / %2$s mB，水 %3$s / %4$s mB',
        '蒸気 %1$s / %2$s mB、水 %3$s / %4$s mB', 'Пар %1$s / %2$s mB, вода %3$s / %4$s mB'),
    'jade.mio_icif.steam_tunnel.energy': v(
        'Buffer %1$s / %2$s EU, output %3$s EU/t', '缓存 %1$s / %2$s EU，输出 %3$s EU/t', 'バッファ %1$s / %2$s EU、出力 %3$s EU/t',
        'Буфер %1$s / %2$s EU, выход %3$s EU/t'),
    'config.jade.plugin_mio_icif.steam_tunnel': v('Steam Tunnel', '蒸汽隧道涡轮机', 'スチームトンネル', 'Паровой туннель'),
    'structure.mio_icif.steam_tunnel': v('Steam Tunnel', '蒸汽隧道涡轮机', 'スチームトンネル'),
    'structure.mio_icif.steam_tunnel.casing': v('Edges: Steam Tunnel Casing', '棱：蒸汽隧道外壳', '辺：スチームトンネル外装'),
    'structure.mio_icif.steam_tunnel.face': v(
        'Middle of a side or the back: casing, fluid port or power port', '侧面中间或后端中心：外壳、流体端口或电力端口',
        '側面の中央か後端の中心：外装、流体ポートか電力ポート'),
    'structure.mio_icif.steam_tunnel.fluid_port': v(
        'Fluid port: steam in, water out', '流体端口：输入蒸汽、输出水', '流体ポート：蒸気を入れ、水を出す'),
    'structure.mio_icif.steam_tunnel.power_port': v(
        'Power port: up to 2,048 EU/t as HV packets (512 EU)', '电力端口：最多 2,048 EU/t，HV 数据包（512 EU）',
        '電力ポート：最大 2,048 EU/t、HV パケット（512 EU）'),
    'structure.mio_icif.steam_tunnel.note.inside': v(
        'The 3 blocks inside must be air; the controller faces out of the front', '内部 3 格必须为空气；控制器朝向前端外侧',
        '内部の 3 ブロックは空気。コントローラーは前端の外を向く'),
    'structure.mio_icif.steam_tunnel.note.output': v(
        '1,024 mB of steam make 512 EU, up to 4 times a tick (2,048 EU/t); 10,000 EU buffer',
        '每 1,024 mB 蒸汽产生 512 EU，每 tick 最多 4 次（2,048 EU/t）；缓存 10,000 EU',
        '蒸気 1,024 mB で 512 EU、1 tick に最大 4 回（2,048 EU/t）。バッファ 10,000 EU'),
    'structure.mio_icif.steam_tunnel.note.water': v(
        'Returns 1 mB of water per 100 mB of steam (2,000 mB tank; the rest is lost)', '每 100 mB 蒸汽返还 1 mB 水（水箱 2,000 mB，溢出部分损失）',
        '蒸気 100 mB ごとに水 1 mB を返す（タンク 2,000 mB、あふれた分は失われる）'),
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
