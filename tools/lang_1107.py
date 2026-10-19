#!/usr/bin/env python3
"""0.1.10.7 language keys (en_us / zh_cn / ja_jp): the /mio_icif perf command and the Performance
config section. Idempotent; keeps the line endings of each file.
Usage: lang_1107.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

KEYS = {
    # %1$s average server tick in ms, %2$s TPS
    'command.mio_icif.perf.header': (
        'SI performance: server tick %1$s ms (%2$s TPS)',
        'SI 性能：服务器 tick %1$s ms（%2$s TPS）',
        'SI パフォーマンス：サーバー tick %1$s ms（%2$s TPS）'),
    # %1$s dimension, %2$s median ms, %3$s 95th percentile ms, %4$s max ms, %5$s ticks measured,
    # %6$s route plans built, %7$s resumed, %8$s stopped by the search budget
    'command.mio_icif.perf.level': (
        '%1$s: energy network %2$s ms median, %3$s ms p95, %4$s ms max (%5$s ticks); route plans built %6$s, resumed %7$s, out of budget %8$s',
        '%1$s：电网 中位 %2$s ms，p95 %3$s ms，最大 %4$s ms（%5$s tick）；路由计划 新建 %6$s，续用 %7$s，预算用尽 %8$s',
        '%1$s：電力網 中央値 %2$s ms、p95 %3$s ms、最大 %4$s ms（%5$s tick）。経路計画 作成 %6$s、再開 %7$s、予算切れ %8$s'),
    # %1$s deliveries, %2$s from path profiles, %3$s visited, %4$s idle passes from the memo, %5$s validations reused
    'command.mio_icif.perf.paths': (
        'Since start: %1$s deliveries (through cables: %2$s from path profiles, %3$s visited); idle passes from memo %4$s; validations reused %5$s',
        '自启动以来：送电 %1$s 次（经电缆：路径档案 %2$s 次，逐格遍历 %3$s 次）；空闲轮次复用 %4$s 次；校验复用 %5$s 次',
        '起動以来：配送 %1$s 件（ケーブル経由：経路プロファイル %2$s 件、逐次走査 %3$s 件）、空転パスの再利用 %4$s 回、検証の再利用 %5$s 回'),
    'command.mio_icif.perf.none': (
        'The SI energy engine is not running.',
        'SI 电网引擎未运行。',
        'SI 電力網エンジンは動いていません。'),
    'mio_icif.configuration.Performance': ('Performance', '性能', 'パフォーマンス'),
    'mio_icif.configuration.automationPacing': (
        'Pace item ejector / pulling upgrades',
        '物品弹出／抽入升级节流',
        'アイテム排出・搬入アップグレードの間引き'),
    'mio_icif.configuration.automationPacing.tooltip': (
        'After a run that moved nothing, wait 4, 8, 16, then 20 ticks (a change of the machine inventory runs them at once). Off: every tick.',
        '一次未移动任何物品后，依次等待 4、8、16、20 tick（机器物品栏变化时立即运行）。关闭：每 tick 运行。',
        '何も動かなかった回の後、4・8・16・20 tick 待つ（機械のインベントリが変わるとすぐ動く）。オフ：毎 tick 動く。'),
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
