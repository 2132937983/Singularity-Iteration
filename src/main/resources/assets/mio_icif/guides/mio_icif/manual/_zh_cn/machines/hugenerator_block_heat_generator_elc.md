---
navigation:
  title: "电力加热机"
  icon: mio_icif:hugenerator/block_heat_generator_elc
  parent: generators.md
  position: 7
item_ids:
  - mio_icif:hugenerator/block_heat_generator_elc
---

# 电力加热机

<Row>
  <BlockImage id="mio_icif:hugenerator/block_heat_generator_elc" scale="3" />
</Row>

## 功能

机器把 EU 转换为热能（HU）。
线圈槽位中的每个线圈都使产热量增加。
机器只从正面输出热能。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | EV (2,048 EU) |
| 最大输入 | 2,048 EU/t |
| 储能 | 10,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置机器，使其正面对着用热机器。
2. 把线圈放入线圈槽位。
3. 把导线连接到机器来供应 EU。
4. 把已充电的电池放入电池槽位作为备用电源。

## 注意

- 没有线圈时，机器不产生热能。

## 配方

<RecipesFor id="mio_icif:hugenerator/block_heat_generator_elc" fallbackText="-" />
