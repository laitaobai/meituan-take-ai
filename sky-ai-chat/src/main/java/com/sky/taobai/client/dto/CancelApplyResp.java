package com.sky.taobai.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 取消订单申请结果
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelApplyResp {

    private String confirmToken;

    private Long orderId;

    private String orderNumber;

    private BigDecimal amount;

    private Integer expireSeconds;
}
