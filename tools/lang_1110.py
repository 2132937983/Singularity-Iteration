#!/usr/bin/env python3
"""0.1.10.10 language keys (en_us / zh_cn / ja_jp): the load line of /mio_icif perf. Idempotent;
keeps the line endings of each file.
Usage: lang_1110.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

KEYS = {
    # per tick over the last 100 ticks: %1$s setChanged, %2$s save marks, %3$s syncs sent, %4$s sync requests,
    # %5$s lamp checks, %6$s neighbour capability lookups, %7$s of them cached, %8$s machine ticks asleep;
    # %9$s saturated windows since the start
    'command.mio_icif.perf.load': (
        'Per tick (last 100 ticks): setChanged %1$s, save marks %2$s, client syncs %3$s of %4$s requests, lamp checks %5$s, neighbour capability lookups %6$s (%7$s cached), machine ticks asleep %8$s; saturated windows since start %9$s',
        '每 tick（最近 100 tick）：setChanged %1$s，保存标记 %2$s，客户端同步 %3$s（请求 %4$s），状态灯检查 %5$s，邻居能力查询 %6$s（缓存 %7$s），休眠的机器 tick %8$s；自启动以来饱和窗口 %9$s',
        '1 tick あたり（直近 100 tick）：setChanged %1$s、保存印 %2$s、クライアント同期 %3$s（要求 %4$s）、ランプ判定 %5$s、隣接能力の取得 %6$s（キャッシュ %7$s）、休眠した機械の tick %8$s。起動以来の飽和ウィンドウ %9$s'),
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
