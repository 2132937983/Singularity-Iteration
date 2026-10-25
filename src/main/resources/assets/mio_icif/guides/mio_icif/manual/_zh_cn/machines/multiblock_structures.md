---
navigation:
  title: "多方块结构"
  icon: mio_icif:producer/block_large_fabricator_core
  parent: heavy.md
  position: 1
---

# 多方块结构

这里是多方块结构，以及需要相邻方块固定摆放的机器的建造图。结构在世界中会自行检查；建造图展示正确的搭建方式。

拖动视图可旋转。JEI 的“多方块结构”分类显示相同的建造图：拖动旋转、滚轮缩放，点击层标签可逐层查看。在 JEI 中查看某个方块的用途，即可找到包含它的结构。

## 核反应堆与核反应仓

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:nuclear_reactor" />
</GameScene>

<SiStructureInfo id="mio_icif:nuclear_reactor" />

页面: [核反应堆与核反应仓](generator_block_nuclear_reactor_generator.md)

## 流体反应堆（5×5×5）

<GameScene zoom="2.5" interactive={true}>
  <SiStructure id="mio_icif:fluid_reactor" />
</GameScene>

<SiStructureInfo id="mio_icif:fluid_reactor" />

页面: [流体反应堆（5×5×5）](reactor_block_reactor_vessel.md)

## GESU

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:gesu" />
</GameScene>

<SiStructureInfo id="mio_icif:gesu" />

页面: [GESU](wiring_block_gesu_core.md)

## 大型物质生成机

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:large_fabricator" />
</GameScene>

<SiStructureInfo id="mio_icif:large_fabricator" />

页面: [大型物质生成机](producer_block_large_fabricator_core.md)

## 石油钻机

<GameScene zoom="3" interactive={true}>
  <SiStructure id="mio_icif:oil_rig" />
</GameScene>

<SiStructureInfo id="mio_icif:oil_rig" />

页面: [石油钻机](oilrig_block_oil_rig_core.md)

## 维度石油钻机

<GameScene zoom="2" interactive={true}>
  <SiStructure id="mio_icif:dimension_oil_rig" />
</GameScene>

<SiStructureInfo id="mio_icif:dimension_oil_rig" />

页面: [维度石油钻机](oilrig_block_dimension_oil_rig_core.md)

## 高炉与热源

<GameScene zoom="5" interactive={true}>
  <SiStructure id="mio_icif:blast_furnace" />
</GameScene>

<SiStructureInfo id="mio_icif:blast_furnace" />

页面: [高炉与热源](producer_block_blast_furnace.md)

## 发酵机与热源

<GameScene zoom="5" interactive={true}>
  <SiStructure id="mio_icif:fermenter" />
</GameScene>

<SiStructureInfo id="mio_icif:fermenter" />

页面: [发酵机与热源](producer_block_fermenter_elc.md)

## 附属模组作者

用 `StructureBlueprintRegistry.register(...)`（包 `core.api.multiblock`）注册建造图后，JEI 会显示它；本手册的页面可在 `GameScene` 中用 `<SiStructure id="mymod:plan" />`，并用 `<SiStructureInfo id="mymod:plan" />` 显示它。详见 API 指南第 20 节。
