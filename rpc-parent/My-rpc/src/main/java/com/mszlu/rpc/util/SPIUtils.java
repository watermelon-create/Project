package com.mszlu.rpc.util;

import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.netty.client.balance.LoadBalance;

import java.util.ServiceLoader;

public class SPIUtils {
    public static LoadBalance loadBalance(String name) {
        ServiceLoader<LoadBalance> load = ServiceLoader.load(LoadBalance.class);
        for (LoadBalance loadBalance : load) {
            if (loadBalance.name().equals(name)) {
                return loadBalance;
            }
        }
        throw new MsRpcException("无对应的负载均衡器");
    }
}
