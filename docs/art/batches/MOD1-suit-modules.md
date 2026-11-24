# 美术批次 MOD1：套装模块图标

先读 [`../README.md`](../README.md) 与 [`../style-suit-modules.md`](../style-suit-modules.md)。这一批把全部 69 张套装模块和配件的图标按新规范重画：量子陶瓷白、纳米碳纤维黑、共用黑白拼接、Mk.II 统一金色标记。代码不改，只替换贴图。

所有文件都在 `src/main/resources/assets/mio_icif/textures/item/module/`，16×16。

“图标建议”只是建议：意思对、16×16 认得出就行，你可以换更好的画法，在报告里说明。“功能色”在量子模块里只用于指示灯，在纳米和共用模块里是图标颜色。

## 1. 量子模块（陶瓷白，规范第 2 节）（25 张）

| 文件 | 名称 | 装在 | 图标建议 | 功能色 |
| --- | --- | --- | --- | --- |
| `item_module_ore_scanner.png` | 地震矿物扫描仪 | 靴、腿 | 一个小矿石菱形，上方两三道向外扩散的弧形地震波 | `#3FD4FF` |
| `item_module_ore_scanner_mk2.png` | 地震矿物扫描仪 Mk.II | 靴、腿 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#3FD4FF` |
| `item_module_grid_telemetry.png` | 电网遥测传感器 | 腿、头 | 半圆仪表弧 + 一道闪电 | `#FFD23F` |
| `item_module_grid_telemetry_mk2.png` | 电网遥测传感器 Mk.II | 腿、头 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#FFD23F` |
| `item_module_entity_esp.png` | 生物扫描仪（ESP） | 头 | 一只眼睛，外面套一对方括号（扫描框） | `#7CFFB2` |
| `item_module_entity_esp_mk2.png` | 生物扫描仪（ESP）Mk.II | 头 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#7CFFB2` |
| `item_module_ballistic.png` | 弹道计算机 | 头 | 十字准星 + 一条抛物线弹道落在准星上 | `#FF9F1C` |
| `item_module_ballistic_mk2.png` | 弹道计算机 Mk.II | 头 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#FF9F1C` |
| `item_module_behavior_predictor.png` | 行为预测器 | 头 | 一条点线弯箭头（预测路径），起点一个小实心点 | `#C77DFF` |
| `item_module_behavior_predictor_mk2.png` | 行为预测器 Mk.II | 头 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#C77DFF` |
| `item_module_holomap.png` | 战术3D全息地图 | 头 | 等距视角的菱形网格（地形），上面一个小标记点 | `#4DE1C1` |
| `item_module_holomap_mk2.png` | 战术3D全息地图 Mk.II | 头 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#4DE1C1` |
| `item_module_deflector.png` | 量子偏转器 | 胸 | 盾形，一支箭撞上后折向一侧 | `#5AB8FF` |
| `item_module_deflector_mk2.png` | 量子偏转器 Mk.II | 胸 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#5AB8FF` |
| `item_module_smart_guidance.png` | 智能弹道诱导单元 | 头 | 四个锁定框角 + 框内一条弯曲的箭头 | `#FF4D6D` |
| `item_module_smart_guidance_mk2.png` | 智能弹道诱导 Mk.II | 头 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#FF4D6D` |
| `item_module_resonance_field.png` | 力场共振解体单元 | 靴、腿 | 底部两三圈同心椭圆（驻波），上方一块裂开的方块 | `#9B7CFF` |
| `item_module_resonance_field_mk2.png` | 力场共振解体 Mk.II | 靴、腿 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#9B7CFF` |
| `item_module_phase_jump_drive.png` | 相位跃迁驱动器 | 腿、靴 | 虚线箭头，从左下一个空心点跳到右上一个实心点 | `#8F7CFF` |
| `item_module_phase_jump_drive_mk2.png` | 相位跃迁驱动器 Mk.II | 腿、靴 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#8F7CFF` |
| `item_module_phase_modulator.png` | 相位调制器 | 胸 | 两个错开叠在一起的人形 / 菱形轮廓，一实一虚 | `#D06BFF` |
| `item_module_phase_modulator_mk2.png` | 相位调制器 Mk.II | 胸 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#D06BFF` |
| `item_module_coherence_capacitor.png` | 相干电容器 | 腿、靴 | 电容符号（两块平行极板）+ 一个 ψ 或波形 | `#5CE1E6` |
| `item_module_entanglement_relay.png` | 量子纠缠中继器 | 胸、腿 | 两个相扣的圆环（像 ∞ 的两环），中间一个小点 | `#6FF3D6` |
| `item_module_compute_core.png` | 量子演算核心 | 胸、腿 | 一块小芯片：方块 + 四边针脚，中心一个点 | `#7FD8FF` |

## 2. 纳米模块（碳纤维黑，规范第 3 节）（26 张）

| 文件 | 名称 | 装在 | 图标建议 | 功能色 |
| --- | --- | --- | --- | --- |
| `item_module_myofiber.png` | 纳米肌纤维 | 腿 | 三四股并排弯曲的肌纤维束，末端收拢 | `#3DFFB4` |
| `item_module_myofiber_mk2.png` | 纳米肌纤维 Mk.II | 腿 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#3DFFB4` |
| `item_module_thermoregulator.png` | 热调节膜 | 胸 | 温度计 + 一条起伏的膜线 | `#FF8A3D` |
| `item_module_thermoregulator_mk2.png` | 热调节膜 Mk.II | 胸 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#FF8A3D` |
| `item_module_emp_emitter.png` | EMP 发射器 | 胸 | 中心一道闪电，外圈一个断开的脉冲环 | `#6FA8FF` |
| `item_module_emp_emitter_mk2.png` | EMP 发射器 Mk.II | 胸 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#6FA8FF` |
| `item_module_optical_camo.png` | 光学迷彩 | 胸 | 一个人形轮廓，一半实线一半点线（正在隐去） | `#BFF6FF` |
| `item_module_optical_camo_mk2.png` | 光学迷彩 Mk.II | 胸 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#BFF6FF` |
| `item_module_micro_capacitor.png` | 微电容晶格 | 任意 | 3×3 的小方格晶格，每格一个亮点 | `#FFD45A` |
| `item_module_micro_capacitor_mk2.png` | 微电容晶格 Mk.II | 任意 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#FFD45A` |
| `item_module_gecko_setae.png` | 壁虎刚毛 | 靴 | 一只张开的壁虎脚掌（四五个圆头脚趾） | `#9BE15D` |
| `item_module_gecko_setae_mk2.png` | 壁虎刚毛 Mk.II | 靴 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#9BE15D` |
| `item_module_shock_gel.png` | 冲击凝胶 | 靴 | 一滴凝胶 / 果冻块，下方一道压扁的冲击线 | `#4FC3F7` |
| `item_module_shock_gel_mk2.png` | 冲击凝胶 Mk.II | 靴 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#4FC3F7` |
| `item_module_heat_sink_fins.png` | 散热鳍片 | 胸、腿 | 三四片竖直的散热鳍片，上方两道热浪线 | `#FF6E40` |
| `item_module_heat_sink_fins_mk2.png` | 散热鳍片 Mk.II | 胸、腿 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#FF6E40` |
| `item_module_thermal_camo.png` | 热迷彩 | 胸 | 一个人形热像轮廓，从亮到暗分两三段（用色阶，不用渐变） | `#9575CD` |
| `item_module_thermal_camo_mk2.png` | 热迷彩 Mk.II | 胸 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#9575CD` |
| `item_module_jammer.png` | 干扰器 | 头 | 一根天线，旁边几道被斜杠打断的信号弧 | `#E040FB` |
| `item_module_jammer_mk2.png` | 干扰器 Mk.II | 头 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#E040FB` |
| `item_module_recon_cloud.png` | 纳米侦察云 | 头 | 一团散开的小点（纳米云），中间一只小眼睛 | `#64FFDA` |
| `item_module_recon_cloud_mk2.png` | 纳米侦察云 Mk.II | 头 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#64FFDA` |
| `item_module_kinetic_mesh.png` | 动能装甲网 | 胸 | 六边形网格组成的盾面 | `#FFB300` |
| `item_module_kinetic_mesh_mk2.png` | 动能装甲网 Mk.II | 胸 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#FFB300` |
| `item_module_solar_skin.png` | 太阳能皮肤 | 头、胸 | 左上一个小太阳，下面一块 2×2 的电池片网格 | `#FFD740` |
| `item_module_solar_skin_mk2.png` | 太阳能皮肤 Mk.II | 头、胸 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#FFD740` |

## 3. 共用模块（黑白对角拼接，规范第 4 节）（10 张）

| 文件 | 名称 | 装在 | 图标建议 | 功能色 |
| --- | --- | --- | --- | --- |
| `item_module_blast_warning.png` | 爆炸预警计时器 | 胸、头 | 三角警示牌，里面一个小计时器 / 感叹号 | `#FF5A36` |
| `item_module_threat_sensor.png` | 威胁传感器 | 胸 | 一只眼睛，瞳孔是红色实心点，外面一圈警戒弧 | `#FF3B30` |
| `item_module_toxin_filter.png` | 毒素·辐射过滤器 | 头 | 防毒面具的圆形滤罐，罐面一个辐射三叶标志（简化） | `#9CFF57` |
| `item_module_toxin_filter_mk2.png` | 毒素·辐射过滤器 Mk.II | 头 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#9CFF57` |
| `item_module_self_repair.png` | 自修复涂层 | 任意 | 一个十字（加号），旁边一把小扳手或几个修复的亮点 | `#69F0AE` |
| `item_module_self_repair_mk2.png` | 自修复涂层 Mk.II | 任意 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#69F0AE` |
| `item_module_hydrophobic.png` | 疏水涂层 | 胸、腿 | 一滴水珠，从一条斜面上弹开（下方两三个小水点） | `#40C4FF` |
| `item_module_hydrophobic_mk2.png` | 疏水涂层 Mk.II | 胸、腿 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#40C4FF` |
| `item_module_kinetic_generator.png` | 动能发电机 | 腿 | 一个脚印 + 一道小闪电 | `#FFEE58` |
| `item_module_kinetic_generator_mk2.png` | 动能发电机 Mk.II | 腿 | 同 Mk.I，加 Mk.II 金色标记（规范第 5 节） | `#FFEE58` |

## 4. 其他配件（规范第 6 节，8 张）

| 文件 | 名称 | 画什么 |
| --- | --- | --- |
| `item_core_graviton.png` | 重力子壁垒核心 | 圆形陶瓷外壳，晶核紫 `#8F6BFF`，晶核周围一圈细的深色引力环 |
| `item_core_propulsion.png` | 高级推进核心 | 圆形陶瓷外壳，晶核青 `#4FC3F7`，下方一个小喷口 |
| `item_core_solar_array_1.png` | 太阳能阵列 I 核心 | 圆形陶瓷外壳，晶核金 `#FFC83D`，晶核里一格电池片纹；旁边 1 个深色刻点 |
| `item_core_solar_array_2.png` | 太阳能阵列 II 核心 | 同上，2 个刻点 |
| `item_core_solar_array_3.png` | 太阳能阵列 III 核心 | 同上，3 个刻点 |
| `item_quantum_bay_expander.png` | 量子扩展槽 | 量子陶瓷方块，中间一个空插槽（深色凹口）和一个小“+”，指示灯 `#9B7CFF` |
| `item_living_nano_lining.png` | 活体纳米内衬 | 碳纤维黑的软布片（圆角、边缘 1 像素缝线），一条绿色 `#5DFFB0` 的活体电路曲线 |
| `item_nano_lattice_densifier.png` | 纳米晶格致密剂 | 碳纤维黑的小罐 / 安瓿，罐身一条青色 `#64FFDA` 的窗，罐口浅灰金属盖 |

## 5. 预览

`docs/art/previews/MOD1.png`，按 README 第 4 节：8 倍放大的四组、1 倍原尺寸一行、新旧对照一行。另外请单独出一张 `docs/art/previews/MOD1-pairs.png`：每个有 Mk.II 的模块，Mk.I 和 Mk.II 并排（8 倍），用来检查 Mk.II 标记是否一眼可辨。

## 6. 完成标准

- 69 个文件全部替换并提交，生成脚本在 `tools/art/modules/`；
- `python3 tools/art/check_modules.py` 输出 `OK`；
- 预览两张已提交；
- 报告用简体中文，写明：哪些图标偏离了建议、哪些在 1 倍下仍然难认、共用模块的对角拼接效果如何（这一节是默认方案，用户还要看效果再定）。
