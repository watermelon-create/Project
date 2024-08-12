package com.mszlu.rpc.proxy;


import com.mszlu.rpc.netty.client.NettyClient;
import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsRequest;
import com.mszlu.rpc.annotation.MsReference;
import com.mszlu.rpc.message.MsResponse;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

//每一个动态代理类的调用处理程序都必须实现InvocationHandler接口，
// 并且每个代理类的实例都关联到了实现该接口的动态代理类调用处理程序中，
// 当我们通过动态代理对象调用一个方法时候，
// 这个方法的调用就会被转发到实现InvocationHandler接口类的invoke方法来调用
@Slf4j
public class MsRpcClientProxy implements InvocationHandler {

    private MsReference msReference;
    private NettyClient nettyClient;
    public MsRpcClientProxy(MsReference msReference,NettyClient nettyClient) {
        this.msReference = msReference;
        this.nettyClient=nettyClient;
    }

    /**
     * proxy:代理类代理的真实代理对象com.sun.proxy.$Proxy0
     * method:我们所要调用某个对象真实的方法的Method对象
     * args:指代代理对象方法传递的参数
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //在这里实现调用
        log.info("rpc的代理实现类 调用了...");
//        1. 构建请求数据MsRequest
//        2. 创建Netty客户端
//        3. 通过客户端向服务端发送请求
//        4. 接收数据
        String version = msReference.version();
        MsRequest msRequest = MsRequest.builder().group("ms_rpc")
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .version(version)
                .parameters(args)
                .paramTypes(method.getParameterTypes())
                .requestId(UUID.randomUUID().toString())
                .build();
//        String host = msReference.host();
//        int port = msReference.port();
        /**
         * 使用Netty发送请求得到响应数据
         */
        Object sendRequest = nettyClient.sendRequest(msRequest);
//        Object sendRequest = nettyClient.sendRequest(msRequest);
        /**
         * 对响应数据处理
         */
        CompletableFuture<MsResponse<Object>> resultCompletableFuture =
                (CompletableFuture<MsResponse<Object>>) sendRequest;
        MsResponse<Object> msResponse=resultCompletableFuture.get();
        if (msResponse==null) {
            throw new MsRpcException("服务调用失败");
        }
        if(!msRequest.getRequestId().equals(msResponse.getRequestId())){
            throw new MsRpcException("响应结果和请求不一致");
        }
        return msResponse.getData();
    }

    /**
     * get the proxy object
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }

}
