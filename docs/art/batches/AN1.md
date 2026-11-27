# 动画批次 AN1：僚机与战甲的 GeckoLib 关键帧动作

先读 [`../README.md`](../README.md) 的第 4 节（分支和报告的规矩）。这一批不画贴图：你在 Blockbench 里做一副人形骨架和一套关键帧动画，导出成 GeckoLib 的 JSON。代码、接入和游戏里的触发由 Claude 负责。

这一批的**契约文件**是 [`../an1/spec.json`](../an1/spec.json)：骨骼名、父子关系、枢轴、每个动画的名字、播放方式、时长、可动骨骼和标记时间都以它为准。检查脚本和接入代码都读它。你不改它，觉得不对就写进报告。

## 1. 为什么要做

僚机（Wingman）是奇点迭代里的一套空战甲：玩家把盔甲和武器交给武装库，它就变成会飞、会打的 AI 队友。它的动作现在全是代码算的：飞行时倾斜、摆腿、挥手，看起来轻飘飘的，没有重量。

接下来僚机要学会在洞穴里**落地步行、跳台阶、用推进器跳上高处**，在地面上近战和找掩体射击。用户希望这些动作“更酷、更有趣、更有力量感”。玩家穿量子、纳米战甲时的起飞、落地、换装、对僚机下令也一起做。

## 2. 技术路线（决定了你能做什么）

- GeckoLib 是**可选**前置，版本 **4.9.4**（Minecraft 1.21.1，NeoForge）。没装 GeckoLib 时游戏照常运行，用代码算的简单动作。
- Claude 不用 GeckoLib 来画模型。游戏里画的仍是原版的人形模型（头、身体、两臂、两腿六块），因为僚机穿的是任意模组的盔甲，这些盔甲都只认原版人形模型。Claude 每一帧从 GeckoLib 里读出你这副骨架各骨骼的旋转和位移，再写到原版模型的六块上。
- 所以：
  - **只有这 7 根骨骼起作用**：`root`、`body`、`head`、`right_arm`、`left_arm`、`right_leg`、`left_leg`。不要加别的骨骼。
  - **没有手肘、膝盖、脊柱弯曲**，四肢都是一整段。蹲下、跪地、拉弓都只能用整段手臂和腿去表现。
  - **只能动旋转和位移，不能缩放**。原版盔甲层不复制缩放，缩放会让盔甲和身体对不上。
  - 你的骨骼可以带方块（当作参考人偶，方便看效果），游戏里不会画出来，也不需要贴图。
- 父子关系按 `spec.json`：`head` 和两臂挂在 `body` 下面，`body` 和两腿挂在 `root` 下面。弯腰时头和手臂会跟着 `body` 走，这一步由 Claude 在代码里换算到原版模型上，你按正常的父子关系做就行。

## 3. 骨架

在 Blockbench 里新建 **GeckoLib Animated Model**（需要装 GeckoLib 插件），模型类型选实体。比例和原版玩家一样（高 32 像素），可以直接照 Blockbench 自带的 Minecraft 玩家模板摆。

| 骨骼 | 父骨骼 | 枢轴（x, y, z） | 对应原版部件 |
| --- | --- | --- | --- |
| `root` | 无 | 0, 0, 0（两脚之间的地面） | 整个模型（代码叠加到整体的位置和朝向上） |
| `body` | `root` | 0, 24, 0 | 身体 |
| `head` | `body` | 0, 24, 0 | 头 |
| `right_arm` | `body` | -5, 22, 0 | 右臂 |
| `left_arm` | `body` | 5, 22, 0 | 左臂 |
| `right_leg` | `root` | -1.9, 12, 0 | 右腿 |
| `left_leg` | `root` | 1.9, 12, 0 | 左腿 |

- 脸朝 -Z（Blockbench 里的北面），角色自己的右臂在 -X 一侧，和 Blockbench 的玩家模板一致。如果你发现模板和这条矛盾，以模板为准并在报告里写明。
- 静止姿势是原版玩家的站姿：手臂、腿都竖直向下，所有骨骼的静止旋转为 0。动作全部放在动画里。
- 骨架文件 `src/main/resources/assets/mio_icif/geo/entity/si_humanoid_rig.geo.json`，标识符 `geometry.mio_icif.si_humanoid_rig`。僚机和玩家共用这一副骨架。

## 4. 动作要求

### 4.1 力量感从哪来

用户最在意的是“有力量感”。请按这些原则做：

- **蓄力、出手、收招三段**。出手前先反方向蓄一下（拳头先往后、身体先下沉）；出手要快，两三帧就到位；收招要有惯性，越过一点再回来。
- **停顿**。重的一下（命中、落地、砸地）在最用力的姿势上停 0.05 到 0.25 秒，再往下走。
- **重心**。走、跑、落地时身体要有上下起伏和前后移动，不要像在冰上滑。落地时 `root` 下沉，然后慢慢回到站姿。
- **跟随**。身体先动，头和手臂晚一两帧跟上；停下时手臂再晃一下才停。
- **曲线**。用 GeckoLib 的缓动（如 `easeOutBack`、`easeInQuad`），或者在关键帧之间手调，不要全是线性。
- 风格参考钢铁侠、泰坦陨落里的铁驭和机甲：干脆、沉重、动作之间没有多余的花样。不要夸张到像卡通或舞蹈。

### 4.2 代码会叠加的部分（你不用做）

- **头的朝向**：僚机看向目标、玩家看向准星，由代码叠加在 `head` 上。你只做头相对身体的小动作（甩头、点头），平视前方为准。
- **瞄准的俯仰**：举枪、指向目标时，手臂的上下角度由代码按目标叠加。你按水平瞄准来做。
- **整体朝向和飞行姿态**：转身、飞行时的倾斜和侧滚还是代码算的。飞行中代码只取你动画里的上半身（`body`、`head`、两臂），腿和 `root` 用飞行姿态。
- **分层**：战斗动画（上身）叠在移动动画（腿）上播放，例如边走边换弹。所以“上身”组的动画不要依赖腿的姿势。
- **快慢**：走、跑、滑行按参考速度做，代码按实际速度调播放速度。
- **拔刀剑**（SlashBlade 模组的刀）有它自己的一套动作，不归这一批。

### 4.3 具体规矩

- 动画名、播放方式（循环、一次、停在末帧）、时长、可动骨骼都按第 5 节的表（也就是 `spec.json`）。时长误差不超过 0.05 秒。
- **标记**写成 GeckoLib 的 `timeline` 指令关键帧，内容就是标记名（例如 `"0.15": "impact"`），时间误差不超过 0.05 秒。代码靠它们定时：
  - `impact`：打中的那一刻，伤害和命中特效在这一帧触发；
  - `leave`：脚离地，推进器在这一帧点火；
  - `contact`：脚着地；
  - `step_l`、`step_r`：左、右脚落地，用于脚步声和扬尘；
  - `release`、`fire`、`loaded`、`mag_out`、`mag_in`、`order`、`helmet`：表里各有说明。
- 关键帧的值只用数字，不用 Molang 表达式。
- 旋转不超过 ±180 度。`root` 位移：x 在 -10 到 10、y 在 -14 到 16、z 在 -12 到 12 像素之间；其他骨骼位移不超过 ±4 像素。
- “循环”的动画首尾两帧完全相同（每个骨骼的每个通道都在 0 和末尾各有一帧，值一样）。
- 只能动表里写的骨骼组：

  | 组 | 骨骼 |
  | --- | --- |
  | 全身 | 全部 7 根 |
  | 上身 | `body`、`head`、`right_arm`、`left_arm` |
  | 上身+root | 上身加 `root` |
  | 腿+root | `root`、`right_leg`、`left_leg` |

- **脚不要明显穿地**。没有膝盖，所以 `root` 往下沉 d 像素时，要把两腿前后张开，让脚底大致还在地面上（腿长 12 像素，张开角度约为 arccos((12 − d) / 12)）。预览里画地面线，方便检查。
- 前后衔接：表里写“起点必须和某某衔接”的，首帧要和那个动画的末帧一致。其他动画首帧尽量接近站姿，代码会用 0.1 到 0.2 秒做过渡。
- 道具：手里的武器由游戏画在手臂末端，跟着 `right_arm` 和 `left_arm` 走。双手武器两臂要配合好。

### 4.4 校准动画

`animation.wingman.calibrate` 不在游戏里播放，只用来让 Claude 核对坐标方向（哪边是右、正角度往哪转）。请按表里写的姿势如实做，不要美化。

## 5. 动画清单

一共三部分，分三次交付。**先交第一部分**，僚机的洞穴步行马上要用到。

### 第一部分：僚机的移动、待机与受击（先交）

| 动画名 | 播放 | 时长（秒） | 可动骨骼 | 标记（秒） | 要求 |
| --- | --- | --- | --- | --- | --- |
| `animation.wingman.calibrate` | 停在末帧 | 0.5 | 全身 | — | 校准用，不在游戏里播放：最后一帧右臂向正前方抬平（指向脸朝的方向），左腿向后摆 30 度，头向角色自己的左边转 45 度，root 向上 2 像素。Claude 用它核对坐标方向。 |
| `animation.wingman.idle_stand` | 循环 | 4.0 | 全身 | — | 站立待机：呼吸、重心在两脚间缓慢转移，手臂自然下垂、手指微张。动作幅度小，循环无缝。 |
| `animation.wingman.idle_look` | 一次 | 2.5 | 上身 | — | 环顾：头先转、身体随后跟一点，看左再看右，最后回正。代码随机触发。 |
| `animation.wingman.idle_check` | 一次 | 3.0 | 上身 | — | 系统自检：抬起右手握拳再张开、转一下手腕，活动一下肩膀，像在检查战甲。 |
| `animation.wingman.walk` | 循环 | 1.0 | 全身 | `step_r` 0.0、`step_l` 0.5 | 步行，按 4.3 米每秒做（原版玩家步行速度）。一个循环两步。重量感来自落脚时身体下沉 0.5 像素左右、上身轻微反向扭转。代码按实际速度调快慢。 |
| `animation.wingman.run` | 循环 | 0.6 | 全身 | `step_r` 0.0、`step_l` 0.3 | 跑，按 7 米每秒做。身体前倾约 12 度，手臂摆动明显，腾空期两脚都离地。 参考速度 7.0 米每秒。 |
| `animation.wingman.skate` | 循环 | 0.8 | 全身 | — | 推力滑行（贴着地面被推进器推着走，不迈步）：低重心、两脚一前一后、上身前倾，手臂向后张开保持平衡。 参考速度 12.0 米每秒。 |
| `animation.wingman.jump_takeoff` | 一次 | 0.25 | 全身 | `leave` 0.1 | 普通起跳（跨一格台阶或跳过坑）：先蹲一下蓄力，leave 时脚离地，之后收腿。 |
| `animation.wingman.thrust_hop` | 一次 | 0.4 | 全身 | `leave` 0.15 | 推进跳（跳上两格以上的高处）：比普通起跳蹲得更深，leave 时推进器点火，双臂向下压、身体挺直上冲，之后收腿。 |
| `animation.wingman.air_rise` | 循环 | 0.5 | 全身 | — | 推进跳上升中：腿收起、身体挺直，轻微晃动。 |
| `animation.wingman.air_fall` | 循环 | 0.5 | 全身 | — | 下落中：腿伸出准备落地，手臂略张开保持平衡。 |
| `animation.wingman.land_light` | 一次 | 0.3 | 全身 | `contact` 0.0 | 轻落地：屈膝缓冲一下回到站姿。 |
| `animation.wingman.land_heavy` | 一次 | 0.8 | 全身 | `contact` 0.0 | 重落地（从高处落下或推进跳砸地）：单膝跪地、一只手撑地的“超级英雄落地”，停住约 0.25 秒，然后有力地站起。最有力量感的一个动作。 |
| `animation.wingman.step_up` | 一次 | 0.3 | 全身 | `leave` 0.08 | 跨上一格台阶：抬腿踏上去、身体随后跟上，不跳。 |
| `animation.wingman.turn_l` | 一次 | 0.4 | 腿+root | — | 原地左转（身体由代码转，这里只做脚下的两步碎步）。 |
| `animation.wingman.turn_r` | 一次 | 0.4 | 腿+root | — | 原地右转，同上。 |
| `animation.wingman.cover_crouch` | 循环 | 1.5 | 全身 | — | 掩体后半蹲（远程僚机在室内的待机姿势）：身体压低约 6 像素，武器收在胸前。 |
| `animation.wingman.hit_front` | 一次 | 0.35 | 上身 | — | 正面受击：上身后仰、头向后甩，然后迅速回正。 |
| `animation.wingman.hit_back` | 一次 | 0.35 | 上身 | — | 背面受击：上身前冲。 |
| `animation.wingman.hit_left` | 一次 | 0.35 | 上身 | — | 左侧受击：上身向右歪。 |
| `animation.wingman.hit_right` | 一次 | 0.35 | 上身 | — | 右侧受击：上身向左歪。 |
| `animation.wingman.stagger` | 一次 | 0.9 | 全身 | — | 被重击踉跄：后退两步、手臂乱一下，再稳住。 |
| `animation.wingman.lowpower_slump` | 停在末帧 | 1.2 | 全身 | — | 低电量待机：像断电一样慢慢单膝跪下、低头、手臂垂下，停在最后一帧。 |
| `animation.wingman.lowpower_wake` | 一次 | 0.8 | 全身 | — | 从低电量姿势重新上电：先抬头，再一下子站起来。起点必须和 lowpower_slump 的最后一帧相同。 |

### 第二部分：僚机的战斗

| 动画名 | 播放 | 时长（秒） | 可动骨骼 | 标记（秒） | 要求 |
| --- | --- | --- | --- | --- | --- |
| `animation.wingman.glare` | 一次 | 0.3 | 上身 | — | 锁定目标时的瞪视：头猛地转向前方并停住，肩膀摆正。 |
| `animation.wingman.melee_1` | 一次 | 0.45 | 上身+root | `impact` 0.15 | 近战第一击：右手斜劈。0 到 impact 是蓄力和出手，impact 之后是收招。蓄力短、出手快、收招带惯性。 |
| `animation.wingman.melee_2` | 一次 | 0.45 | 上身+root | `impact` 0.15 | 近战第二击：反手横扫，接在 melee_1 的收招姿势后面。 |
| `animation.wingman.melee_3` | 一次 | 0.65 | 上身+root | `impact` 0.25 | 近战第三击（收尾）：转身全力劈下，最重的一击，收招时间最长。 |
| `animation.wingman.melee_heavy` | 一次 | 1.0 | 全身 | `impact` 0.55 | 重击：双手举过头顶，停顿蓄力，砸下。用于斧头、锤（重锤）这类重武器。 |
| `animation.wingman.kick` | 一次 | 0.55 | 全身 | `impact` 0.25 | 正蹬：把敌人踢开。只在地面用。 |
| `animation.wingman.dash_stab` | 一次 | 0.5 | 上身+root | `impact` 0.2 | 冲刺突刺：身体前压、武器直刺。root 可以向前位移，最多 12 像素。 |
| `animation.wingman.slam_fall` | 循环 | 0.4 | 全身 | — | 下砸前的下落：在空中举起武器、双腿并拢，循环到落地为止。 |
| `animation.wingman.slam_impact` | 一次 | 0.7 | 全身 | `impact` 0.0 | 下砸落地：落地瞬间武器砸向地面，身体压到最低，然后起身。起点必须和 slam_fall 衔接。 |
| `animation.wingman.shove` | 一次 | 0.4 | 全身 | `impact` 0.15 | 护主时的撞开：用肩膀把冲向主人的怪撞开。 |
| `animation.wingman.aim_rifle` | 循环 | 2.0 | 上身 | — | 双手持枪瞄准：枪托抵肩，头贴近准星，轻微呼吸晃动。俯仰由代码按目标叠加。 |
| `animation.wingman.aim_pistol` | 循环 | 2.0 | 上身 | — | 单手持枪瞄准：右臂伸直，左手空着或扶腕。 |
| `animation.wingman.recoil_light` | 一次 | 0.15 | 上身 | — | 轻武器后坐：手臂和上身被向后推一下，马上回到瞄准姿势。起点和终点要和 aim_rifle 一致。 |
| `animation.wingman.recoil_heavy` | 一次 | 0.35 | 上身+root | — | 重武器后坐（等离子炮、电磁炮）：整个身体被推后半步。 |
| `animation.wingman.reload` | 一次 | 1.2 | 上身 | `mag_out` 0.3、`mag_in` 0.8 | 换弹：左手拔出弹匣（mag_out），从腰间拿新的装上（mag_in），拍一下，回到瞄准。 |
| `animation.wingman.bow_draw` | 停在末帧 | 0.6 | 上身 | — | 拉弓：左手持弓前推，右手拉弦到脸边，停住。 |
| `animation.wingman.bow_release` | 一次 | 0.3 | 上身 | `release` 0.0 | 放箭：右手向后弹开，然后回到放松姿势。 |
| `animation.wingman.crossbow_reload` | 一次 | 1.1 | 上身 | `loaded` 0.9 | 弩上弦：把弩往下一压、拉弦，loaded 时上好。 |
| `animation.wingman.spear_throw` | 一次 | 0.6 | 上身+root | `release` 0.3 | 投掷三叉戟：后引、身体扭转、甩出。 |
| `animation.wingman.peek_left` | 停在末帧 | 0.4 | 全身 | — | 从掩体左侧探身出来瞄准：起点是 cover_crouch，终点是探出的射击姿势。 |
| `animation.wingman.peek_right` | 停在末帧 | 0.4 | 全身 | — | 从掩体右侧探身，同上。 |
| `animation.wingman.intercept` | 一次 | 0.3 | 上身 | `fire` 0.1 | 点防御拦截：手臂猛地抬起指向来袭的弹射物，fire 时开火。 |
| `animation.wingman.kill_flourish` | 一次 | 1.2 | 上身 | — | 击杀后的小动作：甩一下武器、轻轻点头，不要夸张到像跳舞。代码随机触发，频率很低。 |

### 第三部分：玩家穿量子、纳米战甲

| 动画名 | 播放 | 时长（秒） | 可动骨骼 | 标记（秒） | 要求 |
| --- | --- | --- | --- | --- | --- |
| `animation.suit.hover` | 循环 | 3.0 | 全身 | — | 玩家穿量子、纳米战甲悬停：双脚自然垂下、略分开，手臂微张，整体缓慢上下浮动 0.5 像素以内。 |
| `animation.suit.takeoff` | 一次 | 0.4 | 全身 | `leave` 0.15 | 起飞：蹲下蓄力，leave 时腾空，手臂向下压。 |
| `animation.suit.land_heavy` | 一次 | 0.8 | 全身 | `contact` 0.0 | 玩家的超级英雄落地，和僚机的 land_heavy 同一个感觉，但不撑地（玩家要能马上动）：停顿最多 0.15 秒。 |
| `animation.suit.boost` | 一次 | 0.3 | 上身 | — | 加力冲刺瞬间：手臂向后甩、身体绷直。腿由飞行姿态代码控制。 |
| `animation.suit.brake` | 一次 | 0.4 | 上身 | — | 急刹：手臂向前张开、上身后仰。 |
| `animation.suit.suit_up` | 一次 | 1.5 | 全身 | `helmet` 1.1 | 换装：部件一件件飞上身时，玩家张开双臂、抬头，最后头盔扣上（helmet）时握拳。 |
| `animation.suit.eject` | 一次 | 1.0 | 全身 | `release` 0.4 | 脱甲出击：玩家向前伸手推出，release 时盔甲离身组成僚机。 |
| `animation.suit.command_point` | 一次 | 0.6 | 上身 | `order` 0.25 | 对僚机下令：右手指向目标点（方向由代码按视线叠加），order 时命令发出。 |
| `animation.suit.command_recall` | 一次 | 0.7 | 上身 | `order` 0.3 | 召回或集合：右手举起在头顶画一个圈，或者向身后一挥。 |

## 6. 交付文件

| 文件 | 说明 |
| --- | --- |
| `src/main/resources/assets/mio_icif/geo/entity/si_humanoid_rig.geo.json` | 骨架（第 3 节） |
| `src/main/resources/assets/mio_icif/animations/entity/wingman.animation.json` | 第一、二部分的动画，名字前缀 `animation.wingman.` |
| `src/main/resources/assets/mio_icif/animations/entity/suit_player.animation.json` | 第三部分的动画，名字前缀 `animation.suit.` |
| `tools/models/si_humanoid_rig.bbmodel` | Blockbench 工程，包含骨架和全部动画，与导出的 JSON 一致 |
| `tools/art/an1_preview.py` | 预览脚本（第 7 节） |
| `tools/art/an1_*.py` | 如果动画是用脚本生成或批量调整的，脚本也提交 |
| `docs/art/previews/AN1/<文件>_<动画名>.gif` | 每个动画一张预览 GIF，例如 `wingman_walk.gif`、`suit_hover.gif` |
| `docs/art/previews/AN1.png` | 总览：每个动画一格，放最有代表性的一帧（打击动画放 `impact` 那一帧），下面写名字 |

**不改：** Java 代码、`spec.json`、`check_an1.py`、语言文件和其他资源。需要代码配合的地方写进报告的“给 Claude 的接入备注”。

## 7. 预览

`tools/art/an1_preview.py` 读你导出的骨架和动画 JSON（不是 Blockbench 工程），把骨架上的方块当成人偶画出来，生成第 6 节的 GIF 和总览图。可以用 Pillow。要求：

- 每张 GIF 并排画正面和侧面两个视角，正交投影，20 帧每秒，画出地面线；
- “一次”和“停在末帧”的动画放完后停 0.5 秒再从头播；循环动画至少放两圈；
- 有标记的动画，在标记那一帧把人偶描红一帧，并在角上写标记名；
- 画面里写动画名和当前时间。

这个脚本也是你自查的工具：做完一个动画先看 GIF，再交。

## 8. 流程

1. 仓库 `https://github.com/rianfalltwilight-lab/Singularity-Iteration`，从分支 `art/an1-brief` 切新分支 `art/an1`。每交一部分就提交并**推送 `art/an1`** 到同一个仓库，不开 PR，不改其他分支。
2. 按第一、二、三部分的顺序做。每部分交付时运行：
   ```
   python3 tools/art/check_an1.py --part 1
   ```
   （第二部分用 `--part 1 --part 2`，最后不带参数检查全部），要输出 `OK`。只用 Python 标准库，不需要装别的东西。
3. Claude 每收到一部分就接进游戏、截图给用户看，用户的修改意见会写成 `docs/art/batches/AN1-r<轮次>.md`，你按它改。

## 9. 报告（简体中文，每部分一份）

- 交了哪些动画、哪些文件；
- 每个动画一句话说你怎么处理力量感（蓄力多长、停顿在哪）；
- 因为没有手肘、膝盖而做了妥协的动作，怎么妥协的；
- 与本说明或 `spec.json` 不一致的地方及原因，你不满意的地方；
- 给 Claude 的接入备注（例如某个动画建议用多长的过渡、哪两个动画之间不该直接切换）；
- 需要用户拍板的地方。
