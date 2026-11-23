#!/usr/bin/env python3
"""0.1.11.157: the manual pages of the TESU (太级储电罐): controller, input port, output port, in English, Chinese and Japanese.
Running it again writes the same files."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAN = ROOT / 'src/main/resources/assets/mio_icif/guides/mio_icif/manual'

STRUCTURE = {
    'en': ('| Layer | Blocks |\n|---|---|\n'
           '| 1 | Base: a 7×7 layer of TESU Casing |\n'
           '| 2 | The controller in the middle of the front, 8 port slots (2 on each side), the rest casing |\n'
           '| 3–11 | Shell: casing walls with a Reinforced Glass window three blocks wide in the middle of each side; inside, a 3-5-5-5-3 disc of TESU Energy Cells (21 a layer, 189 in all) and casing in the four inner corners |\n'
           '| 12 | Lid: a 7×7 layer of casing |\n'
           '| 13 | Top: a 7×7 layer of casing |\n'),
    'zh': ('| 层 | 方块 |\n|---|---|\n'
           '| 1 | 底层：7×7 的 TESU 外壳 |\n'
           '| 2 | 正面正中是控制器，四面各 2 个端口位共 8 个，其余是外壳 |\n'
           '| 3–11 | 罐身：外壳罐壁，四面中间各有三列宽的防爆玻璃观察窗；罐内中间 3-5-5-5-3 的 21 格是 TESU 储能单元（共 189 块），四个内角是外壳 |\n'
           '| 12 | 罐盖：7×7 外壳 |\n'
           '| 13 | 顶层：7×7 外壳 |\n'),
    'ja': ('| 層 | ブロック |\n|---|---|\n'
           '| 1 | 底面：7×7 の TESU外装 |\n'
           '| 2 | 正面中央にコントローラー、各面 2 か所ずつ計 8 か所のポート位置、残りは外装 |\n'
           '| 3–11 | 胴体：外装の壁、各面中央に幅 3 列の防爆ガラスの窓。内部中央の 3-5-5-5-3 の 21 マスは TESU蓄電セル（計 189 個）、四隅の内側は外装 |\n'
           '| 12 | 蓋：7×7 の外装 |\n'
           '| 13 | 頂部：7×7 の外装 |\n'),
}

PAGES = {
    'block_tesu_controller': {
        'position': 17,
        'en': ('TESU Controller', [
            '## Function', '',
            'The TESU Controller is the core of the TESU (Tera Energy Storage Unit), a 7×7×13 storage box. It stores the energy;',
            'the input and output ports in the base ring move it in and out. It faces out of the front of the structure, and the structure',
            'turns with it. Its own faces take and give nothing: connect cables to the ports.', '',
            '## Power data', '', '| Item | Value |', '|---|---|', '| Energy storage | 227,000,000,000 EU |', '',
            '## Procedure', '',
            '1. Build the structure (see below) and place the controller in the middle of the front of the base ring.',
            '2. Put TESU Input Ports, TESU Output Ports and TESU Wireless Output Ports in the port slots; close the others with TESU Casing.',
            '3. Open the GUI: it shows the stored energy, the ports and their output, or the first wrong block and what belongs there.', '',
            '## Notes', '',
            '- The controller checks the structure every second.',
            '- The GUI has a slot to charge an item and a slot to take EU from a battery.', '',
            '## Structure', '']),
        'zh': ('TESU 控制器', [
            '## 功能', '',
            'TESU 控制器是 TESU（太级储电罐）的核心，这是一座 7×7×13 的长方体储电设备。控制器存电，底圈上的输入端口和输出端口负责进出电。',
            '控制器朝向结构外侧，整个结构随它的朝向转动。控制器自己的面不收发电，电线要接在端口上。', '',
            '## 电力数据', '', '| 项目 | 数值 |', '|---|---|', '| 储电 | 227,000,000,000 EU |', '',
            '## 使用步骤', '',
            '1. 按下面的结构搭建，控制器放在底圈正面正中。',
            '2. 在端口位上放 TESU 输入端口、TESU 输出端口和 TESU 无线输出端口，不用的端口位用 TESU 外壳封住。',
            '3. 打开界面：显示储电、端口数量和输出合计；结构没完成时显示第一个不对的坐标和那里应该放什么。', '',
            '## 注意', '',
            '- 控制器每秒检查一次结构。',
            '- 界面有一个给物品充电的槽和一个从电池取电的槽。', '',
            '## 结构', '']),
        'ja': ('TESUコントローラー', [
            '## 機能', '',
            'TESUコントローラーは TESU（テラ級蓄電タンク）の中核で、7×7×13 の直方体の蓄電設備です。コントローラーが電力を蓄え、',
            '底部リングの入力ポートと出力ポートが出し入れします。構造の外側を向き、構造はその向きに合わせて回ります。',
            'コントローラー自身の面は電力を受け渡ししません。ケーブルはポートにつないでください。', '',
            '## 電力データ', '', '| 項目 | 値 |', '|---|---|', '| 蓄電量 | 227,000,000,000 EU |', '',
            '## 手順', '',
            '1. 下の構造を組み、コントローラーを底部リングの正面中央に置きます。',
            '2. ポート位置に TESU入力ポート、TESU出力ポート、TESU無線出力ポートを置き、使わない位置は TESU外装でふさぎます。',
            '3. GUI を開くと、蓄電量、ポート数と出力合計、または最初の誤ったブロックの座標と必要なブロックが表示されます。', '',
            '## 注意', '',
            '- コントローラーは毎秒構造を確認します。',
            '- GUI にはアイテムを充電するスロットと電池から EU を取り出すスロットがあります。', '',
            '## 構造', '']),
    },
    'block_tesu_input': {
        'position': 18,
        'en': ('TESU Input Port', [
            '## Function', '',
            'The TESU Input Port takes energy of any voltage (up to MAX) into a buffer of two MAX packets and hands up to one MAX packet',
            'a tick to its controller. It works in a port slot of the TESU base ring.', '',
            '## Power data', '', '| Item | Value |', '|---|---|', '| Input voltage | any, up to MAX (2,147,483,647 EU a packet) |',
            '| To the controller | up to 2,147,483,647 EU/t |', '']),
        'zh': ('TESU 输入端口', [
            '## 功能', '',
            'TESU 输入端口接受任何电压（最高 MAX）的电，存进两个 MAX 包大小的缓存，每 tick 最多把一个 MAX 包转交给控制器。',
            '它要装在 TESU 底圈的端口位上才工作。', '',
            '## 电力数据', '', '| 项目 | 数值 |', '|---|---|', '| 输入电压 | 任何，最高 MAX（单包 2,147,483,647 EU） |',
            '| 转交给控制器 | 最多 2,147,483,647 EU/t |', '']),
        'ja': ('TESU入力ポート', [
            '## 機能', '',
            'TESU入力ポートはあらゆる電圧（最大 MAX）の電力を MAX パケット 2 個分のバッファーに受け取り、1 tick に最大 MAX パケット 1 個を',
            'コントローラーへ渡します。TESU 底部リングのポート位置に置くと働きます。', '',
            '## 電力データ', '', '| 項目 | 値 |', '|---|---|', '| 入力電圧 | すべて、最大 MAX（1 パケット 2,147,483,647 EU） |',
            '| コントローラーへ | 最大 2,147,483,647 EU/t |', '']),
    },
    'block_tesu_output': {
        'position': 19,
        'en': ('TESU Output Port', [
            '## Function', '',
            'The TESU Output Port sends the controller\'s energy at the voltage set on it, from IV to MAX. Right-click steps the voltage',
            'up, sneak-right-click down; the port shows the voltage and the packet size. It keeps two packets of its voltage, refilled',
            'from the controller every tick, and sends one packet a tick. It works in a port slot of the TESU base ring.', '',
            '## Power data', '', '| Voltage | EU a packet (= EU/t) |', '|---|---|',
            '| IV | 8,192 |', '| LuV | 32,768 |', '| ZPMV | 131,072 |', '| UV | 524,288 |', '| UHV | 2,097,152 |', '| UEV | 8,388,608 |',
            '| UIV | 33,554,432 |', '| UXV | 134,217,728 |', '| OpV | 536,870,912 |', '| MAX | 2,147,483,647 |', '']),
        'zh': ('TESU 输出端口', [
            '## 功能', '',
            'TESU 输出端口按设定的电压（IV 到 MAX）送出控制器里的电。右键提高电压，潜行右键降低，并提示当前电压和单包大小。',
            '它保留两个所选电压的包，每 tick 从控制器补满，每 tick 送出一包。它要装在 TESU 底圈的端口位上才工作。', '',
            '## 电力数据', '', '| 电压 | 单包 EU（= EU/t） |', '|---|---|',
            '| IV | 8,192 |', '| LuV | 32,768 |', '| ZPMV | 131,072 |', '| UV | 524,288 |', '| UHV | 2,097,152 |', '| UEV | 8,388,608 |',
            '| UIV | 33,554,432 |', '| UXV | 134,217,728 |', '| OpV | 536,870,912 |', '| MAX | 2,147,483,647 |', '']),
        'ja': ('TESU出力ポート', [
            '## 機能', '',
            'TESU出力ポートは設定した電圧（IV～MAX）でコントローラーの電力を送ります。右クリックで電圧を上げ、スニーク右クリックで',
            '下げ、現在の電圧とパケットの大きさを表示します。選んだ電圧のパケットを 2 個保持し、毎 tick コントローラーから補充して',
            '1 tick に 1 パケット送ります。TESU 底部リングのポート位置に置くと働きます。', '',
            '## 電力データ', '', '| 電圧 | 1 パケットの EU（= EU/t） |', '|---|---|',
            '| IV | 8,192 |', '| LuV | 32,768 |', '| ZPMV | 131,072 |', '| UV | 524,288 |', '| UHV | 2,097,152 |', '| UEV | 8,388,608 |',
            '| UIV | 33,554,432 |', '| UXV | 134,217,728 |', '| OpV | 536,870,912 |', '| MAX | 2,147,483,647 |', '']),
    },
}
PAGES.update({
    'block_tesu_wireless_port': {
        'position': 20,
        'en': ('TESU Wireless Output Port', [
            '## Function', '',
            'In a port slot of a formed TESU, the wireless output port is the centre of a wireless field: a cube of ±16 blocks round it.',
            'Every machine inside it with a Wireless Upgrade in an upgrade slot is filled every tick from the TESU, whatever its voltage',
            '(no cable, no overvoltage). The port holds no energy itself. TESU Relays carry the field further.', '',
            '## Use', '',
            '1. Put the port in a port slot of the TESU base ring.',
            '2. Put a Wireless Upgrade in each machine that should be charged (any machine with upgrade slots that uses energy).',
            '3. Right-click the port to see whether it is on a formed TESU.', '',
            '## Notes', '',
            '- A machine inside several fields takes from the first that has energy.',
            '- The Wireless Upgrade fits machines that take a transformer or energy storage upgrade.', '']),
        'zh': ('TESU 无线输出端口', [
            '## 功能', '',
            '装在已成型 TESU 的端口位上时，无线输出端口是一片无线范围的中心：以它为中心 ±16 格的立方体。',
            '范围内升级槽里装了无线升级的机器，每 tick 都由 TESU 充满，无视电压等级（不用电线，也不会过压）。端口本身不存电。用 TESU 中继器可以把范围接得更远。', '',
            '## 用法', '',
            '1. 把端口装在 TESU 底圈的端口位上。',
            '2. 给要充电的机器装上无线升级（有升级槽、会用电的机器都可以）。',
            '3. 右键端口可以查看它是否接入了成型的 TESU。', '',
            '## 注意', '',
            '- 机器同时在几片范围里时，从第一个有电的取电。',
            '- 能装变压器升级或储能升级的机器就能装无线升级。', '']),
        'ja': ('TESU無線出力ポート', [
            '## 機能', '',
            '完成した TESU のポート位置に置くと、無線出力ポートは周囲 ±16 マスの立方体の無線範囲の中心になります。',
            '範囲内でアップグレードスロットに無線アップグレードを入れた機械は、電圧に関係なく毎 tick TESU から満充電されます（ケーブル不要、過電圧なし）。',
            'ポート自身は蓄電しません。TESU中継器で範囲をさらに広げられます。', '',
            '## 使い方', '',
            '1. ポートを TESU 底部リングのポート位置に置きます。',
            '2. 充電したい機械に無線アップグレードを入れます（アップグレードスロットがあり電力を使う機械）。',
            '3. ポートを右クリックすると、完成した TESU に接続しているか確認できます。', '',
            '## 注意', '',
            '- 複数の範囲に入っている機械は、電力のある最初の範囲から受け取ります。',
            '- 変圧器アップグレードか蓄電アップグレードを付けられる機械に無線アップグレードを付けられます。', '']),
    },
    'block_tesu_relay': {
        'position': 21,
        'en': ('TESU Relay', [
            '## Function', '',
            'A relay carries the TESU wireless field: once linked, it charges machines with a Wireless Upgrade within ±16 blocks of itself,',
            'without loss. It links with the TESU Wireless Connector to a wireless output port or another relay, and only if it stands inside',
            'that node\'s field, so the field grows step by step.', '',
            '## Use', '',
            '1. Place the relay inside the field of a wireless output port or of a linked relay.',
            '2. Sneak-right-click that port or relay with the TESU Wireless Connector: it becomes the target (as with the remote of the wireless power transmission node).',
            '3. Right-click the relay with the connector: it is linked to the target. The target stays, so several relays can be linked to one port; to go further, sneak-right-click a linked relay to make it the target. Sneak-right-click the air to clear the target.',
            '4. A powered relay shows a stream of particles from the node it is linked to. Right-click it with an empty hand to see where it is linked and whether it is powered.', '',
            '## Notes', '',
            '- Links cannot make a loop. If a relay on the way is removed, the relays after it lose power until relinked.',
            '- The relay is a pylon three blocks tall: it needs two free blocks above it, and breaking any part of it takes the whole pylon. Its lights glow while it is powered, and the particle stream runs between the tops of the pylons.', '']),
        'zh': ('TESU 中继器', [
            '## 功能', '',
            '中继器把 TESU 的无线范围接力出去：连接后，它给自身 ±16 格内装了无线升级的机器充电，没有损耗。',
            '它用 TESU 无线连接器连到一个无线输出端口或另一个中继器，而且只有站在那个节点的范围里才能连接，所以范围只能一段一段地往外接。', '',
            '## 用法', '',
            '1. 把中继器放在无线输出端口或已连接中继器的范围里。',
            '2. 拿 TESU 无线连接器潜行右键那个端口或中继器，把它设为目标（和无线传输节点的遥控器一样）。',
            '3. 再用连接器右键这个中继器，它就连到目标上。目标不会变，所以可以把多个中继器连到同一个端口；要继续往外接，潜行右键一个已连接的中继器，把它设为新目标。潜行右键空气清除目标。',
            '4. 有电的中继器会显示从它所连节点飞来的粒子流。空手右键中继器可以查看它连到了哪里、有没有电。', '',
            '## 注意', '',
            '- 不能连成环。中间某个中继器被拆掉后，它后面的中继器会断电，重新连接后恢复。',
            '- 中继器是三格高的立柱：放置时上方要留出两格空间，拆掉任意一格都会拆下整根立柱。有电时立柱的灯带会亮起，粒子流在立柱顶端之间流动。', '']),
        'ja': ('TESU中継器', [
            '## 機能', '',
            '中継器は TESU の無線範囲を中継します。接続すると、自分の周囲 ±16 マス内の無線アップグレード付き機械を損失なく充電します。',
            'TESU無線コネクターで無線出力ポートか別の中継器につなぎますが、その範囲内にあるときだけつなげるので、範囲は一段ずつ広がります。', '',
            '## 使い方', '',
            '1. 無線出力ポートか接続済みの中継器の範囲内に中継器を置きます。',
            '2. TESU無線コネクターでそのポートか中継器をスニーク右クリックし、ターゲットにします（無線送電ノードのリモコンと同じ）。',
            '3. コネクターで中継器を右クリックすると、ターゲットに接続されます。ターゲットは変わらないので、一つのポートに複数の中継器をつなげます。さらに先へ延ばすには、接続済みの中継器をスニーク右クリックして新しいターゲットにします。空中でスニーク右クリックするとターゲットを解除します。',
            '4. 給電中の中継器には、接続先のノードから粒子の流れが見えます。素手で中継器を右クリックすると、接続先と給電の有無を確認できます。', '',
            '## 注意', '',
            '- ループにはつなげません。途中の中継器を外すと、その先の中継器は再接続するまで給電されません。',
            '- 中継器は高さ 3 ブロックの柱です。置くには上に 2 ブロックの空きが必要で、どの部分を壊しても柱全体が外れます。電力があると柱のライトが点き、パーティクルの流れは柱の先端どうしをつなぎます。', '']),
    },
})

RECIPE = {'en': '## Recipe', 'zh': '## 配方', 'ja': '## レシピ'}
SCENE_NOTE = {
    'en': 'Drag the view to turn it. The list gives the blocks and their rules. JEI shows the same plan in the category "Multiblock Structures".',
    'zh': '拖动视图可以转动。下面的列表给出各方块和规则。JEI 的"多方块结构"分类里有同样的结构图。',
    'ja': 'ビューをドラッグすると回転します。下の一覧はブロックとその規則です。JEI の「マルチブロック構造」カテゴリーにも同じ図があります。',
}


def page(name, lang, title, body, position):
    item = 'mio_icif:wiring/' + name
    lines = ['---', 'navigation:', '  title: "%s"' % title, '  icon: %s' % item, '  parent: power.md', '  position: %d' % position,
             'item_ids:', '  - %s' % item, '---', '', '# ' + title, '', '<Row>', '  <BlockImage id="%s" scale="3" />' % item, '</Row>', '']
    lines += body
    if name == 'block_tesu_controller':
        lines += [STRUCTURE[lang], SCENE_NOTE[lang], '', '<GameScene zoom="2" interactive={true}>', '  <SiStructure id="mio_icif:tesu" />',
                  '</GameScene>', '', '<SiStructureInfo id="mio_icif:tesu" />', '']
    lines += [RECIPE[lang], '', '<RecipesFor id="%s" fallbackText="-" />' % item, '']
    return '\n'.join(lines)


def main():
    for name, spec in PAGES.items():
        for lang, folder in (('en', MAN / 'machines'), ('zh', MAN / '_zh_cn/machines'), ('ja', MAN / '_ja_jp/machines')):
            title, body = spec[lang]
            path = folder / ('wiring_' + name + '.md')
            path.write_text(page(name, lang, title, body, spec['position']), encoding='utf-8', newline='\n')
            print('wrote', path.relative_to(ROOT))


if __name__ == '__main__':
    main()
