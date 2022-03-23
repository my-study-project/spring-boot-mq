package com.js.controller;

import com.alibaba.fastjson.JSONObject;
import com.js.listen.SendCallbackListener;
import com.js.model.OrderStep;
import com.js.model.ResponseMsg;
import com.js.util.ListSplitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/mqMessageController")
@Slf4j
public class MqMessageController {
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value(value = "${rocketmq.producer.topic}:${rocketmq.producer.sync-tag}")
    private String syncTag;

    @Value(value = "${rocketmq.producer.topic}:${rocketmq.producer.async-tag}")
    private String asyncag;

    @Value(value = "${rocketmq.producer.topic}:${rocketmq.producer.oneway-tag}")
    private String onewayTag;


    /**
     * rocketmq 同步消息
     *
     * @param id 消息
     * @return 结果
     */
    @GetMapping("/pushMessage")
    public String pushMessage(@RequestParam("id") int id) {
        log.info("pushMessage start : " + id);
        // 构建消息
        String messageStr = "order id : " + id;
        Message<String> message = MessageBuilder.withPayload(messageStr)
                .setHeader(RocketMQHeaders.KEYS, id)
                .build();
        // 设置发送地和消息信息并发送同步消息
        SendResult sendResult = rocketMQTemplate.syncSend(syncTag, message);
        log.info("pushMessage finish : " + id + ", sendResult : " + JSONObject.toJSONString(sendResult));
        // 解析发送结果
        if (sendResult.getSendStatus() == SendStatus.SEND_OK) {
            return sendResult.getMsgId() + " : " + sendResult.getSendStatus();
        }
        return "FAIL";
    }

    /**
     * 发送异步消息
     *
     * @param id 消息
     * @return 结果
     */
    @GetMapping("/pushAsyncMessage")
    public String pushAsyncMessage(@RequestParam("id") int id) {
        log.info("pushAsyncMessage start : " + id);
        // 构建消息
        String messageStr = "order id : " + id;
        Message<String> message = MessageBuilder.withPayload(messageStr)
                .setHeader(RocketMQHeaders.KEYS, id)
                .build();
        // 设置发送地和消息信息并发送异步消息
        rocketMQTemplate.asyncSend(asyncag, message, new SendCallbackListener(id));
        log.info("pushAsyncMessage finish : " + id);
        return "msg";
    }

    /**
     * 发送单向消息（不关注发送结果：记录日志）
     *
     * @param id 消息
     * @return 结果
     */
    @GetMapping("/pushOneWayMessage")
    public ResponseMsg pushOneWayMessage(@RequestParam("id") int id) {
        log.info("pushOneWayMessage start : " + id);
        // 构建消息
        String messageStr = "order id : " + id;
        Message<String> message = MessageBuilder.withPayload(messageStr)
                .setHeader(RocketMQHeaders.KEYS, id)
                .build();
        // 设置发送地和消息信息并发送单向消息
        rocketMQTemplate.sendOneWay(onewayTag, message);
        log.info("pushOneWayMessage finish : " + id);
        ResponseMsg msg = new ResponseMsg();
        msg.setSuccessData(null);
        return msg;
    }

    /**
     * 发送包含顺序的单向消息
     *
     * @param id 消息
     * @return 结果
     */
    @GetMapping("/pushSequeueMessage")
    public ResponseMsg pushSequeueMessage(@RequestParam("id") int id) {
        log.info("pushSequeueMessage start : " + id);
        // 创建三个不同订单的不同步骤
        for (int i = 0; i < 3; i++) {
            // 处理当前订单唯一标识
            String myId = id + "" + i;
            // 获取当前订单的操作步骤列表
            List<OrderStep> myOrderSteps = OrderStep.buildOrderSteps(myId);
            // 依次操作步骤下发消息队列
            for (OrderStep orderStep : myOrderSteps) {
                // 构建消息
                String messageStr = String.format("order id : %s, desc : %s", orderStep.getId(), orderStep.getDesc());
                Message<String> message = MessageBuilder.withPayload(messageStr)
                        .setHeader(RocketMQHeaders.KEYS, orderStep.getId())
                        .build();
                // 设置顺序下发
                rocketMQTemplate.setMessageQueueSelector(new MessageQueueSelector() {
                    /**
                     * 设置放入同一个队列的规则
                     * @param list 消息列表
                     * @param message 当前消息
                     * @param o 比较的关键信息
                     * @return 消息队列
                     */
                    @Override
                    public MessageQueue select(List<MessageQueue> list, org.apache.rocketmq.common.message.Message message, Object o) {
                        // 根据当前消息的id，使用固定算法获取需要下发的队列
                        // （使用当前id和消息队列个数进行取模获取需要下发的队列，id和队列数量一样时，选择的队列坑肯定一样）
                        int queueNum = Integer.valueOf(String.valueOf(o)) % list.size();
                        log.info(String.format("queueNum : %s, message : %s", queueNum, new String(message.getBody())));
                        return list.get(queueNum);
                    }
                });
                // 设置发送地和消息信息并发送消息（Orderly）
                rocketMQTemplate.syncSendOrderly(syncTag, message, orderStep.getId());
            }
        }
        log.info("pushSequeueMessage finish : " + id);
        ResponseMsg msg = new ResponseMsg();
        msg.setSuccessData(null);
        return msg;
    }

    /**
     * rocketmq 延迟消息
     *
     * @param id 消息
     * @return 结果
     */
    @GetMapping("/pushDelayMessage")
    public ResponseMsg pushDelayMessage(@RequestParam("id") int id) {
        log.info("pushDelayMessage start : " + id);
        // 构建消息
        String messageStr = "order id : " + id;
        Message<String> message = MessageBuilder.withPayload(messageStr)
                .setHeader(RocketMQHeaders.KEYS, id)
                .build();
        // 设置超时和延时推送
        // 超时时针对请求broker然后结果返回给product的耗时
        // 现在RocketMq并不支持任意时间的延时，需要设置几个固定的延时等级，从1s到2h分别对应着等级1到18
        // private String messageDelayLevel = "1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h";
        SendResult sendResult = rocketMQTemplate.syncSend(syncTag, message, 1 * 1000l, 4);
        log.info("pushDelayMessage finish : " + id + ", sendResult : " + JSONObject.toJSONString(sendResult));
        ResponseMsg msg = new ResponseMsg();
        // 解析发送结果
        if (sendResult.getSendStatus() == SendStatus.SEND_OK) {
            msg.setSuccessData(sendResult.getMsgId() + " : " + sendResult.getSendStatus());
        }
        return msg;
    }

    /**
     * 同时发送10个单向消息（真正的批量）
     *
     * @param id 消息
     * @return 结果
     */
    @GetMapping("/pushBatchMessage")
    public ResponseMsg pushBatchMessage(@RequestParam("id") int id) {
        log.info("pushBatchMessage start : " + id);
        // 创建消息集合
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String myId = id + "" + i;
            // 处理当前订单唯一标识
            String messageStr = "order id : " + myId;
            Message<String> message = MessageBuilder.withPayload(messageStr)
                    .setHeader(RocketMQHeaders.KEYS, myId)
                    .build();
            messages.add(message);
        }
        // 批量下发消息到broker,不支持消息顺序操作，并且对消息体有大小限制（不超过4M）
        ListSplitter splitter = new ListSplitter(messages, 1024 * 1024 * 4);
        while (splitter.hasNext()) {
            List<Message> listItem = splitter.next();
            rocketMQTemplate.syncSend(syncTag, listItem);
        }
        log.info("pushBatchMessage finish : " + id);
        ResponseMsg msg = new ResponseMsg();
        msg.setSuccessData(null);
        return msg;
    }

    /**
     * sql过滤消息
     *
     * @param id 消息
     * @return 结果
     */
    @GetMapping("/pushSqlMessage")
    public ResponseMsg pushSqlMessage(@RequestParam("id") int id) {
        log.info("pushSqlMessage start : " + id);
        // 创建消息集合
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String myId = id + "" + i;
            // 处理当前订单唯一标识
            String messageStr = "order id : " + myId;
            Message<String> message = MessageBuilder.withPayload(messageStr)
                    .setHeader(RocketMQHeaders.KEYS, myId)
                    .setHeader("money", i)
                    .build();
            messages.add(message);
        }
        rocketMQTemplate.syncSend(syncTag, messages);
        log.info("pushSqlMessage finish : " + id);
        ResponseMsg msg = new ResponseMsg();
        msg.setSuccessData(null);
        return msg;
    }

    /**
     * 事务消息
     *
     * @param id 消息
     * @return 结果
     */
    @GetMapping("/pushTransactionMessage")
    public ResponseMsg pushTransactionMessage(@RequestParam("id") int id) {
        log.info("pushTransactionMessage start : " + id);
        // 创建消息
        String messageStr = "order id : " + id;
        Message<String> message = MessageBuilder.withPayload(messageStr)
                .setHeader(RocketMQHeaders.KEYS, id)
                .setHeader("money", 10)
                .setHeader(RocketMQHeaders.TRANSACTION_ID, id)
                .build();
        TransactionSendResult transactionSendResult = rocketMQTemplate.sendMessageInTransaction(syncTag, message, null);
        log.info("pushTransactionMessage result : " + JSONObject.toJSONString(transactionSendResult));
        log.info("pushTransactionMessage finish : " + id);
        ResponseMsg msg = new ResponseMsg();
        msg.setSuccessData(null);
        return msg;
    }

    /*    @GetMapping("/pushMessage")
    public String get(@RequestParam("id") int id) {
        rocketMQTemplate.convertAndSend("first-topic", "你好,Java旅途" + id);
        log.info("推送消息完成");
        return "SUCCESS";
    }*/
}