---
navigation:
  title: "区块加载器"
  icon: mio_icif:producer/block_chunk_loader
  parent: machines.md
  position: 16
item_ids:
  - mio_icif:producer/block_chunk_loader
---

# 区块加载器

<Row>
  <BlockImage id="mio_icif:producer/block_chunk_loader" scale="3" />
</Row>

## 功能

区块加载器使选定的区块保持加载。机器在周围9×9区块范围内最多加载25个区块。
每个加载的区块消耗EU。EU耗尽时机器停止。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 2,500 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将MV导线连接到机器。
2. 打开界面，点击要加载的区块。
3. 为机器保持稳定的EU供应。

## 注意

- 区块加载器所在的区块也在选择网格中。

> **警告：** 电压高于MV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_chunk_loader" fallbackText="-" />
