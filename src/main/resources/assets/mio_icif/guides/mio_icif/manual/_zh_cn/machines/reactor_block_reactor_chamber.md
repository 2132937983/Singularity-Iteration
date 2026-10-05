---
navigation:
  title: "核反应仓"
  icon: mio_icif:reactor/block_reactor_chamber
  parent: heavy.md
  position: 11
item_ids:
  - mio_icif:reactor/block_reactor_chamber
---

# 核反应仓

<Row>
  <BlockImage id="mio_icif:reactor/block_reactor_chamber" scale="3" />
</Row>

## 功能

核反应仓为相邻的核反应堆发电机增加一列元件槽位。
反应仓共享反应堆的物品栏、EU输出和热量。反应仓收到红石信号时，反应堆也会启动。

## 电力数据

该方块不使用EU。

## 使用步骤

1. 放置核反应堆发电机。
2. 在反应堆的六个面上放置最多六个反应仓。
3. 右键反应仓，打开反应堆界面。
4. 在反应仓上连接导线，输出EU。

## 注意

- 反应仓必须只接触一个反应堆。否则反应仓会破坏并掉落为物品。

> **警告：** 反应仓不增加热容量。每次更换元件后，检查反应堆的堆温。


## 配方

<RecipesFor id="mio_icif:reactor/block_reactor_chamber" fallbackText="-" />
