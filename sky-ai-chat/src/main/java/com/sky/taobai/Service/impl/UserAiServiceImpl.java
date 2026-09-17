package com.sky.taobai.Service.impl;

import com.sky.taobai.Service.UserAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserAiServiceImpl implements UserAiService {

     @Autowired
    RestTemplate restTemplate;

    /**
     * 清空购物车商品
     */
    @Override
    public void cleanShoppingCart() {
        String url = "http://localhost:8080/user/shoppingCart/clean";
        restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);

        System.out.println("已调用清空购物车商品业务===================================");
    }
    /**
     * 用户取消订单
     * @param id 订单id
     *  业务异常
     */
    @Override
    public void userCancelById(Long id) {
        try {
            String url = "http://localhost:8080/user/order/cancel/{id}";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    null,          // 无请求头、无请求体时，可以直接传 null
                    String.class,
                    id
            );
            System.out.println("已调用用户取消订单业务============================");
        }catch (Exception e){
            throw new RuntimeException(e.getMessage() + "===============================");
        }

    }
    /**
     * 再来一单
     * @param id 订单id
     */
    @Override
    public void repetition(Long id) {
       try {
           String url = "http://localhost:8080/user/order/repetition/{id}";
           restTemplate.postForObject(url, null, String.class,id);
           System.out.println("已调用再来一单业务=======================================");
       }catch (Exception e){
           throw new RuntimeException(e.getMessage() + "===============================");
       }



    }
    /**
     * 用户催单
     * @param id 订单id
     */
    @Override
    public void reminder(Long id) {
        String url = "http://localhost:8080/user/order/reminder/{id}";
        restTemplate.getForObject(url, String.class,id);

        System.out.println("已调用用户催单业务=====================================");
    }
}
