---
navigation:
  title: "反应堆冷却液注入器(RSH)"
  icon: mio_icif:producer/block_redstone_reactor_coolant_injector
  parent: machines.md
  position: 55
item_ids:
  - mio_icif:producer/block_redstone_reactor_coolant_injector
---

# 反应堆冷却液注入器(RSH)

<Row>
  <BlockImage id="mio_icif:producer/block_redstone_reactor_coolant_injector" scale="3" />
</Row>

## 功能

反应堆冷却液注入器（RSH）修复核反应堆中的红石冷凝模块。每次修复消耗1个红石块和EU。
正面必须接触反应堆或反应堆仓室。机器有存放红石块的储存槽位。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 10,000 EU |
| 工作耗电 | 1,000 EU/t |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置注入器，使正面贴住反应堆或反应堆仓室。
2. 将MV导线连接到机器。
3. 在储存槽位中放满红石块。
4. 定期检查红石块的库存。

## 注意

- 库存用完时，反应堆中的冷凝模块会耗尽。

> **警告：** 电压高于MV时，机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:producer/block_redstone_reactor_coolant_injector" fallbackText="-" />
