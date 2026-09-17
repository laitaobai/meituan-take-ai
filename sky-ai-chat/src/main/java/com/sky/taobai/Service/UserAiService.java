package com.sky.taobai.Service;

public interface UserAiService {


    /**
     * 清空购物车商品
     */
    void cleanShoppingCart();

    /**
     * 用户取消订单
     * @param id 订单id
     * @throws Exception 业务异常
     */
    void userCancelById(Long id) throws Exception;

    /**
     * 再来一单
     * @param id 订单id
     */
    void repetition(Long id);

    /**
     * 用户催单
     * @param id 订单id
     */
    void reminder(Long id);

}
