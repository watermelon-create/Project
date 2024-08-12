package com.mszlu.rpc.proxy;

import com.mszlu.rpc.annotation.MsMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MsHttpClientProxy implements InvocationHandler {
    public MsHttpClientProxy() {

    }
    /**
     * 当接口实现调用的时候，实际上是代理类的invoke方法被调用了
     * @param
     * @param method
     * @param
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //实现业务，向服务提供方发起网络请求，获取结果并返回
        //在这里实现调用
        MsMapping msMapping = method.getAnnotation(MsMapping.class);
        if (msMapping != null){
            RestTemplate restTemplate = new RestTemplate();
            String api = msMapping.api();
            Pattern compile = Pattern.compile("(\\{\\w+})");
            Matcher matcher = compile.matcher(api);
            if (matcher.find()){
                //简单判断一下 代表有路径参数需要替换
                int x = matcher.groupCount();
                for (int i = 0; i< x; i++){
                    String group = matcher.group(i);
                    api = api.replace(group, args[i].toString());
                }
            }
            ResponseEntity forEntity = restTemplate.getForEntity(msMapping.url()+ api, method.getReturnType());
            return forEntity.getBody();
        }
        return null;
    }

    public <T> T getProxy(Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass}, this);
    }
}
