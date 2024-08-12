package com.mszlu.rpc.exception;

public class MsRpcException extends RuntimeException{
    public MsRpcException(String message) {
        super(message);
    }
    public MsRpcException(String message,Exception e) {
        super(message,e);
    }
    public MsRpcException() {
    }
}
