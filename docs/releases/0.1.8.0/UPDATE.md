# 0.1.8.0 更新说明

基于 0.1.7.36。本次提交以已合并 0.1.7.35.1 和 0.1.7.36 的上游 `0.1.7.36-beta`（`dfa8edac378b75d9a9a8fa98ee869b542edd529c`）为基线。构建环境为 Gradle 8.10.2、JDK 21、NeoForge 21.1.218。

## 交付方验证记录

| 项目 | 方法 | 结果 |
|---|---|---|
| 编译与打包 | `gradle build coreJar coreSourcesJar apiJar apiSourcesJar addonExampleJar --offline` | 成功 |
| 核心架构检查 | `checkCoreArchitecture`（核心不引用 `common`；示例附属只依赖核心 JAR） | 通过 |
| 公共 API 纯净性 | 扫描 `mio_icif-api-0.1.8.0.jar` 的字节码常量 | 318 个类，对运行时／预制／`common` 的引用 0 个 |
| 电网公平分配 | 独立 Java 测试 `FairShareCheck`（不含 Minecraft 运行时） | 60,737 项检查，0 失败；三台相同设备各得 10.6667 EU（旧算法 32／0／0） |
| 电网性能 | 32 源 × 128 收的完整窗口，充分预热 | 交付方独立基准：无损 57.9 µs／轮；比例线损最坏情况约 243.4 µs／轮（不代表整包性能） |
| Litematica 位解包 | 独立测试，对照 Litematica `setAt` 算法的参考打包器 | 1,211,179 项，调色板至 5000（2～13 位），跨 long 条目正确 |
| 反应堆模拟器与教程设计 | 独立测试，使用与反应堆方块相同的 `ReactorCycle` 与组件参数 | 教程 1～3 稳定（5／20／300 EU/t）；失败示例预测约 143 秒后熔毁；报告未记录实际熔毁 |
| 思索场景结构文件 | 独立 NBT 解析器回读 | 4 个文件结构完整，无残余字节 |
| 外部 API 签名 | 用 `javap` 对照本地 JAR：SlashBlade Resharped 2.1.9、车万女仆 1.5.3、RTS Building 1.1.7、机械动力 6.0.10（Ponder 1.0.82）、GuideME 21.1.17 | 反射成员与编译期 API 全部存在 |
| 发布 JAR 内容 | 检查 `mio_icif-0.1.8.0.jar` | 版本 0.1.8.0；新资源与类齐全；未打包仅编译依赖；不含开发用类 |

## 本次提交核验（2026-10-12）

* 来源：`SI_0.1.8.0_src.zip`，SHA-256：`5ee621be774af12c9d6314fd0a45da1fc94b8ec15580d2701d99f4b91072d9ef`。保留 0.1.7.35.1 的电动锄、蒸汽再加压机与配方，以及 0.1.7.36 的调节滑块修复。
* 使用缓存依赖完成离线构建与上述六类 JAR 打包；`checkCoreArchitecture`、`checkCoreDistribution` 通过。六个 JAR 解压后的文件内容均与交付包一致。`test` 为 `NO-SOURCE`，不代表运行过 GameTest。
* 对当前生产源独立编译并复跑：FairShare 60,736 项功能断言、LitematicBits 1,211,179 个条目、4 个 Ponder NBT 回读均通过。FairShare 本轮排除了原测试中的一项性能阈值检查。
* 4,033 个 JSON 解析通过；手册 STE 检查留下 12 条文风提示。五个资源工具已改用仓库相对路径或显式基线参数，移除六个 Python 缓存文件。
* 本次未运行游戏、GameTest、视觉或外部模组联动测试。

## 未验证项目（需要在游戏中确认）

* **GameTest 未运行。** 本地 GameTest 服务器无法启动（离线缓存缺少 netty epoll；联网下载经代理时 TLS 失败）。新增与修改的 GameTest 只做了编译。
* **没有在游戏中检查视觉效果。** 包括：套装模型与飞行姿态、分段飞入、全息投影、僚机渲染与 AI、建造无人机、战术地图、锁定框、共振力场、树脂动画、管道与武器模型、充能特效、GuideME 模拟器页面、思索场景。纹理为程序生成，风格需要实机确认。
* **外部模组联动没有实机运行。** 签名已对照本地 JAR 并通过编译，但联动行为（Resharped 刀身渲染方向、女仆装备、RTS 站点捕获、JEI 跳转、机械动力蓝图读取）需要在整合包中确认。
* **量子套装配色** 尚未开放（按需求先做纳米套装，界面已注明后续版本）。
* **武装库界面的扩展槽数量显示**（可选项）未实现。

## 构建依赖说明

* `libs/compile-only/guideme-21.1.17.jar` 与 `libs/compile-only/ponder-neoforge-1.0.82+mc1.21.1.jar` 是从本地文件复制的仅编译依赖（离线构建）。它们不打包进模组。联网构建时可改用 Maven 坐标（见 `build.gradle` 注释）。
* 新增访问转换器 `META-INF/accesstransformer.cfg`（一条：`Biome#getTemperature(BlockPos)`）。

## 主要变更

详见 `CHANGELOG.md` 的 0.1.8.0 一节。附属开发者请阅读 `docs/api/API_GUIDE.en.md` 或 `docs/api/API_GUIDE.zh_cn.md`。

## 生成脚本

资源由 `tools/` 中的脚本生成，可重复运行：`lang_180.py`（三语语言文件）、`assets_180.py`、`assets_builder_180.py`、`tex_180.py`、`tex_builder_180.py`、`tex_modules_180.py`、`tex_pipes_180.py`、`models_swords_180.py`、`ponder_schematics_180.py`、`gen_suit_armor.py`、`gen_suit_palette.py`、`sfx_180.py`。
