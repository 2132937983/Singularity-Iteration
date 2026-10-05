---
navigation:
  title: "电力纳米高炉"
  icon: mio_icif:producer/block_blast_furnace_elc
  parent: machines.md
  position: 21
item_ids:
  - mio_icif:producer/block_blast_furnace_elc
---

# 电力纳米高炉

<Row>
  <BlockImage id="mio_icif:producer/block_blast_furnace_elc" scale="3" />
</Row>

## 功能

电力纳米高炉消耗EU，加工高炉配方。机器不需要热量和压缩空气。
机器只产出主产物。机器不产出炉渣。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | IV (8,192 EU) |
| 最大输入 | 8,192 EU/t |
| 储能 | 320,000 EU |
| 工作耗电 | 8,000 EU/t |
| 单次工作时间 | 40 tick (2 s) |
| 单次耗电 | 320,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 通过变压器将IV电源连接到机器。
2. 把铁放入输入槽位。
3. 安装超频升级以加快加工。
4. 从输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于IV时，机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:producer/block_blast_furnace_elc" fallbackText="-" />
