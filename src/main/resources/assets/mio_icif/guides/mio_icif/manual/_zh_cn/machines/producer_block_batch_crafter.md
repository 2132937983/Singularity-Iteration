---
navigation:
  title: "批量工作台"
  icon: mio_icif:producer/block_batch_crafter
  parent: machines.md
  position: 12
item_ids:
  - mio_icif:producer/block_batch_crafter
---

# 批量工作台

<Row>
  <BlockImage id="mio_icif:producer/block_batch_crafter" scale="3" />
</Row>

## 功能

批量工作台消耗EU，自动合成工作台配方。
界面保存配方图样。机器从9个材料槽位取用材料。
容器残留物（例如空桶）进入单独的输出槽位。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 20,000 EU |
| 工作耗电 | 2 EU/t |
| 单次工作时间 | 40 tick (2 s) |
| 单次耗电 | 80 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将MV导线连接到机器。
2. 在界面的合成网格中放置配方图样。
3. 把材料放入材料槽位，或用管道输入。
4. 安装超频升级以加快加工。
5. 从输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于MV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_batch_crafter" fallbackText="-" />
