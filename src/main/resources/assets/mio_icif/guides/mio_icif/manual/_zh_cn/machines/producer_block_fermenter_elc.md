---
navigation:
  title: "发酵机"
  icon: mio_icif:producer/block_fermenter_elc
  parent: machines.md
  position: 26
item_ids:
  - mio_icif:producer/block_fermenter_elc
---

# 发酵机

<Row>
  <BlockImage id="mio_icif:producer/block_fermenter_elc" scale="3" />
</Row>

## 功能

发酵机利用热量（HU）把生物质转化为沼气。
机器只从正面的热源获取热量。
机器还用处理过的生物质产出肥料。

## 电力数据

该方块不使用EU。

## 使用步骤

1. 把热能发生器贴在发酵机正面。
2. 用生物质单元或管道供应生物质。
3. 把空单元放入单元输入槽位，以收集沼气。
4. 从肥料槽位取出肥料。

## 注意

- 红石信号对发酵机没有作用。
- 升级槽位只接受物品和流体的弹出与抽入升级。
- 发酵机只在能够运行时吸收热量：输入罐中有一次运行所需的生物质（20 mB），且沼气罐有一次运行的空间（400 mB）。每 tick 最多吸收 100 HU。热量条显示本次运行已获得的热量（4000 HU）。

## 热源摆放

拖动视图可旋转。下方列表给出所需方块及其规则。JEI 的“多方块结构”分类中也有相同的建造图。

<GameScene zoom="5" interactive={true}>
  <SiStructure id="mio_icif:fermenter" />
</GameScene>

<SiStructureInfo id="mio_icif:fermenter" />

## 配方

<RecipesFor id="mio_icif:producer/block_fermenter_elc" fallbackText="-" />
