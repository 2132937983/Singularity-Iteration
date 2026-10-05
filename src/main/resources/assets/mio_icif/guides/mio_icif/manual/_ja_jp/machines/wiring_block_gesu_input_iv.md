---
navigation:
  title: "GESU Input Module (IV)"
  icon: mio_icif:wiring/block_gesu_input_iv
  parent: power.md
  position: 14
item_ids:
  - mio_icif:wiring/block_gesu_input_iv
---

# GESU Input Module (IV)

<Row>
  <BlockImage id="mio_icif:wiring/block_gesu_input_iv" scale="3" />
</Row>

## 機能

GESU入力モジュールがケーブルからEUを受け取り、GESUコアへ送る。
このモジュールは構造部品だ。完成したGESU構造の中でだけ動作する。
入力モジュールを1個足すごとに、コアの入力速度が上がる。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MAX (2,147,483,648 EU) |
| 最大入力 | 2,147,483,647 EU/t |
| 蓄電容量 | 4,294,967,294 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. モジュールをGESUコアの面に直接置く。
2. コアの残りの面をモジュールで埋める。
3. 発電機のケーブルを入力モジュールの外側の面に接続する。
4. 入力モジュールを足して入力速度を上げる。

## レシピ

<RecipesFor id="mio_icif:wiring/block_gesu_input_iv" fallbackText="-" />
