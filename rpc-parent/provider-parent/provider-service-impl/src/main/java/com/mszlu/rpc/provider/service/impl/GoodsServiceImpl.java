package com.mszlu.rpc.provider.service.impl;

import com.mszlu.rpc.annotation.MsService;
import com.mszlu.rpc.provider.service.GoodsService;
import com.mszlu.rpc.provider.service.modal.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@MsService(version = "1.0")
public class GoodsServiceImpl implements GoodsService {
    @Value("${server.port}")
    private int port;
    public Goods findGoods(Long id) {
        return new Goods(id,"服务提供方商品:"+port, BigDecimal.valueOf(100));
    }
}
