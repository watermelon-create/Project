package com.mszlu.rpc.netty.client.idle;

import com.mszlu.rpc.netty.client.cache.ChannelCache;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable//用来说明ChannelHandler是否可以在多个channel直接共享使用
@Slf4j
public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter
        implements TimerTask,ChannelHandlerHolder,CacheClearHandler{
    private final Bootstrap bootstrap;
    private final Timer timer;
    private final InetSocketAddress inetSocketAddress;

    private volatile boolean reconnect = true;
    private int attempts;

    private final CompletableFuture<Channel> completableFuture;
    private final ChannelCache channelCache;
    public ConnectionWatchdog(Bootstrap bootstrap, Timer timer, InetSocketAddress inetSocketAddress,
                              CompletableFuture<Channel> channelCompletableFuture, boolean reconnect, ChannelCache channelCache) {
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.inetSocketAddress = inetSocketAddress;
        this.reconnect = reconnect;
        this.completableFuture = channelCompletableFuture;
        this.channelCache=channelCache;
    }



    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("当前链路已经激活了，重连尝试次数重新置为0");
        super.channelActive(ctx);
        attempts=0;
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("通道连接关闭，服务端挂了？网络波动？");
        super.channelInactive(ctx);
        //重试
        if(reconnect){
            log.info("链接关闭，将进行重连");
            if (attempts<12) {
                attempts++;
                log.info("重连次数:{}",attempts);
            }else{
                reconnect=false;
                clear(inetSocketAddress);
            }
            int timeout=2<<attempts;
            timer.newTimeout(this,timeout, TimeUnit.SECONDS);
        }

    }
    @Override
    public void run(Timeout timeout) throws Exception {
        ChannelFuture future=null;
        synchronized (bootstrap){
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast(handlers());
                }
            });
            future=bootstrap.connect(inetSocketAddress);
        }
        //定时任务执行的位置
        bootstrap.connect(inetSocketAddress).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(future.isSuccess()){
                    completableFuture.complete(future.channel());
                    channelCache.set(inetSocketAddress, future.channel());
                }else{
                    future.channel().pipeline().fireChannelInactive();
                }
            }
        });
    }
}