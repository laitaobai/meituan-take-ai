package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 取消订单申请结果，携带一次性确认凭证，供前端向用户二次确认
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelApplyVO implements Serializable {

    /** 一次性确认凭证 */
    private String confirmToken;

    /** 订单id */
    private Long orderId;

    /** 订单号 */
    private String orderNumber;

    /** 订单金额 */
    private BigDecimal amount;

    /** 凭证有效秒数 */
    private Integer expireSeconds;
}
