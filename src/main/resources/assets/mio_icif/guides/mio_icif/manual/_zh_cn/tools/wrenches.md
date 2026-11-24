---
navigation:
  title: "扳手与工具箱"
  icon: mio_icif:item_tool_wrench
  parent: tools.md
  position: 3
item_ids:
  - mio_icif:item_tool_wrench
  - mio_icif:item_tool_wrench_elc
  - mio_icif:normal/item_toolbox
  - mio_icif:normal/item_toolbox_x
  - mio_icif:build/item_obscurator
  - mio_icif:build/item_cf_sprayer
  - mio_icif:build/item_painter
---

# 扳手与工具箱

## 扳手

<ItemLink id="mio_icif:item_tool_wrench" />（耐久 120）和 <ItemLink id="mio_icif:item_tool_wrench_elc" />（50,000 EU）作用于标签 `mio_icif:wrenchable` 里的方块：机器、导线、管道、储物箱、多方块外壳和端口。

| 操作 | 结果 |
|---|---|
| 右键机器正面，或没有朝向的方块 | 拆下，物品保留设置和储电 |
| 右键其他面 | 把正面转向这一面 |
| 潜行右键 | 把正面转向点击面的反面 |
| 右键导线或管道的臂 | 封堵这一面；点中心解除 |

- 拆卸没有损失。储物箱右键直接拆下。
- 电扳手：拆卸 100 EU、转向 5 EU、导线和管道 2 EU。装备控制台可开关“拆卸时保留内容物”和“点正面拆卸”。没电时在动作栏提示；创造模式不耗电。
- 其他模组的扳手：潜行右键拆下本模组的方块。
- 附属模组的方块可以通过 `IWrenchable` 拒绝转向或拆卸，并自定掉落。

## 工具箱

<ItemLink id="mio_icif:normal/item_toolbox" />（9 格）和 <ItemLink id="mio_icif:normal/item_toolbox_x" />（27 格）右键打开，只能放工具。每次放入或取出都立即保存。工具箱打开时锁在手里。

## 建筑工具

- <ItemLink id="mio_icif:build/item_obscurator" />（100,000 EU）：潜行右键一个方块扫描它的外观（5,000 EU），右键本模组的机器或导线外壳套用这个外观（1,000 EU）。
- <ItemLink id="mio_icif:build/item_cf_sprayer" />：装建筑泡沫，右键喷出泡沫填满空处；潜行右键切换模式。逐格检查领地权限。
- <ItemLink id="mio_icif:build/item_painter" />：染色后右键羊毛、陶瓦、混凝土、玻璃等方块改成刷子的颜色，右键建筑泡沫变成同色混凝土。
