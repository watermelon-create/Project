package com.mszlu.rpc.register.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mszlu.rpc.config.MsRpcConfig;

import java.util.List;

public class NacosTemplate {

    private ConfigService configService;//主要用作配置方面的管理功能
    private NamingService namingService;//主要用作服务方面的管理功能

    //初始化namingService和configService;
    public NacosTemplate(){
//        try {
//            //             configService = NacosFactory.createConfigService("localhost:8848");
//            namingService = NacosFactory.createNamingService("localhost:8848");
//        } catch (NacosException e) {
//            e.printStackTrace();
//        }
    }

    public void init(String Host, int Port) {
        try {
    //             configService = NacosFactory.createConfigService("localhost:8848");
             namingService = NacosFactory.createNamingService(Host+":"+Port);
        } catch (NacosException e) {
            e.printStackTrace();
        }
    }

    //注册服务
    public void registerServer(Instance instance) throws Exception{
        namingService.registerInstance(instance.getServiceName(),instance);
    }

    //注册服务
    public void registerServer(String groupName,Instance instance) throws Exception{
        namingService.registerInstance(instance.getServiceName(),groupName,instance);
    }

    //删除服务
    public void deleteServer(Instance instance) throws Exception{
        namingService.deregisterInstance(instance.getServiceName(),instance.getIp(),instance.getPort());
    }

    //随机全部（有可能获取到的不健康）。拿到全部实例后，我们可以按照自己的负载均衡算法进行调用。类似于springcloud的ribbon。
    public List<Instance> getAllServer(String serverName) throws Exception{
        return namingService.getAllInstances(serverName);
    }

    //根据负载均衡算法获取一个健康的实例
    public Instance getOneHealthyInstance(String groupName,String serverName) throws Exception{
        return namingService.selectOneHealthyInstance(serverName,groupName);
    }



    //更多api请参数：https://nacos.io/zh-cn/docs/open-api.html
}
