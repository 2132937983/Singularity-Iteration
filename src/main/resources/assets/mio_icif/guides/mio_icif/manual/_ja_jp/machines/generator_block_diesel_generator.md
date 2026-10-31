---
navigation:
  title: "ディーゼル発電機"
  icon: mio_icif:generator/block_diesel_generator
  parent: generators.md
  position: 5
item_ids:
  - mio_icif:generator/block_diesel_generator
---

# ディーゼル発電機

<Row>
  <BlockImage id="mio_icif:generator/block_diesel_generator" scale="3" />
</Row>

## 機能

機械は軽油を燃やしてEUを作る。
機械は軽油バケツと軽油セルだけを受け入れる。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | EV (2,048 EU) |
| 出力 | 120 EU/t |
| 蓄電容量 | 1,000,000 EU |
| 燃料：ディーゼル燃料 | 16 mB/t → 120 EU/t (7.5 EU/mB) |

この値は本バージョンのブロックエンティティから計測した。

発電倍率（設定 GeneratorOutput）が 1.0 以外のとき、EU の値が変わる。燃料消費は変わらない。

## 使用手順

1. HVケーブルを発電機に接続する。
2. 軽油バケツを入力スロットに入れる。
3. 出力スロットから空の容器を取り出す。
4. 充電するバッテリーをバッテリースロットに入れる。

## レシピ

<RecipesFor id="mio_icif:generator/block_diesel_generator" fallbackText="-" />
