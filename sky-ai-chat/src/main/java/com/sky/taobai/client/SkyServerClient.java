package com.sky.taobai.client;

import com.sky.taobai.client.dto.CancelApplyResp;
import com.sky.taobai.client.dto.SkyResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 调用 sky-server 用户端接口。
 * 令牌按请求显式传入，不使用全局共享令牌。
 * 注意：这里不声明直接取消订单的方法，AI 只能提交取消申请，由用户本人确认后才执行。
 */
@FeignClient(name = "sky-server", path = "/user")
public interface SkyServerClient {

    @DeleteMapping("/shoppingCart/clean")
    SkyResult<Void> cleanShoppingCart(@RequestHeader("authentication") String token);

    @PutMapping("/order/cancel/apply/{id}")
    SkyResult<CancelApplyResp> applyCancel(@PathVariable("id") Long id, @RequestHeader("authentication") String token);

    @PostMapping("/order/repetition/{id}")
    SkyResult<Void> repetition(@PathVariable("id") Long id, @RequestHeader("authentication") String token);

    @GetMapping("/order/reminder/{id}")
    SkyResult<Void> reminder(@PathVariable("id") Long id, @RequestHeader("authentication") String token);
}
