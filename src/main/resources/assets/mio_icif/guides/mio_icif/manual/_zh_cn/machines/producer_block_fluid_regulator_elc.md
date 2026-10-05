---
navigation:
  title: "流体流量调节机"
  icon: mio_icif:producer/block_fluid_regulator_elc
  parent: machines.md
  position: 28
item_ids:
  - mio_icif:producer/block_fluid_regulator_elc
---

# 流体流量调节机

<Row>
  <BlockImage id="mio_icif:producer/block_fluid_regulator_elc" scale="3" />
</Row>

## 功能

流体流量调节机把流体存入储罐，并以设定的速率从正面输出。
界面设置流量和单位，按秒或按刻。每次输送消耗EU。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | EV (2,048 EU) |
| 最大输入 | 512 EU/t |
| 储能 | 10,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将EV导线连接到机器。
2. 把流体泵入调节机，或把装满的容器放入输入槽位。
3. 在正面放置目标储罐或管道。
4. 在界面中设置流量。

## 注意

- 流量为零时，调节机不输送流体。

> **警告：** 电压高于EV时，机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:producer/block_fluid_regulator_elc" fallbackText="-" />
