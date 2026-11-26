---
navigation:
  title: "TESU 无线磁转端口"
  icon: mio_icif:wiring/block_tesu_induction_port
  parent: power.md
  position: 20
item_ids:
  - mio_icif:wiring/block_tesu_induction_port
---

# TESU 无线磁转端口

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_induction_port" scale="3" />
</Row>

## 功能

装在已成型 TESU 的端口位上时，它有 [TESU 无线输出端口](wiring_block_tesu_wireless_port.md) 的全部功能（无线范围的中心、中继器可以连它、装了无线升级的机器会被充满），另外在自身 ±16 格内：

* 所有玩家都会被充电：护甲、手持和背包里的电动物品每 tick 都尽量充满；
* 所有用电的机器每 tick 都会被充满，**不需要**装无线升级，无视电压等级，端口会向每台机器发出粒子射线。发电机、储电设备、电线和节点不在其列。

电从 TESU 控制器里扣，端口本身不存电。

## 用法

1. 把端口装在 TESU 底圈的端口位上。
2. 右键端口可以查看它是否接入了成型的 TESU。

## 配方

<RecipesFor id="mio_icif:wiring/block_tesu_induction_port" fallbackText="-" />
