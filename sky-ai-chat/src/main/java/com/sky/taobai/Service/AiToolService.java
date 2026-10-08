package com.sky.taobai.Service;

import org.springframework.ai.chat.model.ToolContext;


public interface AiToolService {
    void say(String msg);

    /**
     * 清空购物车商品
     */
    void cleanShoppingCart(ToolContext context);

    /**
     * 提交取消订单申请，需用户本人确认后才会真正取消
     * @param id 订单id
     * @param context 工具上下文，携带当前用户令牌
     * @return 给大模型的回执文案
     * @throws Exception 业务异常
     */
    String userCancelById(Long id, ToolContext context) throws Exception;

    /**
     * 再来一单
     * @param id 订单id
     * @param context 工具上下文，携带当前用户令牌
     */
    void repetition(Long id, ToolContext context);


    void reminder(Long id, ToolContext context);

}
