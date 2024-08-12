package com.mszlu.rpc.consumer.rpc;


import com.mszlu.rpc.annotation.MsMapping;
import com.mszlu.rpc.provider.service.modal.Goods;
import org.springframework.web.bind.annotation.PathVariable;

//取消使用Http代理的方式

//@com.mszlu.rpc.annontation.MsHttpClient(value = "goodsHttpRpc")
//public interface GoodsHttpRpc {
//    //实现对应的代理类---声明对应的实现类，注入IOC容器
//    @MsMapping(api = "/provider/goods/{id}",url = "http://localhost:7777")
//    Goods findGoods(Long id);
//}
