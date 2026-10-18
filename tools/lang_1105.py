#!/usr/bin/env python3
"""0.1.10.5 language keys (en_us / zh_cn / ja_jp): Wingman fault and copy messages, the Radiation
Containment Box tooltip. Idempotent; keeps the line endings of each file.
Usage: lang_1105.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

KEYS = {
    'message.mio_icif.wingman.fault': (
        'A Wingman had an error and returned its suit to the Armory (see the server log).',
        '僚机发生错误，已将套装归还武装库（详见服务器日志）。',
        'ウィングマンでエラーが起きたため、スーツを武装庫に戻しました（サーバーログを参照）。'),
    'message.mio_icif.wingman.copy_removed': (
        'A copy of a Wingman whose suit had already returned was removed without its items.',
        '一个套装已归还的僚机副本已被移除，不带任何物品。',
        'スーツがすでに帰還したウィングマンの複製を、アイテムなしで消去しました。'),
    # %1$s used slots, %2$s all slots
    'tooltip.mio_icif.containment_box.slots': (
        'Slots used: %1$s / %2$s',
        '已用格数：%1$s / %2$s',
        '使用中のスロット：%1$s / %2$s'),
    'tooltip.mio_icif.containment_box.info': (
        'Use to open. Radioactive items inside do not irradiate you.',
        '使用即可打开。放在里面的放射性物品不会辐射你。',
        '使うと開く。中の放射性アイテムは被曝させない。'),
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
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(KEYS))


if __name__ == '__main__':
    main()
