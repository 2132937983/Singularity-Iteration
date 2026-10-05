---
navigation:
  title: "传送机"
  icon: mio_icif:producer/block_teleporter_elc
  parent: machines.md
  position: 64
item_ids:
  - mio_icif:producer/block_teleporter_elc
---

# 传送机

<Row>
  <BlockImage id="mio_icif:producer/block_teleporter_elc" scale="3" />
</Row>

## 功能

传送机把站在其上的实体传送到配对的传送机。
每次传送消耗EU。距离越远，消耗越多。
传送机没有界面。机器只在收到红石信号时工作。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | HV (512 EU) |
| 最大输入 | 8,192 EU/t |
| 储能 | 100,000,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置两台传送机，并分别连接HV导线。
2. 用遥控器右键第一台传送机。
3. 右键第二台传送机，完成配对。
4. 向传送机施加红石信号。
5. 站在传送机上，即可到达配对的传送机。

## 注意

- 配对是双向的。

> **警告：** 电压高于HV时，机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:producer/block_teleporter_elc" fallbackText="-" />
