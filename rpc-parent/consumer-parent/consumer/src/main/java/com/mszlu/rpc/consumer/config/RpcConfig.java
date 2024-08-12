package com.mszlu.rpc.consumer.config;

import com.mszlu.rpc.annotation.EnableHttpClient;
import com.mszlu.rpc.annotation.EnableRpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableHttpClient(basePackage = "com.mszlu.rpc.consumer.rpc")
@EnableRpc
public class RpcConfig {

}
