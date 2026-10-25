---
navigation:
  title: "マルチブロック構造"
  icon: mio_icif:producer/block_large_fabricator_core
  parent: heavy.md
  position: 1
---

# マルチブロック構造

マルチブロック構造と、隣のブロックを決まった位置に置く必要がある機械の建造図です。構造はワールドで自分を検査します。建造図は正しい組み方を示します。

ドラッグで視点を回せます。JEI のカテゴリ「マルチブロック構造」にも同じ建造図があります。ドラッグで回転、ホイールで拡大縮小、層ラベルのクリックで 1 層ずつ表示できます。JEI でブロックの用途を調べると、そのブロックを含む構造が見つかります。

## 原子炉とチャンバー

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:nuclear_reactor" />
</GameScene>

<SiStructureInfo id="mio_icif:nuclear_reactor" />

ページ: [原子炉とチャンバー](generator_block_nuclear_reactor_generator.md)

## 流体原子炉（5×5×5）

<GameScene zoom="2.5" interactive={true}>
  <SiStructure id="mio_icif:fluid_reactor" />
</GameScene>

<SiStructureInfo id="mio_icif:fluid_reactor" />

ページ: [流体原子炉（5×5×5）](reactor_block_reactor_vessel.md)

## GESU

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:gesu" />
</GameScene>

<SiStructureInfo id="mio_icif:gesu" />

ページ: [GESU](wiring_block_gesu_core.md)

## 大型物質生成機

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:large_fabricator" />
</GameScene>

<SiStructureInfo id="mio_icif:large_fabricator" />

ページ: [大型物質生成機](producer_block_large_fabricator_core.md)

## 油井

<GameScene zoom="3" interactive={true}>
  <SiStructure id="mio_icif:oil_rig" />
</GameScene>

<SiStructureInfo id="mio_icif:oil_rig" />

ページ: [油井](oilrig_block_oil_rig_core.md)

## 次元油井

<GameScene zoom="2" interactive={true}>
  <SiStructure id="mio_icif:dimension_oil_rig" />
</GameScene>

<SiStructureInfo id="mio_icif:dimension_oil_rig" />

ページ: [次元油井](oilrig_block_dimension_oil_rig_core.md)

## 高炉と熱源

<GameScene zoom="5" interactive={true}>
  <SiStructure id="mio_icif:blast_furnace" />
</GameScene>

<SiStructureInfo id="mio_icif:blast_furnace" />

ページ: [高炉と熱源](producer_block_blast_furnace.md)

## 発酵槽と熱源

<GameScene zoom="5" interactive={true}>
  <SiStructure id="mio_icif:fermenter" />
</GameScene>

<SiStructureInfo id="mio_icif:fermenter" />

ページ: [発酵槽と熱源](producer_block_fermenter_elc.md)

## アドオン作者向け

`StructureBlueprintRegistry.register(...)`（パッケージ `core.api.multiblock`）で建造図を登録すると JEI に表示されます。このガイドのページでは `GameScene` の中の `<SiStructure id="mymod:plan" />` と `<SiStructureInfo id="mymod:plan" />` で表示できます。API ガイドの第 20 節を参照してください。
