package com.js.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单消息模型
 */
@Data
public class OrderStep {
    /**
     * 订单id
     */
    private String id;

    /**
     * 操作步骤
     */
    private String desc;

    /**
     * 步骤构建方法
     *
     * @param id   订单唯一标识
     * @param desc 步骤描述
     */
    public OrderStep(String id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    /**
     * 构建订单的所有操作步骤：创建、支付、回调
     *
     * @param id 订单唯一标识
     * @return 订单操作步骤列表
     */
    public static List<OrderStep> buildOrderSteps(String id) {
        List<OrderStep> orderSteps = new ArrayList<>();
        OrderStep createStep = new OrderStep(id, "create order");
        orderSteps.add(createStep);
        OrderStep payStep = new OrderStep(id, "pay order");
        orderSteps.add(payStep);
        OrderStep callbackStep = new OrderStep(id, "call back order");
        orderSteps.add(callbackStep);
        return orderSteps;
    }
}