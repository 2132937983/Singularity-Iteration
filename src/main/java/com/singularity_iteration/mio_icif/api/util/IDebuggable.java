package com.singularity_iteration.mio_icif.api.util;

/**
 * 可调试接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IDebuggable}。
 * 实现此接口的方块在使用调试模式（扳手 shift+右键）时可以显示调试信息。
 */
public interface IDebuggable {

    /**
     * 检查此方块是否可调试。
     *
     * @return 如果可调试则返回 true
     */
    boolean isDebuggable();

    /**
     * 获取调试信息文本。
     *
     * @return 调试信息
     */
    String getDebugText();
}
