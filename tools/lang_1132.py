#!/usr/bin/env python3
"""0.1.10.32 language values (en_us / zh_cn / ja_jp): the client options of plan C (hit_ripple, suit_gimmicks, armory_fx, fx_lod,
fx_budget) and the 3D preview panels. Idempotent; keeps the line endings of each file. With --check it only reports missing or
different keys. Usage: lang_1132.py <repo root> [--check]
"""
import json
import os
import re
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
F = 'mio_icif.configuration.'

KEYS = {
    F + 'hit_ripple': ('Armour hit ripple and suit boot', '装甲受击波纹与套装启动', '装甲の被弾波紋とスーツ起動'),
    F + 'hit_ripple.tooltip': (
        'A hexagonal ring of light runs over a quantum suit from where a hit was absorbed. When the Armory locks the last piece, the '
        'suit boots: light from the chest core, the visor from its centre, a ring from the core.',
        '量子套装吸收一次攻击时，六角形光环从受击点沿装甲扩散。武装库扣上最后一个部件时套装启动：光从胸口核心扩散，面罩从中央亮起，'
        '核心放出一道光环。',
        '量子スーツが攻撃を吸収すると、被弾点から六角形の光の輪が装甲を広がる。武装庫が最後のパーツを固定すると起動する：胸のコアから光が'
        '広がり、バイザーが中央から灯り、コアから輪が走る。'),
    F + 'suit_gimmicks': ('Suit moving parts', '套装可动机构', 'スーツの可動ギミック'),
    F + 'suit_gimmicks.tooltip': (
        'Quantum suit: the back vents slide open and glow in flight, the shoulder plates slide back while sprinting, the visor lights '
        'from its centre when it comes on.',
        '量子套装：飞行时背部排气口滑开并发光，疾跑时肩甲向后滑动，面罩启用时从中央亮起。',
        '量子スーツ：飛行中は背中の排気口が開いて光り、疾走中は肩装甲が後ろへずれ、バイザーは点灯時に中央から灯る。'),
    F + 'armory_fx': ('Armory gantry and gates', '武装库机库与传送门', '武装庫のガントリーとゲート'),
    F + 'armory_fx.tooltip': (
        'The iris hatch of the Armory, the icons of its suits and its maintenance rings; the transfer gates of summons from far away.',
        '武装库的光圈舱门、所存套装的图标与维护光环；远距离召唤的空间传送门。',
        '武装庫のアイリスハッチ、収納スーツのアイコン、整備リング。遠隔召喚の空間転送ゲート。'),
    F + 'fx_lod': ('Simpler effects far away', '远处简化特效', '遠くのエフェクトを簡略化'),
    F + 'fx_lod.tooltip': ('Fewer segments and no afterimages beyond 24 blocks, only the light beyond 64.',
                           '24 格外减少分段、不画残影，64 格外只画光。', '24 ブロックより遠くは分割を減らし残像を省き、64 ブロックより遠くは光だけにする。'),
    F + 'fx_budget': ('Whole-body effect passes', '全身特效次数上限', '全身エフェクトの描画上限'),
    F + 'fx_budget.tooltip': ('Most whole-body effect passes a frame (hit ripples, hologram grids, nano mesh); the nearest are drawn.',
                              '每帧最多绘制的全身特效次数（受击波纹、全息网格、纳米网甲）；优先绘制最近的。',
                              '1 フレームに描く全身エフェクトの最大数（被弾波紋、ホログラム格子、ナノメッシュ）。近いものから描く。'),
    'gui.mio_icif.preview.title': ('3D preview', '3D 预览', '3D プレビュー'),
    'gui.mio_icif.preview.hint': ('Drag: turn / Wheel: zoom', '拖动：旋转 / 滚轮：缩放', 'ドラッグ：回転 / ホイール：拡大'),
}


def main():
    col = {'en_us': 0, 'zh_cn': 1, 'ja_jp': 2}
    bad = 0
    for lang, i in col.items():
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        if CHECK:
            stray = [k for k, t in KEYS.items() if '%' in re.sub(r'%(?:\d+\$)?([A-Za-z%]|$)', '', t[i])]
            bad += len(stray)
            if stray:
                print(lang, 'stray %:', stray)
            wrong = [k for k, t in KEYS.items() if data.get(k) != t[i]]
            bad += len(wrong)
            print(lang, 'missing or different:', len(wrong), wrong[:5])
            continue
        for key, t in KEYS.items():
            data[key] = t[i]
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(KEYS))
    if CHECK and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
