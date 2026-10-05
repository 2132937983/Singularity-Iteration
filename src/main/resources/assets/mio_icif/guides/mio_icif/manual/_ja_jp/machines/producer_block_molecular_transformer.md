---
navigation:
  title: "Molecular Transformer"
  icon: mio_icif:producer/block_molecular_transformer
  parent: machines.md
  position: 47
item_ids:
  - mio_icif:producer/block_molecular_transformer
---

# Molecular Transformer

<Row>
  <BlockImage id="mio_icif:producer/block_molecular_transformer" scale="3" />
</Row>

## 機能

分子変換機は非常に大量のEUを使い、アイテムを別のアイテムに変える。
グロウストーンダストはサンナリウムに、鉄インゴットはイリジウム鉱石に、ウィザースケルトンの頭蓋骨はネザースターになる。
機械はどの電圧Tierも受け付け、入力の上限はない。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | 上限なし |
| 蓄電容量 | 120,000,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 出力の大きい発電機か蓄電ブロックを機械に接続する。
2. 入力アイテムを入力スロットに入れる。
3. 進捗バーが満杯になるまで待つ。
4. 出力スロットから成果物を取り出す。

## 注意

- ためたEUが多いと、機械は同じtickで複数の処理を終える。

## レシピ

<RecipesFor id="mio_icif:producer/block_molecular_transformer" fallbackText="-" />
