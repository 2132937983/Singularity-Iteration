---
navigation:
  title: "热能离心机"
  icon: mio_icif:producer/block_centrifuge_elc
  parent: machines.md
  position: 67
item_ids:
  - mio_icif:producer/block_centrifuge_elc
---

# 热能离心机

<Row>
  <BlockImage id="mio_icif:producer/block_centrifuge_elc" scale="3" />
</Row>

## 功能

热能离心机消耗EU升温，然后把物品分离为最多3种产物。
机器达到最低热量后才开始加工。断电时热量下降。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 24,000 EU |
| 工作耗电 | 48 EU/t |
| 单次工作时间 | 500 tick (25 s) |
| 单次耗电 | 24,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将MV导线连接到机器。
2. 把粉碎矿石放入输入槽位。
3. 等待机器达到工作热量。
4. 从3个输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于MV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_centrifuge_elc" fallbackText="-" />
