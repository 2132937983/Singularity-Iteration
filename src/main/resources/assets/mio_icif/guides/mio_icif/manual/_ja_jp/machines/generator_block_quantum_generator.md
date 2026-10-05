---
navigation:
  title: "Quantum Generator"
  icon: mio_icif:generator/block_quantum_generator
  parent: generators.md
  position: 23
item_ids:
  - mio_icif:generator/block_quantum_generator
---

# Quantum Generator

<Row>
  <BlockImage id="mio_icif:generator/block_quantum_generator" scale="3" />
</Row>

## 機能

機械は燃料も日光もなしでEUを作る。
機械は6面すべての隣接ブロックにEUを送る。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | HV (512 EU) |
| 出力 | 512 EU/t |
| 蓄電容量 | 100,000,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 発電機を機械またはケーブルの隣に置く。
2. GUIを開いて出力と電圧Tierを確認する。

## 注意

- GUIは状態だけを表示する。GUIに操作ボタンはない。

## レシピ

<RecipesFor id="mio_icif:generator/block_quantum_generator" fallbackText="-" />
