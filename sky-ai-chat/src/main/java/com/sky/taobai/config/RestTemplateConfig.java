package com.sky.taobai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 调用 sky-server 的 HTTP 客户端。
 * 不再从 Redis 读取全局 token：用户令牌由调用方按请求显式携带，避免多用户串号与启动期 NPE。
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
