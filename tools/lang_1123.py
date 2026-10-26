#!/usr/bin/env python3
"""0.1.10.23 language values (en_us / zh_cn / ja_jp): the unit descriptions of the Behavior Predictor (movement model,
countdowns, ranks; Mk.II path data link), the Threat Sensor (incoming projectile warning) and Smart Trajectory Guidance
(smooth arcs, lock memory, shot assignment). Idempotent; keeps the line endings of each file. With --check it only
reports missing or different keys.
Usage: lang_1123.py <repo root> [--check]
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

M = 'module.mio_icif.'

KEYS = {
    M + 'behavior_predictor.desc': (
        'Shows the path of creatures for the next second, with steps, jumps, water and ice; a creature after you stops '
        'in front of you. Marks an attack before it starts with a countdown (AIM 0.6s) and ranks the three most '
        'dangerous creatures (#1 to #3).',
        '显示生物下一秒的路径（包括台阶、跳跃、水与冰），追击你的生物会停在你面前。在攻击开始前标记攻击并显示倒计时（AIM 0.6s），'
        '并为最危险的三个生物排序（#1～#3）。',
        '生物の次の1秒の経路を表示する（段差・ジャンプ・水・氷を含む。着用者を追う生物は目の前で止まる）。攻撃を開始前に'
        'カウントダウン付きでマークし（AIM 0.6s）、最も危険な 3 体に順位を付ける（#1～#3）。'),
    M + 'behavior_predictor.desc.mk2': (
        'Mk.II: Action: Time Dilation, a field that slows hostile creatures and projectiles around you for 4 s while you '
        'move faster. Data link: the paths of nearby creatures follow the route they really walk (LINK).',
        'Mk.II：行动：时间扭曲，在你周围形成 4 秒的力场，使敌对生物与投射物减速，同时你的移动加快。数据链：附近生物的路径沿其实际行走路线显示（LINK）。',
        'Mk.II：アクション：時間歪曲。周囲の敵対生物と投射物を 4 秒間減速させ、自分は速く動ける。データリンク：近くの生物の経路が'
        '実際に歩くルートに沿って表示される（LINK）。'),
    M + 'threat_sensor.desc': (
        'Gives a yellow warning when a creature looks at you. Gives a red warning when it targets you. Warns of incoming '
        'arrows and fireballs with a countdown and shows which way to step aside.',
        '生物注视你时发出黄色警告。生物锁定你时发出红色警告。对飞来的箭与火球发出带倒计时的警告，并指示闪避方向。',
        '生物が着用者を注視すると黄色警告を出す。標的にすると赤色警告を出す。飛んでくる矢や火の玉をカウントダウン付きで警告し、'
        'よける方向を示す。'),
    M + 'smart_guidance.desc': (
        'Opens a lock frame on the HUD. Hostile creatures in the frame lock in 0.6 s (at most 6); a lock that goes behind '
        'cover is kept for 1 s. Your projectiles fly to the locked targets with vernier thrusters, each to the target '
        'where it helps most; fast shots fly a smooth arc. Energy bolts aim at the lead point before they leave the '
        'barrel. 300 EU for each guided projectile.',
        '在HUD中展开锁定框。框内的敌对生物在 0.6 秒内被锁定（最多 6 个）；躲到遮挡物后的锁定保持 1 秒。你的弹射物会借助微型推进器飞向'
        '被锁定的目标，每发飞向最有效的目标；高速弹沿平滑弧线飞行。能量弹在出膛前即瞄准提前量位置。每个被诱导的弹射物消耗 300 EU。',
        'HUDにロックフレームを展開します。フレーム内の敵対モブは0.6秒でロックされます（最大6体）。遮蔽物の陰に入ったロックは 1 秒'
        '保持されます。放った投射物はバーニア推進器でロック目標へ誘導され、1 発ごとに最も効果的な目標へ向かいます。高速弾はなめらかな'
        '弧を描きます。エネルギー弾は発射前から予測位置へ照準済みです。誘導1発につき 300 EU。'),
    M + 'smart_guidance.desc.mk2': (
        'Mk.II: up to 10 locks; guided shots turn up to 20 degrees per tick; locks behind cover are kept for 2 s '
        '(Mk.I: 6 locks, 14 degrees, 1 s).',
        'Mk.II：最多 10 个锁定；诱导弹每 tick 最多转向 20 度；遮挡物后的锁定保持 2 秒（Mk.I：6 个锁定，14 度，1 秒）。',
        'Mk.II：ロックは最大 10。誘導弾は 1 tick に最大 20 度旋回する。遮蔽物の陰のロックは 2 秒保持（Mk.I：6 ロック、14 度、1 秒）。'),
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
