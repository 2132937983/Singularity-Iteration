---
navigation:
  title: "模式扫描机"
  icon: mio_icif:producer/block_scanner_elc
  parent: machines.md
  position: 51
item_ids:
  - mio_icif:producer/block_scanner_elc
---

# 模式扫描机

<Row>
  <BlockImage id="mio_icif:producer/block_scanner_elc" scale="3" />
</Row>

## 功能

模式扫描机消耗EU扫描物品，并记录其模式、UU物质消耗和EU消耗。
扫描完成后，机器消耗该物品。机器把模式存入模式存储水晶或相邻的模式存储机。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | HV (512 EU) |
| 最大输入 | 512 EU/t |
| 储能 | 512,000 EU |
| 工作耗电 | 256 EU/t |
| 单次工作时间 | 3,300 tick (165 s) |
| 单次耗电 | 844,800 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将HV导线连接到机器。
2. 把要扫描的物品放入扫描槽位。
3. 放入空白模式存储水晶，或在旁边放置模式存储机。
4. 扫描完成后，在界面中保存结果。

## 注意

- 存储中已有的物品，机器不会再次扫描。
- 红石信号使机器停止。

> **警告：** 电压高于HV时，机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:producer/block_scanner_elc" fallbackText="-" />
