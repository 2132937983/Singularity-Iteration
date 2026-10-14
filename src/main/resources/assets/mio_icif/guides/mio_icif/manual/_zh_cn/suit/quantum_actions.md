---
navigation:
  title: "量子行动"
  icon: mio_icif:module/item_module_phase_jump_drive
  parent: suit/index.md
  position: 3
item_ids:
  - mio_icif:module/item_module_phase_jump_drive
  - mio_icif:module/item_module_phase_jump_drive_mk2
  - mio_icif:module/item_module_phase_modulator
  - mio_icif:module/item_module_phase_modulator_mk2
  - mio_icif:module/item_module_coherence_capacitor
  - mio_icif:module/item_module_entanglement_relay
  - mio_icif:module/item_module_deflector_mk2
  - mio_icif:module/item_module_behavior_predictor_mk2
  - mio_icif:module/item_quantum_bay_expander
---

# 量子行动

自 0.1.10.0 起，量子套装拥有主动行动。使用行动键 **V** 与 **B**（与纳米爆发相同的按键）。在奇点管理面板中选择各键的行动：选中一件量子部件，使用“量子核心”下方的两行按键设置。默认情况下按键使用你穿戴的前两个行动。

## 相干度

行动消耗 **相干度**，显示为准星周围的圆环（0 至 100）。最后一次行动 1.5 秒后自动回复，每秒 8 点，消耗套装的 EU。
降到 0 时核心 **退相干**：4 秒内无法使用任何行动，正在运行的力场消失，面罩出现故障效果，传感器刷新减半。

## 行动

| 行动 | 单元 | 相干度 | 冷却 |
|---|---|---|---|
| 量子跃迁 | <ItemLink id="mio_icif:module/item_module_phase_jump_drive" /> | 25 | 每次充能 2.5 秒 |
| 相位偏移 | <ItemLink id="mio_icif:module/item_module_phase_modulator" /> | 35 | 8 秒 |
| 力场展开 | <ItemLink id="mio_icif:module/item_module_deflector_mk2" /> | 15，之后每秒 10 | 消失后 6 秒 |
| 时间扭曲 | <ItemLink id="mio_icif:module/item_module_behavior_predictor_mk2" /> | 60 | 25 秒 |
| 纠缠齐射 | <ItemLink id="mio_icif:module/item_module_entanglement_relay" /> | 20 | 10 秒 |

- **量子跃迁：** 瞬移到视线所指的位置，最远 12 格（Mk.II：20 格，两次充能）。不会穿墙：眼睛、腰部与脚部到落点之间必须畅通，并且落点要容得下身体。跃迁后短时间内不受摔落伤害。
- **相位偏移：** 1 秒内（Mk.II：1.5 秒）生物攻击、投射物与爆炸从你身上穿过。期间不能攻击或挖掘。岩浆、火焰、溺水与虚空仍会造成伤害。Mk.II 结束时释放冲击波。
- **力场展开：** 按一次展开半径 4 格的力场罩，再按一次收起。敌方投射物停在空中，力场消失时落下；火球会在力场上爆开。敌对生物被推出去。
- **时间扭曲：** 4 秒内 10 格范围的敌对生物移动减慢 70%（首领减半），其投射物速度降为四分之一，你的移动加快 20%。游戏对所有人只有一个时钟，因此这是局部减速。服务器选项（仅限单人，默认关闭）可以让整个游戏变慢。
- **纠缠齐射：** 本维度内的所有僚机向你的智能制导锁定目标（或你注视的生物）开火。发射时刻经过调整，所有弹体同时命中。

Mk.I 量子偏转器与行为预测器保持原有效果；Mk.II 物品增加行动。只有开启 PvP 时行动才会影响其他玩家（服务器配置 QuantumSuit 中每个行动都有开关）。

## 连击与协同

自 0.1.10.1 起，行动可以组成连锁连击（量子隧穿、冻结齐射、静止弹幕），量子单元可以两两纠缠获得协同。套装计算机还有演算带宽。见“纠缠与协同”。

## 扩展槽

把 <ItemLink id="mio_icif:module/item_quantum_bay_expander" /> 与量子部件一起放入通用套装改装台的单元槽。该部件增加一个单元槽：头盔 5、胸甲 4、护腿 3、靴子 3。扩展是永久的；未扩展的部件保持原来的单元数。

## 相干电容器

<ItemLink id="mio_icif:module/item_module_coherence_capacitor" />（护腿或靴子）使相干度上限增加 50，回复加快 25%。

## 配方

<RecipesFor id="mio_icif:module/item_module_phase_jump_drive" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_phase_jump_drive_mk2" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_phase_modulator" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_phase_modulator_mk2" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_coherence_capacitor" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_entanglement_relay" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_deflector_mk2" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_behavior_predictor_mk2" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_quantum_bay_expander" fallbackText="-" />
