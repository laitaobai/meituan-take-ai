package com.sky.taobai.Service.impl;

import com.sky.taobai.Service.UserAiService;
import com.sky.taobai.client.SkyServerClient;
import com.sky.taobai.client.dto.CancelApplyResp;
import com.sky.taobai.client.dto.SkyResult;
import com.sky.taobai.context.PendingAction;
import com.sky.taobai.context.PendingActionStore;
import com.sky.taobai.utils.Jwt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserAiServiceImpl implements UserAiService {

    private static final String ACTION_CANCEL_ORDER = "cancel_order";

    private static final long DEFAULT_EXPIRE_SECONDS = 300;

    @Autowired
    SkyServerClient skyServerClient;

    @Autowired
    PendingActionStore pendingActionStore;

    private String header(String token) {
        return token == null ? "" : token;
    }

    /**
     * sky-server 业务失败时仍返回 200，需要按 code 判断
     */
    private void check(SkyResult<?> result, String bizName) {
        if (result == null || result.getCode() == null || result.getCode() != 1) {
            throw new RuntimeException(bizName + "失败：" + (result == null ? "服务无响应" : result.getMsg()));
        }
    }

    /**
     * 清空购物车商品
     */
    @Override
    public void cleanShoppingCart(String token) {
        try {
            check(skyServerClient.cleanShoppingCart(header(token)), "清空购物车");
            log.info("已调用清空购物车业务");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 提交取消订单申请，只有用户本人确认后才会真正取消
     */
    @Override
    public String userCancelById(Long id, String token) {
        try {
            SkyResult<CancelApplyResp> result = skyServerClient.applyCancel(id, header(token));
            check(result, "提交取消申请");

            CancelApplyResp data = result.getData();
            if (data == null || data.getConfirmToken() == null || data.getConfirmToken().isBlank()) {
                throw new RuntimeException("提交取消申请失败：未取得确认凭证");
            }

            long expireSeconds = data.getExpireSeconds() == null ? DEFAULT_EXPIRE_SECONDS : data.getExpireSeconds();
            pendingActionStore.put(Jwt.parseUserIdOrNull(token), PendingAction.builder()
                    .type(ACTION_CANCEL_ORDER)
                    .token(data.getConfirmToken())
                    .orderId(data.getOrderId())
                    .orderNumber(data.getOrderNumber())
                    .title("确认取消订单")
                    .description("订单号 " + data.getOrderNumber() + "，确认后将取消该订单，此操作不可撤销")
                    .expireAt(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expireSeconds))
                    .build());

            log.info("已生成取消订单申请, orderId={}, 等待用户确认", id);
            return "已生成取消申请（订单号 " + data.getOrderNumber() + "）。该操作需要用户本人在弹出的确认卡片上点击确认后才会真正取消订单，请提醒用户查看确认卡片。";
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 再来一单
     */
    @Override
    public void repetition(Long id, String token) {
        try {
            check(skyServerClient.repetition(id, header(token)), "再来一单");
            log.info("已调用再来一单业务, orderId={}", id);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 用户催单
     */
    @Override
    public void reminder(Long id, String token) {
        try {
            check(skyServerClient.reminder(id, header(token)), "催单");
            log.info("已调用用户催单业务, orderId={}", id);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
