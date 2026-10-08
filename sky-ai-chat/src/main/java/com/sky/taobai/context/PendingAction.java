package com.sky.taobai.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 等待用户本人确认的动作，例如 AI 发起的取消订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingAction {

    /** 动作类型，如 cancel_order */
    private String type;

    /** 一次性确认凭证 */
    private String token;

    /** 关联订单id */
    private Long orderId;

    /** 关联订单号 */
    private String orderNumber;

    /** 展示给用户的标题 */
    private String title;

    /** 展示给用户的说明 */
    private String description;

    /** 过期时间戳，毫秒 */
    private long expireAt;
}
