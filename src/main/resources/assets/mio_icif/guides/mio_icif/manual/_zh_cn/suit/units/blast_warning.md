---
navigation:
  title: "爆炸预警计时器"
  icon: mio_icif:module/item_module_blast_warning
  parent: suit/index.md
  position: 14
item_ids:
  - mio_icif:module/item_module_blast_warning
---

# 爆炸预警计时器

<ItemImage id="mio_icif:module/item_module_blast_warning" scale="3" />

## 功能

该单元测量28格内（核弹最远100格）爆炸物的引信，检测对象见“检测的爆炸物”。
每个爆炸物上方浮现圆形图标。图标的圆环随引信燃烧而减少，标签显示剩余秒数。
地面区域显示爆炸半径。区域随引信燃烧而填满。
爆炸物靠近你时，瞄准点周围出现带箭头的图标并指向爆炸物。蜂鸣声随引信燃烧而加快。
在爆炸半径内时，视野边缘闪烁红色。

## 检测的爆炸物

| 爆炸物 | 半径 | 计时 |
|---|---|---|
| 已点燃 TNT，以及基于已点燃 TNT 的其他模组 TNT | 4 | 引信 |
| TNT 矿车、膨胀苦力怕（包括其他模组的苦力怕） | 4 / 3（闪电 6） | 引信 / 膨胀 |
| 工业 TNT（ITNT） | 8 | 引信 |
| 核弹 | 40 | 引信（30 秒） |
| 炸药 / 粘性炸药 | 3 / 2 | 引信；遥控粘性炸药显示“--” |
| 火箭弹、恶魂火球、凋灵之首、末影龙火球 | 5 / 2 / 2 / 3 | 命中前时间（仅在朝你飞来时） |
| 其他模组：ID 含爆炸类词（tnt, nuke, bomb, dynamite, grenade, explosive, missile, warhead, landmine, c4, primed, rocket）的实体 | 4（核类词 24） | 其引信方法，否则“--” |

- 数据包可定义任意爆炸物：实体类型数据映射 mio_icif:blast_warning（半径格数、引信秒数、标签），或实体类型标签 mio_icif:blast_warning（半径 4）。
- “--”表示没有计时的爆炸物，区域与图标标出其位置。

## 电力数据

| 项目 | 数值 |
|---|---|
| 适用部位 | 胸甲 / 头盔 |
| 功耗 | 2 EU/t（开启时） |
| 显示 | 需要量子头盔面罩 |

## 使用步骤

1. 把单元装入量子胸甲或头盔。
2. 注视瞄准点周围的图标。
3. 朝箭头相反的方向离开。
4. 爆炸前不要进入地面区域。

## 注意

- 最后一秒图标与区域变为红色。

## 配方

<RecipesFor id="mio_icif:module/item_module_blast_warning" fallbackText="-" />
