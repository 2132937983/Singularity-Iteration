---
navigation:
  title: "量子改装台"
  icon: mio_icif:producer/block_quantum_modification_station
  parent: suit/index.md
  position: 0
item_ids:
  - mio_icif:producer/block_quantum_modification_station
---

# 量子改装台

<Row>
  <BlockImage id="mio_icif:producer/block_quantum_modification_station" scale="3" />
</Row>

## 功能

量子改装台把升级单元安装到量子套装部件中。每次安装消耗EU。
可用单元：地震矿物扫描仪、电网遥测传感器、生物扫描仪、弹道计算机、爆炸预警计时器。
其他单元：行为预测器、战术3D全息地图、威胁传感器、量子偏转器。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | HV (512 EU) |
| 最大输入 | 512 EU/t |
| 储能 | 40,000 EU |
| 工作耗电 | 64 EU/t |
| 单次工作时间 | 100 tick (5 s) |
| 单次耗电 | 6,400 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把HV导线连接到改装台。
2. 把量子套装部件放入上方槽位。
3. 把升级单元放入下方槽位。
4. 等待改装台完成安装。
5. 点击已安装单元旁的×，拆下单元。
6. 在装备综合控制台中开启该单元。

## 注意

- 头盔可装四个单元，胸甲三个，护腿两个，靴子两个。每个单元只适用于指定部位。
- 传感器单元只能通过量子头盔面罩显示数据。开启的单元消耗所在部件的EU。
- 拆下单元不消耗EU，并返还单元。

## 配方

<RecipesFor id="mio_icif:producer/block_quantum_modification_station" fallbackText="-" />
