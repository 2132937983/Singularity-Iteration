---
navigation:
  title: "物质生成机"
  icon: mio_icif:producer/block_matter_elc
  parent: machines.md
  position: 42
item_ids:
  - mio_icif:producer/block_matter_elc
---

# 物质生成机

<Row>
  <BlockImage id="mio_icif:producer/block_matter_elc" scale="3" />
</Row>

## 功能

物质生成机把大量EU转化为UU物质。
放入增幅槽位的废料、废料盒或钍废料盒可降低EU消耗。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | EV (2,048 EU) |
| 最大输入 | 8,192 EU/t |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 通过变压器将EV电源连接到机器。
2. 把废料放入增幅槽位。
3. 把空单元放入容器槽位收集UU物质，或连接管道。
4. 从输出槽位取出UU物质单元。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于EV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_matter_elc" fallbackText="-" />
