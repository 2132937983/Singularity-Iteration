---
navigation:
  title: "Electrolyzer"
  icon: mio_icif:producer/block_electrolyzer_elc
  parent: machines.md
  position: 23
item_ids:
  - mio_icif:producer/block_electrolyzer_elc
---

# Electrolyzer

<Row>
  <BlockImage id="mio_icif:producer/block_electrolyzer_elc" scale="3" />
</Row>

## 機能

電解機はEUを使って水セルの水を分解し、そのエネルギーを化学エネルギーとしてためる。
機械はためたエネルギーを満杯でない隣のブロックへ返す。出力スロットには空セルが入る。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | LV (32 EU) |
| 最大入力 | 32 EU/t |
| 蓄電容量 | 400 EU |
| 稼働時消費 | 10 EU/t |
| 1回の処理時間 | 20 tick (1 s) |
| 1回あたりの電力 | 200 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. LVケーブルを機械に接続する。
2. 水セルを入力スロットに入れる。
3. ためたエネルギーを受け取るため、電解機の隣に蓄電ブロックか機械を置く。
4. 出力スロットから空セルを取り出す。

## 注意


> **警告：** LVを超える過電圧で機械は爆発する。


## レシピ

<RecipesFor id="mio_icif:producer/block_electrolyzer_elc" fallbackText="-" />
