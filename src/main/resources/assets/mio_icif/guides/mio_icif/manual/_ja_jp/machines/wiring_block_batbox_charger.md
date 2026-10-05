---
navigation:
  title: "BatBox Charger"
  icon: mio_icif:wiring/block_batbox_charger
  parent: power.md
  position: 4
item_ids:
  - mio_icif:wiring/block_batbox_charger
---

# BatBox Charger

<Row>
  <BlockImage id="mio_icif:wiring/block_batbox_charger" scale="3" />
</Row>

## 機能

BatBox充電器は上面に充電パッドを持つLVの蓄電ブロックだ。
パッドが上に立つプレイヤーの防具、手持ちアイテム、インベントリ内のアイテムを充電する。
正面がEUを出力する。他の5面がEUを受け取る。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | LV (32 EU) |
| 最大入力 | 32 EU/t |
| 蓄電容量 | 40,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. BatBox充電器を置き、正面を自分の方に向ける。
2. LVの発電機ケーブルを5つの入力面のどれかに接続する。
3. BatBox充電器がEUを蓄えるまで待つ。
4. BatBox充電器の上に立ち、装備を充電する。

## 注意

- レッドストーンモードが出力を許可する間だけ、パッドが充電する。

## レシピ

<RecipesFor id="mio_icif:wiring/block_batbox_charger" fallbackText="-" />
