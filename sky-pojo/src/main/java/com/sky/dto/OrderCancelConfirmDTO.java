package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户确认取消订单的入参
 */
@Data
public class OrderCancelConfirmDTO implements Serializable {

    /** 一次性确认凭证 */
    private String token;
}
