---
navigation:
  title: "创造模式发电机"
  icon: mio_icif:generator/block_creative_generator
  parent: generators.md
  position: 18
item_ids:
  - mio_icif:generator/block_creative_generator
---

# 创造模式发电机

<Row>
  <BlockImage id="mio_icif:generator/block_creative_generator" scale="3" />
</Row>

## 功能

供创造模式使用的调试发电机。它每 tick 向六个面输出设定的功率。
无论数据包多大，它的电能都不会熔断导线、击穿绝缘层、电击玩家，也不会使机器爆炸。

- 只有创造模式的玩家可以放置。没有合成配方，破坏后不掉落任何物品。
- 生存模式下无法挖掘，并且抗爆。
- 只有创造模式的玩家或管理员（权限等级 2）可以打开界面。

## 设置

| 操作 | 效果 |
|---|---|
| 输入框 + 设定（或回车） | 0 至 1,000,000,000,000 EU/t 的任意输出。支持 k、M、G、T 后缀，例如 `2.5M`。 |
| 预设按钮 | 32、128、512、2,048、8,192、32,768、131,072、1M EU/t |
| x10 与 /10 | 输出乘以或除以十 |
| ON / OFF | 开关输出 |

界面还显示上一 tick 离开发电机的 EU。

## 注意

- 机器只接收其剩余容量和自身输入速率允许的电量。
- 导线损耗照常计算。
- 无限发电机保持原样（固定 8,192 EU/t，正常的过载规则）。
