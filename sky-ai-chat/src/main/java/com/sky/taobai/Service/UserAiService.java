package com.sky.taobai.Service;

public interface UserAiService {

    /**
     * 清空购物车商品
     * @param token 当前用户令牌
     */
    void cleanShoppingCart(String token);

    /**
     * 提交取消订单申请，不直接取消订单
     * @param id 订单id
     * @param token 当前用户令牌
     * @return 给大模型的回执文案
     * @throws Exception 业务异常
     */
    String userCancelById(Long id, String token) throws Exception;

    /**
     * 再来一单
     * @param id 订单id
     * @param token 当前用户令牌
     */
    void repetition(Long id, String token);

    /**
     * 用户催单
     * @param id 订单id
     * @param token 当前用户令牌
     */
    void reminder(Long id, String token);

}
