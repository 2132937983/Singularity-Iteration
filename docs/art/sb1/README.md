# SB1 · 联动拔刀

三把刀的出鞘、入鞘和物品栏使用同一机位对照，出入鞘共用握位与画面比例。[三刀同尺度总览](../previews/SB1-lineup.png)

## 纳米刃 · 打刀

第三版把薄刃校正到 +Y，刀和鞘沿同一条弧线从握位向刀尖下落。橙线沿着冷灰切先折面收尖。刀镡保留扁八角和两处短爪，鞘尾顺着弧线延出尖刺。碳纤维刀身上的压电片分组露出，便于在游戏距离分清橙线与切先。物品栏单独加宽了贴在刃面上的发光区，32 像素格里能辨认出橙线和交叉的黑鞘。[出鞘、入鞘与物品栏](../previews/SB1-nano_views.png) · [模板轮廓与五个截面](../previews/SB1-nano_skeleton.png)

| 游戏资源与源文件 | 物品栏与部件 | 材质预览 |
| --- | --- | --- |
| [OBJ 与两张 PNG](../../../src/main/resources/assets/mio_icif/model/slashblade/nano) · [Blender](blender/nano.blend) | [四档物品栏、展示框、掉落](../previews/SB1-nano_inventory.png) · [15 个对象](../previews/SB1-nano_parts.png) | [断刀与碎片](../previews/SB1-nano_broken.png) · [蜂窝蓄力](../previews/SB1-nano_charge.png) · [UV](../previews/SB1-nano_uv.png) |

## 量子刃 · 浅弯太刀

量子刃是三把中弯度最浅的一把。刀面用冷银、暗银和一条偏暖的棱线高光表现镜面钢。沿刃的青色波纹从刀根到刀尖逐渐收密，刀根的短槽留作暗银色。白陶瓷鞘分成四段，青色只出现在鞘口的一小段刻纹和鞘尾细环上。刀镡两条平行亮缝在近景里最清楚。[出鞘、入鞘与物品栏](../previews/SB1-quantum_views.png) · [模板轮廓与五个截面](../previews/SB1-quantum_skeleton.png)

| 游戏资源与源文件 | 物品栏与部件 | 材质预览 |
| --- | --- | --- |
| [OBJ 与两张 PNG](../../../src/main/resources/assets/mio_icif/model/slashblade/quantum) · [Blender](blender/quantum.blend) | [四档物品栏、展示框、掉落](../previews/SB1-quantum_inventory.png) · [15 个对象](../previews/SB1-quantum_parts.png) | [断刀与碎片](../previews/SB1-quantum_broken.png) · [干涉流光](../previews/SB1-quantum_charge.png) · [UV](../previews/SB1-quantum_uv.png) |

## 奇点刃 · 大太刀

三把中最长，末端略向刃口侧回钩。品红刃线在刀根分成几段裂缝。四块线圈围住刀镡内侧的暗芯，五道束箍沿刀鞘排开，鞘尾收成三爪。32 像素格保留品红刃线和环形光点，外观图底部的两个近景分别展示环内暗芯和三只鞘爪。[出鞘、入鞘与物品栏](../previews/SB1-singularity_views.png) · [模板轮廓与五个截面](../previews/SB1-singularity_skeleton.png)

| 游戏资源与源文件 | 物品栏与部件 | 材质预览 |
| --- | --- | --- |
| [OBJ 与两张 PNG](../../../src/main/resources/assets/mio_icif/model/slashblade/singularity) · [Blender](blender/singularity.blend) | [四档物品栏、展示框、掉落](../previews/SB1-singularity_inventory.png) · [15 个对象](../previews/SB1-singularity_parts.png) | [断刀与碎片](../previews/SB1-singularity_broken.png) · [双臂螺旋蓄力](../previews/SB1-singularity_charge.png) · [UV](../previews/SB1-singularity_uv.png) |

## 几何与游戏资源复核

| 刀种 | 长度／模板 | 中线落差／拱高 | 总顶点 | 三态物品栏主体／含发光 | 流光壳顶点 | 总三角面 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 纳米刃 · 打刀 | 102.72% | 21.99／3.13 | 1,756 | 695／1,009 | 56 | 2,846 |
| 量子刃 · 浅弯太刀 | 105.12% | 16.37／1.81 | 2,174 | 893／1,161 | 56 | 3,408 |
| 奇点刃 · 大太刀 | 112.34% | 24.86／4.31 | 2,291 | 895／1,169 | 56 | 3,839 |

长度从柄头量至刀尖，中线落差与拱高按批次脚本的截面位置计算。各刀的预算为总顶点 3,000、三态物品栏主体合计 1,200、流光壳 60。已用批次脚本校验九处横截面、+Y 薄刃及刀鞘贴合，并用 SlashBlade Resharped 2.2.1 的 OBJ 读取器载入。Blender 源文件内嵌两张贴图，重开后从全对象视图层导出，逐面核对三角形绕序、坐标和 UV。

物品栏页直接在 32、48、64、128 像素下渲染游戏模型，复核了完整、断刀、无鞘各自的模板外框和屏幕边缘。骨架页只绘制用户自备模板的二维轮廓和截面，用于比对。三维游戏网格和贴图均为本批原创。

## 给 Claude 的接入备注

模型使用模板握位原点，刀尖沿 −X。按拔刀剑的对象名将 `blade` 和 `sheath` 分别接到 `hardpointA` 与 `hardpointB`。每个文件含 8 个主体、7 个对应的 `_luminous` 对象。发光部分与主体共用 `texture.png`。蓄力时在鞘上叠画 `effect`，滚动 `charge.png` 的 U 坐标。纳米刃的断口露出石墨烯叠层，橙线在断口前熄灭。驻波、挥刀残影和自修复动画由客户端实现。

量子刃断口露出亮银断面和一道暗色心铁线，发光刃纹在断口前收住。B3 的概率云从刀镡双缝展开。两缝的中心在模型坐标 (−32.8, 1.25, ±2.30)，坍缩从切先附近 X ≈ −304 的青紫观测点起始。蓄力使用原有的青色莫尔纹。

奇点刃的两片断面均露出发光内芯。刀镡中心的黑色球壳留在主体层。实时引力透镜和吸积盘由客户端以环心为起点绘制。

预览为离线渲染。实机光影下的加色亮度以及接入后的动作衔接由客户端验收。32 像素格优先呈现刀种轮廓和发光辨识点，细小的刻线见各自的近景。

## 重建

下面以纳米刃为例。预览使用 NumPy、Pillow 和中文字体，可编辑工程用 Blender 4.4.3 生成。骨架图需要传入自备模板 ZIP 或 OBJ 的路径。Blender 默认打开 `Edit - drawn` 视图层，切到 `Export - all parts` 可导出所有状态。

```sh
python tools/art/sb1_nano_texture.py
python tools/art/sb1_nano_model.py
blender -b --factory-startup --python-exit-code 1 --python tools/art/sb1_blender.py -- nano
python tools/art/sb1_preview.py nano
python tools/art/sb1_skeleton.py "/path/to/默认拔刀剑模型.zip" nano
python tools/art/check_sb1.py nano
```

量子刃运行 `python tools/art/sb1_quantum_model.py` 可重建模型和两张贴图。后续命令的刀名改为 `quantum`。

奇点刃依次运行 `python tools/art/sb1_singularity_texture.py` 和 `python tools/art/sb1_singularity_model.py`，后续命令的刀名改为 `singularity`。三把完成后运行 `python tools/art/sb1_lineup.py` 生成同尺度总览。
