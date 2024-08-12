package com.mszlu.rpc.netty.client;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.naming.utils.RandomUtils;
import com.mszlu.rpc.config.MsRpcConfig;
import com.mszlu.rpc.netty.client.balance.LoadBalance;
import com.mszlu.rpc.netty.client.cache.ChannelCache;
import com.mszlu.rpc.netty.client.handler.UnprocessedRequests;
import com.mszlu.rpc.constant.CompressTypeEnum;
import com.mszlu.rpc.constant.MessageTypeEnum;
import com.mszlu.rpc.constant.SerializationTypeEnum;
import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.message.MsRequest;
import com.mszlu.rpc.netty.client.handler.MsNettyClientHandler;
import com.mszlu.rpc.netty.client.idle.ConnectionWatchdog;
import com.mszlu.rpc.netty.client.timer.UpdateNacosServiceTask;
import com.mszlu.rpc.netty.codec.MsRpcDecoder;
import com.mszlu.rpc.netty.codec.MsRpcEncoder;
import com.mszlu.rpc.message.MsResponse;
import com.mszlu.rpc.register.nacos.NacosTemplate;
import com.mszlu.rpc.util.SPIUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyClient implements MsClient{
    private MsRpcConfig msRpcConfig;
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private UnprocessedRequests unprocessedRequests;
    private NacosTemplate nacosTemplate;
    //ip,port
    private final static Set<String> SERVICES=new CopyOnWriteArraySet<>();
    protected final HashedWheelTimer timer=new HashedWheelTimer();
    //缓存channel 的map
    private final ChannelCache channelCache;
    //
    protected HashedWheelTimer serviceTimer;
    public NettyClient(){
        this.channelCache=SingletonFactory.getInstance(ChannelCache.class);
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.nacosTemplate=SingletonFactory.getInstance(NacosTemplate.class);
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //超时时间设置
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000);
//                .handler(new ChannelInitializer<SocketChannel>() {
//                    @Override
//                    protected void initChannel(SocketChannel ch) throws Exception {
//                        //3s没收到写请求，进行心跳检测
//                        ch.pipeline().addLast(new IdleStateHandler(0,3,0,TimeUnit.SECONDS));
//                        ch.pipeline ().addLast ( "decoder",new MsRpcDecoder() );
//                        ch.pipeline ().addLast ( "encoder",new MsRpcEncoder());
//                        ch.pipeline ().addLast ( "handler",new MsNettyClientHandler() );
//                    }
//                });
    }


    public MsRpcConfig getMsRpcConfig() {
        return msRpcConfig;
    }
    LoadBalance loadBalance=null;
    public void setMsRpcConfig(MsRpcConfig msRpcConfig) {
        loadBalance = SPIUtils.loadBalance(msRpcConfig.getLoadBalance());
        this.msRpcConfig = msRpcConfig;
    }

    @Override
    public Object sendRequest(MsRequest msRequest) {

        if(msRpcConfig==null)
           throw new MsRpcException("必须开启EnableRPC");
        CompletableFuture<MsResponse<Object>> resultCompletableFuture
                =new CompletableFuture<>();
        InetSocketAddress inetSocketAddress = null;

        if(!SERVICES.isEmpty()){
            inetSocketAddress=loadBalance.loadBalance(SERVICES);
            log.info("走了缓存");
        }
        //从nacos中获取服务提供方的IP和端口
        if(inetSocketAddress==null){
            Instance oneHealthyInstance=null;
            try {

                oneHealthyInstance = nacosTemplate.getOneHealthyInstance(
                        msRpcConfig.getNacosGroup(),
                        msRequest.getInterfaceName() + msRequest.getVersion());
                inetSocketAddress=new InetSocketAddress(
                            oneHealthyInstance.getIp(), oneHealthyInstance.getPort());

                SERVICES.add(oneHealthyInstance.getIp() + "," + oneHealthyInstance.getPort());
                //触发定时任务
                if(serviceTimer==null){
                    serviceTimer=new HashedWheelTimer();
                    serviceTimer.newTimeout(new UpdateNacosServiceTask(msRequest,msRpcConfig,SERVICES),
                            10,TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                log.error("没有可用的服务提供者：",e);
                resultCompletableFuture.completeExceptionally(e);
                return resultCompletableFuture;
            }
        }

        //连接netty 拿到channel
        CompletableFuture<Channel> channelCompletableFuture = new CompletableFuture<>();
        final ConnectionWatchdog watchdog = new ConnectionWatchdog(bootstrap,timer,
                inetSocketAddress,channelCompletableFuture, true,channelCache) {
            @Override
            public void clear(InetSocketAddress ia) {
                SERVICES.remove(ia.getHostName()+","+ia.getPort());
                channelCache.remove(ia);
                log.info("链路检测狗 触发: 删除provider服务缓存成功...");
            }

            public ChannelHandler[] handlers() {
                return new ChannelHandler[] {
                        this,
                        new IdleStateHandler(0, 3, 0, TimeUnit.SECONDS),
                        new MsRpcDecoder(),
                        new MsRpcEncoder(),
                        new MsNettyClientHandler()
                };
            }
        };
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(watchdog.handlers());
            }
        });

        //inetSocketAddress为key,从channelCache拿可用的channel
        unprocessedRequests.put(msRequest.getRequestId(), resultCompletableFuture);
        Channel channel = null;
        try {
            channel = getChannel(inetSocketAddress,channelCompletableFuture);
            if (!channel.isActive()){
                throw new MsRpcException("连接异常");
            }
            MsMessage msMessage = MsMessage.builder()
                    .codec(SerializationTypeEnum.PROTO_STUFF.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .messageType(MessageTypeEnum.REQUEST.getCode())
                    .data(msRequest)
                    .build();
            channel.writeAndFlush(msMessage).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(future.isSuccess()){
                        log.info("请求完成");
                    }else{
                        log.error("发送请求数据失败");
                        future.channel().close();
                        resultCompletableFuture.completeExceptionally(future.cause());
                    }
                }
            });
        } catch (ExecutionException |InterruptedException e) {
            resultCompletableFuture.completeExceptionally(e);
            log.error("channel 获取失败：",e);
        }
        return resultCompletableFuture;
    }

    private Channel getChannel(InetSocketAddress inetSocketAddress,CompletableFuture<Channel> channelCompletableFuture) throws InterruptedException, ExecutionException {
        Channel channel = channelCache.get(inetSocketAddress);
        if(channel==null){
            channel = doConnect(inetSocketAddress, channelCompletableFuture);
            channelCache.set(inetSocketAddress,channel);
        }
        return channel;
    }
    @SneakyThrows
    private Channel doConnect(InetSocketAddress inetSocketAddress, CompletableFuture<Channel> channelCompletableFuture) {
        bootstrap.connect(inetSocketAddress).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(future.isSuccess()){
                    channelCompletableFuture.complete(future.channel());
                }else{
                    //缓存剔除服务器
                    SERVICES.remove(inetSocketAddress.getHostName()+","+inetSocketAddress.getPort());
                    log.info("缓存删除成功");
                    channelCompletableFuture.completeExceptionally(future.cause());
                    log.info("连接netty服务失败");
                }
            }
        });
        return channelCompletableFuture.get();
    }
}
