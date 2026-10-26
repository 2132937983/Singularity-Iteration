#!/usr/bin/env python3
"""0.1.10.23 guide pages (en / ja / zh): the Behavior Predictor page gets the movement model, the countdowns and the
ranks (and the Mk.II data link), the Threat Sensor page the incoming projectile warning. Inserts before the given
heading; idempotent (a page that already has the new heading is left alone). Keeps LF line endings.
Usage: guide_1123.py <repo root> [--check]
"""
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
M = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual')
DIRS = {'en': '', 'ja': '_ja_jp', 'zh': '_zh_cn'}

# (page, language) -> list of (heading to insert before, new heading, body)
EDITS = {
    ('suit/units/behavior_predictor.md', 'en'): [
        ('## Mk.II', '## Paths, countdowns and ranks', '''- The path follows the movement rules of the game: a creature slides farther on ice, slows down and sinks slowly in water, steps up slabs, jumps onto a one-block ledge (before, the path stopped there) and falls off an edge.
- A creature that was just hit slides back and then walks on. A creature that hunts you stops in front of you.
- The attack mark shows the time to the attack: AIM with the bow draw or the crossbow load, FIRE with the ghast charge, DET with the creeper swell, ATK with the swing interval that the unit learns from the creature.
- #1 to #3 beside the tags rank the most dangerous hostile creatures: how hostile it is, the damage of one hit or its explosion, and how soon it can attack.
- Creatures farther than 12 blocks that do not threaten you get a new path every 4 ticks instead of every 2.'''),
        ('## Recipe', '## Mk.II data link', '''The server sends the Mk.II predictor the next steps of the route of the 8 most dangerous creatures within 24 blocks. Their paths follow that route round corners. They are drawn in violet with a LINK mark.'''),
    ],
    ('suit/units/behavior_predictor.md', 'ja'): [
        ('## Mk.II', '## 経路・カウントダウン・順位', '''- 経路はゲームの移動ルールに従う：氷の上ではより遠くまで滑り、水中では遅くなってゆっくり沈み、ハーフブロックは登り、1 ブロックの段差には跳び乗り（以前はそこで経路が止まった）、端から落ちる。
- 攻撃を受けた直後の生物は後ろへ滑り、その後また歩き出す。着用者を追う生物は目の前で止まる。
- 攻撃予告には攻撃までの時間が付く：AIM は弓の引き・クロスボウの装填、FIRE はガストの溜め、DET はクリーパーの膨張、ATK はその生物から学んだ攻撃の間隔。
- タグの横の #1～#3 は最も危険な敵対生物の順位：敵意の強さ、1 回の攻撃や爆発の大きさ、攻撃できるまでの早さで決まる。
- 12 ブロックより遠く、着用者を脅かしていない生物の経路は 2 tick ごとではなく 4 tick ごとに計算し直す。'''),
        ('## レシピ', '## Mk.II データリンク', '''Mk.II の予測器には、24 ブロック以内で最も危険な 8 体について、歩くルートの次の数歩をサーバーが送る。その経路は曲がり角に沿って表示され、紫色で LINK の印が付く。'''),
    ],
    ('suit/units/behavior_predictor.md', 'zh'): [
        ('## Mk.II', '## 路径、倒计时与排序', '''- 路径遵循游戏的移动规则：在冰上滑得更远，在水中变慢并缓慢下沉，走上半砖，跳上一格高的台阶（此前路径在此处中断），从边缘掉落。
- 刚被击中的生物先向后滑动，随后继续行走。追击你的生物会停在你面前。
- 攻击预告显示距攻击的时间：AIM 为拉弓或弩装填，FIRE 为恶魂蓄力，DET 为苦力怕膨胀，ATK 为本单元从该生物学到的攻击间隔。
- 标签旁的 #1～#3 为最危险的敌对生物排序：依据敌意、单次攻击或爆炸的伤害，以及多快能发动攻击。
- 距离超过 12 格且不威胁你的生物，路径每 4 tick 重新计算一次（而不是每 2 tick）。'''),
        ('## 配方', '## Mk.II 数据链', '''服务器向 Mk.II 预测器发送 24 格内最危险的 8 个生物接下来几步的行走路线。它们的路径沿该路线转弯，以紫色显示并带有 LINK 标记。'''),
    ],
    ('suit/units/threat_sensor.md', 'en'): [
        ('## Recipe', '## Incoming projectiles', '''- The sensor also watches the arrows, tridents, thrown items, llama spit and fireballs within 48 blocks that you did not fire. It follows each one for the next 2 seconds with its own gravity and drag.
- Each one that will pass within 1.5 blocks of you gets an orange marker on the ring with the seconds to its closest pass.
- One that will hit you turns red and beeps once. An arrow beside the aim point shows which way to step: across its path, away from the side it comes at.
- The banner shows INCOMING and the number of hits.
- Homing shulker bullets and rockets are not shown.'''),
    ],
    ('suit/units/threat_sensor.md', 'ja'): [
        ('## レシピ', '## 飛来弾の警告', '''- 48 ブロック以内で、着用者が撃ったもの以外の矢・トライデント・投げたアイテム・ラマの唾・火の玉も監視する。それぞれの重力と空気抵抗で次の 2 秒を予測する。
- 1.5 ブロック以内を通るものには、リングにオレンジの印と最接近までの秒数が付く。
- 当たるものは赤くなり、1 回警告音が鳴る。照準の横の矢印がよける方向を示す：弾の進路を横切る向きで、弾が来る側の反対。
- 上部のバナーに INCOMING と命中する数が出る。
- 追尾するシュルカーの弾とロケット花火は表示しない。'''),
    ],
    ('suit/units/threat_sensor.md', 'zh'): [
        ('## 配方', '## 来袭弹警告', '''- 传感器还会监视 48 格内非你发射的箭、三叉戟、投掷物、羊驼唾沫和火球，按各自的重力与阻力预测接下来 2 秒的飞行。
- 将从你 1.5 格以内经过的弹，在外环上显示橙色标记与到最近点的秒数。
- 将命中你的弹变为红色并响一次警报。准星旁的箭头指示闪避方向：横穿其路径，远离其来的一侧。
- 顶部横幅显示 INCOMING 与将命中的数量。
- 追踪的潜影弹与烟花火箭不显示。'''),
    ],
}


# (page, language) -> (old sentence, new sentence): the Mk.II paths are no longer the same as Mk.I (data link)
REPLACE = {
    ('suit/units/behavior_predictor.md', 'en'): (
        'The Mk.II predictor adds the **Time Dilation** action (see Quantum actions); the paths and attack marks stay the same.',
        'The Mk.II predictor adds the **Time Dilation** action (see Quantum actions) and the data link below.'),
    ('suit/units/behavior_predictor.md', 'ja'): (
        'Mk.II ではアクション **時間歪曲** が加わる（「量子アクション」を参照）。進路と攻撃予告の表示は同じ。',
        'Mk.II ではアクション **時間歪曲**（「量子アクション」を参照）と、下のデータリンクが加わる。'),
    ('suit/units/behavior_predictor.md', 'zh'): (
        'Mk.II 增加行动 **时间扭曲**（见“量子行动”）；路径与攻击预告的显示不变。',
        'Mk.II 增加行动 **时间扭曲**（见“量子行动”）与下面的数据链。'),
}


def main():
    bad = 0
    for (page, lang), edits in EDITS.items():
        path = os.path.join(M, DIRS[lang], page) if DIRS[lang] else os.path.join(M, page)
        text = open(path, 'rb').read().decode('utf-8')
        changed = False
        old_new = REPLACE.get((page, lang))
        if old_new and old_new[0] in text:
            if CHECK:
                print('old sentence still in', lang, page)
                bad += 1
            else:
                text = text.replace(old_new[0], old_new[1])
                changed = True
        for before, heading, body in edits:
            if heading + '\n' in text:
                continue
            if CHECK:
                print('missing', heading, 'in', lang, page)
                bad += 1
                continue
            at = text.find('\n' + before + '\n')
            assert at >= 0, (page, lang, before)
            text = text[:at + 1] + heading + '\n\n' + body + '\n\n' + text[at + 1:]
            changed = True
        if changed:
            open(path, 'wb').write(text.encode('utf-8'))
            print('updated', lang, page)
    if CHECK:
        print('missing sections:', bad)
        if bad:
            sys.exit(1)


if __name__ == '__main__':
    main()
