---
navigation:
  title: "TESU 输出端口"
  icon: mio_icif:wiring/block_tesu_output
  parent: power.md
  position: 19
item_ids:
  - mio_icif:wiring/block_tesu_output
---

# TESU 输出端口

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_output" scale="3" />
</Row>

## 功能

TESU 输出端口按设定的输出功率送出控制器里的电。右键打开界面（参照创造模式发电机）：
- 输入框可以填 0 到 2,147,483,647 之间的任意 EU/t（可用 k、M、G 后缀），点“设定”或按回车生效；
- 十个按钮分别设成 IV 到 MAX 各电压的满包，x10、/10 按比例调整，“开/关”可以随时停止输出；
- 上方显示当前输出、电压、上一 tick 实际送出的电量和 TESU 的储电量；没接入成型的 TESU 时显示红字。

电压随输出自动选择：取能装下这个输出的最低电压（最低 IV），所以小功率输出不会用不必要的高电压。
它保留两个包，每 tick 从控制器补满，每 tick 送出一包（一包就是设定的输出）。它要装在 TESU 底圈的端口位上才工作。旧存档里的端口保持原来的电压满包输出。

## 电力数据

| 电压 | 满包 EU（= 最大 EU/t） |
|---|---|
| IV | 8,192 |
| LuV | 32,768 |
| ZPMV | 131,072 |
| UV | 524,288 |
| UHV | 2,097,152 |
| UEV | 8,388,608 |
| UIV | 33,554,432 |
| UXV | 134,217,728 |
| OpV | 536,870,912 |
| MAX | 2,147,483,647 |

## 配方

<RecipesFor id="mio_icif:wiring/block_tesu_output" fallbackText="-" />
