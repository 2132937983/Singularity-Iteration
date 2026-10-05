---
navigation:
  title: "复制机"
  icon: mio_icif:producer/block_replicator_elc
  parent: machines.md
  position: 57
item_ids:
  - mio_icif:producer/block_replicator_elc
---

# 复制机

<Row>
  <BlockImage id="mio_icif:producer/block_replicator_elc" scale="3" />
</Row>

## 功能

复制机消耗UU物质和EU，按存储的模式复制物品。
模式来自存储槽位中的模式存储水晶，或相邻的模式存储机。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | EV (2,048 EU) |
| 最大输入 | 8,192 EU/t |
| 储能 | 2,000,000 EU |
| 工作耗电 | 512 EU/t |
| 单次工作时间 | 100 tick (5 s) |
| 单次耗电 | 51,200 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 通过变压器将EV电源连接到机器。
2. 用UU物质单元或管道供应UU物质。
3. 把模式存储水晶放入存储槽位，或在机器旁放置模式存储机。
4. 在界面中点击单次或循环开始工作。
5. 从输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于EV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_replicator_elc" fallbackText="-" />
