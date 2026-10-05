---
navigation:
  title: "MFE"
  icon: mio_icif:wiring/block_mfe
  parent: power.md
  position: 26
item_ids:
  - mio_icif:wiring/block_mfe
---

# MFE

<Row>
  <BlockImage id="mio_icif:wiring/block_mfe" scale="3" />
</Row>

## 機能

MFEはHVの蓄電ブロックだ。MFEがEUを蓄え、機械にEUを供給する。
正面がEUを出力する。他の5面がEUを受け取る。
上のスロットがアイテムを充電する。下のスロットがバッテリーからEUを取り出す。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | HV (512 EU) |
| 最大入力 | 512 EU/t |
| 蓄電容量 | 4,000,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. MFEを置き、正面を自分の方に向ける。
2. 発電機のケーブルを5つの入力面のどれかに接続する。
3. HVケーブルで正面を機械に接続する。
4. GUIを開き、レッドストーンモードを選んで出力を制御する。

## 注意

- レッドストーンモードで出力を止めるか、指定の充電量で信号を出す。

> **警告：** 出力はHVだ。電圧Tierが低い機械は過電圧で爆発する。


## レシピ

<RecipesFor id="mio_icif:wiring/block_mfe" fallbackText="-" />
