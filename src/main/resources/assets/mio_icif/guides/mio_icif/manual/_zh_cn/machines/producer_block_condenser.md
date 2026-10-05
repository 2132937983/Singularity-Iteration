---
navigation:
  title: "冷凝机"
  icon: mio_icif:producer/block_condenser
  parent: machines.md
  position: 18
item_ids:
  - mio_icif:producer/block_condenser
---

# 冷凝机

<Row>
  <BlockImage id="mio_icif:producer/block_condenser" scale="3" />
</Row>

## 功能

冷凝机把蒸汽或过热蒸汽变为蒸馏水。
4个散热片槽位中的散热片提高速度。每个散热片消耗EU。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | HV (512 EU) |
| 最大输入 | 512 EU/t |
| 储能 | 10,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把蒸汽泵入冷凝机，或把冷凝机放在蒸汽动能发生机旁边。
2. 把散热片放入散热片槽位，提高速度。
3. 装有散热片时，将HV导线连接到机器。
4. 用管道取出蒸馏水，或把空桶或空单元放入容器槽位。

## 注意

- 没有散热片时，冷凝机不消耗EU。

> **警告：** 电压高于HV时，机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:producer/block_condenser" fallbackText="-" />
