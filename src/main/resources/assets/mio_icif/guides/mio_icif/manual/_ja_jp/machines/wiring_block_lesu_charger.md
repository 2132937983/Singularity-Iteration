---
navigation:
  title: "LESU Charger"
  icon: mio_icif:wiring/block_lesu_charger
  parent: power.md
  position: 24
item_ids:
  - mio_icif:wiring/block_lesu_charger
---

# LESU Charger

<Row>
  <BlockImage id="mio_icif:wiring/block_lesu_charger" scale="3" />
</Row>

## 機能

LESU充電器は上面に充電パッドを持つHVの蓄電ブロックだ。
パッドが上に立つプレイヤーの防具、手持ちアイテム、インベントリ内のアイテムを充電する。
正面がEUを出力する。他の5面がEUを受け取る。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | HV (512 EU) |
| 最大入力 | 512 EU/t |
| 蓄電容量 | 1,000,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. LESU充電器を置き、正面を自分の方に向ける。
2. HVの発電機ケーブルを5つの入力面のどれかに接続する。
3. LESU充電器がEUを蓄えるまで待つ。
4. LESU充電器の上に立ち、装備を充電する。

## 注意

- レッドストーンモードが出力を許可する間だけ、パッドが充電する。

> **警告：** 出力はHVだ。電圧Tierが低い機械は過電圧で爆発する。


## レシピ

<RecipesFor id="mio_icif:wiring/block_lesu_charger" fallbackText="-" />
