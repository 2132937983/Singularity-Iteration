# AN1 第二部分 · 僚机战斗

本批增加 23 个战斗动作。前两批共用同一副七骨人偶，47 个动作都在同一份 [Blockbench 工程](../../../tools/models/si_humanoid_rig.bbmodel) 和[僚机动画 JSON](../../../src/main/resources/assets/mio_icif/animations/entity/wingman.animation.json) 里。下表每个动作都链接到从游戏资源渲染的动图。[47 项总览](../previews/AN1.png)

## 近战与下砸

| 动作 | 姿态与节奏 |
| --- | --- |
| [锁定目标 · glare](../previews/AN1/wingman_glare.gif) | 面罩从偏侧猛地转正，肩线随后摆平，最后 0.11 秒定住。 |
| [连击一 · melee_1](../previews/AN1/wingman_melee_1.gif) | 右臂用 0.07 秒提到外侧，斜劈穿过身前，在 0.15–0.21 秒定住，随后略过中线。 |
| [连击二 · melee_2](../previews/AN1/wingman_melee_2.gif) | 从上一击的收势反向横扫，0.15 秒到达另一侧，停 0.06 秒，右肩随惯性打开。 |
| [连击三 · melee_3](../previews/AN1/wingman_melee_3.gif) | 沿第二击的肩线转身抬臂，0.25 秒劈下后停 0.11 秒，余下的时间收回起手姿势。 |
| [双手重击 · melee_heavy](../previews/AN1/wingman_melee_heavy.gif) | 两只拳套举过面罩，在高位等 0.17 秒，再随重心一起下压，0.55 秒命中后停 0.12 秒。 |
| [正蹬 · kick](../previews/AN1/wingman_kick.gif) | 左靴承重，右靴先提后伸，在 0.25–0.31 秒悬停于身前，再回落。 |
| [冲刺突刺 · dash_stab](../previews/AN1/wingman_dash_stab.gif) | 前 0.10 秒重心稍后撤，随后右臂和胸甲向前，在 0.20 秒伸到位，根骨顺势再向前一点才收回。 |
| [空中蓄势 · slam_fall](../previews/AN1/wingman_slam_fall.gif) | 双靴靠拢、双手举高，用 0.4 秒的小幅循环维持下落姿态，下落距离由实体提供。 |
| [下砸接地 · slam_impact](../previews/AN1/wingman_slam_impact.gif) | 0 秒双脚触地，同时发出命中事件。下一帧（0.05 秒）双臂和胸甲压到最低，停至 0.19 秒再起身。 |
| [护主肩撞 · shove](../previews/AN1/wingman_shove.gif) | 先向另一侧偏载 0.07 秒，转动胸甲让右肩领先，0.15–0.21 秒停在撞击姿势。 |

## 瞄准、装填与投掷

| 动作 | 姿态与节奏 |
| --- | --- |
| [双手瞄准 · aim_rifle](../previews/AN1/wingman_aim_rifle.gif) | 胸甲侧转，使两只手套前后错开约 3.3 像素。面罩转回正前，双手随 2 秒的呼吸循环一同微动。 |
| [单手瞄准 · aim_pistol](../previews/AN1/wingman_aim_pistol.gif) | 右臂单独抬平，左手放在腰侧，轻微的肩线起伏保留了单手轮廓。 |
| [轻后坐 · recoil_light](../previews/AN1/wingman_recoil_light.gif) | 前 0.05 秒双臂上跳、头稍晚跟上，0.15 秒回到双手瞄准的基准姿势。 |
| [重后坐 · recoil_heavy](../previews/AN1/wingman_recoil_heavy.gif) | 先抬双臂，整副机体随后向后退。0.12 秒时根骨后移 4.4 像素，0.35 秒收稳。 |
| [换弹 · reload](../previews/AN1/wingman_reload.gif) | 右手持续保持持物姿势，左手在 0.30 秒取下弹匣，落到腰边，0.80 秒装入，轻拍后回到瞄准。 |
| [拉弓 · bow_draw](../previews/AN1/wingman_bow_draw.gif) | 双手在身前合拢，再转肩使左手前送、右手靠近面罩，0.56 秒拉满时两手前后错开约 9 像素。 |
| [松弦 · bow_release](../previews/AN1/wingman_bow_release.gif) | 从满弓姿势直接发出 0 秒事件。右臂先弹开，左手多留一拍，随后两手落下。 |
| [弩上弦 · crossbow_reload](../previews/AN1/wingman_crossbow_reload.gif) | 双手先下压，用肩部转动拉开前后距离，0.90 秒卡定，短停后抬回双手瞄准。 |
| [投掷三叉戟 · spear_throw](../previews/AN1/wingman_spear_throw.gif) | 右臂从头盔外侧抬起，胸甲先转、手臂跟上，0.30 秒释放，之后手臂沿弧线收低。 |

## 掩体、拦截与收势

| 动作 | 姿态与节奏 |
| --- | --- |
| [向左探身 · peek_left](../previews/AN1/wingman_peek_left.gif) | 从掩体半蹲先探出面罩，再把双手送到自身左侧。0.35 秒肩线定住，面罩稍晚回正。 |
| [向右探身 · peek_right](../previews/AN1/wingman_peek_right.gif) | 从同一半蹲向自身右侧移重，双靴保持原来的落点，仍由右手持物，停在可持续瞄准的尾帧。 |
| [点防御拦截 · intercept](../previews/AN1/wingman_intercept.gif) | 面罩先抬，右臂在 0.10 秒对准并触发拦截，短停后略向上弹，再放下。 |
| [收势点头 · kill_flourish](../previews/AN1/wingman_kill_flourish.gif) | 只在身体外侧短甩一下，0.38–0.45 秒定住手臂，随后低头一次，1.20 秒收稳。 |

## 给 Claude 的接入备注

- 连击一的末帧接连击二的首帧，连击二的末帧接连击三的首帧。轻后坐首尾都接双手瞄准循环的 0 秒姿势，换弹也从这个姿势起落。松弦从满弓末帧接起，左右探身均从第一批掩体半蹲的 0 秒姿势接起。循环正在呼吸时，可用短过渡收掉相位差。
- 下砸下落的循环边界和下砸接地的首帧共用举手姿势，接触与压到最低相隔一个游戏刻。接触效果读 0 秒的 `impact`。播放接地动作时直接切入，能保留首帧接触和下一帧下压。松弦的 `release` 也在 0 秒，需要在进入动作时处理。
- `dash_stab` 的根骨最大前移 8.8 像素，`recoil_heavy` 最大后移 4.4 像素，两者都会收回。实体行程由游戏计算，根骨偏移用于表现惯性。其余瞄准、换弹、弓弩等上身动作可叠在第一批的步伐上。探身的左、右以僚机自身为准。
- 直臂人偶靠转肩和少量肩部位移表现双手握距，正蹬和下砸靠整条腿转动、错步与沉髋表现屈伸。动图中的琥珀色是右肢，青色是左肢。手持物由游戏绘制，具体武器的握点、厚盔甲遮住面罩的程度，以及移动中叠加的效果，留到游戏接入时确认。本批没有待拍板的参数。

## 重建与检查

用 Blockbench 5.2.2 的 GeckoLib 插件打开工程并重新导出后，47 项动作的时长、播放模式、358 条通道、8,566 个向量关键帧和 23 个时间轴事件与游戏 JSON 一致。两批契约检查返回 `OK`。另按七根骨骼的最终变换核对了连击、下砸、瞄准与后坐、换弹、拉弓与松弦，以及掩体两侧探身的 10 处衔接。

制作源文件分为[近战](../../../tools/art/an1_melee.py)、[远程](../../../tools/art/an1_ranged.py)和[掩体及短动作](../../../tools/art/an1_tactics.py)。以下命令重建僚机 JSON、可编辑工程、第二批的 23 张正侧面 GIF 和两批总览。

```sh
python tools/art/an1_wingman.py
python tools/art/an1_rig.py
python tools/art/an1_preview.py --part 2
python tools/art/check_an1.py --part 1 --part 2
```
