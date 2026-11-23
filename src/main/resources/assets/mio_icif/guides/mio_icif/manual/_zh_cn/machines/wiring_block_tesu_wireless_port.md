---
navigation:
  title: "TESU 无线输出端口"
  icon: mio_icif:wiring/block_tesu_wireless_port
  parent: power.md
  position: 20
item_ids:
  - mio_icif:wiring/block_tesu_wireless_port
---

# TESU 无线输出端口

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_wireless_port" scale="3" />
</Row>

## 功能

装在已成型 TESU 的端口位上时，无线输出端口是一片无线范围的中心：以它为中心 ±16 格的立方体。
范围内升级槽里装了无线升级的机器，每 tick 都由 TESU 充满，无视电压等级（不用电线，也不会过压）。端口本身不存电。用 TESU 中继器可以把范围接得更远。
正在给机器充电时，端口会向每台机器发出一道粒子射线（和无线传输节点的一样）。

## 用法

1. 把端口装在 TESU 底圈的端口位上。
2. 给要充电的机器装上无线升级（有升级槽、会用电的机器都可以）。
3. 右键端口可以查看它是否接入了成型的 TESU。

## 注意

- 机器同时在几片范围里时，从第一个有电的取电。
- 能装变压器升级或储能升级的机器就能装无线升级。

## 配方

<RecipesFor id="mio_icif:wiring/block_tesu_wireless_port" fallbackText="-" />
