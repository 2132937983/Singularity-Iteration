---
navigation:
  title: "CESU Charger"
  icon: mio_icif:wiring/block_cesu_charger
  parent: power.md
  position: 6
item_ids:
  - mio_icif:wiring/block_cesu_charger
---

# CESU Charger

<Row>
  <BlockImage id="mio_icif:wiring/block_cesu_charger" scale="3" />
</Row>

## 機能

CESU充電器は上面に充電パッドを持つMVの蓄電ブロックだ。
パッドが上に立つプレイヤーの防具、手持ちアイテム、インベントリ内のアイテムを充電する。
正面がEUを出力する。他の5面がEUを受け取る。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MV (128 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 300,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. CESU充電器を置き、正面を自分の方に向ける。
2. MVの発電機ケーブルを5つの入力面のどれかに接続する。
3. CESU充電器がEUを蓄えるまで待つ。
4. CESU充電器の上に立ち、装備を充電する。

## 注意

- レッドストーンモードが出力を許可する間だけ、パッドが充電する。

> **警告：** 出力はMVだ。電圧Tierが低い機械は過電圧で爆発する。


## レシピ

<RecipesFor id="mio_icif:wiring/block_cesu_charger" fallbackText="-" />
