#!/usr/bin/env python3
"""0.1.9.1 guide additions (English, Japanese, Chinese):
  * Induction Furnace page: heat hold switch, Thermal Retention Unit (item index entry too)
  * Armory page: Wingman tactics and effects
  * Machines index: the inventory of a picked-up machine comes back when the machine is a
    crafting material (Recovered Parts Case)
Inserts each section once (checked by its heading), before the recipe heading of the page. 0.1.10.0: no marker
comment any more - GuideME parses MDX, where an HTML comment is a syntax error.
Usage: guide_191.py <repo root>
"""
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
G = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual')
LANGS = {'en': '', 'ja': '_ja_jp', 'zh': '_zh_cn'}
THERMAL = '<ItemLink id="mio_icif:upgrade/thermal_retention_upgrade" />'
CASE = '<ItemLink id="mio_icif:normal/item_recovered_parts" />'

INDUCTION = {
    'en': f'''## Heat hold

The heat hold switch (flame button in the side dock) keeps the furnace heating while it has nothing to smelt, up to full heat, and holds it there. The next item smelts at full speed at once. Holding full heat costs 1 EU/t.

A {THERMAL} in an upgrade slot stops the heat loss of an idle furnace and makes the hold free. The heat gauge above the slots shows HOLD and INS for the two.

''',
    'ja': f'''## 保温

保温スイッチ（サイドドックの炎のボタン）をオンにすると、溶かす物がない間も最大熱量まで加熱し、そのまま保つ。次のアイテムはすぐ最高速度で精錬される。最大熱量の保持は 1 EU/t。

アップグレードスロットに {THERMAL} を入れると、待機中の放熱がなくなり保温も無料になる。スロット上の熱量ゲージに HOLD と INS で表示される。

''',
    'zh': f'''## 保温

打开保温开关（侧边栏的火焰按钮）后，没有物品可熔炼时也会加热到满热量并保持，下一个物品立即以最高速度熔炼。保持满热量耗电 1 EU/t。

在升级槽中放入 {THERMAL} 后，空闲时不再散热，保温也不耗电。槽位上方的热量条以 HOLD 与 INS 显示这两种状态。

''',
}

ARMORY = {
    'en': '''## Wingman tactics

- **Targets:** the mob you hit (all Wingmen focus it), a creeper near you (first of all when it fuses), the mob that hit you, mobs that hunt you (ranged attackers first), then the nearest hostile mobs. Wingmen spread over different targets unless you call one.
- **Ranged weapons:** the Wingman picks a firing position with a clear line of fire on the far side of the target, up high, never with you in the line. A mob that comes too close makes it boost away with its thrusters.
- **Melee and blades:** it comes in from the flank or from behind, strikes and boosts away, then makes a new pass. A creeper is knocked away from you.
- **Effects:** a scan laser and a hologram marker show its target, in the colour of its mode; the guns flash, the thrusters burst on a boost and small jets correct every turn. Client option wingman_fx.

''',
    'ja': '''## ウィングマンの戦術

- **目標：** あなたが攻撃した敵（全ウィングマンが集中）、近くのクリーパー（着火中は最優先）、あなたを攻撃した敵、あなたを狙う敵（遠距離攻撃の敵を優先）、それ以外の近い敵の順。呼びかけた敵以外は別々の目標に分散する。
- **遠隔武器：** 目標の向こう側の高い位置で、射線が通りあなたが射線に入らない場所を選ぶ。敵が近づきすぎるとスラスターで退避する。
- **近接・刀剣：** 側面や背後から入り、斬って離脱し、また別の角度から攻める。クリーパーはあなたから遠ざけるように弾き飛ばす。
- **演出：** モード色のスキャンレーザーとホログラムマーカーで目標を示し、射撃の閃光、ブースト時の噴射、旋回ごとの姿勢制御噴射が見える。クライアント設定 wingman_fx。

''',
    'zh': '''## 僚机战术

- **目标：** 你攻击的敌人（所有僚机集火）、你附近的苦力怕（点燃时最优先）、攻击你的敌人、以你为目标的敌人（远程攻击者优先），然后是最近的敌对生物。除你指定的目标外，僚机会分散攻击不同目标。
- **远程武器：** 僚机选择目标另一侧的高处、射线通畅且你不在射线上的位置。敌人靠得太近时用推进器闪避。
- **近战与刀剑：** 从侧面或背后切入，攻击后脱离，再从另一角度进攻。苦力怕会被击飞远离你。
- **特效：** 以模式颜色的扫描激光与全息标记显示目标；开火闪光、冲刺喷射、转向时的姿态喷射清晰可见。客户端选项 wingman_fx。

''',
}

MACHINES = {
    'en': f'''## Picked-up machines as materials

A machine picked up with the wrench keeps its inventory, upgrades included. When such a machine is a crafting material (for example for the next tier), its items come back: one stack as it is, several stacks in a {CASE} that unpacks itself in your inventory. This also works in a Crafter and in AE2 molecular assemblers.
''',
    'ja': f'''## レンチで回収した機械を素材にする

レンチで回収した機械は、アップグレードを含む中身を保持する。その機械をクラフト素材（上位機械など）に使うと、中身が返ってくる：1 スタックならそのまま、複数ならインベントリで自動で開く {CASE} に入る。クラフターや AE2 の分子組立室でも同じ。
''',
    'zh': f'''## 用扳手回收的机器作为材料

用扳手回收的机器会保留内部物品（包括升级）。把这样的机器用作合成材料（例如升级到上一级）时，内部物品会返还：一组直接返还，多组装入放入背包时自动拆开的 {CASE}。在合成器与 AE2 分子装配室中同样有效。
''',
}


def insert_before_recipe(path, section):
    text = open(path, encoding='utf-8').read()
    if section.strip().split('\n')[0] in text:
        return False
    lines = text.split('\n')
    idx = None
    for i, line in enumerate(lines):
        if line.startswith('<RecipesFor'):
            for j in range(i, -1, -1):
                if lines[j].startswith('## '):
                    idx = j
                    break
            break
    if idx is None:
        lines.append('')
        idx = len(lines)
    lines[idx:idx] = section.rstrip('\n').split('\n') + ['']
    open(path, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines))
    return True


def add_item_id(path, item):
    text = open(path, encoding='utf-8').read()
    if item in text.split('---')[1]:
        return
    text = text.replace('item_ids:\n', 'item_ids:\n  - ' + item + '\n', 1)
    open(path, 'w', encoding='utf-8', newline='\n').write(text)


def main():
    n = 0
    for lang, sub in LANGS.items():
        base = os.path.join(G, sub) if sub else G
        p = os.path.join(base, 'machines/producer_block_induction_elc.md')
        if os.path.exists(p):
            n += insert_before_recipe(p, INDUCTION[lang])
            add_item_id(p, 'mio_icif:upgrade/thermal_retention_upgrade')
        p = os.path.join(base, 'machines/producer_block_armory.md')
        if os.path.exists(p):
            n += insert_before_recipe(p, ARMORY[lang])
        p = os.path.join(base, 'machines.md')
        if os.path.exists(p):
            text = open(p, encoding='utf-8').read()
            if MACHINES[lang].strip().split('\n')[0] not in text:
                text = text.replace('<SubPages', MACHINES[lang] + '\n<SubPages', 1)
                if 'item_recovered_parts' not in text.split('---')[1]:
                    text = text.replace('position: 11\n', 'position: 11\nitem_ids:\n  - mio_icif:normal/item_recovered_parts\n', 1)
                open(p, 'w', encoding='utf-8', newline='\n').write(text)
                n += 1
    print('sections added:', n)


if __name__ == '__main__':
    main()
