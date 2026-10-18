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
| 每次修复耗电 | 1,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置注入器，使正面贴住反应堆或反应堆仓室。
2. 用MV（或更低）导线连接注入器的空闲面：背面、侧面、顶面或底面。正面以及接触反应堆部件的面不接电。
3. 在储存槽位中放满青金石块。
4. 定期检查青金石块的库存。

## 注意

- 库存用完时，反应堆中的冷凝模块会耗尽。
- 自 0.1.10.4 起，注入器的正面以及接触反应堆、反应仓、压力容器或其接口方块的面不再接受电力。此前反应堆的输出（每个电包最高 8,192 EU）会直接灌入注入器，反应堆一启动注入器就因过压爆炸。
- 注入器不输出红石信号（只提供比较器读数），也不传递热量。

> **警告：** 电压高于MV时，机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:producer/block_lapis_reactor_coolant_injector" fallbackText="-" />
