package com.miophas.singularity_iteration.core.api.world;

import com.miophas.singularity_iteration.core.api.block.IRubberWood;
import com.miophas.singularity_iteration.core.api.item.ITreeTapItem;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 橡胶树体系 API。
 *
 * <p>向附属模组暴露 mio_icif 的橡胶树（树脂采集）系统的判定与采集逻辑，使附属模组可以：
 * <ul>
 *   <li>识别本模组的树液采集器与橡胶木；</li>
 *   <li>在手写方块/物品交互中复用统一的采集逻辑，而不必依赖具体方块或物品类；</li>
 *   <li>让自定义原木通过实现 {@link IRubberWood}、自定义采集器通过实现
 *       {@link ITreeTapItem} 加入该系统。</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * IRubberTreeAPI rubber = MioIcifAPI.instance().getRubberTreeAPI();
 *
 * // 判断手中物品是否为树液采集器
 * if (rubber.isTreeTap(stack) && rubber.canHarvest(state)) {
 *     RubberHarvestResult result = rubber.tryHarvest(level, pos, state, player, hand);
 * }
 * }</pre>
 *
 * <p>除 {@link #tryHarvest}、{@link #tryRegrowResin}、{@link #dropResin} 外的方法均为纯查询，
 * 不会修改世界。修改世界的方法仅在服务端生效。
 */
public interface IRubberTreeAPI {

    // ========== 树脂物品 ==========

    /**
     * 获取树脂（生橡胶）物品。
     *
     * @return 树脂物品
     */
    Item getResinItem();

    /**
     * 创建一个包含指定数量树脂的物品堆。
     *
     * @param count 数量
     * @return 树脂物品堆
     */
    ItemStack createResinStack(int count);

    // ========== 树液采集器判定 ==========

    /**
     * 判断物品堆是否为树液采集器（实现 {@link ITreeTapItem}）。
     *
     * @param stack 物品堆
     * @return 是树液采集器返回 true
     */
    boolean isTreeTap(ItemStack stack);

    /**
     * 判断物品堆是否为电动树液采集器（实现 {@link ITreeTapItem} 且为电动工具）。
     *
     * @param stack 物品堆
     * @return 是电动树液采集器返回 true
     */
    boolean isElectricTreeTap(ItemStack stack);

    // ========== 橡胶木判定 ==========

    /**
     * 判断方块状态是否为橡胶木（实现 {@link IRubberWood}）。
     *
     * @param state 方块状态
     * @return 是橡胶木返回 true
     */
    boolean isRubberWood(BlockState state);

    /**
     * 判断橡胶木当前是否含有可采集的树脂。
     *
     * @param state 方块状态
     * @return 含树脂返回 true
     */
    boolean hasResin(BlockState state);

    /**
     * 判断橡胶木是否具备采集条件（例如已形成采集口）。
     *
     * @param state 方块状态
     * @return 具备采集条件返回 true
     */
    boolean isTappable(BlockState state);

    /**
     * 判断橡胶木当前是否可被采集（具备采集条件且含有树脂）。
     *
     * @param state 方块状态
     * @return 可采集返回 true
     */
    boolean canHarvest(BlockState state);

    /**
     * 判断指定工具是否可用于采集该橡胶木。
     *
     * <p>等价于：工具是树液采集器、方块可采集，且（电动工具时）能量充足。
     *
     * @param state 方块状态
     * @param tool  工具物品堆
     * @return 可用于采集返回 true
     */
    boolean canUseTapOn(BlockState state, ItemStack tool);

    // ========== 采集与再生 ==========

    /**
     * 尝试从橡胶木中采集树脂。
     *
     * <p>仅在服务端且条件满足时执行：消耗采集器耐久（或电能）、掉落树脂，
     * 并将方块置为不含树脂状态。
     *
     * @param level  世界
     * @param pos    方块位置
     * @param state  方块状态
     * @param player 玩家
     * @param hand   交互手
     * @return 采集结果；未执行采集时返回 {@link RubberHarvestResult#NONE}
     */
    RubberHarvestResult tryHarvest(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand);

    /**
     * 计算一次采集可获得的树脂数量（不修改世界）。
     *
     * <p>数量区间由采集器的 {@link ITreeTapItem} 参数决定。
     *
     * @param tool   采集器物品堆
     * @param random 随机源
     * @return 树脂数量，非采集器返回 0
     */
    int rollResinCount(ItemStack tool, RandomSource random);

    /**
     * 在方块与玩家之间掉落指定数量的树脂。
     *
     * <p>掉落位置沿用本模组的默认规则（方块中心与玩家连线的中点上方一格）。
     *
     * @param level  世界
     * @param pos    方块位置
     * @param player 玩家
     * @param count  数量
     */
    void dropResin(Level level, BlockPos pos, Player player, int count);

    /**
     * 尝试让橡胶木自然再生树脂（对应方块的随机刻逻辑）。
     *
     * <p>仅在服务端生效，且仅在方块当前满足再生条件时可能成功。
     *
     * @param level  世界
     * @param pos    方块位置
     * @param state  方块状态
     * @param random 随机源
     * @return 本次是否再生了树脂
     */
    boolean tryRegrowResin(Level level, BlockPos pos, BlockState state, RandomSource random);

    /**
     * 获取橡胶木每次随机刻再生树脂的概率。
     *
     * @return 再生概率 (0.0 ~ 1.0)
     */
    float getResinRegrowChance();
}
