package com.sky.taobai.context;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存放等待用户本人确认的动作。
 * 当前为单实例内存实现，若 AI 服务多实例部署，需改为 Redis 存储。
 */
@Component
public class PendingActionStore {

    private final Map<Long, PendingAction> store = new ConcurrentHashMap<>();

    public void put(Long userId, PendingAction action) {
        store.put(keyOf(userId), action);
    }

    public PendingAction peek(Long userId) {
        PendingAction action = store.get(keyOf(userId));
        if (action == null) {
            return null;
        }
        if (action.getExpireAt() < System.currentTimeMillis()) {
            store.remove(keyOf(userId));
            return null;
        }
        return action;
    }

    public void remove(Long userId) {
        store.remove(keyOf(userId));
    }

    private Long keyOf(Long userId) {
        return userId == null ? 0L : userId;
    }
}
