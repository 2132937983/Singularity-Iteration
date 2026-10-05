---
navigation:
  title: "核反应堆发电机"
  icon: mio_icif:generator/block_nuclear_reactor_generator
  parent: generators.md
  position: 21
item_ids:
  - mio_icif:generator/block_nuclear_reactor_generator
---

# 核反应堆发电机

<Row>
  <BlockImage id="mio_icif:generator/block_nuclear_reactor_generator" scale="3" />
</Row>

## 功能

机器燃烧核燃料棒，产生 EU 和热量。
每个相邻的核反应仓都增加一列元件槽位。
组成核反应堆压力容器结构后，机器切换到流体模式并加热冷却液。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | IV (8,192 EU) |
| 输出 | 8,192 EU/t |
| 储能 | 1,000,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把核反应仓贴在反应堆的各个面上。
2. 把燃料棒和冷却元件放入槽位。
3. 把导线连接到反应堆。
4. 向反应堆输入红石信号来启动它。

## 注意


> **警告：** 堆温达到上限时，反应堆爆炸。


> **警告：** 堆温较高时，反应堆会点燃方块并伤害附近的生物。


## 配方

<RecipesFor id="mio_icif:generator/block_nuclear_reactor_generator" fallbackText="-" />
