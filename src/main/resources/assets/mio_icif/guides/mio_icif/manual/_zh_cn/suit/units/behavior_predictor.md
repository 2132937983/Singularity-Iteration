---
navigation:
  title: "行为预测器"
  icon: mio_icif:module/item_module_behavior_predictor
  parent: suit/index.md
  position: 15
item_ids:
  - mio_icif:module/item_module_behavior_predictor_mk2
  - mio_icif:module/item_module_behavior_predictor
---

# 行为预测器

<ItemImage id="mio_icif:module/item_module_behavior_predictor" scale="3" />

## 功能

该单元跟踪24格内每个生物的移动。
该单元绘制生物下一秒的路径。
该单元在攻击前标出攻击征兆：瞄准、蓄力、施法、膨胀、光束与俯冲。
红线显示正在瞄准的生物的瞄准轴。

## 电力数据

| 项目 | 数值 |
|---|---|
| 适用部位 | 头盔 |
| 功耗 | 8 EU/t（开启时） |
| 显示 | 需要量子头盔面罩 |

## 使用步骤

1. 把单元装入量子头盔。
2. 注视标记旁的意图代码。
3. 射击前离开红色瞄准线。

## 注意

- 近战与远程代码：ATK近战，AIM远程武器，DET爆炸。
- 特殊攻击代码：CAST法术，BEAM守卫者，FIRE恶魂，DIVE恼鬼，AGGRO末影人。

## 路径、倒计时与排序

- 路径遵循游戏的移动规则：在冰上滑得更远，在水中变慢并缓慢下沉，走上半砖，跳上一格高的台阶（此前路径在此处中断），从边缘掉落。
- 刚被击中的生物先向后滑动，随后继续行走。追击你的生物会停在你面前。
- 攻击预告显示距攻击的时间：AIM 为拉弓或弩装填，FIRE 为恶魂蓄力，DET 为苦力怕膨胀，ATK 为本单元从该生物学到的攻击间隔。
- 标签旁的 #1～#3 为最危险的敌对生物排序：依据敌意、单次攻击或爆炸的伤害，以及多快能发动攻击。
- 距离超过 12 格且不威胁你的生物，路径每 4 tick 重新计算一次（而不是每 2 tick）。

## Mk.II

Mk.II 增加行动 **时间扭曲**（见“量子行动”）与下面的数据链。

## Mk.II 数据链

服务器向 Mk.II 预测器发送 24 格内最危险的 8 个生物接下来几步的行走路线。它们的路径沿该路线转弯，以紫色显示并带有 LINK 标记。

## 配方

<RecipesFor id="mio_icif:module/item_module_behavior_predictor" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_behavior_predictor_mk2" fallbackText="-" />
