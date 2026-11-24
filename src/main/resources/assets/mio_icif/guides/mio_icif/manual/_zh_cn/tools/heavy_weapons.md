---
navigation:
  title: "重武器"
  icon: mio_icif:item_tool_rocket_launcher
  parent: tools.md
  position: 6
item_ids:
  - mio_icif:item_tool_rocket_launcher
  - mio_icif:item_rocket
  - mio_icif:item_tool_plasma_launcher
  - mio_icif:item_tool_plasma_air_cannon
---

# 重武器

## 火箭发射器

<ItemLink id="mio_icif:item_tool_rocket_launcher" /> 每发消耗一枚 <ItemLink id="mio_icif:item_rocket" /> 和 20,000 EU，1 秒一发。火箭有重力，爆炸威力 3（中心约 43 伤害）。创造模式不扣火箭。

## 等离子发射器

<ItemLink id="mio_icif:item_tool_plasma_launcher" /> 用来开路和清场。每发 2,000 EU，1 秒一发，满电 20 发。

- 弹体沿途熔炼可熔炼的方块、清除水和岩浆、点燃可燃方块。
- 命中时产生热爆炸：只破坏抗性低的方块；对实体中心伤害约 24，向外衰减。射手不受伤。

## 等离子空气炮

<ItemLink id="mio_icif:item_tool_plasma_air_cannon" /> 按住蓄力，松开发射一道锥形气浪（半角 15°），主要用来击退。伤害 4–10 随蓄力增加，远端减半。每发 1,000 EU。不打驯服的动物、村民、傀儡和同队玩家。

## 地形破坏

服务器配置 `Weapons.weaponsDamageTerrain`（默认开）控制电力等离子枪、火箭、等离子发射器是否破坏方块。关闭时它们只伤害实体。领地保护始终生效。
