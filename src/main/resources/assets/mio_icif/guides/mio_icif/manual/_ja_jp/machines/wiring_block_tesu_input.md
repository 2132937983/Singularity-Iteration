---
navigation:
  title: "TESU入力ポート"
  icon: mio_icif:wiring/block_tesu_input
  parent: power.md
  position: 18
item_ids:
  - mio_icif:wiring/block_tesu_input
---

# TESU入力ポート

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_input" scale="3" />
</Row>

## 機能

TESU入力ポートはあらゆる電圧（最大 MAX）の電力を MAX パケット 2 個分のバッファーに受け取り、1 tick に最大 MAX パケット 1 個を
コントローラーへ渡します。TESU 底部リングのポート位置に置くと働きます。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧 | すべて、最大 MAX（1 パケット 2,147,483,647 EU） |
| コントローラーへ | 最大 2,147,483,647 EU/t |

## レシピ

<RecipesFor id="mio_icif:wiring/block_tesu_input" fallbackText="-" />
