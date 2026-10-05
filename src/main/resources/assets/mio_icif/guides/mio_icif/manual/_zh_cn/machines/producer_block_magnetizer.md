---
navigation:
  title: "磁化机"
  icon: mio_icif:producer/block_magnetizer
  parent: machines.md
  position: 41
item_ids:
  - mio_icif:producer/block_magnetizer
---

# 磁化机

<Row>
  <BlockImage id="mio_icif:producer/block_magnetizer" scale="3" />
</Row>

## 功能

磁化机消耗EU，磁化相连的铁栏杆和铁栅栏。
穿铁、金、下界合金或电动靴子的玩家在磁化栏杆内向上移动。
机器磁化其上下各20格以内的栏杆。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 32 EU/t |
| 储能 | 100 EU |
| 工作耗电 | 5 EU/t |
| 单次工作时间 | 100 tick (5 s) |
| 单次耗电 | 500 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 搭建一根竖直的铁栏杆柱。
2. 把磁化机放在柱子最下方方块的旁边。
3. 将LV导线连接到机器。
4. 穿上铁靴子或电动靴子。
5. 走进栏杆后即可向上移动。

## 注意

- 红石信号使机器停止。

> **警告：** 电压高于LV时，机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:producer/block_magnetizer" fallbackText="-" />
