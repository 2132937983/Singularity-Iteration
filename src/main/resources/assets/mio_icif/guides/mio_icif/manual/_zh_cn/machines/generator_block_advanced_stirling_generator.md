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
下表的比例是设计工况：加热头 650 °C、冷却端 40 °C。发动机的效率随加热头与冷却端之间的卡诺效率变化，在设计比例的 40% 到 110% 之间。热流越大加热头越热（额定热流时 650 °C，四分之一热流时约 530 °C）；冷机要暖机几秒。冷却端是所在群系的空气（沙漠比雪原差）；机器旁边接触到水时，冷却端降到约 15 °C。
界面是电厂模拟屏：热量输入、显示加热头和冷却端温度的发动机、飞轮、发电机与 EU 输出。
额定热流：150 HU/t。

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
