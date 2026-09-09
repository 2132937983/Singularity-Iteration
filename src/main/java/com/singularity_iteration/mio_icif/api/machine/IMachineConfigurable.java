package com.singularity_iteration.mio_icif.api.machine;

import com.singularity_iteration.mio_icif.api.machine.builder.IMachineBuilderAPI;
import org.jetbrains.annotations.Nullable;

/**
 * 可配置的机器方块接口。
 *
 * <p>实现了此接口的方块实体可通过 {@link #getConfiguration()} 获取机器配置信息。
 * 附属模组的自定义机器应实现此接口以支持 API 层的机器类型查询。
 */
public interface IMachineConfigurable {

    /**
     * 获取机器配置。
     *
     * @return 机器配置，如果未配置则返回 null
     */
    @Nullable
    IMachineBuilderAPI.MachineConfiguration getConfiguration();
}
