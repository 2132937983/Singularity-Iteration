---
navigation:
  title: "纳米套装单元"
  icon: mio_icif:armor/item_armor_nano_chestplate
  parent: suit/index.md
  position: 1
item_ids:
  - mio_icif:module/item_module_myofiber
  - mio_icif:module/item_module_thermoregulator
  - mio_icif:module/item_module_emp_emitter
  - mio_icif:module/item_module_optical_camo
  - mio_icif:module/item_module_kinetic_mesh
  - mio_icif:module/item_module_recon_cloud
  - mio_icif:module/item_module_jammer
  - mio_icif:module/item_module_thermal_camo
  - mio_icif:module/item_module_gecko_setae
  - mio_icif:module/item_module_shock_gel
  - mio_icif:module/item_module_heat_sink_fins
  - mio_icif:module/item_module_micro_capacitor
  - mio_icif:module/item_module_solar_skin
  - mio_icif:module/item_module_toxin_filter
  - mio_icif:module/item_module_self_repair
  - mio_icif:module/item_module_hydrophobic
  - mio_icif:module/item_module_kinetic_generator
  - mio_icif:module/item_nano_lattice_densifier
---

# 纳米套装单元

纳米套装可以安装升级单元。纳米单元很轻：满载的纳米套装约耗 10 EU/t。
强力效果都是爆发：爆发消耗纳米储备并增加热量。

## 纳米核心

- **储备**（0 至 100）：爆发会消耗它。穿戴装有单元的纳米部件时每秒回复 2 点，每点从套装扣除 400 EU。
- **热量**（0 至 100，散热鳍片可提高上限）：每次爆发都会增加热量。每秒散热 4 点。
- **过热**：达到上限时过热。热量降到上限的 40% 以下之前不能爆发；没有鳍片时移动速度也降低 20%。

## 爆发键

按 **V**（主）或 **B**（副）。在奇点管理面板中为每个键分配单元。“自动”顺序：动能冲刺、光学迷彩、动能装甲网、EMP、纳米侦察云。
动能装甲网与光学迷彩不能同时分配到两个键上，也不会同时运行。

## 单元

| 单元 | 部位 | LP | 耗电 | 爆发 | 类别 |
|---|---|---|---|---|---|
| <ItemLink id="mio_icif:module/item_module_myofiber" /> | 护腿 | 2 | 3 EU/t | Dash | 纳米 |
| <ItemLink id="mio_icif:module/item_module_thermoregulator" /> | 胸甲 | 2 | 2 EU/t | - | 纳米 |
| <ItemLink id="mio_icif:module/item_module_emp_emitter" /> | 胸甲 | 3 | - | EMP | 纳米 |
| <ItemLink id="mio_icif:module/item_module_optical_camo" /> | 胸甲 | 3 | - | Camo | 纳米 |
| <ItemLink id="mio_icif:module/item_module_kinetic_mesh" /> | 胸甲 | 2 | - | Mesh | 纳米 |
| <ItemLink id="mio_icif:module/item_module_recon_cloud" /> | 头盔 | 2 | - | Recon | 纳米 |
| <ItemLink id="mio_icif:module/item_module_jammer" /> | 头盔 | 2 | 4 EU/t | - | 纳米 |
| <ItemLink id="mio_icif:module/item_module_thermal_camo" /> | 胸甲 | 2 | 2 EU/t | - | 纳米 |
| <ItemLink id="mio_icif:module/item_module_gecko_setae" /> | 靴子 | 2 | 1 EU/t | - | 纳米 |
| <ItemLink id="mio_icif:module/item_module_shock_gel" /> | 靴子 | 1 | - | - | 纳米 |
| <ItemLink id="mio_icif:module/item_module_heat_sink_fins" /> | 胸 / 腿 | 1 | - | - | 纳米 |
| <ItemLink id="mio_icif:module/item_module_micro_capacitor" /> | 任意 | 1 | - | - | 纳米 |
| <ItemLink id="mio_icif:module/item_module_solar_skin" /> | 头 / 胸 | 1 | +2 EU/t | - | 纳米 |
| <ItemLink id="mio_icif:module/item_module_toxin_filter" /> | 头盔 | 1 | 1 EU/t | - | 共用 |
| <ItemLink id="mio_icif:module/item_module_self_repair" /> | 任意 | 1 | 2000 EU / heal | - | 共用 |
| <ItemLink id="mio_icif:module/item_module_hydrophobic" /> | 胸 / 腿 | 1 | - | - | 共用 |
| <ItemLink id="mio_icif:module/item_module_kinetic_generator" /> | 护腿 | 1 | +1 EU / block | - | 共用 |
| <ItemLink id="mio_icif:module/item_module_threat_sensor" /> | 胸甲 | 1 | 6 EU/t | - | 共用 |
| <ItemLink id="mio_icif:module/item_module_blast_warning" /> | 头 / 胸 | 1 | 2 EU/t | - | 共用 |

## Mk.II

每个为纳米晶格设计的单元都有 Mk.II：用 Mk.I 单元、生体电路、4 块中子板与 3 个同系材料合成。
Mk.II 效果更强（提示中有说明），耗电减少 20%，占用的 LP 相同。
升级时把部件放入改装台，在单元槽中放入 1 个 Mk.II。改装台替换 Mk.I 并将其返还。

## 高频同步

主手中开启的纳米剑会与纳米肌纤维和动能装甲网的织物同步。每个 Mk.I 来源 1 级，Mk.II 来源 2 级，最多 3 级。
1 级：攻击速度 +12%，护甲穿透 15%。2 级：+15%，22%。3 级：+18%，30%。等级越高刀刃颤动越强。

## 纳米改造为量子

用纳米部件合成量子部件时，共用单元会转移到量子部件上；纳米专用单元与晶格致密剂作为物品返还。

## 服务器设置

通用配置的 NanoSuit 节包含每个单元的开关、PvP 开关（默认全部开启）以及数值。

<ItemLink id="mio_icif:producer/block_quantum_modification_station" />
