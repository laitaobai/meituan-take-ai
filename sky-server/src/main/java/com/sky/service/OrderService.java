package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderCancelApplyVO;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 历史订单查询
     *
     * @param pageNum
     * @param pageSize
     * @param status   订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     * @return
     */
    PageResult pageQueryForUser(int pageNum, int pageSize, Integer status);

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    OrderVO details(Long id);

    /**
     * 用户取消订单
     * @param id
     */
    void userCancelById(Long id) throws Exception;

    /**
     * 提交取消申请，只签发一次性确认凭证，不改动订单
     * @param id 订单id
     * @return 确认凭证与订单摘要
     */
    OrderCancelApplyVO applyCancel(Long id);

    /**
     * 用户本人确认后执行取消，凭证一次性生效
     * @param token 一次性确认凭证
     */
    void confirmCancel(String token) throws Exception;

    /**
     * 将订单置为已支付待接单，由支付成功的消费者调用，已处于待接单及之后状态的直接忽略
     * @param orderId 订单id
     * @param userId 下单用户id
     */
    void markOrderPaid(Long orderId, Long userId);

    /**
     * 支付成功后商家超时未接单则自动取消，由死信队列触发，已接单或已取消的订单直接忽略
     * @param orderId 订单id
     * @param userId 下单用户id
     */
    void cancelTimeoutOrder(Long orderId, Long userId);

    /**
     * 再来一单
     * @param id
     */
    void repetition(Long id);

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 各个状态的订单数量统计
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 接单
     * @param ordersCancelDTO
     */
    void confirm(OrdersCancelDTO ordersCancelDTO);

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception;

    /**
     * 派送订单
     * @param id
     */
    void delivery(Long id);

    /**
     * 完成订单
     * @param id
     */
    void complete(Long id);

    /**
     * 用户催单
     * @param id
     */
    void reminder(Long id);


    /**
     * 跳过微信支付，由前端发送请求进行修改订单状态
     * @param orderId
     * @param status
     */
    void paySuccessNo(Long orderId , Integer status);
}
