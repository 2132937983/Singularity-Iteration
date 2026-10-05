---
navigation:
  title: "Experience Generator"
  icon: mio_icif:generator/block_experience_generator
  parent: generators.md
  position: 9
item_ids:
  - mio_icif:generator/block_experience_generator
---

# Experience Generator

<Row>
  <BlockImage id="mio_icif:generator/block_experience_generator" scale="3" />
</Row>

## 機能

機械は範囲内の経験値オーブを引き寄せ、EUに変換する。
経験値の多いオーブほど多くのEUを作る。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | ULV (8 EU) |
| 出力 | 160 EU/t |
| 蓄電容量 | 1,000,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 発電機をモブトラップの近くに置く。
2. HVケーブルを発電機に接続する。
3. 発電機の近くでモブを倒して経験値オーブを出す。

## 注意

- 機械はプレイヤーが欲しい経験値オーブも吸い取る。

## レシピ

<RecipesFor id="mio_icif:generator/block_experience_generator" fallbackText="-" />
