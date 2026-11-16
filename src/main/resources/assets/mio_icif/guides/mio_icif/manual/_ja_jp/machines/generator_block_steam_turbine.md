---
navigation:
  title: "蒸気タービン発電機"
  icon: mio_icif:generator/block_steam_turbine
  parent: generators.md
  position: 31
item_ids:
  - mio_icif:generator/block_steam_turbine
---

# 蒸気タービン発電機

<Row>
  <BlockImage id="mio_icif:generator/block_steam_turbine" scale="3" />
</Row>

## 機能

機械は蒸気をEUに変える。
機械はバッテリースロットのバッテリーを充電し、蒸気セルをタンクに空ける。
蒸気はタービン内で入口圧（蒸気：10 bar 飽和、過熱蒸気：40 bar・400 °C）から背圧まで膨張する。大気へ放出するとき背圧は 1 bar。隣に稼働中の凝縮器があると排気は凝縮器へ入り、背圧は 0.1 bar に下がる：同じ蒸気でより多くの仕事をする（蒸気 0.8 EU/mB、従来 0.5。過熱蒸気 1.3、従来 1.0）。
画面はプラントの模擬盤：入口、タービン、発電機、凝縮器か放出口への排気と膨張の値。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | MV (128 EU) |
| 出力 | 64 EU/t |
| 蓄電容量 | 32,000 EU |
| 蒸気消費 | 128 mB/t → 64 EU/t (1 EU あたり 2 mB) |
| 過熱蒸気消費 | 64 mB/t → 64 EU/t (1 EU あたり 1 mB) |
| 凝縮器あり | 蒸気 80 mB/t、過熱蒸気 約 49 mB/t → 64 EU/t |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 機械を蒸気原子炉の隣に置くか、蒸気を運ぶパイプを機械に接続する。
2. MVケーブルを機械に接続する。

## 注意

- 蓄電容量に空きがある間だけ、機械は蒸気を使う。

## レシピ

<RecipesFor id="mio_icif:generator/block_steam_turbine" fallbackText="-" />
