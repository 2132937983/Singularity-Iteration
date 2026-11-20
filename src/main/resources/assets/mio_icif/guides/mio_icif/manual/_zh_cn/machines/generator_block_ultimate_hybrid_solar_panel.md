---
navigation:
  title: "终极混合太阳能发电机"
  icon: mio_icif:generator/block_ultimate_hybrid_solar_panel
  parent: generators.md
  position: 35
item_ids:
  - mio_icif:generator/block_ultimate_hybrid_solar_panel
---

# 终极混合太阳能发电机

<Row>
  <BlockImage id="mio_icif:generator/block_ultimate_hybrid_solar_panel" scale="3" />
</Row>

## 功能

机器是聚光板：透镜把直射阳光聚到电池上产生 EU。白天输出最高，但只在正午前后满功率（辐照达到 850 W/m²）。
机器只用直射光：正上方必须露天，被遮住时完全不发电。下雨约剩 37%，雷暴几乎为零；沙漠等不下雨的地方不受天气影响。
液冷带走四分之三的热：800 W/m² 时电池比气温高约 11 °C。25 °C 以上每升 1 °C 少 0.4%，满功率时也照扣。沙漠正午约为额定的 88%。
夜里不发电。需要夜间供电时，配储电箱，或改用光子振谐太阳能。
界面显示今日记录：照到板上的阳光、晴天的对比曲线和输出，以及直射份额、电池温度和冷却比例。
机器每 tick 以两个 HV 包（各 512 EU）送出 768 EU/t，HV 导线即可。
机器可在四个槽位中为物品充电。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | HV (512 EU) |
| 输出 | 768 EU/t |
| 夜间输出 | 0 EU/t |
| 满功率辐照 | 850 W/m² |
| 储能 | 1,000,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把面板放在阳光直射的露天位置，例如沙漠。
2. 移除面板上方的所有方块。
3. 把 HV 导线连接到面板。
4. 把最多四个电池放入充电槽位。
5. 夜间需要用电时，在电网上接储电箱。

## 配方

<RecipesFor id="mio_icif:generator/block_ultimate_hybrid_solar_panel" fallbackText="-" />
