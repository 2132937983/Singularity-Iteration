package com.miophas.singularity_iteration.core.api.block;

import net.minecraft.world.level.block.state.BlockState;

/**
 * 橡胶木方块接口。
 *
 * <p>mio_icif 的橡胶木（{@code block_rub_wood}、{@code block_have_rub_wood}）均实现此接口，
 * 以统一描述"方块是否含树脂、是否已形成采集口、如何清除/恢复树脂"的状态语义。
 *
 * <p>附属模组可以：
 * <ul>
 *   <li>通过 {@link IRubberTreeAPI} 查询任意橡胶木状态，而无需依赖具体方块类；</li>
 *   <li>让自定义原木实现本接口，即可被树液采集器以及本模组的采集 API 统一处理。</li>
 * </ul>
 *
 * <p>实现约定：{@link #isTappable(BlockState)} 描述结构条件（例如是否已形成采集口），
 * {@link #hasResin(BlockState)} 描述是否当前含有可采集的树脂。只有当二者同时为真时，
 * 橡胶木才可被采集。
 */
public interface IRubberWood {

    /**
     * 该方块状态当前是否含有可采集的树脂。
     *
     * @param state 方块状态
     * @return 含树脂返回 true
     */
    boolean hasResin(BlockState state);

    /**
     * 该方块状态是否具备采集条件（例如橡胶木已形成采集口）。
     *
     * <p>此方法不考虑当前是否含树脂，仅描述结构上是否允许被树液采集器采集。
     *
     * @param state 方块状态
     * @return 可采集返回 true
     */
    boolean isTappable(BlockState state);

    /**
     * 返回修改了树脂状态的方块状态。
     *
     * @param state    原始方块状态
     * @param hasResin true 置为含树脂，false 置为不含树脂
     * @return 修改后的方块状态
     */
    BlockState withResin(BlockState state, boolean hasResin);

    /**
     * 该方块状态当前是否可以自然再生树脂（随机刻触发）。
     *
     * <p>默认实现为"具备采集条件且当前不含树脂"。
     *
     * @param state 方块状态
     * @return 可再生返回 true
     */
    default boolean canRegrowResin(BlockState state) {
        return isTappable(state) && !hasResin(state);
    }
}
