package com.mszlu.rpc.provider.service;

import com.mszlu.rpc.annotation.MsService;
import com.mszlu.rpc.provider.service.modal.Goods;



public interface GoodsService {

    /**
     * 根据商品id 查询商品
     * @param id
     * @return
     */
    Goods findGoods(Long id);
}
