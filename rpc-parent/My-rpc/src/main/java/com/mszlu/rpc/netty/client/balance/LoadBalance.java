package com.mszlu.rpc.netty.client.balance;

import java.net.InetSocketAddress;
import java.util.Set;

public interface LoadBalance {
    /**
     * spi
     * @return
     */
    String name();
    /**
     * 从缓存中以一定策略选择服务器
     * 随机
     * 轮询
     * @param services
     * @return
     */

    InetSocketAddress loadBalance(Set<String> services);
}
