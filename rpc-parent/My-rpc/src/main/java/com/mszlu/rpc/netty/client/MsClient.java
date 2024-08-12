package com.mszlu.rpc.netty.client;

import com.mszlu.rpc.message.MsRequest;

public interface MsClient {
    /**
     * 发送请求，并接收数据
     * @param msRequest
     * @param host
     * @param port
     * @return
     */
    Object sendRequest(MsRequest msRequest);
}
