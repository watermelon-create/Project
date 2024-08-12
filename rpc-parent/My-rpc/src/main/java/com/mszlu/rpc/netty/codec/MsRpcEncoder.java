package com.mszlu.rpc.netty.codec;

import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.compress.Compress;
import com.mszlu.rpc.constant.CompressTypeEnum;
import com.mszlu.rpc.constant.MsRpcConstants;
import com.mszlu.rpc.constant.SerializationTypeEnum;
import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;

public class MsRpcEncoder extends MessageToByteEncoder<MsMessage> {

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    // 4B  magic number（魔法数）
    // 1B version（版本）
    // 4B full length（消息长度）
    // 1B messageType（消息类型）
    // 1B codec（序列化类型）
    // 1B compress（压缩类型）
    // 4B  requestId（请求的Id）
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext,
                          MsMessage msg, ByteBuf out) throws Exception {
        //拿到message 要进行编码处理
        out.writeBytes(MsRpcConstants.MAGIC_NUMBER);
        out.writeByte(MsRpcConstants.VERSION);
        // 预留数据长度位置
        out.writerIndex(out.writerIndex() + 4);
        byte messageType = msg.getMessageType();
        out.writeByte(messageType);
        out.writeByte(msg.getCodec());
        out.writeByte(msg.getCompress());
        //请求id 原子操作 线程安全 相对加锁 效率高
        out.writeInt(ATOMIC_INTEGER.getAndIncrement());
        // build full length
        byte[] bodyBytes = null;
        //header 长度为 16
        int fullLength = MsRpcConstants.HEAD_LENGTH;
        // fullLength = head length + body length
        // 序列化数据
        Serializer serializer = loadSerializer(msg.getCodec());
        bodyBytes = serializer.serialize(msg.getData());
        // 压缩数据
        Compress compress = loadCompress(msg.getCompress());
        bodyBytes = compress.compress(bodyBytes);
        fullLength += bodyBytes.length;
        out.writeBytes(bodyBytes);
        int writeIndex = out.writerIndex();
        //将fullLength写入之前的预留的位置
        out.writerIndex(writeIndex - fullLength + MsRpcConstants.MAGIC_NUMBER.length + 1);
        out.writeInt(fullLength);
        out.writerIndex(writeIndex);
    }
    private Serializer loadSerializer(byte codecType) {
        String serializerName = SerializationTypeEnum.getName(codecType);
        ServiceLoader<Serializer> load = ServiceLoader.load(Serializer.class);
        for (Serializer serializer : load) {
            if (serializer.name().equals(serializerName)) {
                return serializer;
            }
        }
        throw new MsRpcException("无对应的序列化类型");
    }

    private Compress loadCompress(byte compressType) {
        String compressName = CompressTypeEnum.getName(compressType);
        ServiceLoader<Compress> load = ServiceLoader.load(Compress.class);
        for (Compress compress : load) {
            if (compress.name().equals(compressName)) {
                return compress;
            }
        }
        throw new MsRpcException("无对应的压缩类型");
    }
}
