package com.sky.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单消息生产者
 */
@Component
@Slf4j
public class OrderMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 广播支付成功事件，订单状态、来单提醒、支付通知三个消费者会同时收到
     */
    public void sendPaySuccess(PaySuccessEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.PAY_EXCHANGE, "", event);
        log.info("已发送支付成功消息, orderId={}, orderNumber={}", event.getOrderId(), event.getOrderNumber());
    }

    /**
     * 投递到无消费者的 TTL 队列，到期后自动进入死信交换机触发超时检查。
     * 使用默认交换机按队列名路由，无需声明额外交换机。
     */
    public void sendTimeoutCheck(PaySuccessEvent event) {
        rabbitTemplate.convertAndSend("", RabbitMQConfig.ORDER_TTL_QUEUE, event);
        log.info("已投递超时检查消息, orderId={}, ttl={}ms", event.getOrderId(), RabbitMQConfig.ACCEPT_TIMEOUT_MILLIS);
    }
}
