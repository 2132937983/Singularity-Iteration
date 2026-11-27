# AN1 第三部分 · 玩家战甲动作

量子与纳米战甲共用这 9 个玩家动作。加上前两批的 47 个僚机动作，可编辑工程共收录 56 项。每项动图并排展示正面和右侧面，琥珀色标示右肢，青色标示左肢，红色描边标示游戏事件。[玩家动画 JSON](../../../src/main/resources/assets/mio_icif/animations/entity/suit_player.animation.json) · [Blockbench 工程](../../../tools/models/si_humanoid_rig.bbmodel) · [56 项总览](../previews/AN1.png)

| 动作 | 姿态与节奏 |
| --- | --- |
| [悬停 · hover](../previews/AN1/suit_hover.gif) | 双靴自然下垂、稍微分开，肩甲和拳套随身体慢半拍摆动。3 秒一圈，根骨从最低到最高只走 0.38 像素。 |
| [起飞 · takeoff](../previews/AN1/suit_takeoff.gif) | 0.09–0.11 秒屈身蓄力，0.15 秒双靴离地、手臂下压，0.40 秒双脚垂回悬停姿势。 |
| [重落地 · land_heavy](../previews/AN1/suit_land_heavy.gif) | 首帧着地，髋部下沉、双腿前后错开，双手悬空。低位从 0.12 秒停到 0.22 秒，约 0.57 秒已站起，余下时间收稳。 |
| [加力冲刺 · boost](../previews/AN1/suit_boost.gif) | 双手先略向前提，再向后甩，胸甲随之向前压，0.12–0.16 秒维持收紧的轮廓。 |
| [急刹 · brake](../previews/AN1/suit_brake.gif) | 胸甲先后仰，双臂随后向前、向两边张开卸力，0.15–0.20 秒达到最大开幅，再收回悬停手位。 |
| [换装扣盔 · suit_up](../previews/AN1/suit_suit_up.gif) | 右肩先开，左肩随后展开，玩家抬头接盔。1.10 秒面罩落正，两只拳套同步内旋，锁住 0.08 秒再收臂。 |
| [脱甲出击 · eject](../previews/AN1/suit_eject.gif) | 身体先后收，再把两只手一同推向前方。0.40 秒释放，双手停到 0.46 秒，随后胸口先回弹。 |
| [指向目标 · command_point](../previews/AN1/suit_command_point.gif) | 右肩短促后收，整条右臂抬到水平，0.25 秒发令，指向目标的姿势保持到 0.36 秒。 |
| [召回集合 · command_recall](../previews/AN1/suit_command_recall.gif) | 右手从头盔外侧举起，再向右肩后方挥。0.30 秒发令，到 0.39 秒才沿较低的弧线收手。 |

## 给 Claude 的接入备注

- 起飞在 `leave`（0.15 秒）点火，末帧和悬停首帧的七骨变换逐项相同。加力、急刹的首尾均接悬停循环起点的上身姿势。两者仅使用胸、头和双臂，飞行倾角与腿部控制可继续叠加。
- 玩家与僚机各有一个重落地，接入时使用完整名 `animation.suit.land_heavy`。玩家的 `contact` 在 0 秒进入动作时触发。没有膝关节，低姿由错腿和沉髋形成，双手最低仍离地约 4.4 像素。
- 扣盔的 `helmet` 在 1.10 秒，脱甲的 `release` 在 0.40 秒。手部是刚体，扣盔时用拳套内旋和肩线收紧表现握拳。护甲部件飞入、离身与僚机出现由游戏接入对应事件。预览里始终画同一副参考人偶。
- 指向与召回的 `order` 分别在 0.25 秒和 0.30 秒。指向按正前方水平制作，由代码叠加准星方向。召回采用向身后挥手的版本，动图侧面可直接辨认两条指令。

## 重建与验收

玩家动画由[飞行动作](../../../tools/art/an1_suit_flight.py)和[换装及指令动作](../../../tools/art/an1_suit_gestures.py)生成。运行下面的命令可重建玩家资源、共用工程、9 张动图和总览。

```sh
python tools/art/an1_suit.py
python tools/art/an1_rig.py
python tools/art/an1_preview.py --part 3
python tools/art/check_an1.py
```

全量契约检查返回 `OK`。Blockbench 5.2.2 的 GeckoLib 插件打开工程并重新导出后，几何、56 项动作的播放模式和时长、430 条通道的 11,100 个向量关键帧及 29 个时间轴事件均与游戏资源相符。第三批 GIF 共 314 帧，每帧 50 毫秒，6 个事件均在约定帧标红。

已核对本批与契约一致。穿戴实际量子或纳米盔甲后，肩甲厚度对挥手轮廓的影响，以及脱甲时部件与双掌的对位，留待游戏接入时确认。本批没有待拍板项。
