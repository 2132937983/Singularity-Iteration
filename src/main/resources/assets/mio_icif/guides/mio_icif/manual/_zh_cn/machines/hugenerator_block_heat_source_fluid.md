---
navigation:
  title: "热交换机"
  icon: mio_icif:hugenerator/block_heat_source_fluid
  parent: generators.md
  position: 15
item_ids:
  - mio_icif:hugenerator/block_heat_source_fluid
---

# 热交换机

<Row>
  <BlockImage id="mio_icif:hugenerator/block_heat_source_fluid" scale="3" />
</Row>

## 功能

机器把岩浆或热冷却液转换为热能（HU）。
岩浆变为绳状熔岩。热冷却液变为冷却液。
每个热传导器都使产热量增加。机器只从正面输出热能。

## 电力数据

该方块不使用EU。

## 使用步骤

1. 放置机器，使其正面对着用热机器。
2. 把热传导器放入传导器槽位。
3. 把岩浆桶或热冷却液单元放入热流体输入槽位。
4. 把空容器放入冷流体槽位来收集产物流体。

## 传热面

- 机器只从正面（带铜质传热板的面）输出热量。工作时传热板变红。
- 放置时，传热板朝向你。
- 用热机器的受热面必须贴住传热板：斯特林发电机用背面与正面受热（正面是铜板那一面，即放置时朝向你的那一面），动能斯特林发电机用转轴以外的所有面，蒸汽发生器用正面以外的所有面。
- 在想让传热板朝向的面上使用扳手。潜行使用扳手则转向相反的面。对传热板本身使用扳手会拆下机器。
- 其余 5 个面不输出热量，可用于管道、单元与自动化。

## 注意

- 输出储罐满时，机器停止工作。
- 没有热传导器时，机器不产生热能。

## 配方

<RecipesFor id="mio_icif:hugenerator/block_heat_source_fluid" fallbackText="-" />
