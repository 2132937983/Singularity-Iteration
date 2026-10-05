---
navigation:
  title: "感应炉"
  icon: mio_icif:producer/block_induction_elc
  parent: machines.md
  position: 31
item_ids:
  - mio_icif:producer/block_induction_elc
---

# 感应炉

<Row>
  <BlockImage id="mio_icif:producer/block_induction_elc" scale="3" />
</Row>

## 功能

感应炉消耗EU，按熔炉配方同时冶炼两个物品。
机器在运行中升温。热量越高，速度越快。断电时热量下降。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 10,000 EU |
| 工作耗电 | 15 EU/t |
| 单次工作时间 | 4,000 tick (200 s) |
| 单次耗电 | 60,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将MV导线连接到机器。
2. 把要冶炼的物品放入2个输入槽位。
3. 让机器持续工作，以保持热量。
4. 从2个输出槽位取出产物。

## 注意

- 红石信号使机器保持高温，并暂停加工。

> **警告：** 电压高于MV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_induction_elc" fallbackText="-" />
