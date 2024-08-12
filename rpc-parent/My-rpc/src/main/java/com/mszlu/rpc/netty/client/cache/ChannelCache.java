package com.mszlu.rpc.netty.client.cache;


import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ChannelCache {
    //channelCache...
    private final Map<String, Channel> channelMap;

    public ChannelCache(){
        channelMap = new ConcurrentHashMap<>();
    }

    public Channel get(InetSocketAddress inetSocketAddress){
        //获取缓存中的channel
        String key = inetSocketAddress.toString();
        if (channelMap.containsKey(key)){
            Channel channel = channelMap.get(key);
            if (channel != null && channel.isActive()){
                return channel;
            }else{
                remove(inetSocketAddress);
            }
        }
        return null;
    }

    public void set(InetSocketAddress inetSocketAddress, Channel channel){
        //放入缓存
        log.info("有channel 放入缓存：{}",inetSocketAddress.toString());
        channelMap.put(inetSocketAddress.toString(),channel);
    }

    public void remove(InetSocketAddress inetSocketAddress){
        //通道挂了，从缓存移除
        log.info("移出缓存：{}",inetSocketAddress.toString());
        channelMap.remove(inetSocketAddress.toString());
    }
}
