---
navigation:
  title: "光子振谐太阳能发电机"
  icon: mio_icif:generator/block_photon_resonance_solar_generator
  parent: generators.md
  position: 22
item_ids:
  - mio_icif:generator/block_photon_resonance_solar_generator
---

# 光子振谐太阳能发电机

<Row>
  <BlockImage id="mio_icif:generator/block_photon_resonance_solar_generator" scale="3" />
</Row>

## 功能

机器是光子振谐板：板面的纳米谐振结构吸收很宽的光谱，散射光也有效。弱光下就能满功率（辐照达到 450 W/m²）。
被遮住时仍有 45% 的天光；下雨约剩 85%，雷暴约剩 61%。
板温影响很小：25 °C 以上每升 1 °C 只少 0.2%，最多少 15%。
夜里正上方露天时，机器把自身的热量辐射给寒冷的晴空并从中发电，输出为额定的 25%（96 EU/t）。下雨减半，雷暴只剩四分之一。
界面显示今日记录：照到板上的阳光、晴天的对比曲线和输出，以及夜空系数。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | HV (512 EU) |
| 输出 | 384 EU/t |
| 夜间输出 | 96 EU/t |
| 满功率辐照 | 450 W/m² |
| 储能 | 40,000,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把发电机放在露天位置；雨多的地方或被遮挡的地方也能用。
2. 把 HV 导线连接到发电机。
3. 把电池放入充电槽位。

## 配方

<RecipesFor id="mio_icif:generator/block_photon_resonance_solar_generator" fallbackText="-" />
