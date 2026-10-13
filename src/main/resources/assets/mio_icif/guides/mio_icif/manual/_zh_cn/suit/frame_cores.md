---
navigation:
  title: "框架核心"
  icon: mio_icif:module/item_core_propulsion
  parent: suit/index.md
  position: 2
item_ids:
  - mio_icif:module/item_core_solar_array_1
  - mio_icif:module/item_core_solar_array_2
  - mio_icif:module/item_core_solar_array_3
  - mio_icif:module/item_core_graviton
  - mio_icif:module/item_core_propulsion
---

# 框架核心

框架核心装在套装头盔或胸甲的**核心插槽**中。插槽独立于单元槽（量子）与晶格（纳米），核心不会占用单元的位置。

| 核心 | 插槽 | 效果 |
|---|---|---|
| <ItemLink id="mio_icif:module/item_core_solar_array_1" /> | 纳米 / 量子头盔 | 白天 8 EU/t，夜间 1 EU/t（露天） |
| <ItemLink id="mio_icif:module/item_core_solar_array_2" /> | 纳米 / 量子头盔 | 白天 64 EU/t，夜间 8 EU/t |
| <ItemLink id="mio_icif:module/item_core_solar_array_3" /> | 量子头盔 | 白天 512 EU/t，夜间 64 EU/t |
| <ItemLink id="mio_icif:module/item_core_graviton" /> | 量子胸甲 | 重力锚（免疫摔落伤害、不受击退、不被爆炸冲飞），护甲 +1，生命低于 20% 时紧急修复 |
| <ItemLink id="mio_icif:module/item_core_propulsion" /> | 量子胸甲 | +90,000,000 EU，无飞行高度上限，推力 +15%，特殊机动矢量加速增强，护甲 +1，击退抗性 +0.5，修复 2.0 生命/秒，电弧放电 |

## 安装、更换、取出

将部件与核心放入 <ItemLink id="mio_icif:producer/block_quantum_modification_station" />（核心放入单元槽）。安装需要通常的安装时间与电力。
向已有核心的部件放入另一个核心即为更换：旧核心回到单元槽，在你取出之前留在那里。
改装台界面的核心插槽框显示核心；点击旁边的小取出按钮即可取出核心（交给你）。
装有核心的部件在图标角落显示核心颜色的小菱形。

## 外装

核心会为穿戴的部件添加外装：桅杆上的太阳能板、带翼面板、带扇的太阳冠、重力环与陀螺仪及肩垫，或双推进器与胸前反应堆。
太阳能翼在露天白天展开，黑暗时收起；套装有电时重力环与陀螺仪旋转；推进喷口随推力发光。
可在奇点管理面板中用每个部件的"核心外装（3D）"开关关闭，或在**外观**（客户端选项 core_attachments）中全部关闭。

## 核心爆发

装有高级推进核心时按 **R**（核心爆发）：高压电弧放电命中周围所有生物，冲击波将其击退。消耗 1,500,000 EU，冷却 8 秒（服务器配置 FrameCores）。

## 退役物品

0.1.9.0 退役了太阳能头盔系列、高级量子胸甲与重型量子胸甲。旧物品不会丢失：携带时、放入改装台、由僚机穿戴或单独合成时，会变为装有对应核心的标准部件。

| 旧物品 | 转换为 |
|---|---|
| 太阳能头盔 | 太阳能皮肤单元 |
| 高级太阳能头盔 | 纳米头盔 + 太阳能阵列 I |
| 混合太阳能头盔 | 量子头盔 + 太阳能阵列 II |
| 终极混合太阳能头盔 | 量子头盔 + 太阳能阵列 III |
| 重型量子胸甲 | 量子胸甲 + 重力子壁垒 |
| 高级量子胸甲 | 量子胸甲 + 高级推进 |

储存的能量、单元、开关、附魔与名称都会保留。高级量子胸甲原先每秒 10 点的修复，在高级推进核心上为每秒 2.0 点。
