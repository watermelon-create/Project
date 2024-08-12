package com.mszlu.rpc.netty.codec;

import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.message.MsRequest;
import com.mszlu.rpc.message.MsResponse;
import com.mszlu.rpc.compress.Compress;
import com.mszlu.rpc.constant.CompressTypeEnum;
import com.mszlu.rpc.constant.MessageTypeEnum;
import com.mszlu.rpc.constant.MsRpcConstants;
import com.mszlu.rpc.constant.SerializationTypeEnum;
import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.util.ServiceLoader;

public class MsRpcDecoder extends LengthFieldBasedFrameDecoder {
    public MsRpcDecoder() {
        this(8*1024*1024,5,4,-9,0);
    }

    /**
     *
     * @param maxFrameLength 最大帧长度。它决定可以接收的数据的最大长度。如果超过，数据将被丢弃,根据实际环境定义
     * @param lengthFieldOffset 数据长度字段开始的偏移量, magic code+version=长度为5
     * @param lengthFieldLength 消息长度的大小  full length（消息长度） 长度为4
     * @param lengthAdjustment 补偿值 lengthAdjustment+数据长度取值=长度字段之后剩下包的字节数(x + 16=7 so x = -9)
     * @param initialBytesToStrip 忽略的字节长度，如果要接收所有的header+body 则为0，如果只接收body 则为header的长度 ,我们这为0
     */
    public MsRpcDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decode = super.decode(ctx, in);
        //数据发过来后，先进入到这里，进行解码
        if(decode instanceof ByteBuf){
            ByteBuf frame=(ByteBuf)decode;
            int length = frame.readableBytes();
            if(length< MsRpcConstants.TOTAL_LENGTH){
                throw new RuntimeException("数据长度不符");
            }
            return decodeFrame(frame);
        }
        return decode;
    }

    private Object decodeFrame(ByteBuf frame) {
        //顺序读取
        checkMagicCode(frame);
        //检测版本
        checkVersion(frame);
        //数据长度
        int fullLength = frame.readInt();
        //MessageType 消息类型
        byte messageType = frame.readByte();
        //1B codec (序列化类型)
        byte codecType = frame.readByte();
        //6.压缩类型
        byte compressType = frame.readByte();
        //7. 请求id
        int requestId = frame.readInt();
        //8. 读取数据
        MsMessage msMessage = MsMessage.builder()
                .codec(codecType)
                .compress(compressType)
                .messageType(messageType)
                .requestId(requestId)
                .build();
        int bodyLength = fullLength - MsRpcConstants.TOTAL_LENGTH;
        if (bodyLength > 0){
            //有数据,读取body的数据
            byte[] bodyData = new byte[bodyLength];
            frame.readBytes(bodyData);
            //解压缩 使用gzip
            Compress compress = loadCompress(compressType);
            bodyData = compress.decompress(bodyData);
            //反序列化
            Serializer serializer=loadSerializer(codecType);
            //根据不同的业务 进行反序列化
            //客户端 发请求 服务端 响应数据
            //MsRequest MsResponse
            if(MessageTypeEnum.REQUEST.getCode()==messageType){
                MsRequest msRequest = (MsRequest)serializer.deserialize(bodyData, MsRequest.class);
                msMessage.setData(msRequest);
            }
            if(MessageTypeEnum.RESPONSE.getCode()==messageType){
                MsResponse msResponse=(MsResponse)serializer.deserialize(bodyData, MsResponse.class);
                msMessage.setData(msResponse);
            }

        }
        return msMessage;

    }

    private Serializer loadSerializer(byte codecType) {

        String name = SerializationTypeEnum.getName(codecType);
        ServiceLoader<Serializer> load = ServiceLoader.load(Serializer.class);
        for (Serializer serializer:load){
            if(serializer.name().equals(name)){
                return serializer;
            }
        }

        throw new MsRpcException("无对应的序列化类型");
    }

    private Compress loadCompress(byte compressType) {
        String compressName = CompressTypeEnum.getName(compressType);
        ServiceLoader<Compress> load = ServiceLoader.load(Compress.class);
        for (Compress compress:load){
            if (compress.name().equals(compressName)) {
                return compress;
            }
        }
        throw new MsRpcException("无对应的压缩类型");
    }

    private void checkVersion(ByteBuf frame) {
        byte b = frame.readByte();
        if(b!=MsRpcConstants.VERSION){
            throw new MsRpcException("未知的version");
        }
    }

    private void checkMagicCode(ByteBuf frame) {
        int length = MsRpcConstants.MAGIC_NUMBER.length;
        byte[] tmp=new byte[length];
        frame.readBytes(tmp);
        for (int i = 0; i < length; i++) {
            if(tmp[i]!=MsRpcConstants.MAGIC_NUMBER[i]){
                throw new RuntimeException("传递的魔法数有误");
            }

        }
    }
}
