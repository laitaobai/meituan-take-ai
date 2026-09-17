package com.sky.taobai.Service;


public interface AiToolService {
    void say(String msg);

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


    void reminder(Long id);

}
