# 0.1.10.36 更新说明与游戏内测试清单：IC2C 更新（译名、市场、护甲、采矿、核燃料、深层铱矿、扫描仪定价、蒸汽反应堆）

基于 0.1.10.35。按批准的 IC2C 更新计划（决定 D1–D16）分阶段实施，提交在本地分支 `feature/0.1.10.36-ic2c-update`：

| 提交 | 内容 |
|---|---|
| `dbaaa44d` | 阶段 0：反应堆增殖完成后永久停机的修复 |
| `d9be7815` | 阶段 1：译名修正、隐藏无限发电机、EMI 多产物、采矿镭射枪 3x3 与超高能爆破 |
| `594242c3` | 阶段 2–3：市场标签匹配、护甲附魔与光效、采矿机镭射与收回管道、深层铱矿与核爆嬗变、氚 / 锂 / 质子燃料 |
| `08daf99e` | 阶段 4.1：扫描仪为其他模组物品定价（复制机不变） |
| `7b693cda` | 采矿机指南源数据补交 |
| `d15a772e` | 阶段 4.2：蒸汽反应堆、蒸汽反应仓、蒸汽散热片与蒸汽涡轮发电机 |

本文按功能列出每一项改动（新方块、新物品、逻辑修改、数据与界面），每节后附游戏内测试步骤与预期结果。CHANGELOG 中的 `## 0.1.10.36` 一节是本文的摘要。物品名称写成“中文名（English Name，`注册 id`）”，注册 id 省略命名空间 `mio_icif:`。

---

## 0. 测试准备

- **世界**：创造模式。核爆嬗变测试需要深板岩矿石（Y 0 以下，或手动放置 `minecraft:deepslate_iron_ore` 等）。
- **电源**：创造模式发电机（Creative Generator，`generator/block_creative_generator`）。无限发电机已从创造栏隐藏（见 2.3）。
- **配置**：`config/mio_icif-common.toml` 的 `[NuclearExplosion]`：
  - `enableLaserNuclearExplosive = true`：默认 `false`。为 `false` 时超高能爆破模式不能发射（测试 4.2 需要打开）。
  - `enableNuclearExplosion = true`（默认）：核弹与反应堆核爆（测试 8.2）需要它。
- **给予物品**：`/give @s mio_icif:<id>`，例如 `/give @s mio_icif:generator/block_steam_reactor`。
- **反应堆启动**：反应堆或任一已连接的反应仓需要红石信号（红石块贴在旁边即可）。
- **指令**：`/mio_icif uu scanner price|forget <物品 id>` 需要权限等级 2（单人游戏开启作弊即可）。

---

## 1. 反应堆增殖完成后永久停机（阶段 0，修复）

### 改动

- **原问题**：锂单元（Lithium Cell，`reactor/item_reactor_lithium_cell`）增殖完成变为燃料棒（氚）（Fuel Rod (Tritium)，`normal/item_tritium_cell`），或近衰变同位素棒（Near-Depleted Isotope Rod，`reactor/item_reactor_isotope_rod`）变为重新浓缩的铀（Re-Enriched Uranium，`normal/item_re_enriched_uranium`）后，反应堆读取不了这个产物，该次循环抛出异常，之后每个循环都失败，反应堆永久停机。
- **修复**：产物按惰性元件读取。
  - 燃料棒（氚）留在槽位内（对齐 IC2 `isUsefulItem`）。0.1.10.36 起它是中子源，见 9.2。
  - 重新浓缩的铀在下一个循环开始时弹出到反应堆旁边（掉落物）。
- 涉及：`ReactorInventory.java`（产物读取、`staysInReactor`）、`mio_icif_nuclear_reactor_generator.java`（`dropUnfitting` 保留氚燃料棒）。

### 测试

1. 放置核反应堆发电机（Nuclear Reactor Generator，`generator/block_nuclear_reactor_generator`），放入 1 根燃料棒(铀)，旁边放 1 个锂单元，加红石信号。
2. 等锂单元增殖完成（快速测试：把堆温调高，或用已接近完成的锂单元）。
3. **预期**：锂单元变为燃料棒(氚)并留在原槽位；反应堆继续运行（燃料值继续下降），不停机。
4. 用近衰变同位素棒重复。**预期**：变为重新浓缩的铀，下一秒弹出到反应堆旁；反应堆继续运行。

---

## 2. 文本与显示（阶段 1）

### 2.1 中文名称去掉重复词（zh_cn）

采矿镭射枪与 CF 喷枪的名称不变。

| 注册 id / 语言键 | 原名 | 新名 |
|---|---|---|
| `item_tool_electric_rifle` | 电力激光镭射步枪 | 电力激光步枪 |
| `item_tool_advanced_electric_rifle` | 进阶电力激光镭射步枪 | 进阶电力激光步枪 |
| `item_tool_tactical_laser_rifle` | 铱制刺刀战术激光镭射步枪 | 铱制刺刀战术激光步枪 |
| `item_tool_electric_plasma_gun` | 电力等离子电浆发射器 | 电力等离子发射器 |
| `producer/block_laser_defense_tower`（方块与界面标题） | 镭射激光防御塔 | 激光防御塔 |
| `producer/block_sky_patrol_laser_tower`（方块与界面标题） | 巡天镭射激光防御站 | 巡天激光防御站 |
| 死亡信息 `death.attack.laser_tower`（含 `.player`） | 被镭射激光防御塔汽化了 | 被激光防御塔汽化了 |

### 2.2 材料改名（注册 id 不变，四种语言）

| 注册 id | 中文 原 → 新 | English 原 → 新 |
|---|---|---|
| `resource/item_coal_chunk` | 煤块 → 石墨块 | Coal Chunk → Graphite Block |
| `resource/item_nano_living_metal` | 纳米活体金属 → 纳米超流体金属 | Nano Living Metal → Nano Superfluid Metal |
| `resource/item_neutron_plate` | 简并态中子板 → 简并态零素 | Neutron Plate → Degenerate Zero Matter |
| 模块提示 `module.mio_icif.tooltip.mk2_available` | （生体电路 + 中子板）→（生体电路 + 简并态零素） | neutron plates → Degenerate Zero Matter |
| `normal/item_advbat_lev0` | 高级充电电池 → **高级电池** | Advanced Rechargeable Battery → **Advanced Battery** |
| `normal/item_advchargebat_lev0` | 高级充电电池（不变） | Advanced Rechargeable Battery → **Advanced Charging Battery** |

高级电池与高级充电电池此前同名，现在四种语言都区分开。日语、俄语同步修正。纳米单元与激光防御塔的指南页随之更新。

### 2.3 隐藏内容

- “工业事故”（Industrial Accident）成就改为隐藏（`advancement/industrial_accident.json`：`"hidden": true`）。
- 无限发电机（Infinite Generator，`generator/block_unlimit_generator`）：
  - 从创造栏移除，改用创造模式发电机。
  - 加入 `c:hidden_from_recipe_viewers`，在 JEI / EMI 中隐藏。
  - 删除其指南页（英 / 中 / 日）。
  - 方块仍然注册，已放置的保留并继续工作。
- `c:hidden_from_recipe_viewers` 的已有条目改为 `{ "id": ..., "required": false }` 写法（缺少物品时不报错）。

### 测试

1. 切换到简体中文，查看 2.1 / 2.2 表中物品的名称（创造栏搜索）。**预期**：新名称；高级电池与高级充电电池名称不同。
2. 切换到 English，查看 `item_advbat_lev0` 与 `item_advchargebat_lev0`。**预期**：Advanced Battery / Advanced Charging Battery。
3. 打开成就界面。**预期**：未取得时看不到“工业事故”；取得后显示（对机器输入过高电压使其爆炸即可取得）。
4. 在创造栏与 JEI / EMI 中搜索“无限发电机”。**预期**：找不到；创造模式发电机仍在。
5. 用 `/give @s mio_icif:generator/block_unlimit_generator` 放置一个。**预期**：方块仍可放置、仍能工作。

---

## 3. EMI：带副产物的机器显示全部产物（阶段 1）

### 改动（`MioIcifEmiRecipe.java`）

| 机器 | 原显示 | 新显示 |
|---|---|---|
| 热能离心机（Thermal Centrifuge，`producer/block_centrifuge_elc`） | 只有主产物 | 全部产物排成一行，下方显示“最低热量” |
| 洗矿机（Ore Washer，`producer/block_washer_elc`） | 只有主产物 | 全部产物排成一行，下方显示“用水量” |
| 高炉（Blast Furnace） | 只有主产物 | 主产物与副产物（配方有副产物时） |

只有一个产物的配方仍居中显示。JEI 的显示不变（JEI 原本就显示全部产物）。

### 测试

1. 安装 EMI，查看任一粉碎矿石的热能离心机配方。**预期**：该配方的所有产物排成一行（与 JEI 中相同），下方有最低热量。
2. 查看洗矿机配方。**预期**：全部产物 + 用水量。
3. 查看高炉的配方。**预期**：有副产物的配方同时显示主产物与副产物。

---

## 4. 采矿镭射枪（Mining Laser，`item_tool_laser_miner`）（阶段 1）

### 4.1 3x3 模式

- **原问题**：一次发射 9 道平行激光，从玩家眼睛两侧出发，各自打中自己线路上的第一个方块（例如玩家身边的箱子）。
- **现在**：发射 1 道激光，命中后以命中的方块为中心，在被击中面的平面上挖掘 3x3。
  - 中心方块照常挖掘。
  - 外圈 8 格跳过带方块实体的方块（箱子、机器等）。
- 耗电 10,000 EU / 次（不变）。

### 4.2 超高能爆破模式（Ultra-High Energy Blast Mode）

- 直击目标受到 32767 点伤害（`Short.MAX_VALUE`）。
- 爆炸半径内（约 18.6 格）的全部实体也受到 32767 点伤害，掩体后面也受到（`LaserUltraBlastDamage`）。
- 半径以外到 2 倍半径的区域按原版衰减计算。
- 爆炸威力（地形破坏）不变。耗电 100,000 EU / 次（不变）。
- 需要配置 `enableLaserNuclearExplosive = true`（见第 0 节）。

### 测试

1. 镭射枪切到“3x3模式”（3x3 Mode），站在箱子旁边，对着 5 格外的石墙发射。**预期**：身边的箱子不受影响；墙上以命中点为中心出现 3x3 的洞。
2. 在石墙上放一个箱子作为 3x3 的外圈方块，对准它旁边的石头发射。**预期**：箱子保留，其余 8 格被挖掉。
3. 对准箱子本身发射。**预期**：中心的箱子被挖掉（中心方块照常处理）。
4. 打开配置后切到“超高能爆破模式”，向远处一群生物（含铁傀儡、凋零等高血量生物，可放在墙后）发射。**预期**：半径约 18 格内的生物全部死亡，墙后的也死亡。
5. 配置为 `false` 时尝试发射。**预期**：提示“已被服务器禁用”，不发射。

---

## 5. 期货市场：同材料标签的物品也可交付（阶段 2）

### 改动（`ItemMatch.java` 等）

- 悬赏与委托接受所请求物品本身，以及与它共享同一材料标签的其他模组物品（类似旧的矿物词典）。例如其他模组的铜锭可交付本模组铜锭的需求。
- 只计算具体的材料子标签：`c:<族>/<材料>`，族为 ingots、nuggets、dusts、gems、plates、gears、rods、raw_materials、storage_blocks、ores。不计算 `c:ingots` 这样的宽泛父标签。
- 交付时优先取所请求的物品本身，不够时再取同标签的其他物品；数量不足时什么都不取。
- 只计算“普通”物品：附魔、改名或损坏的物品不会被当作普通物品交付。
- 委托：发布者收到的是所请求的物品。记录、国库与邮箱仍记为所请求的物品。
- 数据包中的悬赏需求可以写 `"tag"`，指定接受的标签（不写时为物品自身的材料标签）。保存在悬赏数据的 `"Tag"` 中。
- 悬赏提示显示“也接受：#c:ingots/copper”（`gui.mio_icif.exchange.bounty_accepts`）。
- 测试用的标签 `c:gears/si_test_unification` 只在 `src/addonExample`（GameTest）中，不随模组发布。

### 测试

需要另一个提供铜锭（或其他 `c:ingots/*`）的模组；没有时可用数据包把原版物品加入某个材料标签代替。

1. 打开期货机（Futures Machine，`producer/block_future_elc`）的“悬赏”页，找一个要铜锭（或其他锭 / 粉）的悬赏，把鼠标悬停在需求上。**预期**：提示中有“也接受：#c:ingots/copper”。
2. 背包中只带其他模组的铜锭，交付该悬赏。**预期**：交付成功并获得奖励。
3. 背包中同时带本模组铜锭与其他模组铜锭，交付。**预期**：先扣本模组铜锭。
4. 带一个改名或附魔过的同标签物品交付。**预期**：该物品不计入。
5. 在“委托”页发布一个要铜锭的委托，用另一个玩家以其他模组铜锭完成。**预期**：完成；发布者收到的是本模组铜锭。

---

## 6. 护甲附魔与 3D 套装附魔光效（阶段 2）

### 6.1 附魔

- 青铜套装（`armor/item_armor_bronze_*`）与防化服套装（`armor/item_armor_hazmat_*`）加入原版的 `minecraft:head_armor` / `chest_armor` / `leg_armor` / `foot_armor` 标签，因此可以附原版护甲的全部附魔（含经验修补、耐久）。
- 纳米与量子套装（`armor/item_armor_nano_*`、`armor/item_armor_quantum_*`）加入 `minecraft:enchantable/<槽位>_armor`、`enchantable/equippable`、`enchantable/vanishing`，不加入 `enchantable/durability`：
  - 可附：保护类、荆棘、水下呼吸、水下速掘、迅捷潜行、摔落保护、深海探索者、冰霜行者、灵魂疾行，以及绑定诅咒、消失诅咒。
  - 不可附：经验修补、耐久（这两件没有耐久度，D11）。
  - 附魔台也能附魔：`mio_icif_armor_elc.isEnchantable` 对 `#minecraft:enchantable/armor` 中的物品返回 true；原版规则要求有耐久度，原先因此拒绝。
- 喷气背包与能量背包不在标签中，保持原样。
- 深海探索者与量子靴子的速度可以叠加（D12，未改动）。

### 6.2 3D 套装附魔光效

- 新渲染类型 `SUIT_ARMOR_GLINT`（`SiRenderTypes`）按套装的 128x128 贴图缩放附魔光效，注册为固定缓冲。
- 显示位置：
  - 穿在身上（`HumanoidArmorLayerGlintMixin`，已加入 `mio_icif.mixins.json` 的 client 列表）。
  - 第一人称手臂（`SuitHandRenderer`）。
  - 物品图标（`BakedArmorItemRenderer`）。
  - 武装库（`ArmoryPieceRenderer`）。
- 没有附魔的套装不显示光效。

### 测试

1. 用铁砧把“保护 IV”“经验修补”附魔书加到青铜头盔与防化头盔上。**预期**：都可以加上。
2. 把量子头盔放到附魔台。**预期**：可以附魔（之前不行）。
3. 用铁砧给量子胸甲加“保护 IV”。**预期**：成功。再加“经验修补”或“耐久”。**预期**：不能加上。
4. 给量子靴子加“深海探索者”“摔落保护”。**预期**：成功。
5. 穿上附魔的量子套装，按 F5 第三人称查看。**预期**：套装有细致的紫色附魔光效，贴图不拉伸。第一人称看手臂。**预期**：手臂有光效。
6. 查看背包中附魔量子胸甲的图标，以及武装库中的展示。**预期**：都有光效；未附魔的没有。

---

## 7. 采矿机：采矿镭射枪与收回管道（阶段 2）

只修改电力采矿机（Miner / Electric Miner，`producer/block_miner_elc`）。高级采矿机（Advanced Miner）完全不变（D6）。

### 7.1 钻头槽接受采矿镭射枪

- 镭射枪只能在“采矿模式”（Mining Mode）或“超级热线模式”（Super Heat Mode）下工作；其他模式在按钮上显示“采矿镭射枪须处于采矿模式或超级热线模式”。
- 速度：钻头的 5 倍（`LASER_SPEED = 5`）。
- 耗电：机器按钻石钻头的耗电计算；此外每条采矿路线从镭射枪自身扣 250 EU（`MINER_LASER_COST`）。
- 掉落：与普通钻石镐相同；超级热线模式会把掉落物冶炼（例如铁矿石 → 铁锭）。
- 采矿机会用自身储能给槽位中的镭射枪充电，即使会耗尽缓冲（D7）。

### 7.2 收回管道（IC2C）

- 界面右上角新增“收回”（Retract）按钮，再按一次变为“开采”（Mine），恢复开采。
- 收回条件：按下“收回”，或钻头槽为空。红石信号不阻止收回。
- 收回过程：每 5 tick 收回 1 根管道，每根 5 EU；每次收回最下面的一根（最先是钻头位置的那一根）。
- 管道去向：依次放入管道槽、采矿机存储栏、相邻容器。
- 收回期间停止开采；未完成的采矿路线先放弃。
- 按钮状态提示：
  - 正在收回管道。
  - 没有空间放管道（清空管道槽或存储栏）：暂停。
  - 保护阻止了移除管道：领地保护时显示。
  - 能量不足。
- 按钮状态保存在存档中（使用原先未用的 `IsPaused` 键）。
- 采矿机的指南页（英 / 中 / 日）已更新。

### 测试

1. 采矿机接电，放入采矿管道，钻头槽放采矿镭射枪（采矿模式，已充电），开始开采。**预期**：开采速度明显快于钻石钻头（约 5 倍）；镭射枪电量每条路线减少约 250 EU；采矿机给镭射枪补电。
2. 镭射枪切到超级热线模式再开采一段铁矿区域。**预期**：得到铁锭而不是粗铁。
3. 镭射枪切到“战斗模式”。**预期**：采矿机不开采，按钮提示须处于采矿或超级热线模式。
4. 管道下到约 20 格深后按“收回”。**预期**：从最下面开始，管道每 0.25 秒收回一根，回到管道槽；按钮显示“正在收回管道”；直到采矿机下方没有管道。
5. 管道槽与存储栏都放满，旁边不放箱子，再按“收回”。**预期**：暂停并显示“没有空间放管道”；在旁边放一个箱子后继续收回到箱子里。
6. 不按按钮，直接取出钻头。**预期**：自动收回管道。
7. 收回过程中断开电源。**预期**：显示“能量不足”并暂停。
8. 收回一半时保存并重新进入世界。**预期**：按钮状态保留，继续收回。
9. 确认高级采矿机的钻头槽仍不接受镭射枪。

---

## 8. 深层铱矿石与核爆嬗变（阶段 3）

### 8.1 新方块 / 新物品

| 名称 | 注册 id | 说明 |
|---|---|---|
| 深层铱矿石（Deepslate Iridium Ore） | `block_ore_iridium_in_deep` | 新方块。没有世界生成，只能由核爆嬗变产生。需要钻石镐。掉落小撮铱粉（受时运影响，`ore_drops` 公式）；精准采集掉落矿石本身。标签：`c:ores`、`c:ores/iridium`、`c:ores_in_ground/deepslate`（方块与物品）。 |
| 小撮铱粉（Small Iridium Dust） | `resource/item_iridium_dust_small` | 新物品。标签 `c:dusts/small_iridium`、`c:tiny_dusts/iridium`。 |

新配方：

| 配方 | 输入 | 输出 |
|---|---|---|
| `shaped_iridium_shard_from_small_dust`（工作台） | 9 × 小撮铱粉（3x3） | 1 × 铱碎片（Iridium Shard，`resource/item_shard_iridium`） |
| `compressor/iridium_dust_small_to_shard`（压缩机） | 9 × 小撮铱粉 | 1 × 铱碎片（100 tick，5 EU/t） |
| `powder/deepslate_iridium_ore_to_small_dust`（打粉机） | 1 × 深层铱矿石 | 2 × 小撮铱粉 |

### 8.2 核爆嬗变（D5）

- 核弹（Nuclear Bomb）与反应堆核爆在爆炸椭球内（含地下）把所有 `#c:ores_in_ground/deepslate` 矿石转为深层铱矿石，转换率 100%。
  - 数据标签 `mio_icif:nuke_transmutable` 包含 `#c:ores_in_ground/deepslate`，可用数据包扩展。
  - 范围是爆炸椭球本身（半径与竖直半径），不是 1.5 倍的辐射尘范围。
  - 采矿镭射枪的超高能爆破不触发嬗变。
- 嬗变随辐射尘逐区块执行：
  - 跳过调色板中没有矿石的区段。
  - 每步最多 256 格。
  - 进度保存在存档中，中途退出后继续。
  - 旧存档中已经发生的爆炸不追溯（旧区域 `transmute` 为 false）。
- 涉及：`NuclearFalloutState`、`NuclearFalloutManager`、`NuclearFalloutChunkTask.transmuteStep`。

### 测试

1. 在深板岩层（Y < 0）放若干深板岩铁 / 铜 / 金 / 钻石矿石，部分埋在 10 格以下的地下。离开 30 格以上，用核弹引爆。
2. 等辐射尘处理完成（可能需要几十秒；附近区块必须保持加载）。
3. 挖开爆炸范围内的位置（注意辐射：穿全套防化服或量子套装）。**预期**：爆炸椭球内的深板岩矿石全部变为深层铱矿石，含地下的；范围外的不变；普通石头层的矿石（非深板岩）不变。
4. 用钻石镐挖深层铱矿石。**预期**：掉落小撮铱粉；时运 III 掉得更多；精准采集掉落矿石本身；铁镐挖不出东西。
5. 9 个小撮铱粉在工作台合成 1 个铱碎片；放入压缩机（9 → 1）；矿石放入打粉机（1 → 2 小撮）。
6. 让反应堆核爆（例如蒸汽反应堆或核反应堆过热）在深板岩层附近发生。**预期**：同样嬗变。
7. 用采矿镭射枪超高能爆破炸深板岩矿石。**预期**：不嬗变。
8. 嬗变进行中保存退出再进入。**预期**：继续处理，不重复也不丢失。

---

## 9. 核燃料：锂增殖、氚中子源、质子燃料棒（阶段 3，D3 / D4）

### 9.1 锂单元（Lithium Cell，`reactor/item_reactor_lithium_cell`）：真实增殖

- 每次吸收脉冲，增殖进度增加 1 + 堆温 / 3000（IC2 实验版公式），因此冷的反应堆也能增殖。达到 10,000 时变为燃料棒(氚)。
- 每次吸收脉冲向反应堆外壳放热 1 HU（锂-6 + 中子 → 氚 + 氦-4 + 4.8 MeV）。
- 不产生 EU（IC2）。

### 9.2 燃料棒(氚)（Fuel Rod (Tritium)，`normal/item_tritium_cell`）：中子源

- 物品 id 不变。新物品类 `mio_icif_tritium_cell`，带反应堆元件数据。
- 相邻的每根燃料棒多受 1 次脉冲：EU 与热量都按多出的这次脉冲增加。
- 每个周期衰变 1，共 10,000 个周期（反应堆运行时每秒 1 个周期）；每个周期向外壳放热 1 HU。
- 衰变结束后变为燃料棒(空)（Fuel Rod (Empty)，`normal/item_fuel_rod`），氦-3 逸出。
- 新的提示行：
  - “中子源：每根相邻燃料棒多受一次脉冲”。
  - “衰变：x / 10000 个周期”。
- 由锂单元增殖得到的氚燃料棒从未衰变状态开始（增殖进度不当作衰变进度）。
- 不论燃料是否启用，衰变都继续。

### 9.3 质子燃料棒（新物品 6 种）

| 名称 | 注册 id |
|---|---|
| 燃料棒(质子)（Fuel Rod (Proton)） | `reactor/item_reactor_proton_simple` |
| 双联燃料棒(质子)（Dual Fuel Rod (Proton)） | `reactor/item_reactor_proton_dual` |
| 四联燃料棒(质子)（Quad Fuel Rod (Proton)） | `reactor/item_reactor_proton_quad` |
| 燃料棒(枯竭质子) / 双联 / 四联（Depleted Proton） | `reactor/item_reactor_proton_{simple,dual,quad}_depleted` |

- 数值（占位值，D4）：EU 为同规格铀燃料棒的 3 倍，热量为 1.5 倍，寿命 40,000 个周期（铀为 20,000）。
- 单根单独使用：铀为 5 EU/t、4 HU；质子为 15 EU/t、6 HU。

配方：

| 配方 | 输入 | 输出 |
|---|---|---|
| 粒子聚合发生器（Particle Aggregator，`producer/block_neutron_polymerizer`），`neutron_polymerizer/uranium_rod_to_proton_rod` | 1 × 满燃料值的燃料棒(铀)（20000 / 20000） | 1 × 燃料棒(质子)。4000 tick，2048 EU/t |
| `shaped_proton_dual`（工作台） | 燃料棒(质子) + 铁板 + 燃料棒(质子)（`UIU`） | 双联燃料棒(质子) |
| `shaped_proton_quad`（工作台） | `UIU / CIC / UIU`：U 燃料棒(质子)，I 铁板，C 铜板 | 四联燃料棒(质子) |
| 热能离心机 `centrifuge/proton_simple_depleted`（最低热量 4000） | 燃料棒(枯竭质子) | 4 × 铀-238 + 1 × 铁粉 |
| 热能离心机 `centrifuge/proton_dual_depleted` | 双联燃料棒(枯竭质子) | 8 × 铀-238 + 3 × 铁粉 |
| 热能离心机 `centrifuge/proton_quad_depleted` | 四联燃料棒(枯竭质子) | 16 × 铀-238 + 7 × 铁粉 |

### 9.4 其他

- 反应堆模拟器（指南）的调色板每行 17 格，能放下新燃料（`ReactorSimulatorWidget`）。
- 反应堆布局分析识别氚燃料棒（`ReactorLayoutAnalysis`）。
- 事故调查的元件种类新增“中子源”。
- 质子燃料棒贴图由铀燃料棒派生（燃料窗口为紫色），见 `tools/tex_1136.py`。

### 测试

1. 核反应堆中只放 1 根燃料棒(铀)，接红石，查看输出。**预期**：5 EU/t。换成燃料棒(质子)。**预期**：15 EU/t，堆温上升更快（6 HU/s 对 4 HU/s，需有散热时看散热片）。
2. 燃料棒(铀)旁放一根燃料棒(氚)。**预期**：输出变为 10 EU/t（多一次脉冲）；氚燃料棒提示中的衰变数每秒增加 1；堆温略有上升。
3. 用 NBT 或等待，使氚燃料棒衰变满 10000。**预期**：变为燃料棒(空)，输出回到 5 EU/t。
4. 燃料棒(铀)旁放锂单元，反应堆保持冷却（堆温 0）。**预期**：锂单元进度每秒仍增加（冷堆也能增殖），堆温有少量上升（每次脉冲 1 HU）。
5. 粒子聚合发生器放入满燃料值的燃料棒(铀)。**预期**：产出燃料棒(质子)；燃料值不满的燃料棒(铀)不会被加工。
6. 合成双联、四联质子燃料棒；枯竭质子燃料棒放入热能离心机（热量 ≥ 4000）。**预期**：按上表产出。
7. 打开指南中的反应堆模拟器。**预期**：调色板每行 17 格，有质子燃料棒与氚燃料棒。

---

## 10. 扫描仪为其他模组物品定价（阶段 4.1，D15）

复制机（Replicator）、模式存储机（Pattern Storage）与现有的全局定价（`UuPricingLifecycle` / `UuRecipeSolver`）都没有改动。新的定价逻辑全部属于模式扫描机（Pattern Scanner，`producer/block_scanner_elc`），代码在 `common/processing/scanner/`。

### 10.1 定价方式

只对“没有已发布价格、也没有被拒绝”的物品生效。已有价格的物品照旧。

- **按配方推算**：以已发布价格为固定点，用现有配方求解器（`UuCraftingRecipes`、`UuProcessingRecipes`、`UuRecipeSolver`，最便宜路径、防循环）推出一切可以合成的物品。
  - 配方中不含任何原版物品的其他模组物品也能推算。
  - 互相合成的循环组（如紫水晶碎片 ↔ 紫水晶块）补充估算后再求解。
  - 补充估算的价格不低于该物品自身的配方成本，以防复制过于便宜。
- **估算**：配方无法得到的输入（其他模组的矿石、植物、生物掉落），以及推算后仍然无法到达的物品：
  - 有同一 `c:` 材料标签的物品：用该标签中已发布物品的价格。
  - 方块：按所需工具等级（无 / 石 / 铁 / 钻石 / 下界合金）× 硬度 / 3 计算；矿石再 × 3。
  - 其他物品：基础值 × 稀有度（普通 / 罕见 / 稀有 / 史诗 = 1 / 3 / 10 / 30）× 堆叠（64 / 16 / 1 = 1 / 2 / 5）×（1 + 耐久 / 500）；防火物品再 × 2。
  - 参数在数据包 `data/mio_icif/uu/scanner/estimate_policy.json`。
- **排除**：
  - `data/mio_icif/uu/scanner/excluded_namespaces.json`：魔法模组，默认 29 项，如 botania、ars_*、irons_spellbooks、bloodmagic。`"name*"` 匹配前缀，多个数据包合并，`"replace": true` 覆盖下层数据包。
  - 仅创造模式可得的物品。
- **扫描形态**：使用物品的默认形态，除非该组件变体本身有价格（如药水、附魔书）。改名、损坏或装有内容的物品按全新物品扫描，内容不会被复制。

### 10.2 保存与读取

- 扫描所得价格按世界保存（存档数据 `si_scanned_uu_prices`），并作为“扫描仪价格”登记进 `UuQuoteBook`。复制机照常读取存储水晶并在复制时核对 `UuQuoteBook`，因此能复制该物品。
- 之后数据包给出该物品的价格时，以数据包为准。
- 市场的基本价值只使用已发布价格，不使用扫描仪价格（`ExchangeService.uuBuckets` 使用 `publishedQuote`）。

### 10.3 界面与指令

- 扫描仪界面显示价格来源：“按配方计算”“估算”；被排除的物品显示“不可扫描”。菜单同步数据第 15 号为 `priceSource`（共 16 个）。
- 管理员指令（权限 2）：
  - `/mio_icif uu scanner price <物品 id>`：显示已发布价格、已保存的扫描仪价格，或“尚未扫描；扫描将得到 X B（按配方计算 / 估算）”，或“不为此物品定价 / 无法计算”。
  - `/mio_icif uu scanner forget <物品 id>`：删除已保存的扫描仪价格，下次扫描重新计算。

### 测试

需要装一个其他内容模组（例如 Create、Mekanism、Farmer's Delight）；不装时可用 `price` 指令寻找一个尚无价格的原版物品。

1. `/mio_icif uu scanner price minecraft:diamond`。**预期**：显示已发布价格。
2. `/mio_icif uu scanner price <其他模组的机器或零件 id>`。**预期**：“尚未扫描；扫描将得到 X B（按配方计算）”。
3. 把该物品放入模式扫描机，放入模式存储水晶，接电完成扫描。**预期**：界面显示“按配方计算”，扫描完成，水晶记录该物品。
4. 用复制机读取该水晶，供给 UU 物质与电力。**预期**：复制成功；复制机显示的 UU 量与指令显示的价格对应。
5. 扫描一个其他模组的矿石或作物（没有配方能产出）。**预期**：界面显示“估算”。
6. 扫描一个魔法模组物品（如 botania）或仅创造物品。**预期**：界面显示“不可扫描”，不进行扫描。
7. 扫描一把改名并损坏的其他模组工具。**预期**：按全新物品定价，复制出来的是全新的、未改名的工具。
8. 保存退出再进入，执行 `price` 指令。**预期**：显示已保存的扫描仪价格。执行 `forget` 后再 `price`。**预期**：回到“尚未扫描”。
9. 打开期货机的行情页。**预期**：其他模组物品不会因为被扫描而出现市场价格。
10. 确认复制机界面与行为没有任何变化。

---

## 11. 蒸汽反应堆系统（阶段 4.2，D16）

### 11.1 新方块 / 新物品

| 名称 | 注册 id | 说明 |
|---|---|---|
| 蒸汽反应堆（Steam Reactor） | `generator/block_steam_reactor` | 新方块。核反应堆发电机的子类，方块实体 `steam_reactor` |
| 蒸汽反应仓（Steam Reactor Chamber） | `reactor/block_steam_reactor_chamber` | 新方块。方块实体 `steam_reactor_chamber` |
| 蒸汽涡轮发电机（Steam Turbine Generator） | `generator/block_steam_turbine` | 新方块。方块实体 `steam_turbine`，菜单类型 `steam_turbine_menu`。名称避开已有物品“蒸汽涡轮”（Steam Turbine，`normal/item_steam_turbin`） |
| 蒸汽散热片（Steam Vent） | `reactor/item_reactor_steam_vent` | 新物品 |
| 反应堆蒸汽散热片（Reactor Steam Vent） | `reactor/item_reactor_steam_vent_core` | 新物品 |
| 超频蒸汽散热片（Overclocked Steam Vent） | `reactor/item_reactor_steam_vent_overclocked` | 新物品 |
| 高级蒸汽散热片（Advanced Steam Vent） | `reactor/item_reactor_steam_vent_advanced` | 新物品 |

创造栏位置：
- 蒸汽散热片 4 种：铱超频散热片之后。
- 蒸汽反应堆与蒸汽涡轮发电机：核反应堆发电机之后。
- 蒸汽反应仓：核反应仓之后。

### 11.2 蒸汽反应堆

- 燃料、热量计算、堆温阶段（起火、沸水、辐射、熔岩）、事故、核爆、黑匣子与事故调查都沿用核反应堆。
- 不产生 EU：输出速率为 0，不向导线输出（没有注册 EU 能力，也不在能量配置中）。
- 不组成 5x5x5 流体反应堆结构。
- 储罐：
  - 水 / 蒸馏水 16,000 mB，只能注入水或蒸馏水。
  - 蒸汽 128,000 mB，只能抽出蒸汽。
- 流体从反应堆本体或任一蒸汽反应仓进出。没有桶或单元槽：只能用管道、泵等流体设备注水。
- 每 tick 向相邻的流体设备推送蒸汽：每个面最多 8,000 mB，不推给反应仓与反应堆本身。
- 界面：流体反应堆界面，标题“蒸汽反应堆”，显示水储罐、蒸汽储罐，以及上一个周期产生的蒸汽量（mB）。
- 列数：3 列 + 每个相邻蒸汽反应仓 1 列（最多 9 列）。
- 启动：反应堆或任一已连接的反应仓需要红石信号。
- 方块标签与核反应堆相同（`tools/data_1136.py` 按核反应堆所在的标签复制），包括 `mio_icif:reactor_heat_immune`（堆温阶段不破坏它）与镐可挖。
- 反应堆核爆时同样触发 8.2 的嬗变。
- 修复：流体反应堆界面原先只认核反应堆方块，蒸汽反应堆的界面会立即关闭（GameTest 发现，已修复）。

### 11.3 蒸汽反应仓与连接规则

- 蒸汽反应仓只连接蒸汽反应堆；核反应仓只连接核反应堆发电机。放置、存活与连接判定都区分两种（`ChamberTopology.nearbyCores(..., steam)`）。
- 反应仓必须只接触一个对应类型的反应堆，否则不能放置。
- 蒸汽反应仓共享反应堆的物品栏、热量、水与蒸汽：右键打开反应堆界面，管道可以从反应仓注水、抽蒸汽。
- 蒸汽反应仓也在 `reactor_heat_immune` 等标签中（与核反应仓相同）。

### 11.4 蒸汽散热片

| 名称 | 耐热 | 每周期产汽能力（单位散热热量） | 每周期从外壳抽热 | 提示中的最大蒸汽量 |
|---|---|---|---|---|
| 蒸汽散热片 | 1100 | 12 | 0 | 480 mB / 20 tick |
| 反应堆蒸汽散热片 | 1100 | 10 | 10 | 400 mB / 20 tick |
| 超频蒸汽散热片 | 1100 | 40 | 72 | 1600 mB / 20 tick |
| 高级蒸汽散热片 | 1100 | 24 | 0 | 960 mB / 20 tick |

- 在蒸汽反应堆中（新元件种类 `ReactorCycle.Kind.STEAM_VENT`，序号追加在末尾，旧黑匣子记录不受影响）：
  1. 先从外壳抽热（表中第 4 列）。
  2. 效率 =（自身热量 − 100）/ 1000，限制在 0～1：100 HU 时为 0，1100 HU 时为 1。
  3. 产汽单位 = min(⌊能力 × 效率⌋, 自身热量, 水与蒸汽空间允许的量)。
  4. 1 单位 = 40 mB 蒸汽（`FluidReactorCycle.HU_OUTPUT_MODIFIER`），耗水 0.4 mB（每 1 mB 水产 100 mB 蒸汽，与蒸汽发生器相同）。
  5. 转为蒸汽的热量离开散热片，不算作排出热量。
  6. 缺水或蒸汽储罐满时，热量留在散热片中，散热片会越来越热，直到熔毁。
- 在核反应堆或流体反应堆中：作为普通散热片，排出“能力”数值的热量。
- 提示新增一行（青色）：“在蒸汽反应堆中：用自身热量产生蒸汽，最多 X mB / 20 tick，越热越多”。
- 事故调查界面显示蒸汽散热片的热量条，种类名“蒸汽散热片”。
- 反应堆模拟器的调色板不含蒸汽散热片（调色板空间有限），模拟器不模拟蒸汽。

### 11.5 蒸汽涡轮发电机

- 电压等级 MV（指南实测：输出 MV、64 EU/t、储能 32,000 EU）。
- 每 2 mB 蒸汽产生 1 EU，最高 64 EU/t，即最多消耗 128 mB/t（IC2C 的蒸汽涡轮约为 2 mB / EU）。
- 蒸汽储罐 16,000 mB，储能 32,000 EU。
- 沿用半流质发电机的界面与槽位：
  - 输入槽放满的蒸汽单元（Steam Cell）→ 倒入储罐，空单元进入输出槽。
  - 电池槽给电池充电。
- 储能满时停止消耗蒸汽（蒸汽留在储罐中）。
- 实现：半流质发电机新增覆写点（可用燃料、燃料价值、每单位 mB 数、储罐容量、速率、电压等级），默认值保持半流质发电机原有行为；涡轮使用独立菜单类型，复用半流质发电机的界面。
- 标签：镐可挖、`c:machine`、`c:needs_stone_tool`、`mio_icif:generator`（与半流质发电机相同）。

### 11.6 配方

| 配方 id | 形状 | 材料 | 产物 |
|---|---|---|---|
| `shaped_steam_reactor` | `AXA / YYY / ACA` | A 高级合金板（`c:plates/alloy`），X 高级电路板（`c:circuits/advanced`），Y 蒸汽反应仓，C 核反应堆发电机 | 蒸汽反应堆 |
| `shaped_steam_reactor_chamber` | `XYX / YCY / XYX` | X 高级合金板，Y 防爆玻璃（Blast Glass，`build/block_alloy_glass`），C 核反应仓 | 蒸汽反应仓 |
| `shaped_steam_turbine` | `SSS / GET / SSS` | S 精炼铁质外壳（`c:casings/steel`），G 火力发电机（Thermal Generator），E 电动马达（Electric Motor），T 蒸汽涡轮（`normal/item_steam_turbin`） | 蒸汽涡轮发电机 |
| `shaped_steam_vent` | `I#I / #X# / I#I` | I 玻璃（`c:glass_blocks`），# 铁栏杆，X 散热片 | 蒸汽散热片 |
| `shaped_steam_vent_core` | 同上 | X 反应堆散热片 | 反应堆蒸汽散热片 |
| `shaped_steam_vent_overclocked` | 同上 | X 超频散热片 | 超频蒸汽散热片 |
| `shaped_steam_vent_advanced` | 同上 | X 高级散热片 | 高级蒸汽散热片 |

另有 3 个方块的战利品表：挖掘时掉落方块本身，带 `survives_explosion` 条件（被爆炸破坏时按原版规则可能不掉落）。

### 测试

参考数据：一根双联燃料棒(铀)每秒约 24 HU，平均分给相邻的散热片。2 个高级蒸汽散热片时，每个每周期得 12 HU，在约 600 HU（效率 0.5）处平衡：共约 960 mB 蒸汽 / 秒（约 48 mB/t），耗水约 9.6 mB / 秒，涡轮约 24 EU/t。散热片从 0 升到平衡约需 50 秒；100 HU 以下不产汽。

1. **放置与连接**：
   1. 放蒸汽反应堆，在它的一个面放蒸汽反应仓。**预期**：可以放置；界面中多一列槽位（4 列）。
   2. 在它的另一个面放核反应仓。**预期**：放不上。
   3. 在核反应堆发电机旁放蒸汽反应仓。**预期**：放不上。
   4. 在两个蒸汽反应堆之间放蒸汽反应仓（同时接触两个）。**预期**：放不上。
2. **界面**：右键蒸汽反应堆与蒸汽反应仓。**预期**：都打开“蒸汽反应堆”界面（显示水与蒸汽储罐），界面不会立即关闭。
3. **注水**：用泵（Pump，`producer/block_pump_elc`）加青铜管道（Bronze Fluid Pipe），或输水管道，接到反应堆或蒸汽反应仓。**预期**：水储罐上升到 16,000 mB。试注入熔岩或其他流体。**预期**：不接受。
4. **燃料与散热片**：放入 1 根双联燃料棒(铀)，左右各放 1 个高级蒸汽散热片。悬停查看散热片提示。**预期**：有青色的蒸汽说明行，高级的为“最多 960 mB / 20 tick”。
5. **涡轮**：在反应堆本体的另一面（不是反应仓）放蒸汽涡轮发电机，用 MV 导线接一个储电箱，再加红石信号启动反应堆。
6. **预期**：
   - 散热片热量从 0 上升；超过 100 HU 后开始产汽。
   - 反应堆界面显示每周期产汽量，水储罐缓慢下降。
   - 涡轮储罐有蒸汽，储电箱开始充电（平衡后约 24 EU/t）。
   - 反应堆本身没有 EU 输出（导线接到反应堆上没有电）。
7. **不接涡轮**：拆掉涡轮。**预期**：蒸汽储罐逐渐上升到 128,000 mB。蒸汽满后散热片热量持续上升（不要等到熔毁，看到上升即可）。
8. **缺水**：停止注水，等水用完。**预期**：不再产汽；散热片热量持续上升。约 30 秒后取出燃料，或恢复注水。
9. **普通散热片**：把一个高级蒸汽散热片换成普通高级散热片。**预期**：该散热片照常排热，不产汽。
10. **在核反应堆中**：把蒸汽散热片放进核反应堆发电机。**预期**：作为普通散热片工作（每周期排出能力数值的热量）。
11. **涡轮单独测试**：
    1. 用蒸汽单元放入涡轮输入槽。**预期**：蒸汽进入储罐，空单元出现在输出槽。
    2. 接 MV 导线。**预期**：最高 64 EU/t。
    3. 储能满且不接用电器。**预期**：蒸汽不再减少。
12. **多反应仓**：蒸汽反应堆六个面中五个放蒸汽反应仓，一个放涡轮。**预期**：8 列槽位。用管道从某个反应仓抽蒸汽。**预期**：可以抽出。
13. **保存与重载**：运行中保存退出再进入。**预期**：水、蒸汽储罐与散热片热量保留，继续运行。
14. **事故**：拿走散热片让反应堆过热。**预期**：与核反应堆相同的堆温阶段与爆炸；事故调查与黑匣子正常；蒸汽反应堆与反应仓不被堆温阶段破坏。
15. **指南**：打开指南的发电机分类。**预期**：有蒸汽反应堆、蒸汽涡轮发电机页面；反应堆分类有蒸汽反应仓页面（英 / 中 / 日）。蒸汽反应堆页面的“电力数据”写“该方块不使用EU”，并链接到核反应堆的堆温阶段、辐射屏蔽、事故调查；涡轮页面显示 MV、64 EU/t、32,000 EU。
16. **配方**：在 JEI / EMI 中查看 11.6 的 7 个配方。**预期**：全部存在、可合成。

---

## 12. 资源、工具与测试代码

- **语言**：`tools/lang_1136.py` 统一写入四种语言（en_us、zh_cn、ja_jp、ru_ru），带 `--check` 检查。本版新增或修改的键涵盖 2.1、2.2 的名称、悬赏“也接受”、采矿机按钮与状态、质子燃料、中子源、深层铱矿、扫描仪界面与指令、蒸汽反应堆系统。
- **贴图**：`tools/tex_1136.py`（PIL）从现有贴图派生：
  - 质子燃料棒：由铀燃料棒派生，紫色燃料窗口。
  - 深层铱矿石与小撮铱粉：由深板岩铅矿石与小撮铅粉派生。
  - 蒸汽反应堆与蒸汽反应仓：核反应堆与核反应仓改为钢蓝色。
  - 蒸汽散热片：由散热片改为青色。
  - 蒸汽涡轮发电机：由动能发电机派生，外壳淡钢蓝色，正面转子孔为蒸汽青色。
  - 同时生成方块状态与模型。
- **数据**：`tools/data_1136.py` 生成配方、战利品表、进度（配方解锁）与标签。向已有标签文件插入条目时保持原有格式，不重新排版。
- **指南**：
  - 无限发电机页面已删除；纳米单元、激光防御塔、采矿机页面已更新。
  - 新增蒸汽反应堆、蒸汽反应仓、蒸汽涡轮发电机页面（英 / 中 / 日）。
  - 源数据：`tools/guide_data/generators.json`、`heavy.json`、`producers.json`、`si_specs.json`（只加入涡轮的实测数据）。
  - 页面由 `tools/gen_guide.py` 在临时目录生成后只复制这些页面，不重建整个手册。
- **GameTests**：`src/addonExample/java/example/sicore/Round1136GameTests.java`，14 项全部通过：
  - `finishedLithiumCellStaysAsTritiumAndTheReactorKeepsRunning`、`finishedIsotopeRodIsThrownOutAndTheReactorKeepsRunning`
  - `laserThreeByThreeMinesAroundTheAimedBlockOnly`、`laserUltraBlastHitsEverythingInTheRadiusFor32767`
  - `marketAcceptsItemsOfTheSameMaterialTag`
  - `suitArmorTakesEnchantmentsButNanoAndQuantumNeverMending`
  - `minerTakesTheLaserAndRetractsItsPipes`
  - `protonTritiumAndLithiumFollowTheirProfiles`
  - `deepslateIridiumOreDropsSmallDust`、`nuclearBlastTurnsDeepslateOresIntoIridiumOre`
  - `scannerPricesItemsWithoutAPublishedPrice`、`scannerRegistersAPriceTheReplicatorCanRead`
  - `steamVentsTurnTheirHeatIntoSteamWithinWaterAndRoom`、`steamReactorTakesSteamChambersAndFeedsATurbine`
- **其他测试代码**：
  - `Round33GameTests` 的指南完整性测试跳过 `c:hidden_from_recipe_viewers` 中的方块（无限发电机已无指南页）。
- **开发用**：`CaptureDirector.scene1136`（`-Dsi.capture.scene=1136`），用于客户端截图核对，仅开发环境使用。

---

## 13. 未改动的部分与已知限制

- **未改动（按决定）**：
  - 复制机、模式存储机、全局 UU 定价（D15）。
  - 高级采矿机（D6）。
  - 深海探索者与量子靴子的速度叠加（D12）。
  - 无过热蒸汽（D16）。
- **游戏内未核对**：蒸汽反应堆、蒸汽反应仓、蒸汽涡轮发电机、蒸汽散热片的贴图与界面没有在客户端截图核对（GameTest 只验证了逻辑）。套装附魔光效与采矿机界面已截图核对。
- **蒸汽反应堆没有物品注水方式**：没有桶或单元槽，只能通过流体设备（管道、泵）进出水与蒸汽。
- **反应堆模拟器不含蒸汽散热片**，不模拟蒸汽产量。
- **蒸汽涡轮发电机没有机器运行音效**；JEI / EMI 中没有蒸汽反应堆的专用配方类别。
- **既有的 GameTest 失败**：`runAddonGameTestServer` 共 47 项失败，与本版本改动前相同，均为既有问题。其中 `listingtradesbetweenplayerswithescrow` 是测试世界跨次累积交易记录导致的“前 5 名交易对”断言失败。
- **CHANGELOG 第 3 行**：“累计记录 `0.1.7.16` 至 `0.1.10.35`”尚未改为 0.1.10.36。

---

## 附录 A：新方块与新物品总表

| 类型 | 名称 | 注册 id | 获取 |
|---|---|---|---|
| 方块 | 深层铱矿石 | `block_ore_iridium_in_deep` | 核爆嬗变 |
| 方块 | 蒸汽反应堆 | `generator/block_steam_reactor` | 合成 |
| 方块 | 蒸汽反应仓 | `reactor/block_steam_reactor_chamber` | 合成 |
| 方块 | 蒸汽涡轮发电机 | `generator/block_steam_turbine` | 合成 |
| 物品 | 小撮铱粉 | `resource/item_iridium_dust_small` | 挖深层铱矿石；打粉机 |
| 物品 | 燃料棒(质子) | `reactor/item_reactor_proton_simple` | 粒子聚合发生器 |
| 物品 | 双联燃料棒(质子) | `reactor/item_reactor_proton_dual` | 合成 |
| 物品 | 四联燃料棒(质子) | `reactor/item_reactor_proton_quad` | 合成 |
| 物品 | 燃料棒(枯竭质子) ×3 规格 | `reactor/item_reactor_proton_{simple,dual,quad}_depleted` | 质子燃料棒燃尽 |
| 物品 | 蒸汽散热片 ×4 | `reactor/item_reactor_steam_vent{,_core,_overclocked,_advanced}` | 合成 |
| 物品（改为元件） | 燃料棒(氚) | `normal/item_tritium_cell`（id 不变） | 锂单元增殖 |

## 附录 B：新配方总表

| 配方 id | 类型 |
|---|---|
| `shaped_iridium_shard_from_small_dust` | 工作台 |
| `compressor/iridium_dust_small_to_shard` | 压缩机 |
| `powder/deepslate_iridium_ore_to_small_dust` | 打粉机 |
| `neutron_polymerizer/uranium_rod_to_proton_rod` | 粒子聚合发生器 |
| `shaped_proton_dual`、`shaped_proton_quad` | 工作台 |
| `centrifuge/proton_simple_depleted`、`proton_dual_depleted`、`proton_quad_depleted` | 热能离心机 |
| `shaped_steam_reactor`、`shaped_steam_reactor_chamber`、`shaped_steam_turbine` | 工作台 |
| `shaped_steam_vent`、`shaped_steam_vent_core`、`shaped_steam_vent_overclocked`、`shaped_steam_vent_advanced` | 工作台 |

## 附录 C：新数据包文件与标签

| 文件 | 用途 |
|---|---|
| `data/mio_icif/uu/scanner/excluded_namespaces.json` | 扫描仪不定价的命名空间 |
| `data/mio_icif/uu/scanner/estimate_policy.json` | 扫描仪估算参数 |
| `data/mio_icif/tags/block/nuke_transmutable.json` | 核爆嬗变为深层铱矿石的方块 |
| `data/c/tags/{block,item}/ores/iridium.json`、`c:dusts/small_iridium`、`c:tiny_dusts/iridium` | 铱矿与铱粉标签 |
| `data/minecraft/tags/item/{head,chest,leg,foot}_armor.json` | 青铜、防化服 |
| `data/minecraft/tags/item/enchantable/{head,chest,leg,foot}_armor.json`、`equippable.json`、`vanishing.json` | 纳米、量子 |
| `data/c/tags/item/hidden_from_recipe_viewers.json` | 加入无限发电机 |
| 方块标签 `mineable/pickaxe`、`needs_stone_tool`、`needs_diamond_tool`、`c:machine`、`c:ores`、`c:ores_in_ground/deepslate`、`mio_icif:generator`、`mio_icif:producer`、`mio_icif:reactor_heat_immune` | 加入新方块 |
