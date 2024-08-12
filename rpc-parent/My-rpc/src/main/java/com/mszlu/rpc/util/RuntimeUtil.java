package com.mszlu.rpc.util;

public class RuntimeUtil {
    public static int cpus(){
        return Runtime.getRuntime().availableProcessors();
    }
}
