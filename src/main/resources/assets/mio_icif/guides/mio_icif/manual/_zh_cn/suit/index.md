---
navigation:
  title: "量子套装升级"
  icon: mio_icif:armor/item_armor_quantum_helmet
  parent: index.md
  position: 5
---

# 量子套装升级

量子套装可以安装升级单元。每个单元增加一项传感或防御功能。
通用套装改装台负责安装和拆卸单元。自 0.1.8.5 起纳米套装也可以安装单元（见“纳米套装单元”）。
每个部件的单元位数量固定：头盔4，胸甲3，护腿2，靴子2。
量子扩展槽可以为每个部件增加一个单元槽（见“量子行动”）。
两个量子单元可以纠缠以获得协同；套装计算机有演算带宽（见“纠缠与协同”）。
装有量子纠缠中继器时，套装可以指挥僚机：指挥轮盘、命令、全息台（见“量子指挥”）。
HUD单元只在穿戴量子头盔时显示数据。头盔就是面罩。
每个已安装单元在装备控制台中增加一个开关。开关打开时单元才耗电。

| 单元 | 适用部位 | 功耗 |
|---|---|---|
| <ItemLink id="mio_icif:module/item_module_ore_scanner" /> | 靴子 / 护腿 | 24 EU/t（开启时） |
| <ItemLink id="mio_icif:module/item_module_grid_telemetry" /> | 护腿 / 头盔 | 4 EU/t（开启时） |
| <ItemLink id="mio_icif:module/item_module_entity_esp" /> | 头盔 | 12 EU/t（开启时） |
| <ItemLink id="mio_icif:module/item_module_ballistic" /> | 头盔 | 4 EU/t（开启时） |
| <ItemLink id="mio_icif:module/item_module_blast_warning" /> | 胸甲 / 头盔 | 2 EU/t（开启时） |
| <ItemLink id="mio_icif:module/item_module_behavior_predictor" /> | 头盔 | 8 EU/t（开启时） |
| <ItemLink id="mio_icif:module/item_module_holomap" /> | 头盔 | 10 EU/t（开启时） |
| <ItemLink id="mio_icif:module/item_module_threat_sensor" /> | 胸甲 | 6 EU/t（开启时） |
| <ItemLink id="mio_icif:module/item_module_deflector" /> | 胸甲 | 每点伤害4000 EU，每次最多12点 |
| <ItemLink id="mio_icif:module/item_module_phase_jump_drive" /> | 护腿 / 靴子 | 行动：量子跃迁（相干度） |
| <ItemLink id="mio_icif:module/item_module_phase_modulator" /> | 胸甲 | 行动：相位偏移（相干度） |
| <ItemLink id="mio_icif:module/item_module_entanglement_relay" /> | 胸甲 / 护腿 | 行动：纠缠齐射（相干度） |
| <ItemLink id="mio_icif:module/item_module_coherence_capacitor" /> | 护腿 / 靴子 | 被动：相干度上限 +50 |
| <ItemLink id="mio_icif:module/item_module_compute_core" /> | 胸甲 / 护腿 | 开启时 6 EU/t；带宽 +8 QB，链接 +1 |

<SubPages id="suit/index.md" icons={true} />
