---
navigation:
  title: "物品提取管道"
  icon: mio_icif:pipe/block_pipe_item_input
  parent: heavy.md
  position: 2
item_ids:
  - mio_icif:pipe/block_pipe_item_input
---

# 物品提取管道

<Row>
  <BlockImage id="mio_icif:pipe/block_pipe_item_input" scale="3" />
</Row>

## 功能

物品提取管道从相邻容器取出物品，并送入相连的物品传输管道。
没有EU时，管道每次移动一个物品。MV导线供给的EU可增加每次移动的数量。

## 电力数据

该方块不使用EU。

## 使用步骤

1. 把物品提取管道放在源箱子旁边。
2. 用物品传输管道连接提取管道和目标容器。
3. 连接MV导线，使每次移动更多物品。
4. 用扳手右键管道的一个分支，关闭该方向。

## 注意

- 提取管道不会向容器放入物品。只有传输管道能接收物品时，提取管道才工作。

## 配方

<RecipesFor id="mio_icif:pipe/block_pipe_item_input" fallbackText="-" />
