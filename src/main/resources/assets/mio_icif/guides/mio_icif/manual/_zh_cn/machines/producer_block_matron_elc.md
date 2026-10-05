---
navigation:
  title: "作物监管机"
  icon: mio_icif:producer/block_matron_elc
  parent: machines.md
  position: 19
item_ids:
  - mio_icif:producer/block_matron_elc
---

# 作物监管机

<Row>
  <BlockImage id="mio_icif:producer/block_matron_elc" scale="3" />
</Row>

## 功能

作物监管机照料周围9×3×9范围内的作物架。
机器向每个作物架施加肥料、水和除草剂。

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
3. 把肥料放入肥料槽位。
4. 把作物监管除草剂放入除草剂槽位。
5. 用水单元或管道供水。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于LV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_matron_elc" fallbackText="-" />
