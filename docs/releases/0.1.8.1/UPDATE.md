# 0.1.8.1 更新说明

基于 0.1.8.0（上游导入的工作树 `pr-0.1.8.0-20261012`，其中包含 0.1.8.0 交付后的文档与工具脚本修正）。构建环境为 Gradle 8.10.2、JDK 21、NeoForge 21.1.218，离线构建。

## 交付方验证记录

| 项目 | 方法 | 结果 |
|---|---|---|
| 编译与打包 | `gradle build coreJar coreSourcesJar apiJar apiSourcesJar addonExampleJar --offline` | 成功（含新增 GameTest 的编译） |
| 核心架构检查 | `checkCoreArchitecture`、`checkCoreDistribution` | 通过 |
| 公共 API 纯净性 | 扫描 `mio_icif-api-0.1.8.1.jar` 的字节码常量 | 318 个类，对运行时／预制／`common` 的引用 0 个 |
| 作物公式 | 独立 Java 测试 `CropMathCheck`：`CropGrowthMath` 对照 0.1.8.0 的内联公式 | 1,202,703 项检查，0 失败（生长点、惩罚、死亡判定、属性继承、参与值、湿度、空气质量；土壤营养按新规则） |
| 作物客户端流量 | 同一测试中模拟 1000 株作物 × 400 个周期的资源衰减 | 数据包 400,000 → 9,884（2.5%） |
| 电网公平分配（回归） | `FairShareCheck`，使用 0.1.8.1 的核心类 | 60,736 项检查，0 失败 |
| Litematica 位解包（回归） | `LitematicBitsCheck`，使用 0.1.8.1 的生产类 | 1,211,179 项一致 |
| 机器音效审计 | `tools/sound_audit_181.py`（读源码，不启动游戏） | 0.1.8.0：35 台有运行状态却无音效；0.1.8.1：0 台。100 台有循环音，12 台带理由静音，65 台为被动方块 |
| 新音效客观检查 | `sfx_analyze.py`：峰值、RMS、直流、削波、循环接缝、频段能量 | 9 个文件无削波、直流 ≤ 0.002；喷气背包循环低频（< 200 Hz）占 23～48%，中高频均有能量 |
| 子网面板布局 | `subnet_layout_check.py`：按代码算术建模，英／中／日长字符串 | 204 个用例 0 重叠（同样字符串下 0.1.8.0 布局 12 行中 6 行重叠） |
| 三语注释 | `tools/doc_lint_181.py` | 40 个文件、189 个 Javadoc 块，0 错误、0 警告 |
| 思索场景与反应堆 | SHA-256 对照 0.1.8.0 | 4 个 NBT 与 `ReactorCycle` 未改动 |

## 未验证项目（需要在游戏中确认）

* **GameTest 未运行。** 本地 GameTest 服务器仍无法启动。新增的 `CropAudit181GameTests`（土壤营养、升级作物架杂交状态、杂草重置、收割机存储已满）与既有 GameTest 只做了编译。
* **音效没有试听。** 新音效为程序合成，只做了客观指标检查；音量平衡、喷气背包的临场感、循环接缝需要实机试听。
* **界面没有在游戏中查看。** 子网面板使用模型检查（字体宽度为上限估计），需要在三种语言下实机确认。
* **多人环境的喷气背包同步** 没有实测（其他玩家的推进音依赖 `jetpack_thrust` 数据包）。
* **不存在核燃料喷气背包。** 本模组只有电力喷气背包、高级喷气背包与量子胸甲（含高级量子胸甲）的喷气功能；附属可通过 `IJetpackItem#getThrusterSound()` 选择音效档。
* 日语语言文件整体仍只覆盖约 604／2475 个键（本次只补齐能源管理终端）。

## 兼容性

* 存档格式：作物收割机新增可选的 `PendingDrops` 列表；旧存档读取正常。其他存档字段不变。
* 网络：新增服务器到客户端数据包 `mio_icif:jetpack_thrust`；通道版本随模组版本变化（客户端与服务器必须同为 0.1.8.1）。
* API：`IJetpackItem` 新增默认方法 `getThrusterSound()` 与常量 `THRUSTER_ELECTRIC`／`THRUSTER_ADVANCED`／`THRUSTER_IONIC`（源码与二进制兼容）。

## 主要变更

详见 `CHANGELOG.md` 的 0.1.8.1 一节。注释规范见 `docs/dev/COMMENT_STYLE.md`。

## 生成脚本

`tools/sfx_181.py`（9 个新音效，WAV，用 ffmpeg 转为单声道 OGG）、`tools/lang_181.py`（三语键与 `sounds.json`）、`tools/sound_audit_181.py`（机器音效审计）、`tools/doc_lint_181.py`（三语注释检查）。
