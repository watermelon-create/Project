package com.mszlu.rpc.netty.handler;

import com.mszlu.rpc.constant.MsRpcConstants;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.message.MsRequest;
import com.mszlu.rpc.message.MsResponse;
import com.mszlu.rpc.constant.MessageTypeEnum;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MsNettyServerHandler extends ChannelInboundHandlerAdapter {
    MsRequestHandler msRequestHandler;
    public MsNettyServerHandler() {
        msRequestHandler= SingletonFactory.getInstance(MsRequestHandler.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            //接收客户端发来的数据，数据肯定包括了要调用的服务提供者的 接口，方法，
            // 解析消息，去找到对应的服务提供者，然后调用，得到调用结果，发消息给客户端就可以
            if(msg instanceof MsMessage){
                MsMessage msgMessage = (MsMessage) msg;
                byte messageType = msgMessage.getMessageType();
                //收到ping,回复pong
                if(MessageTypeEnum.HEARTBEAT_PING.getCode()==messageType){
                    msgMessage.setMessageType(MessageTypeEnum.HEARTBEAT_PONG.getCode());
                    msgMessage.setData(MsRpcConstants.HEART_PONG);
                }
                if(MessageTypeEnum.REQUEST.getCode()==messageType){
                    MsRequest msRequest = (MsRequest) msgMessage.getData();
                    //处理业务，使用反射找到方法 发起调用 获取结果
                    Object result=msRequestHandler.handler(msRequest);
                    msgMessage.setMessageType(MessageTypeEnum.RESPONSE.getCode());
                    if(ctx.channel().isActive()&&ctx.channel().isWritable()){
                        MsResponse<Object> success = MsResponse.success(result, msRequest.getRequestId());
                        msgMessage.setData(success);
                    }else{
                        msgMessage.setData(MsResponse.fail("net fail"));
                    }
                }
                //失败时才关闭
                ctx.writeAndFlush(msgMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } catch (Exception e) {
            log.error("读取消息出错",e);
        }finally {
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 客户端10s内没有读请求进行心跳检测
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent evt1 = (IdleStateEvent) evt;
            IdleState state = evt1.state();
            if(state==IdleState.READER_IDLE){
                log.info("收到了心跳检测，超时未读取。。。");
            }else{
                super.userEventTriggered(ctx,evt);
            }
        }

    }

    /**
     * 出异常关闭链接
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        ctx.close();
    }
}
