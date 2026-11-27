# SB1 · 纳米刃

纳米刃采用碳纤维黑、冷灰陶瓷片和橙色刃线。出鞘时，亮线经过切先的折角收向刀尖。入鞘后，外侧三道斜向散热缝接替刃线，保留这把刀的辨识色。[出鞘、入鞘和物品栏同图](../previews/SB1-nano_views.png)

## 模型与预览

| 内容 | 文件 |
| --- | --- |
| 游戏资源 | [模型 OBJ](../../../src/main/resources/assets/mio_icif/model/slashblade/nano/model.obj) · [128×256 主贴图](../../../src/main/resources/assets/mio_icif/model/slashblade/nano/texture.png) · [64×64 蓄力流光](../../../src/main/resources/assets/mio_icif/model/slashblade/nano/charge.png) |
| 可编辑源文件 | [Blender 工程](blender/nano.blend)，内嵌两张贴图。默认显示出鞘姿态，各状态按集合分开。切到 `Export - all parts` 视图层可一次导出全部对象。 |
| 完整与入鞘 | [主图与两套四视图](../previews/SB1-nano_views.png) · [15 个对象拆分](../previews/SB1-nano_parts.png) |
| 物品栏 | [完整、断刀与无鞘三态](../previews/SB1-nano_inventory.png)，分别按 32、48、64、128 像素直接渲染，附展示框背面及掉落视图。 |
| 材质细节 | [手持断刀与落地碎片](../previews/SB1-nano_broken.png) · [刀鞘蓄力前后](../previews/SB1-nano_charge.png) · [UV 展开](../previews/SB1-nano_uv.png) |

## 给 Claude 的接入备注

- 完整刀、刀鞘和流光壳共用握位原点。刀尖朝游戏模型空间的 −X 方向。参考模板仅用于核对坐标、握位、方向和物品栏外框。所有交付网格和像素均为本批原创。
- `blade_damaged` 与 `blade_fragment` 沿同一条断口分开，各自带有石墨烯叠层截面。橙色刃线在断口前收住。七个 `_luminous` 对象使用主贴图，在完整、断裂、佩鞘和物品栏状态分别叠画。
- 蓄力时在 `sheath` 上叠画 `effect`，用 `charge.png` 沿鞘长方向滚动 UV，可看到蜂窝亮点沿刀鞘移动。驻波、挥刀残影和修复动画由客户端接入。预览使用交付的游戏资源离线渲染。不同光影包的加色亮度仍需在游戏中确认。
- 本次供评审的是纳米刃轮廓与物品栏交叉构图。设计沿用批次设定，刀长取模板的 102.72%，模型总长 341.74 单位，握柄末端仍为 X = 41.74。

## 验收

| 计数 | 使用量 | 批次上限 |
| --- | ---: | ---: |
| 全部 15 个对象 | 1,578 顶点 | 3,000 |
| 三个物品栏主体 | 695 顶点，计入对应发光层共 839 | 1,200 |
| 刀鞘流光外壳 | 56 顶点 | 60 |

仓库检查返回 `nano: OK`，SlashBlade Resharped 2.2.1 的 OBJ 读取器成功载入全部 15 个对象和 2,624 个三角面。用 Blender 重新打开源文件并导出后，逐面核对了三角形绕序、位置和 UV。七个发光对象实际覆盖的不透明像素平均亮度均高于 150。三种物品栏状态逐一对照各自的模板外框，并核对了四档 GUI 采样的边缘。

## 重建

在仓库根目录运行下列命令。预览脚本使用 NumPy、Pillow 和中文字体，Blender 工程由 4.4.3 生成。

```sh
python tools/art/sb1_nano_texture.py
python tools/art/sb1_nano_model.py
blender -b --factory-startup --python-exit-code 1 --python tools/art/sb1_blender.py
python tools/art/sb1_preview.py nano
python tools/art/check_sb1.py nano
```
