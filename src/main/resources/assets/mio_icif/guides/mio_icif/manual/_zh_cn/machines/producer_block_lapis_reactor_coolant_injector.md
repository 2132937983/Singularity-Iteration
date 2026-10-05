---
navigation:
  title: "反应堆冷却液注入器(LZH)"
  icon: mio_icif:producer/block_lapis_reactor_coolant_injector
  parent: machines.md
  position: 54
item_ids:
  - mio_icif:producer/block_lapis_reactor_coolant_injector
---

# 反应堆冷却液注入器(LZH)

<Row>
  <BlockImage id="mio_icif:producer/block_lapis_reactor_coolant_injector" scale="3" />
</Row>

## 功能

反应堆冷却液注入器（LZH）修复核反应堆中的青金石冷凝模块。每次修复消耗1个青金石块和EU。
正面必须接触反应堆或反应堆仓室。机器有存放青金石块的储存槽位。

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
3. 在储存槽位中放满青金石块。
4. 定期检查青金石块的库存。

## 注意

- 库存用完时，反应堆中的冷凝模块会耗尽。

> **警告：** 电压高于MV时，机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:producer/block_lapis_reactor_coolant_injector" fallbackText="-" />
