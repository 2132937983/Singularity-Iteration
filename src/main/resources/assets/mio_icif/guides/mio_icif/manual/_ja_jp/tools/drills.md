---
navigation:
  title: "ドリルとマイニングレーザー"
  icon: mio_icif:item_tool_iridium_driller
  parent: tools.md
  position: 1
item_ids:
  - mio_icif:item_tool_iron_driller
  - mio_icif:item_tool_diamond_driller
  - mio_icif:item_tool_iridium_driller
  - mio_icif:item_tool_laser_miner
  - mio_icif:item_tool_bronze_pickaxe
  - mio_icif:item_tool_bronze_axe
  - mio_icif:item_tool_bronze_shovel
  - mio_icif:item_tool_bronze_hoe
---

# ドリルとマイニングレーザー

## ドリル

ドリルは電動のツルハシとシャベルである。電力があると速く掘り、ブロックをドロップする。電力がないと素手と同じになる。

| ドリル | ドロップ等級 | 速度 | 1 ブロックあたりの消費電力 | 容量 | 範囲（G キー） | 手持ちの攻撃力 |
|---|---|---|---|---|---|---|
| <ItemLink id="mio_icif:item_tool_iron_driller" /> | 鉄 | 8 | 50 EU | 30,000 EU | 1×1 | 6 |
| <ItemLink id="mio_icif:item_tool_diamond_driller" /> | ダイヤモンド | 16 | 80 EU | 30,000 EU | 1×1、3×3 | 7 |
| <ItemLink id="mio_icif:item_tool_iridium_driller" /> | ネザライト | 25 | 800 EU | 1,000,000 EU | 1×1、3×3、5×5 | 10 |

- **範囲採掘。**範囲内の各ブロックはプレイヤー自身が掘るのと同じように壊れ、1 ブロックごとに 1 回分の電力を使う。3×3 は速度が半分、5×5 は 0.3 倍。中心よりずっと硬いブロック（硬度が中心 × 2 + 1 を超える）は掘らない。視線の上下の角度が 45° を超えると水平面を、それ以外は垂直面を掘る。
- **イリジウムドリル。**スニーク + G キーで幸運 III とシルクタッチを切り替える。
- **攻撃。**命中ごとに 1 回分の電力を使う。電力がないと攻撃ダメージはない。
- **採掘機。**3 種類のドリルはすべて採掘機に入れられる。

## マイニングレーザー

<ItemLink id="mio_icif:item_tool_laser_miner" /> はレーザーでブロックを掘る。G キーでモードを切り替える：採掘、低集束、長距離、水平、戦闘、超高熱（ドロップを製錬する）、拡散、爆破、3×3、超高エネルギー爆破（サーバーでは既定で無効）。各モードの消費電力と射程はツールチップに出る。レーザーはブロックごとに保護区域の権限を確認する。

## 青銅ツール

青銅のツルハシ、斧、シャベル、クワ、剣は普通のツールである。採掘等級は鉄と同じ、耐久値 250、青銅インゴットで修理でき、対応するエンチャントをすべて付けられる。
