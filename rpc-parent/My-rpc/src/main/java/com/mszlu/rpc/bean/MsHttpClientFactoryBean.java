package com.mszlu.rpc.bean;

import com.mszlu.rpc.proxy.MsHttpClientProxy;
import org.springframework.beans.factory.FactoryBean;

public class MsHttpClientFactoryBean<T> implements FactoryBean<T> {
    private Class<T> interfaceClass;

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }
    @Override
    public T getObject() throws Exception {
        return new MsHttpClientProxy().getProxy(interfaceClass);
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
