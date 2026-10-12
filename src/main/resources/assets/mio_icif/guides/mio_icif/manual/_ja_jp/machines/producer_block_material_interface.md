---
navigation:
  title: "武装庫資材インターフェース"
  icon: mio_icif:producer/block_material_interface
  parent: machines.md
  position: 11
item_ids:
  - mio_icif:producer/block_material_interface
---

# 武装庫資材インターフェース

<Row>
  <BlockImage id="mio_icif:producer/block_material_interface" scale="3" />
</Row>

## 機能

[ブループリントビルダー](producer_block_blueprint_builder.md)のドローンへ建材を供給します。
27スロットを持ち、6面に隣接するストレージも読み取ります。

## 規則

* アイテムハンドラーを持つストレージなら使えます：チェスト、樽、引き出し、AE2インターフェース、Refined Storageのインターフェースなど。
* ドローンは自身のスロットから先に、次に隣接ストレージから取り出します。
* 使わなかった建材はインターフェースへ戻します。
* ホッパーやパイプで27スロットを補充できます。RTS Buildingはストレージとしてリンクできます。
* ビルダーは16ブロック以内のインターフェースを使います。

## レシピ

<RecipesFor id="mio_icif:producer/block_material_interface" fallbackText="-" />
