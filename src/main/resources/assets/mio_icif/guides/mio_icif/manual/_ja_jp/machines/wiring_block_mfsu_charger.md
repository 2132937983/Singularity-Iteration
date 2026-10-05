---
navigation:
  title: "MFSU Charger"
  icon: mio_icif:wiring/block_mfsu_charger
  parent: power.md
  position: 29
item_ids:
  - mio_icif:wiring/block_mfsu_charger
---

# MFSU Charger

<Row>
  <BlockImage id="mio_icif:wiring/block_mfsu_charger" scale="3" />
</Row>

## 機能

MFSU充電器は上面に充電パッドを持つEVの蓄電ブロックだ。
パッドが上に立つプレイヤーの防具、手持ちアイテム、インベントリ内のアイテムを充電する。
正面がEUを出力する。他の5面がEUを受け取る。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | EV (2,048 EU) |
| 最大入力 | 2,048 EU/t |
| 蓄電容量 | 40,000,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. MFSU充電器を置き、正面を自分の方に向ける。
2. EVの発電機ケーブルを5つの入力面のどれかに接続する。
3. MFSU充電器がEUを蓄えるまで待つ。
4. MFSU充電器の上に立ち、装備を充電する。

## 注意

- レッドストーンモードが出力を許可する間だけ、パッドが充電する。

> **警告：** 出力はEVだ。電圧Tierが低い機械は過電圧で爆発する。


## レシピ

<RecipesFor id="mio_icif:wiring/block_mfsu_charger" fallbackText="-" />
