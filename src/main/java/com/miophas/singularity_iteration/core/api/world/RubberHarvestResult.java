package com.miophas.singularity_iteration.core.api.world;

/**
 * 一次树脂采集的结果。
 *
 * @param harvested  是否成功完成采集（服务端且条件满足时为 true）
 * @param resinCount 本次掉落的树脂数量；未采集时为 0
 */
public record RubberHarvestResult(boolean harvested, int resinCount) {

    /** 未发生采集的结果。 */
    public static final RubberHarvestResult NONE = new RubberHarvestResult(false, 0);

    /** 采集成功的结果。 */
    public static RubberHarvestResult success(int resinCount) {
        return new RubberHarvestResult(true, resinCount);
    }
}
