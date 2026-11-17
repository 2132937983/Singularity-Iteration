---
navigation:
  title: "高度ドロップ発電機"
  icon: mio_icif:generator/block_advanced_drop_generator
  parent: generators.md
  position: 0
item_ids:
  - mio_icif:generator/block_advanced_drop_generator
---

# 高度ドロップ発電機

<Row>
  <BlockImage id="mio_icif:generator/block_advanced_drop_generator" scale="3" />
</Row>

## 機能

機械はドロップアイテムを引き寄せ、破壊してEUを作る。
機械の範囲と出力はドロップ発電機より大きい。
レアなアイテムは普通のアイテムより多くのEUを作る。普通のアイテムはかまどの燃焼時間で計算する。
機械は炉内温度をもつ焼却炉だ。燃えるアイテムは炉を 1,000 °C に向けて温め、燃えないアイテム（石・土）は燃料にならず 1 個ごとに 25 °C 冷やす。燃料を EU に変える割合は炉温に従い、300 °C 未満はなし、850 °C で全量。冷えた機械は最初の燃料で昇温する。
アイテムは灰受けに灰を残す（燃えるもの 1 個、その他 2 個）。灰が 64 個になると吸い込みを止める。手・ホッパー・配管で灰を取り出すこと。
画面は記録計：直近 12 秒の炉内温度とアイテムごとの印、効率、出力、灰の量。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | ULV (8 EU) |
| 出力 | 40 EU/t |
| 蓄電容量 | 100,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 発電機をアイテム回収ラインの出口に置く。
2. HVケーブルを発電機に接続する。
3. 不要なアイテムを発電機の近くに落とす。

## 注意


> **警告：** 機械は範囲内のすべてのドロップアイテムを破壊する。プレイヤーのアイテムも対象だ。


## レシピ

<RecipesFor id="mio_icif:generator/block_advanced_drop_generator" fallbackText="-" />
