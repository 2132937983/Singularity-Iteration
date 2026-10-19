#!/usr/bin/env python3
"""0.1.10.8 language keys (en_us / zh_cn / ja_jp): the reuse line of /mio_icif perf, the machine
sleep config option, and the pacing option (now also for fluid upgrades). Idempotent; keeps the line
endings of each file.
Usage: lang_1108.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

KEYS = {
    # %1$s native windows from the route plan cache, %2$s machine ticks skipped by idle sleep
    'command.mio_icif.perf.reuse': (
        'Route plans from cache %1$s; machine ticks skipped by idle sleep %2$s',
        '路由计划缓存复用 %1$s 次；空闲休眠跳过的机器 tick %2$s 次',
        '経路計画のキャッシュ再利用 %1$s 回、休眠で省いた機械の tick %2$s 回'),
    'mio_icif.configuration.automationPacing': (
        'Pace ejector / pulling upgrades',
        '弹出／抽入升级节流',
        '排出・搬入アップグレードの間引き'),
    'mio_icif.configuration.automationPacing.tooltip': (
        'Item and fluid upgrades: after a run that moved nothing, wait 4, 8, 16, then 20 ticks (a change of the machine inventory or tanks runs them at once). Off: every tick.',
        '物品与流体升级：一次未移动任何东西后，依次等待 4、8、16、20 tick（机器物品栏或储罐变化时立即运行）。关闭：每 tick 运行。',
        'アイテム・流体アップグレード：何も動かなかった回の後、4・8・16・20 tick 待つ（機械のインベントリかタンクが変わるとすぐ動く）。オフ：毎 tick 動く。'),
    'mio_icif.configuration.machineSleep': (
        'Idle sleep of basic machines',
        '基础机器空闲休眠',
        '基本機械の休眠'),
    'mio_icif.configuration.machineSleep.tooltip': (
        'A macerator, electric furnace, extractor, compressor, recycler or metal former that stood idle for 1 second stops checking its recipe until its inventory, settings or stored energy change (at the latest 1 second later). Off: every tick.',
        '粉碎机、电炉、提取机、压缩机、回收机、金属成型机空闲 1 秒后，在物品栏、设置或储电变化之前（最迟 1 秒后）不再检查配方。关闭：每 tick 检查。',
        '粉砕機・電気炉・抽出機・圧縮機・リサイクラー・金属成形機は、1 秒間止まっていると、インベントリ・設定・蓄電量が変わるまで（遅くとも 1 秒後まで）レシピを調べない。オフ：毎 tick 調べる。'),
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
