package com.sky.taobai.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplateConfig {

    @Autowired
    public RedisTemplate redisTemplate;

 /*   @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }*/


    @Bean     //给请求头添加token
    public RestTemplate restTemplate(){
        RestTemplate restTemplate = new RestTemplate();

          String token = redisTemplate.opsForValue().get("token").toString();
//        String token = "8888888888888888888";
        System.out.println("jwt令牌：" + token + "=========================");

        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set("authentication",token);
            return execution.execute(request,body);

        });

        return restTemplate;
    }




}
