---
navigation:
  title: "纠缠与协同"
  icon: mio_icif:module/item_module_compute_core
  parent: suit/index.md
  position: 4
item_ids:
  - mio_icif:module/item_module_compute_core
  - mio_icif:module/item_module_smart_guidance_mk2
  - mio_icif:module/item_module_resonance_field_mk2
---

# 纠缠与协同

自 0.1.10.1 起，可以把两个量子单元 **纠缠** 在一起。纠缠的一对单元作为 **协同** 运作，产生单个单元都没有的效果。

## 链接

- 量子胸甲是 **链接中枢**，链接保存在胸甲上。不穿量子胸甲时任何协同都不起作用。
- 中枢可容纳 **2 条链接**，<ItemLink id="mio_icif:module/item_module_compute_core" /> 启用时为 3 条。
- 一对单元都必须安装在穿戴中的量子部件上。两个单元都开启并有供电时协同才起作用。
- 更换单元后链接仍保存在胸甲上，单元装回后重新起作用。指向已移除模组单元的链接会保留，但不起作用。

## 纠缠矩阵

打开奇点管理面板，选中量子胸甲，点击“量子核心”下方的 **链接** 行。

- 左侧：你的量子单元作为节点排列在圆周上。淡虚线：这一对可以链接。实线：已链接但有一个单元关闭。脉动的双螺旋：协同正在起作用。
- 右侧：所有协同及其状态（LINKED、READY、OFFLINE、OVER CAP、MISSING）。点击一行即可链接或解除。
- 也可以把一个单元 **拖动** 到另一个单元上。拖动时会显示这一对的协同，松开即链接或解除。
- 底部显示已用链接数与演算带宽。

HUD 上，正在起作用的协同以小标签显示在准星旁。

## 协同

| 协同 | 单元 | 效果 |
|---|---|---|
| 预测诱导 | <ItemLink id="mio_icif:module/item_module_smart_guidance" /> + <ItemLink id="mio_icif:module/item_module_behavior_predictor" /> | 诱导弹飞向目标将要到达的位置，沿生物行走的路径预测。每 tick 转向 +4 度，锁定 +2。 |
| 威胁透视 | <ItemLink id="mio_icif:module/item_module_entity_esp" /> + <ItemLink id="mio_icif:module/item_module_threat_sensor" /> | 正在追猎你的生物透过墙壁发出红色轮廓，并以圆环倒数其攻击到达的时间。 |
| 地脉全息 | <ItemLink id="mio_icif:module/item_module_holomap" /> + <ItemLink id="mio_icif:module/item_module_ore_scanner" /> | 扫描到的矿石以 3D 体素显示在全息地图与战术地图中。 |
| 弹道同步 | <ItemLink id="mio_icif:module/item_module_ballistic" /> + <ItemLink id="mio_icif:module/item_module_smart_guidance" /> | 锁定时间减少 30%，并绘制诱导弹的预定轨迹。 |
| 爆心预报 | <ItemLink id="mio_icif:module/item_module_blast_warning" /> + <ItemLink id="mio_icif:module/item_module_behavior_predictor" /> | 即将发生的爆炸以 3D 球体显示半径，并用箭头指示撤离方向。 |
| 共振偏转 | <ItemLink id="mio_icif:module/item_module_deflector" /> + <ItemLink id="mio_icif:module/item_module_resonance_field" /> | 偏转器挡下的伤害有 25% 蓄积为共振；达到 8 时爆发，伤害 4 格内的敌对生物。 |
| 残像诱饵 | <ItemLink id="mio_icif:module/item_module_phase_jump_drive" /> + <ItemLink id="mio_icif:module/item_module_behavior_predictor" /> | 跃迁会在出发点留下诱饵。2 秒内原本追猎你的生物会走向诱饵，且无法重新锁定你。 |
| 相位反击 | <ItemLink id="mio_icif:module/item_module_phase_modulator" /> + <ItemLink id="mio_icif:module/item_module_deflector" /> | 相位偏移期间穿透你的伤害（最多 20）在结束时化为冲击波释放。 |
| 舰队数据链 | <ItemLink id="mio_icif:module/item_module_entanglement_relay" /> + <ItemLink id="mio_icif:module/item_module_entity_esp" /> | 僚机看到的敌对生物显示在你的 HUD 上；僚机会攻击你瞄准的生物。 |

每种协同在服务器配置（QuantumSuit 的 synergies）中都有开关。只有开启 PvP 时协同爆发才会命中玩家。

## 连锁连击

连击不需要链接，只看时机。触发时会显示横幅并播放音效。

- **量子隧穿：** 量子跃迁后 1 秒内的相位偏移只消耗一半相干度。
- **冻结齐射：** 在你自己的时间扭曲中进行纠缠齐射。最多 8 个被减速的生物成为目标，弹体无需蓄力同时命中，伤害为 1.5 倍。
- **静止弹幕：** 用按键收起力场时，力场中停住的投射物全部飞回其射手（视为你的射击）。

## 演算带宽

量子部件上每个启用的单元都会占用套装计算机的负载（QB）。同一单元装在两个部件上只计算一次；Mk.II 单元多占 1 QB。

| QB | 单元 |
|---|---|
| 3 | 生物扫描仪（ESP）、行为预测器、战术全息地图、地震矿物扫描仪、相位调制器、量子纠缠中继器 |
| 2 | 智能弹道诱导、威胁传感器、相位跃迁驱动器 |
| 1 | 弹道计算机、爆炸预警计时器、电网遥测传感器、量子偏转器、力场共振解体 |
| 0 | 其他所有单元 |

预算为 **20 QB**，演算核心启用时为 28 QB。0.1.9.2 允许的所有配置都在 20 QB 之内。超出预算时套装 **演算饱和**：传感器刷新减半，相干度回复速度减半。不会关闭任何功能。准星附近的 QB 表会变红。

## 量子演算核心

<ItemLink id="mio_icif:module/item_module_compute_core" />（胸甲或护腿，6 EU/t）使带宽预算增加 8 QB，中枢链接增加 1 条。

## Mk.II：诱导与共振解体

- <ItemLink id="mio_icif:module/item_module_smart_guidance_mk2" />：最多 10 个锁定，诱导弹每 tick 最多转向 20 度（Mk.I：6 个锁定，14 度）。
- <ItemLink id="mio_icif:module/item_module_resonance_field_mk2" />：半径 3.75，每 tick 最多 9 个方块（Mk.I：半径 2.5，6 个方块）。

地震矿物扫描仪、电网遥测传感器、生物扫描仪与弹道计算机的 Mk.II 见各自页面。

## 配方

<RecipesFor id="mio_icif:module/item_module_compute_core" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_smart_guidance_mk2" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_resonance_field_mk2" fallbackText="-" />
