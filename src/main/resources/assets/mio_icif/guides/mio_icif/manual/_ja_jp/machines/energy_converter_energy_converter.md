---
navigation:
  title: "Energy Converter"
  icon: mio_icif:energy_converter/energy_converter
  parent: machines.md
  position: 24
item_ids:
  - mio_icif:energy_converter/energy_converter
---

# Energy Converter

<Row>
  <BlockImage id="mio_icif:energy_converter/energy_converter" scale="3" />
</Row>

## 機能

エネルギー変換機はEUとFEの間でエネルギーを変換する。比率は1 EU = 4 FEだ。
EUからFEのモードでは、機械はEUを受け、正面からFEを出す。
FEからEUのモードでは、機械は残りの5面からFEを取り、HVでケーブル網へEUを出す。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | HV (512 EU) |
| 最大入力 | 2,048 EU/t |
| 蓄電容量 | 100,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 正面を送り先に向けて変換機を置く。
2. スニークして変換機を右クリックし、モードを切り替える。
3. 残りの面にエネルギー源を接続する。

## 注意


> **警告：** HVを超える過電圧で機械は爆発する。


## レシピ

<RecipesFor id="mio_icif:energy_converter/energy_converter" fallbackText="-" />
