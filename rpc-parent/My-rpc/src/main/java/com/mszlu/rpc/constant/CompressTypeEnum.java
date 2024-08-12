package com.mszlu.rpc.constant;

import com.mszlu.rpc.compress.GzipCompress;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CompressTypeEnum {
    //读取协议这的压缩类型，来此枚举进行匹配
    GZIP((byte) 0x01, "gzip");

    private final byte code;
    private final String name;

    public static String getName(byte code) {
        for (CompressTypeEnum c : CompressTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}
