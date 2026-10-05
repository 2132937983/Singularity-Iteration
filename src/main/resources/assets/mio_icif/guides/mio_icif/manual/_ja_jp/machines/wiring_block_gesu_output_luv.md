---
navigation:
  title: "GESU Output Module (LuV)"
  icon: mio_icif:wiring/block_gesu_output_luv
  parent: power.md
  position: 16
item_ids:
  - mio_icif:wiring/block_gesu_output_luv
---

# GESU Output Module (LuV)

<Row>
  <BlockImage id="mio_icif:wiring/block_gesu_output_luv" scale="3" />
</Row>

## 機能

GESU出力モジュール（LuV）がGESUコアからEUを取り出し、LuVでケーブルへ送る。
このモジュールは構造部品だ。完成したGESU構造の中でだけ動作する。
出力モジュールを1個足すごとに、コアの出力速度が上がる。

## 電力データ

| 項目 | 値 |
|---|---|
| 蓄電容量 | 65,536 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. モジュールをGESUコアの面に直接置く。
2. コアの残りの面をモジュールで埋める。
3. LuVケーブルを出力モジュールの外側の面に接続する。
4. ケーブルと電圧Tierの低い機械の間に変圧器を置く。

## 注意


> **警告：** 出力はLuVだ。電圧Tierが低い機械は過電圧で爆発する。


## レシピ

<RecipesFor id="mio_icif:wiring/block_gesu_output_luv" fallbackText="-" />
