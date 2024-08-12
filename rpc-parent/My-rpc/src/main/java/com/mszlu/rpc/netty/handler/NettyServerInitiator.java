package com.mszlu.rpc.netty.handler;

import com.mszlu.rpc.netty.codec.MsRpcDecoder;
import com.mszlu.rpc.netty.codec.MsRpcEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.concurrent.TimeUnit;

public class NettyServerInitiator extends ChannelInitializer<SocketChannel> {
    private EventExecutorGroup eventExecutors;
    public NettyServerInitiator(DefaultEventExecutorGroup eventExecutors) {
        this.eventExecutors=eventExecutors;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        //定义TCP协议 数据报文格式
        //10s内 没有读请求 触发心跳检测
        socketChannel.pipeline().addLast(new IdleStateHandler(10,0,0, TimeUnit.SECONDS));
        //解码器
        socketChannel.pipeline ().addLast ( "decoder",new MsRpcDecoder() );
        //编码器
        socketChannel.pipeline ().addLast ( "encoder",new MsRpcEncoder());

        //消息处理器，线程池处理
        socketChannel.pipeline ().addLast ( eventExecutors,"handler",new MsNettyServerHandler());
    }
}
