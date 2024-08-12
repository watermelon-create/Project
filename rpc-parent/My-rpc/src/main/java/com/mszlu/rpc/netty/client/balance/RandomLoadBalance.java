package com.mszlu.rpc.netty.client.balance;

import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
@Slf4j
public class RandomLoadBalance implements LoadBalance{
    @Override
    public String name() {
        return "random";
    }

    @Override
    public InetSocketAddress loadBalance(Set<String> SERVICES) {
        int size = SERVICES.size();
        if(size>0){
            //随机均衡算法
            int i = new Random().nextInt(size);
            Optional<String> first = SERVICES.stream().skip(i).findFirst();
            if(first.isPresent()){
                String memo = first.get();
                log.info("走了随机的负载均衡器");
                return new InetSocketAddress(
                        memo.split(",")[0], Integer.parseInt(memo.split(",")[1]));

            }
        }
        return null;
    }
}
