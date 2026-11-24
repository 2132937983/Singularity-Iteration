---
navigation:
  title: "钻头与采矿镭射枪"
  icon: mio_icif:item_tool_iridium_driller
  parent: tools.md
  position: 1
item_ids:
  - mio_icif:item_tool_iron_driller
  - mio_icif:item_tool_diamond_driller
  - mio_icif:item_tool_iridium_driller
  - mio_icif:item_tool_laser_miner
  - mio_icif:item_tool_bronze_pickaxe
  - mio_icif:item_tool_bronze_axe
  - mio_icif:item_tool_bronze_shovel
  - mio_icif:item_tool_bronze_hoe
---

# 钻头与采矿镭射枪

## 钻头

钻头是电动的镐和锹。有电时挖得快并掉落方块；没电时像空手。

| 钻头 | 掉落等级 | 速度 | 每块耗电 | 容量 | 范围（G 键） | 手持攻击 |
|---|---|---|---|---|---|---|
| <ItemLink id="mio_icif:item_tool_iron_driller" /> | 铁 | 8 | 50 EU | 30,000 EU | 1×1 | 6 |
| <ItemLink id="mio_icif:item_tool_diamond_driller" /> | 钻石 | 16 | 80 EU | 30,000 EU | 1×1、3×3 | 7 |
| <ItemLink id="mio_icif:item_tool_iridium_driller" /> | 下界合金 | 25 | 800 EU | 1,000,000 EU | 1×1、3×3、5×5 | 10 |

- **范围挖掘。**范围内每一块都按玩家自己挖的方式破坏，每块扣一次电。3×3 速度为一半，5×5 为 0.3 倍。比中心方块硬得多的方块（硬度大于中心 × 2 + 1）不挖。俯仰超过 45° 时挖水平面，否则挖竖直面。
- **铱钻头。**潜行 + G 在时运 III 和精准采集之间切换。
- **攻击。**每次命中扣一次电。没电时没有攻击伤害。
- **采矿机。**三种钻头都能装进采矿机。

## 采矿镭射枪

<ItemLink id="mio_icif:item_tool_laser_miner" /> 发射激光挖掘方块。G 键切换模式：采矿、低焦、远距、水平、战斗、超级热线（熔炼掉落）、散射、爆破、3×3、超高能爆破（服务器默认禁用）。每个模式的耗电和射程写在提示里。激光逐块检查领地权限。

## 青铜工具

青铜镐、斧、锹、锄、剑是普通工具，采掘等级同铁，耐久 250，用青铜锭修理，能附所有对应附魔。
