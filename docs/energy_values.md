# 奇点迭代：电力数值表

- 本表汇总电力相关的数值（发电、储电、输电、耗电、电力物品），供规划新机器（例如核聚变多方块结构）时对照。
- 来源：GameTest EnergyValuesDumpGameTests 在游戏里实测的方块实体和物品（储能、输入输出上限、电压等级、额定输出、耗电和工时、物品容量与速率）；燃料、动能（KU）、热能（HU）和工作条件取自游戏手册各页的「电力数据」表；导线表取自手册「输电与储电」。
- 更新：先运行 gradlew runAddonGameTestServer，再运行 python tools/energy_table_11582.py .（同时生成 docs/energy_values.md 和 docs/SI机器数值说明.xlsx）。
- 方块实体是脱离世界创建的，随状态变化的数值显示为空闲值（例如反应堆按布局发电），这类以说明列为准。
- 发电量受配置 GeneratorOutput 的倍率影响，本表是默认倍率 1.0 下的值。和其他模组换算：4 FE = 1 EU。

## 1. 电压等级

单个电力包的上限，也是导线和机器的耐受等级。超过导线上限会熔毁导线，超过机器输入等级会让机器爆炸。

| 等级 | 单包上限 EU |
|---|---|
| LV | 32 |
| MV | 128 |
| HV | 512 |
| EV | 2,048 |
| IV | 8,192 |
| LUV | 32,768 |
| ZPMV | 131,072 |
| UV | 524,288 |
| UHV | 2,097,152 |
| UEV | 8,388,608 |
| UIV | 33,554,432 |
| UXV | 134,217,728 |
| OPV | 536,870,912 |
| MAX | 2,147,483,647 |

## 2. 导线

| 导线 | 等级 | 单包上限 | 不电击所需绝缘 | 线损（每格） |
|---|---|---|---|---|
| 锡（低压） | LV | 32 EU | 1 层 | 0.2 EU |
| 铁（中压） | MV | 128 EU | 1 层 | 0.2 EU |
| 铜（高压） | HV | 512 EU | 2 层 | 0.4 EU |
| 金（超高压） | EV | 2,048 EU | 3 层 | 0.8 EU |
| 玻璃纤维（IV） | IV | 8,192 EU | 不会电击 | 0.025 EU |
| 超导 | 任意 | 无上限 | 不会电击 | 无 |
| 检测、分流、断路器、保险丝导线 | IV | 8,192 EU | 不会电击 | 0.5 EU |

额定值是单包上限，不是通量上限：一根导线每 tick 可以送任意多个不超过上限的包。

线损默认关闭（服务器选项 lineLossModel = proportional 时才按表扣除）。

## 3. 发电机（EU）

按额定输出从高到低排列。额定输出为 0 或空的，发电量随状态变化，见「说明」列。

「输出等级」写成 A→B 的（三种动能发电机、核反应堆和聚变堆），电力包随产出变大：A 是最低等级，产出高时升到 B，「单包上限」按 B 计。接线要按 B 选导线和机器。

| 名称 | ID | 输出等级 | 单包上限 EU | 额定输出 EU/t | 内部储能 EU | 每 tick 最多送出 EU | 说明（燃料、条件） |
|---|---|---|---|---|---|---|---|
| 无限发电机 | `generator/block_unlimit_generator` | IV | 8,192 | 8,192 | 100,000 | 8,192 | 创造模式专用 |
| 量子太阳能发电机 | `generator/block_quantum_solar_panel` | IV | 8,192 | 6,144 | 10,000,000 | 8,192 | 夜间输出：0 EU/t；满功率辐照：850 W/m² |
| 终极光子振谐太阳能发电机 | `generator/block_ultimate_photon_resonance_solar_generator` | IV | 8,192 | 3,072 | 400,000,000 | 8,192 | 夜间输出：1,075 EU/t；满功率辐照：350 W/m² |
| 终极混合太阳能发电机 | `generator/block_ultimate_hybrid_solar_panel` | HV | 512 | 768 | 1,000,000 | 1,024 | 夜间输出：0 EU/t；满功率辐照：850 W/m² |
| 光子振谐太阳能发电机 | `generator/block_photon_resonance_solar_generator` | HV | 512 | 384 | 40,000,000 | 512 | 夜间输出：96 EU/t；满功率辐照：450 W/m² |
| 柴油发电机 | `generator/block_diesel_generator` | EV | 2,048 | 120 | 1,000,000 | 512 | 燃料：柴油：0.5 mB/t → 120 EU/t (240 EU/mB) |
| 混合太阳能发电机 | `generator/block_hybrid_solar_panel` | MV | 128 | 96 | 100,000 | 128 | 夜间输出：0 EU/t；满功率辐照：850 W/m² |
| 蒸汽涡轮发电机 | `generator/block_steam_turbine` | MV | 128 | 64 | 32,000 | 128 | 蒸汽消耗：128 mB/t → 64 EU/t (每 EU 2 mB)；过热蒸汽消耗：64 mB/t → 64 EU/t (每 EU 1 mB)；配冷凝机：蒸汽 80 mB/t、过热蒸汽约 49 mB/t → 64 EU/t |
| 进阶太阳能发电机 | `generator/block_mets_advanced_solar_generator` | MV | 128 | 48 | 200,000 | 128 | 夜间输出：7 EU/t；满功率辐照：600 W/m² |
| 进阶半流质发电机 | `generator/block_advanced_semifluid_generator` | MV | 128 | 36 | 100,000 | 128 | 燃料：沼气：0.9 mB/t → 36 EU/t (40 EU/mB)；燃料：生物质：3.6 mB/t → 36 EU/t (10 EU/mB)；燃料：原油：1.8 mB/t → 36 EU/t (20 EU/mB)；燃料：柴油：0.225 mB/t → 36 EU/t (160 EU/mB) |
| 地热发电机 | `generator/block_geo_generator` | LV | 32 | 20 | 2,400 | 32 | 燃料：熔岩：2 mB/t → 20 EU/t (10 EU/mB)；周围岩浆（2 格内）：紧贴 1 EU/t，更远 0.5 EU/t，× 深度，最多 12 EU/t |
| 半流质发电机 | `generator/block_semifluid_generator` | LV | 32 | 16 | 32,000 | 32 | 燃料：沼气：0.5 mB/t → 16 EU/t (32 EU/mB)；燃料：生物质：1 mB/t → 8 EU/t (8 EU/mB)；燃料：原油：0.5 mB/t → 8 EU/t (16 EU/mB)；燃料：柴油：0.25 mB/t → 32 EU/t (128 EU/mB) |
| 高级太阳能发电机 | `generator/block_advanced_solar_panel` | LV | 32 | 12 | 32,000 | 32 | 夜间输出：0 EU/t；满功率辐照：850 W/m² |
| 火力发电机 | `generator/block_thermal_generator` | LV | 32 | 10 | 4,000 | 32 | - |
| 太阳能发电机 | `generator/block_solar_generator` | LV | 32 | 1 | 2 | 32 | - |
| 核聚变堆控制台 | `fusion/block_fusion_controller` | IV→UIV | 33,554,432 | - | 20,000,000 | 0 | 托卡马克聚变堆：输出不封顶（0.1.11.98 起；示例包层 41,440,000 EU/t，超过 Mek 聚变堆的二倍，更强的包层更多，只在电网最高电压 2,147,483,647 EU/t 处截断），经能量输出端口送出；聚变功率每档加料 612,500 EU/t；储能为输入（LuV），磁体维持 392,000 EU/t，启动 300,000,000 EU；燃料为氘燃料棒和氚燃料棒，满功率每 625 tick 各用一根；破裂缓解用 10k/30k/60k 冷却单元；包层热量到上限即熔毁核爆（默认为核反应堆最强爆炸的 50 倍，配置 fusionExplosionMultiplier）；聚变功率：每档加料 612,500 EU/t，16 档为 9,800,000 EU/t；输入储能：20,000,000 EU；磁体维持：392,000 EU/t；启动（中心螺线管）：300,000,000 EU，最快 30 秒；加热：每个加热端口 245,000 EU/t，最多为聚变功率的十分之一 |
| 进阶掉落物发电机 | `generator/block_advanced_drop_generator` | HV | 512 | - | 100,000 | 512 | 烧掉落物：40 EU/t |
| 进阶经验球发电机 | `generator/block_advanced_experience_generator` | EV | 2,048 | - | 4,000,000 | 2,048 | 烧经验球：320 EU/t |
| 创造模式发电机 | `generator/block_creative_generator` | MAX | 2,147,483,647 | - | 8,192 | 8,192 | 创造模式专用 |
| 掉落物发电机 | `generator/block_drop_generator` | MV | 128 | - | 10,000 | 128 | 烧掉落物：20 EU/t |
| 经验球发电机 | `generator/block_experience_generator` | HV | 512 | - | 1,000,000 | 512 | 烧经验球：160 EU/t |
| 地磁发电机 | `generator/block_geomagnetic_generator` | ZPMV | 131,072 | 0 | 400,000,000 | 400,000,000 | 手册：输出 扰动为 1 时 30,720 EU/t（平均约 8,300 EU/t） |
| 动能发电机 | `generator/block_kinetic_generator` | MV→HV | 512 | 0 | 100,000 | 4,096 | 输出等级随动能输入：产出不超过 128 EU/t 时 MV 包，超过时（输入超过 512 KU/t）HV 包；产出：每 4 KU 产 1 EU，最多 250 EU/t；动能输入：最多 1,000 KU/t |
| 核反应堆发电机 | `generator/block_nuclear_reactor_generator` | IV→LuV | 32,768 | 0 | 1,000,000 | 1,000,000 | 按燃料棒布局发电；产出不超过 524,288 EU/t 时发 IV 包（8,192 EU，每 tick 最多 64 个），超过时发 LuV 包（32,768 EU）；每 tick 最多送出 1,000,000 EU（等于储能）；氚燃料棒（10,000 周期）和氘燃料棒（5,000 周期，0.1.11.94）是中子源：相邻燃料棒每周期多一次脉冲 |
| 量子发电机 | `generator/block_quantum_generator` | IV | 8,192 | - | 100,000,000 | 8,192 | 8,192 EU/t（有自己的配置项，再乘发电倍率） |
| 放射性同位素温差发电机 | `generator/block_rt_generator` | LV | 32 | 0 | 20,000 | 32 | 1–32 EU/t，按放入的同位素靶丸数 |
| 蒸汽反应堆 | `generator/block_steam_reactor` | EV | 2,048 | 0 | 1,000,000 | 1,000,000 | 不发 EU：产蒸汽，交给蒸汽涡轮或蒸汽动能发生机 |
| 蒸汽隧道电力端口 | `generator/block_steam_tunnel_power_port` | HV | 512 | - | 8,192 | 0 | 蒸汽隧道多方块的电力端口，发电量由隧道结构决定（见手册「蒸汽隧道」） |
| 涡轮增压动能发电机 | `generator/block_turbo_kinetic_generator` | HV→EV | 2,048 | 0 | 200,000 | 2,048 | 输出等级随动能输入：产出不超过 512 EU/t 时 HV 包，超过时 EV 包（风力或水力超过 602 KU/t，其他来源超过 1,462 KU/t）；产出：风力或水力动能发生机每 KU 0.85 EU，其他来源每 KU 0.35 EU；动能输入：最多 2,000 KU/t (最多 1,700 EU/t) |
| 双涡轮增压动能发电机 | `generator/block_twin_turbo_kinetic_generator` | EV→IV | 8,192 | 0 | 400,000 | 8,192 | 输出等级随动能输入：产出不超过 2,048 EU/t 时 EV 包，超过时 IV 包（风力或水力超过 1,706 KU/t；其他来源最多 1,800 EU/t，始终 EV）；产出：风力或水力动能发生机每 KU 1.2 EU，其他来源每 KU 0.45 EU；动能输入：最多 4,000 KU/t (最多 4,800 EU/t) |
| 水力发电机 | `generator/block_water_generator` | LV | 32 | 0 | 64,000 | 32 | 手册：输出 最多 8 EU/t；水箱中的水：1 mB/t → 4 EU/t (4 EU/mB)；正面的水：平均水流 2 EU/t，最多 4 EU/t |
| 风力发电机 | `generator/block_wind_generator` | LV | 32 | 0 | 64,000 | 32 | 按高度、遮挡和天气，最多 32 EU/t |

## 4. 动能（KU）与热能（HU）来源

这些方块不直接发 EU，产出的动能或热能由动能发电机、斯特林发电机等转换成 EU。数值取自手册。

| 名称 | ID | 手册数据 |
|---|---|---|
| 流体加热机 | `hugenerator/block_fluid_heat_generator` | 燃料：沼气：0.5 mB/t → 32 HU/t (64 HU/mB)；燃料：生物质：1 mB/t → 16 HU/t (16 HU/mB)；燃料：原油：0.5 mB/t → 32 HU/t (64 HU/mB)；燃料：柴油：0.25 mB/t → 768 HU/t (3,072 HU/mB)；燃料箱：10,000 mB |
| 电力加热机 | `hugenerator/block_heat_generator_elc` | 输入电压等级：EV (2,048 EU)；最大输入：2,048 EU/t；储能：10,000 EU；转换：1 EU → 1 HU；热量输出：每个线圈 10 HU/t，最多 100 HU/t (10 个线圈) |
| 热交换机 | `hugenerator/block_heat_source_fluid` | 熔岩 → 熔岩岩浆：1 mB → 1 mB，每 mB 20 HU；热冷却液 → 冷却液：1 mB → 1 mB，每 mB 20 HU；热量输出：每个热传导器 10 HU/t，最多 100 HU/t；流体消耗：每个热传导器 0.5 mB/t，最多 5 mB/t |
| 放射性同位素温差加热机 | `hugenerator/block_rt_heat_generator` | 热量输出：1 至 6 个靶丸：2、4、8、16、32、64 HU/t；靶丸消耗：无，靶丸不会耗尽 |
| 固体加热机 | `hugenerator/block_solid_heat_generator` | 热量输出：燃烧时 20 HU/t；燃烧时间：熔炉燃烧时间 ÷ 4 (煤炭：400 t，8,000 HU) |
| 电力动能发生机 | `kugenerator/block_kinetic_generator_elc` | 输入电压等级：LV (32 EU)；最大输入：1,000 EU/t；储能：40,000 EU；工作耗电：100 EU/t |
| 手摇动能发生机 | `kugenerator/block_manual_kinetic_generator` | - |
| 斯特林动能发生机 | `kugenerator/block_stirling_kinetic_generator` | 转换：4 HU + 1 mB 水 → 12 KU + 1 mB 热水；飞轮：20,000 KU，满时每 tick 最多给出 2,000 KU；热量缓冲：1,000 HU |
| 水力动能发生机 | `kugenerator/block_water_kinetic_generator` | - |
| 风力动能发生机 | `kugenerator/block_wind_kinetic_generator` | - |
| 蒸汽动能发生机 | `producer/block_steam_kinetic_generator` | 蒸汽：每 mB 2 KU，每 1,000 mB 产生 900 mB 废蒸汽和 1 mB 蒸馏水；过热蒸汽：每 mB 4 KU，全部作为废蒸汽排出；节流：KU × (1 − 蒸馏水 ÷ 1,000 mB) |

## 5. 储电方块

| 名称 | ID | 电压等级 | 单包上限 EU | 储能 EU | 每 tick 最大输入 EU | 每 tick 最大输出 EU |
|---|---|---|---|---|---|---|
| 储电盒 | `wiring/block_bat_box` | LV | 32 | 40,000 | 32 | 32 |
| CESU | `wiring/block_cesu` | MV | 128 | 300,000 | 128 | 128 |
| LESU储电箱 | `wiring/block_lesu` | HV | 512 | 1,000,000 | 512 | 512 |
| MFE | `wiring/block_mfe` | HV | 512 | 4,000,000 | 512 | 512 |
| MFSU | `wiring/block_mfsu` | EV | 2,048 | 40,000,000 | 2,048 | 2,048 |
| EESU储电箱 | `wiring/block_eesu` | IV | 8,192 | 400,000,000 | 8,192 | 8,192 |
| 大型储电GESU输入模块 | `wiring/block_gesu_input_iv` | MAX | 2,147,483,647 | 4,294,967,294 | 2,147,483,647 | 0 |
| 大型储电GESU输出模块（LuV） | `wiring/block_gesu_output_luv` | LUV | 32,768 | 65,536 | 0 | 32,768 |
| 大型储电GESU输出模块（IV） | `wiring/block_gesu_output_iv` | IV | 8,192 | 16,384 | 0 | 8,192 |
| 大型储电GESU核心 | `wiring/block_gesu_core` | MAX | 2,147,483,647 | 2,147,483,647 | 0 | 0 |

GESU 是多方块结构（核心加 6 个模块）：核心储能，输入和输出模块各自按等级收发。模块的储能是它们的缓冲。

## 6. 充电座

| 名称 | ID | 电压等级 | 单包上限 EU | 储能 EU | 每 tick 最大输入 EU | 每 tick 最大输出 EU |
|---|---|---|---|---|---|---|
| 充电座 | `wiring/block_batbox_charger` | LV | 32 | 40,000 | 32 | 32 |
| CESU充电座 | `wiring/block_cesu_charger` | MV | 128 | 300,000 | 128 | 128 |
| LESU充电座 | `wiring/block_lesu_charger` | HV | 512 | 1,000,000 | 512 | 512 |
| MFE充电座 | `wiring/block_mfe_charger` | HV | 512 | 4,000,000 | 512 | 512 |
| MFSU充电座 | `wiring/block_mfsu_charger` | EV | 2,048 | 40,000,000 | 2,048 | 2,048 |
| EESU充电座 | `wiring/block_eesu_charger` | IV | 8,192 | 400,000,000 | 8,192 | 8,192 |

## 7. 变压器

| 名称 | ID | 低压侧 | 低压侧单包 EU | 高压侧 | 高压侧单包 EU |
|---|---|---|---|---|---|
| 低压–中压变压器 | `wiring/transformer_lv_mv` | LV | 32 | MV | 128 |
| 中压–高压变压器 | `wiring/transformer_mv_hv` | MV | 128 | HV | 512 |
| 高压–超高压变压器 | `wiring/transformer_hv_ev` | HV | 512 | EV | 2,048 |
| 超高压–IV 变压器 | `wiring/transformer_ev_sc` | EV | 2,048 | IV | 8,192 |
| IV–LuV 变压器 | `wiring/transformer_iv_luv` | IV | 8,192 | LUV | 32,768 |
| LuV–ZPMV 变压器 | `wiring/transformer_luv_zpmv` | LUV | 32,768 | ZPMV | 131,072 |

变压器在相邻两个等级之间转换：高压侧收进的大包拆成低压侧的小包送出，反过来升压。

## 8. 机器耗电

「单次耗电」= 耗电 × 工时（默认配方、没有升级）。实际配方可能有自己的工时和耗电（例如精炼机），以 JEI 为准。

| 名称 | ID | 输入等级 | 单包上限 EU | 耗电 EU/t | 工时 tick | 单次耗电 EU | 内部储能 EU |
|---|---|---|---|---|---|---|---|
| 分子重组仪 | `producer/block_molecular_transformer` | MAX | 不限 | 32,768 | 100 | 3,276,800 | 120,000,000 |
| 电力纳米高炉 | `producer/block_blast_furnace_elc` | IV | 8,192 | 8,000 | 40 | 320,000 | 320,000 |
| 巡天激光防御站 | `producer/block_sky_patrol_laser_tower` | HV | 512 | 2,500 | 100 | 250,000 | 500,000 |
| 粒子聚合发生器 | `producer/block_neutron_polymerizer` | EV | 2,048 | 1,536 | 1,500 | 2,304,000 | 2,304,000 |
| 激光防御塔 | `producer/block_laser_defense_tower` | MV | 128 | 1,250 | 100 | 125,000 | 250,000 |
| 反应堆冷却液注入器(LZH) | `producer/block_lapis_reactor_coolant_injector` | MV | 128 | 1,000 | 1 | 1,000 | 48,000 |
| 反应堆冷却液注入器(RSH) | `producer/block_redstone_reactor_coolant_injector` | MV | 128 | 1,000 | 1 | 1,000 | 48,000 |
| 复制机 | `producer/block_replicator_elc` | EV | 2,048 | 512 | 10,000 | 5,120,000 | 2,000,000 |
| 特斯拉线圈 | `producer/block_tesla` | MV | 128 | 500 | 100 | 50,000 | 10,000 |
| 模式扫描机 | `producer/block_scanner_elc` | HV | 512 | 256 | 3,300 | 844,800 | 512,000 |
| 电力动能发生机 | `kugenerator/block_kinetic_generator_elc` | LV | 32 | 100 | 1 | 100 | 40,000 |
| 地形转换机 | `producer/block_terra_elc` | HV | 512 | 100 | 1 | 100 | 100,000 |
| 通用套装改装台 | `producer/block_quantum_modification_station` | HV | 512 | 64 | 100 | 6,400 | 40,000 |
| 进阶金属成型机 | `producer/block_metal_former_advanced` | MV | 128 | 50 | 20 | 1,000 | 1,000 |
| 方块切割机 | `producer/block_block_cutter` | MV | 128 | 48 | 900 | 43,200 | 43,200 |
| 热能离心机 | `producer/block_centrifuge_elc` | MV | 128 | 48 | 500 | 24,000 | 24,000 |
| 进阶高压压缩机 | `producer/block_compressor_advanced_elc` | MV | 128 | 25 | 70 | 1,750 | 1,750 |
| 洗矿机 | `producer/block_washer_elc` | MV | 128 | 16 | 400 | 6,400 | 8,000 |
| 感应炉 | `producer/block_induction_elc` | MV | 128 | 15 | 4,000 | 60,000 | 10,000 |
| 电解机 | `producer/block_electrolyzer_elc` | LV | 32 | 10 | 20 | 200 | 400 |
| 金属成型机 | `producer/block_metal_former` | LV | 32 | 10 | 200 | 2,000 | 2,000 |
| 精炼机 | `producer/block_oil_refinery_elc` | MV | 128 | 8 | 10 | 80 | 2,000 |
| 磁化机 | `producer/block_magnetizer` | LV | 32 | 5 | 100 | 500 | 100 |
| 进阶旋风打粉机 | `producer/block_powder_advanced_elc` | MV | 128 | 5 | 150 | 750 | 750 |
| 装罐机 | `producer/block_canner_elc` | LV | 32 | 4 | 200 | 800 | 800 |
| 电炉 | `producer/block_furnace_elc` | LV | 32 | 3 | 100 | 300 | 300 |
| 批量工作台 | `producer/block_batch_crafter` | MV | 128 | 2 | 40 | 80 | 20,000 |
| 压缩机 | `producer/block_compressor_elc` | LV | 32 | 2 | 300 | 600 | 600 |
| 提取机 | `producer/block_extractor_elc` | LV | 32 | 2 | 300 | 600 | 600 |
| 打粉机 | `producer/block_powder_elc` | LV | 32 | 2 | 300 | 600 | 600 |
| 收割机 | `producer/block_harvest_elc` | LV | 32 | 1 | 10 | 10 | 10,000 |
| 作物监管机 | `producer/block_matron_elc` | LV | 32 | 1 | 10 | 10 | 10,000 |
| 泵 | `producer/block_pump_elc` | LV | 32 | 1 | 20 | 20 | 20 |
| 回收机 | `producer/block_recycler_elc` | LV | 32 | 1 | 45 | 45 | 45 |
| 电力加热机 | `hugenerator/block_heat_generator_elc` | EV | 2,048 | 0 | 1 | 0 | 10,000 |
| 高级采矿机 | `producer/block_advanced_miner_elc` | HV | 512 | 0 | 20 | 0 | 4,000,000 |
| 武装库 | `producer/block_armory` | HV | 512 | 0 | 100 | 0 | 1,000,000 |
| 蓝图建造机 | `producer/block_blueprint_builder` | MV | 128 | 0 | 100 | 0 | 200,000 |
| 冷凝机 | `producer/block_condenser` | HV | 512 | 0 | 10,000 | 0 | 10,000 |
| 流体分配机 | `producer/block_fluid_distributor_elc` | LV | 32 | 0 | 10 | 0 | 0 |
| 流体流量调节机 | `producer/block_fluid_regulator_elc` | EV | 2,048 | 0 | 1 | 0 | 10,000 |
| 物质生成机 | `producer/block_matter_elc` | EV | 2,048 | 0 | 1,000,000 | 0 | 0 |
| 采矿机 | `producer/block_miner_elc` | LV | 32 | 0 | 20 | 0 | 10,000 |
| 电动分拣机 | `producer/block_sorter_elc` | MV | 128 | 0 | 20 | 0 | 15,000 |
| 传送机 | `producer/block_teleporter_elc` | HV | 512 | 0 | 20 | 0 | 100,000,000 |
| 高级流体分配机 | `producer/block_weighted_fluid_distributor_elc` | LV | 32 | 0 | 1 | 0 | 0 |

## 9. 其他带电方块

| 名称 | ID | 电压等级 | 单包上限 EU | 储能 EU | 每 tick 最大输入 EU | 每 tick 最大输出 EU |
|---|---|---|---|---|---|---|
| 电压检测器 | `checker/block_checker` | IV | 8,192 | 0 | 0 | 0 |
| 并网接口 | `energy_converter/energy_converter` | EV | 2,048 | 200,000 | 2,048 | 2,048 |
| 聚变能量输出端口 | `fusion/block_fusion_power_port` | IV→UIV | 33,554,432 | 2,147,483,647 | 0 | 0 |
| 维度石油钻机核心 | `oilrig/block_dimension_oil_rig_core` | EV | 2,048 | 200,000 | 0 | 0 |
| 石油钻机核心 | `oilrig/block_oil_rig_core` | HV | 512 | 50,000 | 0 | 0 |
| 石油钻机输入模块 | `oilrig/block_oil_rig_input` | HV | 512 | 50,000 | 512 | 0 |
| 区块加载器 | `producer/block_chunk_loader` | MV | 128 | 2,500 | 128 | 0 |
| 期货机 | `producer/block_future_elc` | LV | 32 | 10,000 | 100 | 100 |
| 大型物质生成机核心 | `producer/block_large_fabricator_core` | MAX | 2,147,483,647 | 0 | 0 | 0 |
| 大型物质生成机输入模块(IV) | `producer/block_large_fabricator_input_iv` | IV | 8,192 | 800,000 | 800,000 | 0 |
| 大型物质生成机废料输入模块 | `producer/block_large_fabricator_scrap` | MAX | 2,147,483,647 | 0 | 0 | 0 |
| 大型物质生成机储罐模块 | `producer/block_large_fabricator_tank` | MAX | 2,147,483,647 | 0 | 0 | 0 |
| 模式存储机 | `producer/block_pattern_storage` | LV | 32 | 100,000 | 128 | 0 |
| 交易终端 | `producer/block_trade_terminal` | LV | 32 | 10,000 | 100 | 100 |
| 无线电力传输节点 | `wiring/block_wireless_power_transmission_node` | LUV | 32,768 | 3,145,728 | 524,288 | 524,288 |

## 10. 电池与储能物品

「速率」是每 tick 充电或放电的最大 EU。「速率对应等级」是能容纳这个速率的最低电压等级（推算值，便于和机器、导线对照）。

| 名称 | ID | 容量 EU | 速率 EU/t | 速率对应等级 | 从空充满秒数 |
|---|---|---|---|---|---|
| 充电电池 | `normal/item_bat_lev0` | 10,000 | 100 | MV | 5.00 |
| RE充电电池 | `normal/item_chargebat_lev0` | 40,000 | 128 | MV | 15.62 |
| 无线管理器 | `item_electric_wireless_manager` | 50,000 | 128 | MV | 19.53 |
| 地磁探测器 | `item_geomagnetic_detector` | 50,000 | 128 | MV | 19.53 |
| 锂电池 | `normal/item_lithium_battery` | 50,000 | 128 | MV | 19.53 |
| 高级电池 | `normal/item_advbat_lev0` | 100,000 | 256 | HV | 19.53 |
| 高级锂电池 | `normal/item_adv_lithium_battery` | 200,000 | 128 | MV | 78.12 |
| 高级充电电池 | `normal/item_advchargebat_lev0` | 400,000 | 1,024 | EV | 19.53 |
| 能量水晶 | `normal/item_crystal_lev0` | 1,000,000 | 2,048 | EV | 24.41 |
| 能量水晶充电电池 | `normal/item_crystal_chargebat_lev0` | 4,000,000 | 8,192 | IV | 24.41 |
| 蓝波顿水晶 | `normal/item_lapotron_crystal_lev0` | 10,000,000 | 8,192 | IV | 61.04 |
| 蓝波顿水晶充电电池 | `normal/item_lamacrystal_chargebat_lev0` | 40,000,000 | 32,768 | LUV | 61.04 |
| 超级兰波顿水晶 | `normal/item_super_lapotron_crystal` | 100,000,000 | 8,192 | IV | 610.35 |
| 充电超级兰波顿水晶 | `normal/item_charging_super_lapotron_crystal` | 400,000,000 | 8,192 | IV | 2,441.41 |
| 钍电池 | `normal/item_thorium_battery` | 2,147,483,647 | 9 | LV | 11,930,464.71 |

## 11. 电动工具与武器

| 名称 | ID | 容量 EU | 工具等级 | 充电速率 EU/t | 每次耗电 EU | 满电可用次数 |
|---|---|---|---|---|---|---|
| 小型驱动把手 | `item_tool_power_unit_small` | 5,000 | 1 | 5,000 | 25 | 200 |
| 电动树脂提取器 | `item_tool_treetap_elc` | 5,000 | 1 | 5,000 | 10 | 500 |
| 电力营养供应器 | `item_electric_nutrition_supply` | 10,000 | 1 | 32 | 100 | 100 |
| 电动锄 | `item_tool_iron_hoe` | 10,000 | 1 | 100 | 10 | 1,000 |
| 驱动把手 | `item_tool_power_unit` | 10,000 | 1 | 10,000 | 50 | 200 |
| 风力计 | `item_tool_windmeter` | 10,000 | 1 | 100 | 50 | 200 |
| 电动鱼竿 | `item_electric_fishing_rod` | 12,000 | 1 | 32 | 100 | 120 |
| 钻石钻头 | `item_tool_diamond_driller` | 30,000 | 1 | 100 | 80 | 375 |
| 铁链锯 | `item_tool_iron_chainsaw` | 30,000 | 1 | 100 | 100 | 300 |
| 铁钻头 | `item_tool_iron_driller` | 30,000 | 1 | 100 | 50 | 600 |
| 等离子射线枪 | `item_tool_plasma_launcher` | 40,000 | 3 | 128 | 100 | 400 |
| 电力激光步枪 | `item_tool_electric_rifle` | 50,000 | 2 | 128 | 100 | 500 |
| 钢制防爆盾 | `item_tool_steel_shield` | 50,000 | 2 | 128 | 100 | 500 |
| 电动扳手 | `item_tool_wrench_elc` | 50,000 | 1 | 50,000 | 100 | 500 |
| 遮蔽器 | `build/item_obscurator` | 100,000 | 1 | 100 | 100 | 1,000 |
| OD扫描器 | `item_tool_od_scanner` | 100,000 | 1 | 100,000 | 48 | 2,083 |
| 便携期货交易终端 | `normal/item_portable_futures_trader` | 100,000 | 2 | 128 | 10 | 10,000 |
| 纳米剑 | `item_tool_nanosaber` | 160,000 | 3 | 500 | 100 | 1,600 |
| 电力光源产生器 | `item_electric_lighter` | 200,000 | 1 | 128 | 500 | 400 |
| 等离子空气炮 | `item_tool_plasma_air_cannon` | 200,000 | 2 | 128 | 100 | 2,000 |
| 采矿镭射枪 | `item_tool_laser_miner` | 300,000 | 3 | 512 | 1,250 | 240 |
| 纳米复合弓 | `item_tool_nano_bow` | 500,000 | 3 | 500,000 | 512 | 976 |
| 电力生命维护仪 | `item_electric_first_aid_life_support` | 1,000,000 | 3 | 2,048 | 1,000 | 1,000 |
| 电力伤害吸收仪 | `item_electric_force_field_generator` | 1,000,000 | 2 | 512 | 5,000 | 200 |
| 电力等离子发射器 | `item_tool_electric_plasma_gun` | 1,000,000 | 3 | 512 | 5,000 | 200 |
| 铱钻头 | `item_tool_iridium_driller` | 1,000,000 | 3 | 2,000 | 800 | 1,250 |
| OV扫描器 | `item_tool_ov_scanner` | 1,000,000 | 1 | 1,000,000 | 96 | 10,416 |
| 电力火箭筒 | `item_tool_rocket_launcher` | 1,000,000 | 3 | 512 | 100 | 10,000 |
| 次世代作物扫描仪 | `normal/item_crop_analyzer` | 4,000,000 | 4 | 2,048 | 8 | 500,000 |
| 进阶电力激光步枪 | `item_tool_advanced_electric_rifle` | 5,000,000 | 4 | 2,048 | 5,000 | 1,000 |
| 量子剑 | `item_tool_quantum_sword` | 10,000,000 | 4 | 2,048 | 100 | 100,000 |
| 铱制刺刀战术激光步枪 | `item_tool_tactical_laser_rifle` | 100,000,000 | 5 | 8,192 | 10,000 | 10,000 |
| 先进速子裂解枪 | `item_tool_tachyon_disruptor` | 400,000,000 | 5 | 8,192 | 50,000 | 8,000 |

「每次耗电」是工具的基础值。部分工具的实际耗电随模式或目标变化（例如镭射枪各模式、钻头挖硬方块），以物品提示为准。

## 12. 电力护甲、背包与喷气背包

「待机耗电」是穿戴时每 tick 固定消耗的 EU（0 = 只在受击或使用功能时耗电）。

| 名称 | ID | 容量 EU | 护甲等级 | 充电速率 EU/t | 待机耗电 EU/t |
|---|---|---|---|---|---|
| 电力喷气背包 | `armor/item_armor_jetpack_electric` | 30,000 | 1 | 60 | 2 |
| 充电背包 | `armor/item_armor_batpack` | 60,000 | 1 | 100 | 0 |
| 潜水面具 | `armor/item_armor_diving_mask` | 100,000 | 2 | 128 | 15 |
| 夜视镜 | `armor/item_armor_nightvision_goggles` | 400,000 | 1 | 200 | 1 |
| 高级充电背包 | `armor/item_armor_advbatpack` | 600,000 | 2 | 1,000 | 0 |
| 进阶电力超频喷气背包 | `armor/item_armor_advanced_jetpack` | 1,000,000 | 3 | 512 | 4 |
| 纳米靴子 | `armor/item_armor_nano_boots` | 1,000,000 | 5 | 1,000,000 | 0 |
| 纳米胸甲 | `armor/item_armor_nano_chestplate` | 1,000,000 | 5 | 1,000,000 | 0 |
| 纳米头盔 | `armor/item_armor_nano_helmet` | 1,000,000 | 5 | 1,000,000 | 0 |
| 纳米护腿 | `armor/item_armor_nano_leggings` | 1,000,000 | 5 | 1,000,000 | 0 |
| 能量背包 | `armor/item_armor_energypack` | 2,000,000 | 3 | 1,000 | 0 |
| 量子靴子 | `armor/item_armor_quantum_boots` | 10,000,000 | 6 | 12,000 | 0 |
| 量子胸甲 | `armor/item_armor_quantum_chestplate` | 10,000,000 | 6 | 12,000 | 0 |
| 量子头盔 | `armor/item_armor_quantum_helmet` | 10,000,000 | 6 | 12,000 | 0 |
| 量子护腿 | `armor/item_armor_quantum_leggings` | 10,000,000 | 6 | 12,000 | 0 |
| 蓝波顿背包 | `armor/item_armor_lappack` | 20,000,000 | 4 | 2,500 | 0 |

## 13. 规划参考：可达输出最高的 10 种发电机

不含创造模式发电机和随布局变化的核反应堆。

| 名称 | 最大输出 EU/t | 依据 | 输出等级 | 单包上限 EU | 每秒 EU | 每天（24000 tick）EU |
|---|---|---|---|---|---|---|
| 核聚变堆控制台 | 41,440,000 | 示例包层（质子四联燃料棒＋铱超频散热片）；0.1.11.98 起不封顶，更强的包层更多 | IV→UIV | 33,554,432 | 828,800,000 | 994,560,000,000 |
| 地磁发电机 | 30,720 | 手册：扰动为 1 时；平均约 8,300 | ZPMV | 131,072 | 614,400 | 737,280,000 |
| 量子发电机 | 8,192 | 额定（代码） | IV | 8,192 | 163,840 | 196,608,000 |
| 量子太阳能发电机 | 6,144 | 额定（实测） | IV | 8,192 | 122,880 | 147,456,000 |
| 双涡轮增压动能发电机 | 4,800 | 手册：4,000 KU/t 风力或水力动能 | EV→IV | 8,192 | 96,000 | 115,200,000 |
| 终极光子振谐太阳能发电机 | 3,072 | 额定（实测） | IV | 8,192 | 61,440 | 73,728,000 |
| 蒸汽隧道电力端口 | 2,048 | 手册：蒸汽隧道，每 tick 4 个包 | HV | 512 | 40,960 | 49,152,000 |
| 涡轮增压动能发电机 | 1,700 | 手册：2,000 KU/t 风力或水力动能 | HV→EV | 2,048 | 34,000 | 40,800,000 |
| 终极混合太阳能发电机 | 768 | 额定（实测） | HV | 512 | 15,360 | 18,432,000 |
| 光子振谐太阳能发电机 | 384 | 额定（实测） | HV | 512 | 7,680 | 9,216,000 |

核反应堆的输出按燃料棒布局，手册「反应堆模拟器」和 JEI「反应堆布局」里的内置示例最高 660 EU/t（6 个反应仓，进料倍率 1x）。

## 14. 规划参考：储能最大的方块

| 名称 | 储能 EU | 电压等级 | 单包上限 EU | 最大输出 EU/t | 满电可放多少秒（按最大输出） |
|---|---|---|---|---|---|
| 大型储电GESU核心 | 2,147,483,647 | MAX | 2,147,483,647 | - | - |
| EESU储电箱 | 400,000,000 | IV | 8,192 | 8,192 | 2,441.41 |
| MFSU | 40,000,000 | EV | 2,048 | 2,048 | 976.56 |
| MFE | 4,000,000 | HV | 512 | 512 | 390.62 |
| LESU储电箱 | 1,000,000 | HV | 512 | 512 | 97.66 |

GESU 核心经输出模块送电：每个模块 8,192（IV）或 32,768（LuV）EU/t。

设计一台新发电机时可以对照：同一等级里，单包不能超过该等级上限；需要更大输出时，每 tick 送多个包（导线没有通量上限），或者提高输出等级并配相应的导线和变压器。
