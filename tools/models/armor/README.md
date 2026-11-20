# 纳米与量子套装模型

穿戴资源使用模组自带的烘焙护甲渲染器。两个 Blockbench 工程都内嵌贴图，可直接打开编辑。下列命令从仓库根目录运行，需要 Python 3.10 或以上版本及 Pillow。

[查看游戏内穿戴、染色与第一人称截图](preview-in-game.png)

| 套装 | 编辑工程 | 重建游戏资源 |
|---|---|---|
| 纳米 | [nano_suit_pure.bbmodel](nano/nano_suit_pure.bbmodel) | `python tools/models/armor/nano/bake_nano.py` |
| 量子 | [quantum_armor.bbmodel](quantum/quantum_armor.bbmodel) | `python tools/models/armor/quantum/bake.py` |

纳米套的 4 张 16×16 物品栏图标可用 `python tools/models/armor/nano/generate_nano_icons.py` 重建。它们只用于 GUI，手持和掉落物视角继续使用 3D 模型。气密头罩随头盔上的毒素·辐射过滤器开关显示。[纳米穿戴预览](nano/preview.png)

量子套的原始几何、六段编辑动画与模块绑定分别位于游戏资源的 `mio_icif:geo/quantum_armor.geo.json`、`mio_icif:animations/quantum_armor.animation.json` 和 `mio_icif:armor_models/quantum_bindings.json`。每个主加速舱的磁轨末端有一个空定位骨，分别为 `thruster_nozzle_left` 和 `thruster_nozzle_right`。动画采样后，将定位骨的局部原点变换到世界空间即可取得喷口中心，局部出流轴为 `[0, -1, 0]`。游戏内沿用当前版本的特殊机动油门驱动舱体和动态尾焰。[量子套收纳与展开预览](quantum/preview.png)
