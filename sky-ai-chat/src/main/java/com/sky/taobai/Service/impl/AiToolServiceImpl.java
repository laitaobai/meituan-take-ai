package com.sky.taobai.Service.impl;

import com.sky.taobai.Service.AiToolService;
import com.sky.taobai.Service.UserAiService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AiToolServiceImpl implements AiToolService {

    @Autowired
    public RestTemplate restTemplate;

    @Autowired
    public UserAiService userAiService;



    @Tool(description = "无作用")
    @Override
    public void say(@ToolParam(description = "单号") String msg) {

    }

    /**
     * 清空购物车商品
     */
    @Tool(description = "清空购物车")
    @Override
    public void cleanShoppingCart() {
       userAiService.cleanShoppingCart();
    }
    /**
     * 用户取消订单
     * @param id 订单id
     * @throws Exception 业务异常
     */
    @Tool(description = "取消订单")
    @Override
    public void userCancelById(@ToolParam(description = "订单号")Long id) throws Exception {
     userAiService.userCancelById(id);
    }
    /**
     * 再来一单
     * @param id 订单id
     */
    @Tool(description = "再来一单")
    @Override
    public void repetition(@ToolParam(description = "订单号")Long id) {
         userAiService.repetition(id);
    }

    /**
     * 催单
     */
    @Tool(description = "催单")
    @Override
    public void reminder(@ToolParam(description = "订单号") Long id) {

         userAiService.reminder(id);
    }
}
