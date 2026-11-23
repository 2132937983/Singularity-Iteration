---
navigation:
  title: "無限武装庫資材インターフェース"
  icon: mio_icif:producer/block_infinite_material_interface
  parent: machines.md
  position: 11
item_ids:
  - mio_icif:producer/block_infinite_material_interface
---

# 無限武装庫資材インターフェース

<Row>
  <BlockImage id="mio_icif:producer/block_infinite_material_interface" scale="3" />
</Row>

## 機能

クリエイティブ用のブロックです。無限発電機と同じくレシピはありません。[ブループリントビルダー](producer_block_blueprint_builder.md)のドローンに、すべての建材を無限に供給します。

## ルール

* ビルダーから16ブロック以内に置くだけです。中に何も入れる必要はありません。画面もスロットもありません。
* 設計図に必要なブロックをすべて供給し、何も消費しません。
* ドローンが持ち帰った余りの建材は消えます。
* ホッパーやパイプはつながりません。
* ビルダーにもっと近い通常の[武装庫資材インターフェース](producer_block_material_interface.md)があれば、そちらが先に使われます。そこにない建材は無限インターフェースから供給されます。
