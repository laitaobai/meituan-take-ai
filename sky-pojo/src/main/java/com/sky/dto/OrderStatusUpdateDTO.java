package com.sky.dto;

import lombok.Data;

@Data
public class OrderStatusUpdateDTO {

    private Long orderId;
    private Integer status;
}
