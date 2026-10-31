---
navigation:
  title: "流体加热机"
  icon: mio_icif:hugenerator/block_fluid_heat_generator
  parent: generators.md
  position: 10
item_ids:
  - mio_icif:hugenerator/block_fluid_heat_generator
---

# 流体加热机

<Row>
  <BlockImage id="mio_icif:hugenerator/block_fluid_heat_generator" scale="3" />
</Row>

## 功能

机器燃烧半流质燃料并产生热能（HU）。
机器接受沼气、生物质、原油和柴油。
机器只从正面输出热能。

## 电力数据

| 项目 | 数值 |
|---|---|
| 燃料：沼气 | 0.5 mB/t → 32 HU/t (64 HU/mB) |
| 燃料：生物质 | 1 mB/t → 16 HU/t (16 HU/mB) |
| 燃料：原油 | 0.5 mB/t → 32 HU/t (64 HU/mB) |
| 燃料：柴油 | 0.25 mB/t → 768 HU/t (3,072 HU/mB) |
| 燃料箱 | 10,000 mB |

该方块不使用EU。

## 使用步骤

1. 放置机器，使其正面对着用热机器。
2. 把燃料桶或燃料单元放入输入槽位。
3. 从输出槽位取出空容器。

## 配方

<RecipesFor id="mio_icif:hugenerator/block_fluid_heat_generator" fallbackText="-" />
