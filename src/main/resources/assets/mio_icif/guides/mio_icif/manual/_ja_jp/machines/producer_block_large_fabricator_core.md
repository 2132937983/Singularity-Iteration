---
navigation:
  title: "大型物質生成機コア"
  icon: mio_icif:producer/block_large_fabricator_core
  parent: machines.md
  position: 34
item_ids:
  - mio_icif:producer/block_large_fabricator_core
---

# 大型物質生成機コア

<Row>
  <BlockImage id="mio_icif:producer/block_large_fabricator_core" scale="3" />
</Row>

## 機能

大型物質生成機コアはマルチブロックの中心だ。6面すべてにモジュールが接する必要がある。
構造には入力モジュール（IV）とタンクモジュールが最低1つずつ必要だ。スクラップ入力モジュールは最大1つだ。
コアは入力モジュールのEUをUU物質に変え、タンクモジュールにためる。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 大型物質生成機コアを置く。
2. コアの6面それぞれにモジュールを1つ置く。
3. IVケーブルを入力モジュールに接続する。
4. パイプでタンクモジュールからUU物質を取り出す。

## 注意

- GUIはモジュールの数と、構造が不完全なときのメッセージを表示する。

## 構造

ドラッグで視点を回せます。下のリストは必要なブロックとそのルールです。JEI のカテゴリ「マルチブロック構造」にも同じ建造図があります。

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:large_fabricator" />
</GameScene>

<SiStructureInfo id="mio_icif:large_fabricator" />

## レシピ

<RecipesFor id="mio_icif:producer/block_large_fabricator_core" fallbackText="-" />
