---
navigation:
  title: "Fluid Flow Regulator"
  icon: mio_icif:producer/block_fluid_regulator_elc
  parent: machines.md
  position: 28
item_ids:
  - mio_icif:producer/block_fluid_regulator_elc
---

# Fluid Flow Regulator

<Row>
  <BlockImage id="mio_icif:producer/block_fluid_regulator_elc" scale="3" />
</Row>

## 機能

流体流量調整機は流体をタンクにため、設定した速さで正面から出す。
GUIで量と単位（毎秒か毎tick）を設定する。送るたびにEUを使う。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | EV (2,048 EU) |
| 最大入力 | 512 EU/t |
| 蓄電容量 | 10,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. EVケーブルを機械に接続する。
2. 流体をパイプで調整機に入れるか、満タンの容器を入力スロットに入れる。
3. 正面に送り先のタンクかパイプを置く。
4. GUIで流量を設定する。

## 注意

- 流量がゼロのとき、調整機は流体を送らない。

> **警告：** EVを超える過電圧で機械は爆発する。


## レシピ

<RecipesFor id="mio_icif:producer/block_fluid_regulator_elc" fallbackText="-" />
