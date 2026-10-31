---
navigation:
  title: "斯特林发电机"
  icon: mio_icif:generator/block_stirling_generator
  parent: generators.md
  position: 30
item_ids:
  - mio_icif:generator/block_stirling_generator
---

# 斯特林发电机

<Row>
  <BlockImage id="mio_icif:generator/block_stirling_generator" scale="3" />
</Row>

## 功能

机器把热能（HU）转换为 EU。
机器从背面和正面（铜板那一面，即放置时朝向你的那一面）接收热能。两面都有热源时先从背面接收。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | MV (128 EU) |
| 储能 | 200,000 EU |
| 转换 | 2 HU → 1 EU |

发电倍率（配置 GeneratorOutput）不为 1.0 时，EU 数值会改变。

## 使用步骤

1. 放置加热机，使其正面对着斯特林发电机的背面或正面。例如热交换机与斯特林发电机可以铜板对铜板摆放。
2. 把 MV 导线连接到斯特林发电机的其他面（正面也输出 EU，背面不输出）。
3. 为加热机供应燃料或 EU。

## 配方

<RecipesFor id="mio_icif:generator/block_stirling_generator" fallbackText="-" />
