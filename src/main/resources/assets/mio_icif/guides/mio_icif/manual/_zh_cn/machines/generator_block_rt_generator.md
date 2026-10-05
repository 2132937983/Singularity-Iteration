---
navigation:
  title: "放射性同位素温差发电机"
  icon: mio_icif:generator/block_rt_generator
  parent: generators.md
  position: 26
item_ids:
  - mio_icif:generator/block_rt_generator
---

# 放射性同位素温差发电机

<Row>
  <BlockImage id="mio_icif:generator/block_rt_generator" scale="3" />
</Row>

## 功能

机器利用放射性同位素燃料靶丸产生 EU。
每增加一个靶丸，发电量翻倍。靶丸不会耗尽。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | LV (32 EU) |
| 输出 | 32 EU/t |
| 储能 | 20,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把 LV 导线连接到发电机。
2. 把最多六个燃料靶丸放入靶丸槽位。

## 配方

<RecipesFor id="mio_icif:generator/block_rt_generator" fallbackText="-" />
