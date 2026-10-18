#!/usr/bin/env python3
"""0.1.10.4 language keys (en_us / zh_cn / ja_jp): the action keys in the unit tooltips follow the
bound keys (positional arguments; the code passes the key names last), the compute core state in
the Entanglement Matrix. Idempotent; keeps the line endings of each file.
Usage: lang_1104.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
M = 'module.mio_icif.tooltip.'
E = 'gui.mio_icif.entangle.core.'

KEYS = {
    # %1$s action name, %2$s coherence, %3$s cooldown, %4$s the two keys as bound now
    M + 'action.single': ('Action (%4$s): %1$s, %2$s coherence, cooldown %3$s s',
                          '行动（%4$s）：%1$s，%2$s 相干度，冷却 %3$s 秒',
                          'アクション（%4$s）：%1$s、コヒーレンス %2$s、クールダウン %3$s 秒'),
    # %1$s name, %2$s coherence to start, %3$s per second, %4$s cooldown, %5$s keys
    M + 'action.sustained': ('Toggle action (%5$s): %1$s, %2$s coherence to start, %3$s per second, cooldown %4$s s after',
                             '开关行动（%5$s）：%1$s，启动 %2$s 相干度，每秒 %3$s，结束后冷却 %4$s 秒',
                             'トグルアクション（%5$s）：%1$s、開始にコヒーレンス %2$s、毎秒 %3$s、終了後クールダウン %4$s 秒'),
    E + 'active': ('Core ON', '演算核心 运行', '演算コア 稼働'),
    E + 'off': ('Core switched off', '演算核心 已关闭', '演算コア オフ'),
    E + 'no_power': ('Core: no suit EU', '演算核心 缺电', '演算コア 電力不足'),
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
