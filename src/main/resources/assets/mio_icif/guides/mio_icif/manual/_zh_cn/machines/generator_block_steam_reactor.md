---
navigation:
  title: "蒸汽反应堆"
  icon: mio_icif:generator/block_steam_reactor
  parent: generators.md
  position: 30
item_ids:
  - mio_icif:generator/block_steam_reactor
---

# 蒸汽反应堆

<Row>
  <BlockImage id="mio_icif:generator/block_steam_reactor" scale="3" />
</Row>

## 功能

机器燃烧核燃料棒并产生热量，不产生 EU。
机器内的蒸汽散热片把热量转换为蒸汽。散热片越热，蒸汽越多。
每个相邻的蒸汽反应仓增加一列元件槽位。核反应仓不连接。

## 电力数据

该方块不使用EU。

## 使用步骤

1. 把蒸汽反应仓放在反应堆的各个面上。
2. 把燃料棒和蒸汽散热片放入槽位。
3. 把水或蒸馏水泵入反应堆或反应仓。
4. 在反应堆旁放置蒸汽涡轮发电机或流体管道。
5. 向反应堆输入红石信号以启动反应堆。

## 堆温阶段、辐射屏蔽与事故调查

堆温阶段、辐射屏蔽和事故调查与[核反应堆发电机](generator_block_nuclear_reactor_generator.md)相同。

## 注意

- 反应堆把蒸汽推入相邻的流体机器和管道。蒸汽储罐满时，反应堆停止产生蒸汽。

> **警告：** 没有水或蒸汽空间时，热量留在蒸汽散热片中。散热片和堆温会升高。


> **警告：** 堆温达到上限时，反应堆爆炸。


## 结构

拖动视图可旋转。下方列表给出所需方块及其规则。JEI 的“多方块结构”分类中也有相同的建造图；“反应堆布局”分类给出蒸汽散热片的摆放方式。

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:steam_reactor" />
</GameScene>

<SiStructureInfo id="mio_icif:steam_reactor" />

## 配方

<RecipesFor id="mio_icif:generator/block_steam_reactor" fallbackText="-" />
