---
navigation:
  title: "喷射管道"
  icon: mio_icif:pipe/block_pipe_ejector
  parent: machines.md
  position: 12
item_ids:
  - mio_icif:pipe/block_pipe_ejector
---

# 喷射管道

<Row>
  <BlockImage id="mio_icif:pipe/block_pipe_ejector" scale="3" />
</Row>

## 功能

把管道线路或容器中的物品和流体喷出到世界中。
喷嘴朝向远离放置所依附方块的方向。

## 规则

* 管道和机器可以从喷嘴以外的所有面送入物品和流体。
* 内部为空时，从背后的容器取出最多 16 个物品或 1 桶流体。
* 每 8 tick 把物品从喷嘴高速射出。
* 每秒一次：若有满 1 桶，在喷嘴前方放置该流体的源方块。位置被占用时等待，不会删除流体。
* 红石信号使其停止。

**注意：** 喷出的流体会像倒桶一样扩散。请检查喷嘴前方的区域。

## 配方

<RecipesFor id="mio_icif:pipe/block_pipe_ejector" fallbackText="-" />
