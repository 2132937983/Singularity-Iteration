#!/usr/bin/env python3
"""0.1.11.89: the resources of the fusion reactor (tokamak) blocks: blockstates, block and item models, loot tables, the
mineable / tool / machine tags and the names (en_us, zh_cn, ja_jp, ru_ru). Idempotent: a second run changes nothing.

Usage: fusion_assets_11589.py <repo root>
"""
import json
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
RES = os.path.join(ROOT, 'src/main/resources')
A = os.path.join(RES, 'assets/mio_icif')
D = os.path.join(RES, 'data')

# name, model kind, en, zh, ja, ru (0.1.11.92)
BLOCKS = [
    ('controller', 'orientable', 'Fusion Reactor Console', '核聚变堆控制台', '核融合炉コントローラー', 'Контроллер термоядерного реактора'),
    ('central_solenoid', 'column', 'Central Solenoid', '中心螺线管', '中心ソレノイド', 'Центральный соленоид'),
    ('toroidal_coil', 'coil', 'Toroidal Field Coil', '环向场线圈', 'トロイダル磁場コイル', 'Катушка тороидального поля'),
    ('poloidal_coil', 'column', 'Poloidal Field Coil', '极向场线圈', 'ポロイダル磁場コイル', 'Катушка полоидального поля'),
    ('vacuum_vessel', 'all', 'Vacuum Vessel Wall', '真空室壁', '真空容器壁', 'Стенка вакуумной камеры'),
    ('blanket', 'all', 'Blanket Module', '包层模块', 'ブランケットモジュール', 'Модуль бланкета'),
    ('divertor', 'all', 'Divertor', '偏滤器', 'ダイバータ', 'Дивертор'),
    ('cryostat', 'all', 'Cryostat Casing', '低温恒温器外壳', 'クライオスタット外装', 'Оболочка криостата'),
    ('nbi_port', 'orientable', 'Neutral Beam Injector', '中性束注入器', '中性粒子ビーム入射装置', 'Инжектор нейтральных пучков'),
    ('ecrh_port', 'orientable', 'Electron Cyclotron Heater', '电子回旋加热器', '電子サイクロトロン加熱装置', 'Электронно-циклотронный нагреватель'),
    ('fuel_port', 'orientable', 'Fuel Injection Port', '加料端口', '燃料注入ポート', 'Порт подачи топлива'),
    ('exhaust_port', 'orientable', 'Exhaust Port', '排气端口', '排気ポート', 'Выхлопной порт'),
    ('cryo_port', 'orientable', 'Cryogenic Port', '低温端口', '極低温ポート', 'Криогенный порт'),
    ('power_port', 'orientable', 'Fusion Power Output Port', '聚变能量输出端口', '核融合電力出力ポート', 'Порт вывода термоядерной энергии'),
    ('mitigation_port', 'orientable', 'Disruption Mitigation Port', '破裂缓解端口', 'ディスラプション緩和ポート', 'Порт подавления срывов'),
]
FRONT = {'controller': 'controller_front'}   # orientable blocks: the front texture (default: the block's own name)


def rid(name):
    return 'fusion/block_fusion_' + name


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
    if os.path.exists(path) and open(path, encoding='utf-8').read().replace('\r\n', '\n') == text:
        return False
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)
    return True


def tex(name):
    return 'mio_icif:block/fusion/' + name


def model(name, kind):
    if kind == 'all':
        return {'parent': 'minecraft:block/cube_all', 'textures': {'all': tex(name)}}
    if kind == 'column':
        return {'parent': 'minecraft:block/cube_column', 'textures': {'end': tex(name + '_end'), 'side': tex(name + '_side')}}
    if kind == 'coil':
        # 0.1.11.94: the toroidal field coil faces outward along its coil's plane: front and back its narrow steel edge,
        # the sides its copper turns, top and bottom the cross-section of its winding pack
        edge, side, end = tex(name + '_edge'), tex(name), tex(name + '_end')
        return {'parent': 'minecraft:block/cube', 'textures': {'particle': side, 'north': edge, 'south': edge, 'east': side,
                                                                'west': side, 'up': end, 'down': end}}
    return {'parent': 'minecraft:block/orientable',
            'textures': {'top': tex('cryostat'), 'side': tex('cryostat'), 'front': tex(FRONT.get(name, name))}}


def blockstate(name, kind):
    m = 'mio_icif:block/' + rid(name)
    if kind not in ('orientable', 'coil'):
        return {'variants': {'': {'model': m}}}
    turns = (('north', 0), ('east', 90), ('south', 180), ('west', 270))
    if name == 'controller':
        # 0.1.11.92: the screen lights up while there is plasma (the block's "active" state)
        out = {}
        for f, y in turns:
            for active in ('false', 'true'):
                model_id = m + ('_active' if active == 'true' else '')
                out['active=%s,facing=%s' % (active, f)] = {'model': model_id, 'y': y} if y else {'model': model_id}
        return {'variants': out}
    return {'variants': {'facing=%s' % f: ({'model': m, 'y': y} if y else {'model': m}) for f, y in turns}}


def loot(name):
    return {'type': 'minecraft:block', 'pools': [{'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                                                  'entries': [{'type': 'minecraft:item', 'name': 'mio_icif:' + rid(name)}], 'rolls': 1.0}],
            'random_sequence': 'mio_icif:blocks/' + rid(name)}


def add_to_tag(path, ids):
    """Appends the ids (optional entries, as the file's other entries) keeping the file's CRLF format."""
    raw = open(path, 'rb').read().decode('utf-8')
    eol = '\r\n' if '\r\n' in raw else '\n'
    data = json.loads(raw)
    have = {(v['id'] if isinstance(v, dict) else v) for v in data['values']}
    added = [i for i in ids if i not in have]
    data['values'] += [{'id': i, 'required': False} for i in added]
    if added:
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        open(path, 'wb').write(text.replace('\n', eol).encode('utf-8'))
    return len(added)


def add_lang(code, column):
    """Inserts each missing name after the name of the block before it (the first after the Steam Tunnel's last key); the file
    keeps its CRLF lines and order."""
    path = os.path.join(A, 'lang', code + '.json')
    raw = open(path, 'rb').read().decode('utf-8')
    eol = '\r\n' if '\r\n' in raw else '\n'
    s = raw.replace('\r\n', '\n')
    added, anchor = 0, '"block.mio_icif.generator.block_steam_tunnel_power_port"'
    for b in BLOCKS:
        key = '"block.mio_icif.%s"' % rid(b[0]).replace('/', '.')
        if key not in s:
            j = s.index('\n', s.index(anchor)) + 1
            s = s[:j] + '  %s: %s,\n' % (key, json.dumps(b[column], ensure_ascii=False)) + s[j:]
            added += 1
        anchor = key
    if added:
        json.loads(s)   # still valid JSON
        open(path, 'wb').write(s.replace('\n', eol).encode('utf-8'))
    return added


def main():
    changed = 0
    for name, kind, *_ in BLOCKS:
        changed += write_json(os.path.join(A, 'blockstates', rid(name) + '.json'), blockstate(name, kind))
        changed += write_json(os.path.join(A, 'models/block', rid(name) + '.json'), model(name, kind))
        if name == 'controller':
            active = model(name, kind)
            active['textures']['front'] = tex('controller_front_active')
            changed += write_json(os.path.join(A, 'models/block', rid(name) + '_active.json'), active)
        changed += write_json(os.path.join(A, 'models/item', rid(name) + '.json'), {'parent': 'mio_icif:block/' + rid(name)})
        changed += write_json(os.path.join(D, 'mio_icif/loot_table/blocks', rid(name) + '.json'), loot(name))
    ids = ['mio_icif:' + rid(b[0]) for b in BLOCKS]
    tags = sum(add_to_tag(os.path.join(D, p), ids) for p in ('minecraft/tags/block/mineable/pickaxe.json',
                                                          'c/tags/block/needs_stone_tool.json', 'c/tags/block/machine.json'))
    names = sum(add_lang(code, col) for code, col in (('en_us', 2), ('zh_cn', 3), ('ja_jp', 4), ('ru_ru', 5)))
    print('files written:', changed, '| tag entries added:', tags, '| names added:', names)


if __name__ == '__main__':
    main()
