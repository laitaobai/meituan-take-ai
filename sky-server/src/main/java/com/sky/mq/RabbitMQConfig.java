package com.sky.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付成功消息的交换机与队列声明。
 * 支付成功后广播给「订单状态」「来单提醒」「支付通知」三个消费者并行处理。
 * 超时未接单自动取消采用 TTL + 死信队列实现，不依赖延迟消息插件：
 * TTL 队列没有消费者，消息存活到期后由 RabbitMQ 自动投递到死信交换机。
 */
@Configuration
public class RabbitMQConfig {

    /** 支付成功广播交换机 */
    public static final String PAY_EXCHANGE = "sky.order.pay.exchange";

    /** 消费者一：修改订单状态 */
    public static final String PAY_STATUS_QUEUE = "sky.order.pay.status.queue";

    /** 消费者二：向服务端推送来单提醒 */
    public static final String PAY_REMIND_QUEUE = "sky.order.pay.remind.queue";

    /** 消费者三：向用户推送支付成功通知 */
    public static final String PAY_NOTIFY_QUEUE = "sky.order.pay.notify.queue";

    /** 无消费者的 TTL 队列，消息到期后进入死信交换机 */
    public static final String ORDER_TTL_QUEUE = "sky.order.ttl.queue";

    /** 死信交换机 */
    public static final String DLX_EXCHANGE = "sky.order.dlx.exchange";

    /** 死信队列 */
    public static final String DLX_QUEUE = "sky.order.dlx.queue";

    /** 死信路由键 */
    public static final String DLX_ROUTING_KEY = "order.timeout";

    /** 支付成功后允许商家接单的时限，超时自动取消，单位毫秒 */
    public static final int ACCEPT_TIMEOUT_MILLIS = 15 * 60 * 1000;

    /**
     * 使用 JSON 序列化消息，便于排查与跨语言消费
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public FanoutExchange payExchange() {
        return new FanoutExchange(PAY_EXCHANGE, true, false);
    }

    @Bean
    public Queue payStatusQueue() {
        return QueueBuilder.durable(PAY_STATUS_QUEUE).build();
    }

    @Bean
    public Queue payRemindQueue() {
        return QueueBuilder.durable(PAY_REMIND_QUEUE).build();
    }

    @Bean
    public Queue payNotifyQueue() {
        return QueueBuilder.durable(PAY_NOTIFY_QUEUE).build();
    }

    @Bean
    public Binding payStatusBinding() {
        return BindingBuilder.bind(payStatusQueue()).to(payExchange());
    }

    @Bean
    public Binding payRemindBinding() {
        return BindingBuilder.bind(payRemindQueue()).to(payExchange());
    }

    @Bean
    public Binding payNotifyBinding() {
        return BindingBuilder.bind(payNotifyQueue()).to(payExchange());
    }

    /**
     * TTL 队列：不注册任何消费者，消息存活到期后自动成为死信
     */
    @Bean
    public Queue orderTtlQueue() {
        return QueueBuilder.durable(ORDER_TTL_QUEUE)
                .ttl(ACCEPT_TIMEOUT_MILLIS)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with(DLX_ROUTING_KEY);
    }
}
