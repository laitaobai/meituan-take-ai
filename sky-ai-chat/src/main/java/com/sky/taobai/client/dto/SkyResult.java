package com.sky.taobai.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * sky-server 统一响应结构
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SkyResult<T> {

    private Integer code;

    private String msg;

    private T data;
}
