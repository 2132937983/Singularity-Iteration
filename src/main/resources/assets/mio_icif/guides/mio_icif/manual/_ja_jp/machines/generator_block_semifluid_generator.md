---
navigation:
  title: "半流体発電機"
  icon: mio_icif:generator/block_semifluid_generator
  parent: generators.md
  position: 27
item_ids:
  - mio_icif:generator/block_semifluid_generator
---

# 半流体発電機

<Row>
  <BlockImage id="mio_icif:generator/block_semifluid_generator" scale="3" />
</Row>

## 機能

機械は半流体燃料を燃やしてEUを作る。
機械はバイオガス、バイオマス、原油、軽油を受け入れる。
燃料ごとにEU出力が異なる。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | LV (32 EU) |
| 出力 | 8〜32 EU/t（燃料による） |
| 蓄電容量 | 32,000 EU |
| 燃料：バイオガス | 0.5 mB/t → 16 EU/t (32 EU/mB) |
| 燃料：バイオマス | 1 mB/t → 8 EU/t (8 EU/mB) |
| 燃料：原油 | 0.5 mB/t → 8 EU/t (16 EU/mB) |
| 燃料：ディーゼル燃料 | 0.25 mB/t → 32 EU/t (128 EU/mB) |

この値は本バージョンのブロックエンティティから計測した。

発電倍率（設定 GeneratorOutput）が 1.0 以外のとき、EU の値が変わる。燃料消費は変わらない。

## 使用手順

1. LVケーブルを発電機に接続する。
2. 燃料バケツまたは燃料セルを入力スロットに入れる。
3. 出力スロットから空の容器を取り出す。
4. 充電するバッテリーをバッテリースロットに入れる。

## レシピ

<RecipesFor id="mio_icif:generator/block_semifluid_generator" fallbackText="-" />
