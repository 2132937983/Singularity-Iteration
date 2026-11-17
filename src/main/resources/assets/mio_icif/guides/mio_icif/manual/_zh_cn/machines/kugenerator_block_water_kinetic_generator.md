---
navigation:
  title: "水力动能发生机"
  icon: mio_icif:kugenerator/block_water_kinetic_generator
  parent: generators.md
  position: 38
item_ids:
  - mio_icif:kugenerator/block_water_kinetic_generator
---

# 水力动能发生机

<Row>
  <BlockImage id="mio_icif:kugenerator/block_water_kinetic_generator" scale="3" />
</Row>

## 功能

机器通过转子把水流转换为动能（KU）。
正面那一格必须是水。机器只从背面输出 KU。
转子在正面那一格为中心、与朝向垂直的一个圆盘里转动，直径等于转子直径。输出是流过这个圆盘的水流功率：随流速的三次方增长，而且只算盘面里是水的格，所以要把转子完全沉入水中。
- 河流群系：稳定水流，下雨时功率约 +30%，雷暴约 +60%。
- 海洋和沙滩群系：潮流，一天转向两次（第 0、6,000、12,000、18,000 tick 为转潮）。满月和新月是大潮（功率 1.3 倍），上下弦是小潮（0.7 倍）。
- 任何地方的流动水（流水渠）沿流向运动，按沿转子轴向的分量计算；下落的水也算。
- 盘面全是水、平均水流时给出 (直径 − 3) × 25 + 100 KU/t（海洋为 +150），即以前的平均值。
- 前方 16 格内同一直线、同一朝向的水力转子会让水变慢（尾流）。
界面显示从上游看过去的盘面（流动的水、静水、空气、实心方块）、流速、盘面里水的比例，以及一天的水流曲线和当前时刻。

## 电力数据

该方块不使用EU。

## 使用步骤

1. 把转子放入转子槽位。
2. 把机器放在河流或海洋中，使正面浸在水中。
3. 放置动能发电机，使其正面对着机器背面。

## 注意

- 河流和海洋以外的静水不流动，转子在那里不转。
- 转子在运行中持续损耗。在转子损坏前更换它。

## 配方

<RecipesFor id="mio_icif:kugenerator/block_water_kinetic_generator" fallbackText="-" />
