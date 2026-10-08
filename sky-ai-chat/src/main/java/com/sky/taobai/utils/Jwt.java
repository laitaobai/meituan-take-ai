package com.sky.taobai.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 仅用于从用户令牌中读取 userId 做会话隔离，不做签名校验。
 * 真正的鉴权由 sky-server 的 JWT 拦截器完成，此处只取明文载荷用于路由。
 */
public final class Jwt {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Jwt() {
    }

    public static Long parseUserIdOrNull(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode node = MAPPER.readTree(payload);
            JsonNode userId = node.get("userId");
            if (userId == null || userId.isNull()) {
                return null;
            }
            return userId.asLong();
        } catch (Exception e) {
            return null;
        }
    }
}
