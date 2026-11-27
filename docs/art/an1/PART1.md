# AN1 第一部分 · 僚机移动、待机与受击

[24 个动作总览](../previews/AN1.png) · [Blockbench 工程](../../../tools/models/si_humanoid_rig.bbmodel) · [共用骨架](../../../src/main/resources/assets/mio_icif/geo/entity/si_humanoid_rig.geo.json) · [僚机动画 JSON](../../../src/main/resources/assets/mio_icif/animations/entity/wingman.animation.json)

下表的动图由导出的游戏 JSON 绘制。正面用来看左右换重，侧面用来看脚掌和地面、上身前倾及落地缓冲。标记帧有红色轮廓，标签显示契约事件。

| 动作 | 姿态与节奏 |
| --- | --- |
| [校准 · calibrate](../previews/AN1/wingman_calibrate.gif) | 0.5 秒到达校准姿势并停住。右臂朝前水平，左腿向后 30°，头向自身左侧转 45°，整体上移 2 像素。仅供接入时校对。 |
| [站立待机 · idle_stand](../previews/AN1/wingman_idle_stand.gif) | 4 秒内重心从一只靴子换到另一只，胸口小幅抬起，双手稍晚才随呼吸摆回。 |
| [环顾 · idle_look](../previews/AN1/wingman_idle_look.gif) | 面罩先转向左边，胸口晚半拍跟上；第二次转得更快，回正时略过中心。 |
| [自检 · idle_check](../previews/AN1/wingman_idle_check.gif) | 抬起右臂，把前臂甲送到面罩前，停住后旋转整臂检查，再收肩放下。 |
| [步行 · walk](../previews/AN1/wingman_walk.gif) | 左右落脚相隔半秒，着地时由承重腿接住身体，上身和手臂反向摆动。 |
| [跑动 · run](../previews/AN1/wingman_run.gif) | 前倾加深，双臂加大摆幅，左右支撑之间有双脚离地的一拍。 |
| [推力滑行 · skate](../previews/AN1/wingman_skate.gif) | 压低重心，两靴保持前后错位，双臂向后张开，在滑行中小幅调整平衡。 |
| [普通起跳 · jump_takeoff](../previews/AN1/wingman_jump_takeoff.gif) | 浅压后蹬离地面，0.10 秒起跳，随后收腿。 |
| [推进跳 · thrust_hop](../previews/AN1/wingman_thrust_hop.gif) | 比普通起跳压得更低，双臂下压，在 0.15 秒点火后伸身上冲。 |
| [上升 · air_rise](../previews/AN1/wingman_air_rise.gif) | 上身收稳，两靴向后收，空中循环只有小幅的姿态调整。 |
| [下落 · air_fall](../previews/AN1/wingman_air_fall.gif) | 放出双腿，脚掌对准地面，两臂略向两旁张开准备接触。 |
| [轻落地 · land_light](../previews/AN1/wingman_land_light.gif) | 首帧接地，短促卸力，0.30 秒内恢复站姿。 |
| [重落地 · land_heavy](../previews/AN1/wingman_land_heavy.gif) | 首帧接地后压进错腿低姿，从 0.14 到 0.38 秒由右掌和左靴撑住，再将身体推起。 |
| [跨台阶 · step_up](../previews/AN1/wingman_step_up.gif) | 先提越阶脚，0.08 秒标记离位，身体随后转移到支撑侧。 |
| [原地左转 · turn_l](../previews/AN1/wingman_turn_l.gif) | 先挪出脚尖、再收另一脚，步子短，便于代码同时改变整体朝向。 |
| [原地右转 · turn_r](../previews/AN1/wingman_turn_r.gif) | 与左转交换支撑脚，用两个小步骤完成站位调整。 |
| [掩体半蹲 · cover_crouch](../previews/AN1/wingman_cover_crouch.gif) | 压低髋部并错开双腿，两臂收向胸前，在低位保持呼吸。 |
| [正面受击 · hit_front](../previews/AN1/wingman_hit_front.gif) | 胸口先向后让，头与手臂稍晚承受冲量，回正时有一次小幅过冲。 |
| [背面受击 · hit_back](../previews/AN1/wingman_hit_back.gif) | 肩线向前折，面罩与双手延迟摆动，再由躯干拉回。 |
| [左侧受击 · hit_left](../previews/AN1/wingman_hit_left.gif) | 身体朝自身右边侧倾，两侧手臂用不同幅度恢复平衡。 |
| [右侧受击 · hit_right](../previews/AN1/wingman_hit_right.gif) | 冲量从右肩传到左侧，头和较松的一臂在胸口之后回位。 |
| [踉跄 · stagger](../previews/AN1/wingman_stagger.gif) | 第一步急促后撤，第二步接住身体；双臂张开幅度不同，最后才收回来。 |
| [低电跪伏 · lowpower_slump](../previews/AN1/wingman_lowpower_slump.gif) | 先低头、再沉髋，后腿接近地面，两臂失去支撑垂下，尾帧保持断电姿势。 |
| [重新上电 · lowpower_wake](../previews/AN1/wingman_lowpower_wake.gif) | 从断电末帧开始，先抬面罩，身体稍压再拔起；双手摆过中位后归位。 |

## 给 Claude 的接入备注

- 动画与玩家盔甲共用契约规定的 7 根骨骼。旋转和位移需按父子层级换算，尤其要把胸口带动的头和两臂计算在内。以 `calibrate` 末帧核对导出动作与原版渲染的方向。[骨架与事件契约](spec.json)
- 步行、跑动、滑行各以 4.3、7、12 米/秒为播放速率基准。移动距离和台阶高度取游戏实体的位置，关键帧中的 `root.position` 是局部姿态偏移。重落地和举步的标记用于触发声音、推进或扬尘。
- 四向轻受击和两套上身待机可以叠在步伐上。左右转身片段只动腿与根骨，整体朝向交给控制器。低电跪伏停在最后一帧，唤醒从同一姿势接续，过渡时直接连接这两个动作。
- 人偶的四肢是整段。自检时用转臂和肩部表现查看护甲；半蹲与跪地靠两腿前后错开、髋部下沉与质心侧移。预览包含参考人偶，游戏中由玩家实际装备决定外观。
- 预览以 20 帧/秒播放。跨阶的 `leave` 发生在 0.08 秒，GIF 用最接近的 0.10 秒帧标红并注明真实时刻。游戏时间轴和可编辑工程均保留 0.08 秒。工程使用 100 Hz 编辑网格，对齐 Blockbench 的手工导出精度。
- 建议优先评审步行的承重、推进跳与普通起跳的差别、重落地停住的 0.24 秒。实体开始跨一整格和高处落地时，地形碰撞高度与任意模组盔甲遮住手脚的程度，留到接入后用实机画面判断。

## 重建与检查

Blockbench 5.2.2 已打开该工程并通过 GeckoLib 插件重新导出。回导的骨架、24 个动作的时长与播放方式、188 条旋转/位移通道的 3,729 个关键帧，以及 9 个事件均与游戏资产相符。插件会在导出的指令末尾补分号，接入时可像仓库检查器一样去除尾分号再比较事件名。

```sh
python tools/art/an1_wingman.py
python tools/art/an1_rig.py
python tools/art/an1_preview.py
python tools/art/check_an1.py --part 1
```
