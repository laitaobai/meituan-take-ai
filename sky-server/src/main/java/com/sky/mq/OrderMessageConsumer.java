package com.sky.mq;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import com.sky.service.OrderService;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * 支付成功消息的消费者。
 * 订单状态、来单提醒、支付通知三个消费者同时接收同一条事件，各自处理互不阻塞；
 * 超时未接单由 TTL 队列过期后投递到死信队列，再由第四个消费者处理。
 */
@Component
@Slf4j
public class OrderMessageConsumer {

    private static final String CONSUMED_KEY_PREFIX = "sky:order:mq:consumed:";

    @Autowired
    private OrderService orderService;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private OrderMessageProducer orderMessageProducer;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 消费者一：修改订单状态为已支付待接单，并投递超时检查消息
     */
    @RabbitListener(queues = RabbitMQConfig.PAY_STATUS_QUEUE)
    public void handlePayStatus(PaySuccessEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            if (isEmpty(event)) {
                log.warn("支付成功消息内容为空，直接确认");
                channel.basicAck(tag, false);
                return;
            }
            if (!markConsumed("status", event.getOrderId())) {
                log.info("订单状态消息已处理过，跳过, orderId={}", event.getOrderId());
                channel.basicAck(tag, false);
                return;
            }

            log.info("订单状态消费者收到支付成功消息, orderId={}", event.getOrderId());
            orderService.markOrderPaid(event.getOrderId(), event.getUserId());
            orderMessageProducer.sendTimeoutCheck(event);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("处理订单状态消息失败, orderId={}", event == null ? null : event.getOrderId(), e);
            channel.basicNack(tag, false, true);
        }
    }

    /**
     * 消费者二：向服务端（商家管理端）推送来单提醒
     */
    @RabbitListener(queues = RabbitMQConfig.PAY_REMIND_QUEUE)
    public void handlePayRemind(PaySuccessEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            if (isEmpty(event)) {
                channel.basicAck(tag, false);
                return;
            }
            if (!markConsumed("remind", event.getOrderId())) {
                log.info("来单提醒消息已处理过，跳过, orderId={}", event.getOrderId());
                channel.basicAck(tag, false);
                return;
            }

            log.info("来单提醒消费者收到支付成功消息, orderId={}", event.getOrderId());
            HashMap map = new HashMap();
            map.put("type", 1);
            map.put("orderId", event.getOrderId());
            map.put("content", "订单号：" + event.getOrderNumber());
            webSocketServer.sendToAllClient(JSON.toJSONString(map));
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("处理来单提醒消息失败, orderId={}", event == null ? null : event.getOrderId(), e);
            channel.basicNack(tag, false, true);
        }
    }

    /**
     * 消费者三：向用户本人推送支付成功通知，避免用户一直等待支付结果
     */
    @RabbitListener(queues = RabbitMQConfig.PAY_NOTIFY_QUEUE)
    public void handlePayNotify(PaySuccessEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            if (isEmpty(event)) {
                channel.basicAck(tag, false);
                return;
            }
            if (!markConsumed("notify", event.getOrderId())) {
                log.info("支付通知消息已处理过，跳过, orderId={}", event.getOrderId());
                channel.basicAck(tag, false);
                return;
            }

            log.info("支付通知消费者收到支付成功消息, orderId={}", event.getOrderId());
            HashMap map = new HashMap();
            map.put("type", 3);
            map.put("orderId", event.getOrderId());
            map.put("orderNumber", event.getOrderNumber());
            map.put("content", "支付成功，订单号：" + event.getOrderNumber());
            webSocketServer.sendToUser(event.getUserId(), JSON.toJSONString(map));
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("处理支付通知消息失败, orderId={}", event == null ? null : event.getOrderId(), e);
            channel.basicNack(tag, false, true);
        }
    }

    /**
     * 消费者四：TTL 到期后由死信队列投递过来，商家仍未接单则自动取消
     */
    @RabbitListener(queues = RabbitMQConfig.DLX_QUEUE)
    public void handleAcceptTimeout(PaySuccessEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            if (isEmpty(event)) {
                channel.basicAck(tag, false);
                return;
            }
            log.info("超时未接单消息到达, orderId={}", event.getOrderId());
            orderService.cancelTimeoutOrder(event.getOrderId(), event.getUserId());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("处理超时未接单消息失败, orderId={}", event == null ? null : event.getOrderId(), e);
            channel.basicNack(tag, false, true);
        }
    }

    private boolean isEmpty(PaySuccessEvent event) {
        return event == null || event.getOrderId() == null;
    }

    /**
     * 消费幂等标记，防止消息重复投递导致重复处理
     */
    private boolean markConsumed(String consumer, Long orderId) {
        String key = CONSUMED_KEY_PREFIX + consumer + ":" + orderId;
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 1, TimeUnit.HOURS);
        return Boolean.TRUE.equals(ok);
    }
}
