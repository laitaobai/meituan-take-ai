package com.sky.taobai.Service.impl;

import com.sky.taobai.Service.AiToolService;
import com.sky.taobai.Service.UserAiService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiToolServiceImpl implements AiToolService {

    public static final String TOKEN_KEY = "token";

    @Autowired
    public UserAiService userAiService;

    /**
     * 从工具上下文中取出当前用户令牌，该参数不会暴露给大模型
     */
    private String tokenOf(ToolContext context) {
        if (context == null || context.getContext() == null) {
            return null;
        }
        Object value = context.getContext().get(TOKEN_KEY);
        return value == null ? null : String.valueOf(value);
    }

    @Tool(description = "无作用")
    @Override
    public void say(@ToolParam(description = "单号") String msg) {

    }

    /**
     * 清空购物车商品
     */
    @Tool(description = "清空购物车")
    @Override
    public void cleanShoppingCart(ToolContext context) {
        userAiService.cleanShoppingCart(tokenOf(context));
    }

    /**
     * 提交取消订单申请，需用户本人确认后才会真正取消
     * @param id 订单id
     * @throws Exception 业务异常
     */
    @Tool(description = "取消订单")
    @Override
    public String userCancelById(@ToolParam(description = "订单号") Long id, ToolContext context) throws Exception {
        return userAiService.userCancelById(id, tokenOf(context));
    }

    /**
     * 再来一单
     * @param id 订单id
     */
    @Tool(description = "再来一单")
    @Override
    public void repetition(@ToolParam(description = "订单号") Long id, ToolContext context) {
        userAiService.repetition(id, tokenOf(context));
    }

    /**
     * 催单
     */
    @Tool(description = "催单")
    @Override
    public void reminder(@ToolParam(description = "订单号") Long id, ToolContext context) {
        userAiService.reminder(id, tokenOf(context));
    }
}
