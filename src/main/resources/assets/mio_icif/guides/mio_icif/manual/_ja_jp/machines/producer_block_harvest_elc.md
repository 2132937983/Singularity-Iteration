---
navigation:
  title: "Harvester"
  icon: mio_icif:producer/block_harvest_elc
  parent: machines.md
  position: 30
item_ids:
  - mio_icif:producer/block_harvest_elc
---

# Harvester

<Row>
  <BlockImage id="mio_icif:producer/block_harvest_elc" scale="3" />
</Row>

## 機能

収穫機は周囲9×3×9の範囲のクロップスティックから熟した作物を集める。
機械は収穫物を保管スロットに入れる。
作物分析器を入れると、機械は最適サイズでのみ収穫する。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | LV (32 EU) |
| 最大入力 | 32 EU/t |
| 蓄電容量 | 10,000 EU |
| 稼働時消費 | 1 EU/t |
| 1回の処理時間 | 10 tick (0.5 s) |
| 1回あたりの電力 | 10 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 機械を畑の中央に置く。
2. LVケーブルを機械に接続する。
3. 最適サイズで収穫するため、分析器スロットに作物分析器を入れる。
4. 保管スロットから作物を取り出す。

## 注意

- 作物分析器は収穫ごとのEU消費を増やす。
- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** LVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_harvest_elc" fallbackText="-" />
