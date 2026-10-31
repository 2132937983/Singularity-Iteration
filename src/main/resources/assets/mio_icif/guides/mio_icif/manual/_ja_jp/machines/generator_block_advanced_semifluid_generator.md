---
navigation:
  title: "高度半流体発電機"
  icon: mio_icif:generator/block_advanced_semifluid_generator
  parent: generators.md
  position: 2
item_ids:
  - mio_icif:generator/block_advanced_semifluid_generator
---

# 高度半流体発電機

<Row>
  <BlockImage id="mio_icif:generator/block_advanced_semifluid_generator" scale="3" />
</Row>

## 機能

機械はバイオガスを燃やしてEUを作る。
機械はバイオガスバケツとバイオガスセルだけを受け入れる。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | MV (128 EU) |
| 出力 | 36 EU/t |
| 蓄電容量 | 100,000 EU |
| 燃料：バイオガス | 1.5 mB/t → 36 EU/t (24 EU/mB) |

この値は本バージョンのブロックエンティティから計測した。

発電倍率（設定 GeneratorOutput）が 1.0 以外のとき、EU の値が変わる。燃料消費は変わらない。

## 使用手順

1. MVケーブルを発電機に接続する。
2. バイオガスバケツを入力スロットに入れる。
3. 出力スロットから空の容器を取り出す。
4. 充電するバッテリーをバッテリースロットに入れる。

## レシピ

<RecipesFor id="mio_icif:generator/block_advanced_semifluid_generator" fallbackText="-" />
