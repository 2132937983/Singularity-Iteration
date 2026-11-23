---
navigation:
  title: "物品缓冲机"
  icon: mio_icif:producer/block_item_buffer_elc
  parent: machines.md
  position: 33
item_ids:
  - mio_icif:producer/block_item_buffer_elc
---

# 物品缓冲机

<Row>
  <BlockImage id="mio_icif:producer/block_item_buffer_elc" scale="3" />
</Row>

## 功能

物品缓冲机把物品存放在两个各24格的区域（界面左右两半）。本方块不消耗EU。
- 顶面和底面可以访问全部48格，放入和取出都可以。
- 侧面放入的物品只进右半区；侧面取出时先取右半区，右半区空了再取左半区。
- 两个升级槽位可放弹出升级和抽入升级，每个升级都作用于全部48格。

## 电力数据

该方块不使用EU。

## 使用步骤

1. 把物品缓冲机放在两个容器之间。
2. 用管道从侧面或顶面输入物品，在下方放漏斗或管道取出（也可以从侧面取出）。
3. 不用管道时，在一个升级槽位放抽入升级、另一个放弹出升级。要指定方向，拿着升级右键一个普通方块（不是机器）对应朝向的那一面；再右键同一面恢复为任意方向。

## 配方

<RecipesFor id="mio_icif:producer/block_item_buffer_elc" fallbackText="-" />
