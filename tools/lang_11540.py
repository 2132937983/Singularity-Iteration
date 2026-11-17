#!/usr/bin/env python3
"""0.1.11.40 language values (en_us / zh_cn / ja_jp / ru_ru), Wingman audit W1: the texts of the squad HUD and the command
wheel that were English in the code (where, order and mode tags), the order names, and one word per thing (D-2): zh
"武装库" (not "军械库", except the material interface of the blueprint builder), ja "僚機" and "武装庫" (not "ウィングマン",
"アーモリー"), ru "арсенал" (not "оружейная").
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys, removed keys that are still present and old words that are still used.
Usage: lang_11540.py <repo root> [--check]
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
    'gui.mio_icif.command.where.unloaded': v('UNLOADED', '未加载', '未読み込み', 'НЕ ЗАГРУЖЕН'),
    'gui.mio_icif.command.where.other_dim': v('OTHER DIM', '其他维度', '別ディメンション', 'ДРУГОЕ ИЗМ.'),
    'gui.mio_icif.command.hud.datalink_on': v('DATALINK', '数据链', 'データリンク', 'ЛИНИЯ ДАННЫХ'),
    'gui.mio_icif.command.hud.datalink_off': v('DL OFF', '数据链 关', 'DL オフ', 'ЛД ВЫКЛ'),
    'gui.mio_icif.command.hud.recall_cd': v('QR %ss', '回收 %s秒', '回収 %s秒', 'КО %sс'),
    'gui.mio_icif.wingman.mode_short.offensive': v('OFF', '进攻', '攻勢', 'АТК'),
    'gui.mio_icif.wingman.mode_short.dispersal': v('DSP', '分散', '分散', 'РАС'),
    'gui.mio_icif.wingman.mode_short.escort': v('ESC', '护航', '護衛', 'ЭСК'),
    'gui.mio_icif.wingman.mode_short.fulton': v('FUL', '富尔顿', 'フルトン', 'ФУЛ'),
    'gui.mio_icif.wingman.order.none': v('FORM', '编队', '編隊', 'СТРОЙ'),
    'gui.mio_icif.wingman.order.move': v('MOVE', '移动', '移動', 'ДВИЖ.'),
    'gui.mio_icif.wingman.order.hold': v('HOLD', '待机', '待機', 'ОЖИД.'),
    'gui.mio_icif.wingman.order.suppress': v('SUPPRESS', '压制', '制圧', 'ПОДАВЛ.'),
    'gui.mio_icif.wingman.order.focus': v('FOCUS', '集火', '集中', 'ФОКУС'),
    'gui.mio_icif.wingman.order.rtb': v('RTB', '返航', '帰還', 'ВОЗВР.'),
    'gui.mio_icif.wingman.order_short.none': v('', '', '', ''),
    'gui.mio_icif.wingman.order_short.move': v('MOVE', '移动', '移動', 'ДВИЖ'),
    'gui.mio_icif.wingman.order_short.hold': v('HOLD', '待机', '待機', 'ОЖИД'),
    'gui.mio_icif.wingman.order_short.suppress': v('SUPP', '压制', '制圧', 'ПОДВ'),
    'gui.mio_icif.wingman.order_short.focus': v('FOCS', '集火', '集中', 'ФОКС'),
    'gui.mio_icif.holo_table.hint': v('Left: MOVE · Right: SUPPRESS · Left on a creature: FOCUS · H: HOLD · F: FORM UP · 1-6: Wingman · 0: all',
        '左键：移动 · 右键：区域压制 · 左键点生物：集中攻击 · H：待机 · F：归队 · 1-6：僚机 · 0：全部',
        '左：移動・右：区域制圧・生物を左：集中攻撃・H：待機・F：編隊復帰・1-6：僚機・0：全機',
        'ЛКМ: движение · ПКМ: подавление зоны · ЛКМ по существу: сосредоточенный огонь · H: ожидание · F: сбор · 1-6: ведомый · 0: все'),
}

REMOVE = []

# (language, old, new, keys containing one of these stay as they are)
WORDS = [
    ('zh_cn', '军械库', '武装库', ('material_interface', 'builder', 'blueprint')),
    ('ja_jp', 'ウィングマン', '僚機', ()),
    ('ja_jp', 'アーモリー', '武装庫', ()),
    ('ru_ru', 'в оружейную', 'в арсенал', ()),
    ('ru_ru', 'оружейной', 'арсенала', ()),
    ('ru_ru', 'Оружейная', 'Арсенал', ()),
    ('ru_ru', 'оружейная', 'арсенал', ()),
    ('ru_ru', 'Ирисовый люк арсенала, значки её', 'Ирисовый люк арсенала, значки его', ()),   # арсенал is masculine
]


def words(lang, data):
    """Applies WORDS to the values of {@code data}; returns the keys that still use an old word."""
    left = []
    for key, text in list(data.items()):
        if not isinstance(text, str):
            continue
        new = text
        for l, old, rep, keep in WORDS:
            if l == lang and old in new and not any(k in key for k in keep):
                new = new.replace(old, rep)
        if new != text:
            left.append(key)
            data[key] = new
    return left

def main():
    bad = 0
    for lang in LANGS:
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        mine = {k: t[lang] for k, t in KEYS.items() if lang in t}
        changed = words(lang, data)
        if CHECK:
            bad += len(changed)
            if changed:
                print(lang, 'old words still in:', len(changed), changed[:5])
            stray = [k for k, t in mine.items() if '%' in re.sub(r'%(?:\d+\$)?([A-Za-z%]|$)', '', t)]
            bad += len(stray)
            if stray:
                print(lang, 'stray %:', stray)
            wrong = [k for k, t in mine.items() if data.get(k) != t]
            left = [k for k in REMOVE if k in data]
            bad += len(wrong) + len(left)
            print(lang, 'missing or different:', len(wrong), wrong[:5], 'still present:', len(left), left)
            continue
        removed = sum(1 for k in REMOVE if data.pop(k, None) is not None)
        for key, t in mine.items():
            data[key] = t
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(mine), 'removed:', removed, 'words changed in:', len(changed))
    if CHECK and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
