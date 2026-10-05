---
navigation:
  title: "方块切割机"
  icon: mio_icif:producer/block_block_cutter
  parent: machines.md
  position: 14
item_ids:
  - mio_icif:producer/block_block_cutter
---

# 方块切割机

<Row>
  <BlockImage id="mio_icif:producer/block_block_cutter" scale="3" />
</Row>

## 功能

方块切割机消耗EU并使用切割刀片切割方块。金属块变为板。原木变为木板。
每个配方要求最低刀片硬度。钻石切割刀片比铁切割刀片更硬。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 43,200 EU |
| 工作耗电 | 48 EU/t |
| 单次工作时间 | 900 tick (45 s) |
| 单次耗电 | 43,200 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将MV导线连接到机器。
2. 把切割刀片放入刀片槽位。
3. 把要切割的方块放入输入槽位。
4. 从输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于MV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_block_cutter" fallbackText="-" />
