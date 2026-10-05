---
navigation:
  title: "模式存储机"
  icon: mio_icif:producer/block_pattern_storage
  parent: machines.md
  position: 52
item_ids:
  - mio_icif:producer/block_pattern_storage
---

# 模式存储机

<Row>
  <BlockImage id="mio_icif:producer/block_pattern_storage" scale="3" />
</Row>

## 功能

模式存储机最多保存64个用于UU物质复制的物品模式。
机器通过模式存储水晶导入和导出模式。每次操作消耗EU。
相邻的模式扫描机把新模式存到这里。相邻的复制机使用选中的模式。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 100,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把模式存储机放在模式扫描机和复制机之间。
2. 将LV导线连接到机器。
3. 使用界面中的箭头按钮选择模式。
4. 把模式存储水晶放入槽位，导入或导出模式。

## 注意


> **警告：** 电压高于LV时，机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:producer/block_pattern_storage" fallbackText="-" />
