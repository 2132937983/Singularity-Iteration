---
navigation:
  title: "收割机"
  icon: mio_icif:producer/block_harvest_elc
  parent: machines.md
  position: 30
item_ids:
  - mio_icif:producer/block_harvest_elc
---

# 收割机

<Row>
  <BlockImage id="mio_icif:producer/block_harvest_elc" scale="3" />
</Row>

## 功能

收割机收获周围9×3×9范围内作物架上的成熟作物。
机器把产物放入储存槽位。
装有作物分析仪时，机器只在最佳尺寸收割。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 32 EU/t |
| 储能 | 10,000 EU |
| 工作耗电 | 1 EU/t |
| 单次工作时间 | 10 tick (0.5 s) |
| 单次耗电 | 10 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把机器放在农田中央。
2. 将LV导线连接到机器。
3. 放入作物分析仪，使机器在最佳尺寸收割。
4. 从储存槽位取出作物。

## 注意

- 作物分析仪会增加每次收割的EU消耗。
- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于LV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_harvest_elc" fallbackText="-" />
