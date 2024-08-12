package com.mszlu.rpc.netty.client.timer;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mszlu.rpc.config.MsRpcConfig;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsRequest;
import com.mszlu.rpc.register.nacos.NacosTemplate;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.TimeUnit;
@Slf4j
public class UpdateNacosServiceTask implements TimerTask {
    private NacosTemplate nacosTemplate;
    private MsRpcConfig msRpcConfig;
    private MsRequest msRequest;
    private Set<String> services;
    public UpdateNacosServiceTask(MsRequest msRequest, MsRpcConfig msRpcConfig, Set<String> services){
        this.services = services;
        this.msRequest = msRequest;
        this.msRpcConfig = msRpcConfig;
        this.nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
    }

    @Override
    public void run(Timeout timeout) throws Exception {

        String serviceName = msRequest.getInterfaceName()+msRequest.getVersion();
        Instance oneHealthyInstance = nacosTemplate.getOneHealthyInstance(msRpcConfig.getNacosGroup(),serviceName);
        services.add(oneHealthyInstance.getIp()+","+oneHealthyInstance.getPort());
        log.info("获取到了新的实例：",oneHealthyInstance.getIp()+","+oneHealthyInstance.getPort());
        //执行完，继续任务
        timeout.timer().newTimeout(timeout.task(),10, TimeUnit.SECONDS);
    }
}
