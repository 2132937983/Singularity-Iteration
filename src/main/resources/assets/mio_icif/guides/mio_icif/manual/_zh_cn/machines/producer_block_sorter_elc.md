---
navigation:
  title: "电动分拣机"
  icon: mio_icif:producer/block_sorter_elc
  parent: machines.md
  position: 22
item_ids:
  - mio_icif:producer/block_sorter_elc
---

# 电动分拣机

<Row>
  <BlockImage id="mio_icif:producer/block_sorter_elc" scale="3" />
</Row>

## 功能

电动分拣机按过滤设置把物品送往6个面。每个面有独立的过滤槽位。
不匹配任何过滤的物品送往默认输出面。每移动一个物品消耗EU。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 15,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将MV导线连接到机器。
2. 在输出面旁放置容器。
3. 在界面中把样品物品放入各面的过滤槽位。
4. 在界面中选择默认输出面。
5. 把物品送入分拣机。

## 注意

- 过滤槽位中的堆叠数量决定该过滤的每批数量。

> **警告：** 电压高于MV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_sorter_elc" fallbackText="-" />
