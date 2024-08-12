package com.mszlu.rpc.server;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mszlu.rpc.config.MsRpcConfig;
import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.netty.NettyServer;
import com.mszlu.rpc.annotation.MsService;

import com.mszlu.rpc.register.nacos.NacosTemplate;
import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Slf4j
public class MsServiceProvider {
    private MsRpcConfig msRpcConfig;
    private final Map<String,Object> serviceMap;
    private NacosTemplate nacosTemplate;
    public MsServiceProvider() {
        serviceMap=new ConcurrentHashMap<>();
        nacosTemplate=SingletonFactory.getInstance(NacosTemplate.class);
    }

    public void publishService(MsService msService, Object bean) {

//        System.out.println(canonicalName);
        //先将服务注册--寻找到一个合适服务实例
        registerService(msService,bean);
        //启动NettyServer,这个方法会调用多次，服务只能启动一次
        NettyServer nettyServer = SingletonFactory.getInstance(NettyServer.class);
        nettyServer.setMsServiceProvider(this);
        if(!nettyServer.isRunning()){
            nettyServer.run();
        }
    }
    private void registerService(MsService msService,Object bean){
        String interfaceName = bean.getClass().getInterfaces()[0].getCanonicalName();
        String serviceName = interfaceName+msService.version();
        log.info("发布了服务{}",interfaceName);
        serviceMap.put(serviceName,bean);
        if(msRpcConfig==null)throw new MsRpcException("必须开启EnableRPC");
        //service要进行注册, 先创建一个map进行存储
        //同步注册nacos
        //group 只有在同一个组内 调用关系才能成立，不同的组之间是隔离的
        try {
            Instance instance = new Instance();
            instance.setPort(msRpcConfig.getProviderPort());
            instance.setIp(InetAddress.getLocalHost().getHostAddress());
            instance.setClusterName("ms-rpc-service-provider");
            instance.setServiceName(serviceName);
            nacosTemplate.registerServer(msRpcConfig.getNacosGroup(),instance);
        } catch (Exception e) {
            log.error("nacos注册失败:",e);
        }
        log.info("发现服务{}并注册",serviceName);
    }

    /**
     * 获得注册在nacos上面的Bean
     * @param serviceName
     * @return
     */
    public Object getService(String serviceName){
        return  serviceMap.get(serviceName);
    }

    public MsRpcConfig getMsRpcConfig() {
        return msRpcConfig;
    }

    public void setMsRpcConfig(MsRpcConfig msRpcConfig) {
        this.msRpcConfig = msRpcConfig;
    }
}
