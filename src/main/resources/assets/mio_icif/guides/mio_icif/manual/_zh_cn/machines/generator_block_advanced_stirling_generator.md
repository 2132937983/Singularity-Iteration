---
navigation:
  title: "进阶斯特林发电机"
  icon: mio_icif:generator/block_advanced_stirling_generator
  parent: generators.md
  position: 4
item_ids:
  - mio_icif:generator/block_advanced_stirling_generator
---

# 进阶斯特林发电机

<Row>
  <BlockImage id="mio_icif:generator/block_advanced_stirling_generator" scale="3" />
</Row>

## 功能

机器把热能（HU）转换为 EU，转换比高于斯特林发电机。
机器从正面朝向自身的相邻加热机抽取热能。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | MV (128 EU) |
| 储能 | 1,000,000 EU |
| 转换 | 100 HU → 85 EU |
| 热量输入 | 最多 2,000 HU/t |

发电倍率（配置 GeneratorOutput）不为 1.0 时，EU 数值会改变。

## 使用步骤

1. 在机器旁边放置一个或多个加热机。
2. 让每个加热机的正面朝向机器。
3. 把 MV 导线连接到机器。

## 配方

<RecipesFor id="mio_icif:generator/block_advanced_stirling_generator" fallbackText="-" />
