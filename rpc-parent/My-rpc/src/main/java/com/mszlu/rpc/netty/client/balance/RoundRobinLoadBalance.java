package com.mszlu.rpc.netty.client.balance;

import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Slf4j
public class RoundRobinLoadBalance implements LoadBalance{
    @Override
    public String name() {
        return "roundRobin";
    }
    Integer pos=0;
    @Override
    public InetSocketAddress loadBalance(Set<String> SERVICES) {
        ArrayList<String> list = new ArrayList<>(SERVICES);
        if (list.size() > 0){
            synchronized (pos) {
                if (pos >= SERVICES.size()) {
                    pos = 0;
                }
                String ipPort = list.get(pos);
                log.info("使用了轮询负载均衡器", ipPort);
                pos++;
                return new InetSocketAddress(
                        ipPort.split(",")[0], Integer.parseInt(ipPort.split(",")[1]));
            }
        }
        return null;
    }
}
