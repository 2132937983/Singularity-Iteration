package com.singularity_iteration.mio_icif.api.network;

import java.util.List;

/**
 * 网络数据提供者接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code INetworkDataProvider}。
 * 实现此接口的方块实体可以声明哪些字段需要网络同步。
 */
public interface INetworkDataProvider {

    /**
     * 获取需要同步的字段名列表。
     * <p>
     * 返回的字段将在方块实体加载时自动同步到客户端，
     * 并在服务端修改时通过 {@link NetworkHelper} 更新。
     *
     * @return 需要同步的字段名列表
     */
    List<String> getNetworkedFields();
}
