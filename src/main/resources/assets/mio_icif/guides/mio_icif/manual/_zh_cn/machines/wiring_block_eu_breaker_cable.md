---
navigation:
  title: "EU断路器导线"
  icon: mio_icif:wiring/block_eu_breaker_cable
  parent: power.md
  position: 10
item_ids:
  - mio_icif:wiring/block_eu_breaker_cable
---

# EU断路器导线

<Row>
  <BlockImage id="mio_icif:wiring/block_eu_breaker_cable" scale="3" />
</Row>

## 功能

EU断路器导线是内置断路器的 IV 导线，保护其后方的线路。

- **过载**：超过额定流量后延时断开。2 倍额定流量时经过设定的延时断开，过载越大越快，10 倍额定流量时立即断开。
- **过电压**：数据包超过额定电压时立即断开，在该数据包熔毁后方导线或炸毁机器之前断开。
- **重合闸**：3 秒后自动重新合上。连续第 4 次跳闸后保持断开（闭锁），直到手动复位。
- 红石信号可使其保持断开（与分流导线相同）。

让靠近机器的断路器延时短于干线断路器。发生故障时只有最近的断路器断开。

## 电力数据

此方块不消耗EU。

## 使用步骤

1. 将线路中的一格导线替换为断路器导线。
2. 空手右击打开设置。
3. 设置额定流量（EU/t）、额定电压、延时与自动重合闸，点击“应用”。
4. 闭锁后，在设置界面或能源管理终端的监控面板中点击“复位”。

## 配方

<RecipesFor id="mio_icif:wiring/block_eu_breaker_cable" fallbackText="-" />
