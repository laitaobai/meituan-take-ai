package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.mq.OrderMessageProducer;
import com.sky.mq.PaySuccessEvent;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderCancelApplyVO;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    /** 取消订单确认凭证在 Redis 中的前缀 */
    private static final String CANCEL_CONFIRM_KEY = "sky:order:cancel:confirm:";

    /** 确认凭证有效期，单位分钟 */
    private static final long CANCEL_CONFIRM_TTL_MINUTES = 5;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private OrderMessageProducer orderMessageProducer;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional // 添加事务，具有原子性
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
//        异常情况的处理（收货地址为空、超出配送范围、购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

//        检查用户的收货地址是否超出配送范围
        checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

        Long currentId = BaseContext.getCurrentId();

        ShoppingCart shoppingCart = new ShoppingCart();
//        只能查询当前用户数据
        shoppingCart.setUserId(currentId);

//        查询当前用户的购物车数据
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //构造订单数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(currentId);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);
        order.setOrderTime(LocalDateTime.now());
        orderMapper.insert(order);

//        订单明细数据
        ArrayList<OrderDetail> orderDetailList = new ArrayList<>();
        shoppingCartList.forEach(cart->{
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        });

//        向明细表中查询n条数据
        orderDetailMapper.insertBatch(orderDetailList);

//        清理购物车中的数据
        shoppingCartMapper.deleteByUserId(currentId);

//        封装返回结果
        OrderSubmitVO submitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();

//        下单本身不发消息，支付成功才是真正的业务事件
        return submitVO;
    }

    /**
     * 事务提交成功后才发送消息，避免支付事务回滚但消息已投递出去
     */
    private void publishPaySuccess(PaySuccessEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    orderMessageProducer.sendPaySuccess(event);
                }
            });
        } else {
            orderMessageProducer.sendPaySuccess(event);
        }
    }

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     * @throws Exception
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
//        当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(String.valueOf(userId));

        String orderNumber = ordersPaymentDTO.getOrderNumber();
        Orders orders = orderMapper.getByNumberAndUserId(orderNumber, userId);

//        调用微信支付接口，生成预支付交易单
      /*  JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(),
                orders.getAmount(),
                "苍穹外卖订单" + orders.getId(),
                user.getOpenid()
        );*/
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功：只投递消息，状态变更、来单提醒、支付通知全部由消费者异步完成，避免阻塞用户等待
     * @param outTradeNo
     */
    @Override
    public void paySuccess(String outTradeNo) {
//        当前登录用户id
        Long userId = BaseContext.getCurrentId();

//        根据订单号查询当前用户的订单
        Orders orderDB = orderMapper.getByNumberAndUserId(outTradeNo, userId);
        if (orderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        publishPaySuccess(PaySuccessEvent.builder()
                .orderId(orderDB.getId())
                .orderNumber(orderDB.getNumber())
                .userId(userId)
                .amount(orderDB.getAmount())
                .payTime(LocalDateTime.now())
                .build());
    }

    /**
     * 历史订单查询
     *
     * @param pageNum
     * @param pageSize
     * @param status   订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     * @return
     */
    @Override
    public PageResult pageQueryForUser(int pageNum, int pageSize, Integer status) {
//        设置分页
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

//        分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        ArrayList<OrderVO> list = new ArrayList<>();

//        查询出订单明细，并封装入OrderVo进行响应
        if (page != null && page.getTotal() > 0) {
            page.forEach(orders -> {
                Long ordersId = orders.getId();

//                查询订单明细
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(ordersId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                list.add(orderVO);
            });
        }

        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {
//        根据id查询订单
        Orders orders = orderMapper.getById(id);

//        查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

//        将订单及其详情封装到OrderVo并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用户取消订单
     * @param id
     */
    @Override
    @Transactional
    public void userCancelById(Long id) throws Exception {
//        根据id查询订单
        Orders orderDB = orderMapper.getById(id);

//        校验订单是否存在
        if (orderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

//        校验订单归属，防止取消他人订单
        Long currentId = BaseContext.getCurrentId();
        if (currentId != null && !currentId.equals(orderDB.getUserId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

//      订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (orderDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(orderDB.getId());

//        订单处于待接单的状态下取消，需要进行退款
        if (orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
//            调用微信支付退款接口  ，代码没问题。单纯支付不了
            /*weChatPayUtil.refund(
                    orderDB.getNumber(),
                    orderDB.getNumber(),
                    orders.getAmount(),
                    orders.getAmount()
            );
*/
//            支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

//        更新订单状态，取消原因、时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 提交取消申请，只签发一次性确认凭证，不改动订单数据
     * @param id 订单id
     */
    @Override
    public OrderCancelApplyVO applyCancel(Long id) {
        Long userId = BaseContext.getCurrentId();

        Orders orderDB = orderMapper.getById(id);
        if (orderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (userId == null || !userId.equals(orderDB.getUserId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
//      只有待付款(1)、待接单(2)可以申请取消
        if (orderDB.getStatus() > Orders.TO_BE_CONFIRMED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(
                CANCEL_CONFIRM_KEY + token,
                userId + ":" + orderDB.getId(),
                CANCEL_CONFIRM_TTL_MINUTES,
                TimeUnit.MINUTES);

        return OrderCancelApplyVO.builder()
                .confirmToken(token)
                .orderId(orderDB.getId())
                .orderNumber(orderDB.getNumber())
                .amount(orderDB.getAmount())
                .expireSeconds((int) TimeUnit.MINUTES.toSeconds(CANCEL_CONFIRM_TTL_MINUTES))
                .build();
    }

    /**
     * 用户本人确认后执行取消，凭证一次性生效
     * @param token 一次性确认凭证
     */
    @Override
    @Transactional
    public void confirmCancel(String token) throws Exception {
        if (token == null || token.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.CANCEL_CONFIRM_INVALID);
        }
        Long userId = BaseContext.getCurrentId();
        String key = CANCEL_CONFIRM_KEY + token;
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            throw new OrderBusinessException(MessageConstant.CANCEL_CONFIRM_INVALID);
        }

        String[] parts = value.split(":");
        if (parts.length != 2 || !Long.valueOf(parts[0]).equals(userId)) {
            throw new OrderBusinessException(MessageConstant.CANCEL_CONFIRM_MISMATCH);
        }

//        凭证一次性，确认后立即失效
        stringRedisTemplate.delete(key);

        userCancelById(Long.valueOf(parts[1]));
    }

    /**
     * 支付成功后将订单置为已支付待接单，重复消息直接忽略
     */
    @Override
    @Transactional
    public void markOrderPaid(Long orderId, Long userId) {
        Orders orderDB = orderMapper.getById(orderId);
        if (orderDB == null) {
            return;
        }
//        幂等：已到待接单及之后的状态说明处理过，不重复改
        if (orderDB.getStatus() >= Orders.TO_BE_CONFIRMED) {
            log.info("订单已支付，跳过重复处理, orderId={}, status={}", orderId, orderDB.getStatus());
            return;
        }

        Orders orders = Orders.builder()
                .id(orderId)
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
        log.info("订单已置为已支付待接单, orderId={}, userId={}", orderId, userId);
    }

    /**
     * 支付成功后商家超时未接单则自动取消，由死信队列触发
     */
    @Override
    @Transactional
    public void cancelTimeoutOrder(Long orderId, Long userId) {
        Orders orderDB = orderMapper.getById(orderId);
        if (orderDB == null) {
            return;
        }
//        只有仍处于待接单才说明商家未接单，已接单或已取消的直接忽略
        if (!Orders.TO_BE_CONFIRMED.equals(orderDB.getStatus())) {
            log.info("订单已被接单或已取消，跳过超时取消, orderId={}, status={}", orderId, orderDB.getStatus());
            return;
        }

        Orders orders = new Orders();
        orders.setId(orderId);
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("商家超时未接单，系统自动取消");
        orders.setCancelTime(LocalDateTime.now());
//        已支付订单超时取消需要退款，支付状态置为退款
        orders.setPayStatus(Orders.REFUND);
        orderMapper.update(orders);
        log.info("商家超时未接单，系统自动取消订单, orderId={}, userId={}", orderId, userId);
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
//        查询当前用户id
        Long userId = BaseContext.getCurrentId();

//        根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

//        将订单详情对象转为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();

//            将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(orderDetail, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

//        将购物车对象批量添加到购物车
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> pageQuery = orderMapper.pageQuery(ordersPageQueryDTO);

//        部分订单状态，需要额外返回订单菜品信息，将orders转化为orderVo
        List<OrderVO> orderVoList = getOrderVoList(pageQuery);
        return new PageResult(pageQuery.getTotal(), orderVoList);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
//        根据状态，分别查询出接待单，待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

//        将查询出的数据封装
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);

        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersCancelDTO
     */
    @Override
    public void confirm(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
//        根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

//        订单只有存在且状态为2（待接单）才可以拒单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

//        支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orders.PAID) {
//            用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    ordersDB.getAmount(),
//                    ordersDB.getAmount()
//            );
//            log.info("申请退款：{}", refund);
        }

//        拒单需要退款，根据订单id更新订单状态，拒单原因，取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    @Override
    @Transactional
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
//        根据id查询订单
        Orders orderDB = orderMapper.getById(ordersCancelDTO.getId());

//        校验订单是否存在
        if (orderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

//      已完成(5)、已取消(6)的订单不允许再次取消
        if (orderDB.getStatus() >= Orders.COMPLETED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

//        支付状态
        Integer payStatus = orderDB.getPayStatus();
        if (payStatus == Orders.PAID) {
//            用于已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    orderDB.getNumber(),
//                    orderDB.getNumber(),
//                    orderDB.getAmount(),
//                    orderDB.getAmount()
            //);
//            log.info("申请退款：{}", refund);
        }

//      管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void delivery(Long id) {
//        根据id查询订单
        Orders orderDB = orderMapper.getById(id);

//        校验订单是否存在，并且状态为3
        if (orderDB == null || !orderDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(orderDB.getId());

//        更新订单状态，状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {
//        根据id查询订单
        Orders orderDB = orderMapper.getById(id);

//        校验订单是否存在，并且状态为4
        if (orderDB == null || !orderDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(orderDB.getId());

//        更新订单状态，状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 用户催单
     * @param id
     */
    @Override
    public void reminder(Long id) {
//        查询订单是否存在
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

//        基于WebSocket实现催单
        HashMap map = new HashMap();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content", "订单号：" + orders.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 跳过微信支付，由前端发送请求进行修改订单状态
     * @param orderId
     * @param status
     */
    public void paySuccessNo(Long orderId, Integer status) {
        //当前登录用户id
        Long userId = BaseContext.getCurrentId();

//        根据订单号查询当前用户的订单
        Orders orderDB = orderMapper.getByOrderIdAndUserId(orderId, userId);
        if (orderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

//        与 paySuccess 保持一致，跳过支付时同样只投递消息
        publishPaySuccess(PaySuccessEvent.builder()
                .orderId(orderDB.getId())
                .orderNumber(orderDB.getNumber())
                .userId(userId)
                .amount(orderDB.getAmount())
                .payTime(LocalDateTime.now())
                .build());
    }

    /**
     * 部分订单状态，需要额外返回订单菜品信息，将orders转化为orderVo
     * @param page
     * @return
     */
    private List<OrderVO> getOrderVoList(Page<Orders> page) {
//        需要返回订单菜品信息，自定义OrderVo响应结果
        ArrayList<OrderVO> orderVOArrayList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            ordersList.forEach(orders -> {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);

                String orderDishStr = getOrderDishStr(orders);
                orderVO.setOrderDishes(orderDishStr);
                orderVOArrayList.add(orderVO);
            });
        }
        return orderVOArrayList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     * @param orders
     * @return
     */
    private String getOrderDishStr(Orders orders) {
//        查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

//        将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> ordewrDishList = orderDetailList.stream().map(orderDetail -> orderDetail.getName() + "*" + orderDetail.getNumber() + ";").collect(Collectors.toList());

//        将该订单对应的所有菜品信息拼接在一起
        return String.join("", ordewrDishList);
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address) {
        HashMap map = new HashMap();
        map.put("address", shopAddress);
        map.put("output", "json");
        map.put("ak", ak);

//        获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3/", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("店铺地址解析失败");
        }
        System.out.println("百度返回内容：" + jsonObject);
//        数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");

//        店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address", address);

//        获取用户地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

//        数据解析
        JSONObject jsonObjectUser = JSON.parseObject(userCoordinate);
        JSONObject UserLocation = jsonObjectUser.getJSONObject("result").getJSONObject("location");
        lat = UserLocation.getString("lat");
        lng = UserLocation.getString("lng");

//        用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin", shopLngLat);
        map.put("destination", userLngLat);
        map.put("steps_info", "0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("配送线路规划失败");
        }

//        数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 500000){
            //配送距离超过500000米
            throw new OrderBusinessException("超出配送范围");
        }
    }



}
