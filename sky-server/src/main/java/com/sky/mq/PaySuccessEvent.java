package com.sky.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付成功事件，广播给订单状态、来单提醒、支付通知三个消费者
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaySuccessEvent implements Serializable {

    private Long orderId;

    private String orderNumber;

    private Long userId;

    private BigDecimal amount;

    private LocalDateTime payTime;
}
