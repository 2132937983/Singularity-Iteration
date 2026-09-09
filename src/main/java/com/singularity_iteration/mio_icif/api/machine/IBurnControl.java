package com.singularity_iteration.mio_icif.api.machine;

/**
 * 可强制启停的发电机应实现的接口。
 *
 * <p>{@code GeneratorHelper} 通过此接口操作发电机的燃烧进度，
 * 而不直接写入具体实现类的 {@code burnTime} 字段。
 */
public interface IBurnControl {

    /** @return 默认燃烧时长（tick），若无则返回 0 */
    int getDefaultBurnTime();

    /** 设置当前燃烧剩余时长（tick） */
    void setBurnTime(int ticks);
}
