package com.singularity_iteration.mio_icif.api.network;

/**
 * 网络更新监听接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code INetworkUpdateListener}。
 * 实现此接口的方块实体可以在网络字段更新时收到通知，
 * 并获取服务端同步的字段最新值。
 */
public interface INetworkUpdateListener {

    /**
     * 当网络同步的字段被更新时调用（带字段值）。
     * <p>
     * 此方法在客户端执行，用于响应服务端同步的字段变化。
     * {@code value} 是服务端发送的字段最新值，以 {@code double} 类型传输，
     * 可以安全地转换为 {@code int}、{@code long}、{@code float} 或 {@code boolean}。
     * <p>
     * 实现者应根据字段名将值应用到对应的字段：
     * <pre>{@code
     * switch (fieldName) {
     *     case "progress" -> this.progress = (int) value;
     *     case "energy"   -> this.energy = (long) value;
     *     case "active"   -> this.active = value != 0;
     *     default         -> {} // 未知字段，忽略
     * }
     * }</pre>
     *
     * @param fieldName 被更新的字段名
     * @param value     字段的新值（从服务端同步）
     */
    default void onNetworkUpdate(String fieldName, double value) {
        onNetworkUpdate(fieldName);
    }

    /**
     * 当网络同步的字段被更新时调用（仅字段名，无值）。
     * <p>
     * 此方法在客户端执行。当旧版网络包（不携带值）到达时调用此方法。
     * 默认实现为空，实现者可以选择覆盖此方法以支持旧版兼容，
     * 或覆盖 {@link #onNetworkUpdate(String, double)} 以获取字段值。
     *
     * @param fieldName 被更新的字段名
     * @deprecated 请使用 {@link #onNetworkUpdate(String, double)} 以获取服务端同步的字段值。
     */
    @Deprecated
    default void onNetworkUpdate(String fieldName) {
    }
}