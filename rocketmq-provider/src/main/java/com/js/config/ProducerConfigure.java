package com.js.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ProducerConfigure {


    @Value("${rocketmq.producer.group}")
    private String groupName;

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.instanceName}")
    private String instanceName;

    @Bean(name = "rocketMQProducer")
    public DefaultMQProducer rocketMQProducer() throws MQClientException {
        if (StringUtils.isBlank(groupName)) {
            throw new IllegalArgumentException("groupName is blank");
        }

        if (StringUtils.isBlank(nameServer)) {
            throw new IllegalArgumentException("nameServerAddr is blank");
        }

        DefaultMQProducer producer = new DefaultMQProducer(groupName);
        producer.setNamesrvAddr(nameServer);

        //如果需要同一个jvm中不同的producer往不同的mq集群发送消息，需要设置不同的instanceName
        if (StringUtils.isNotBlank(instanceName)) {
            producer.setInstanceName(instanceName);
        }

        producer.setSendMsgTimeout(3000);//默认值 3s
        producer.setRetryTimesWhenSendFailed(1);//如果发送消息失败，设置重试次数，默认为2次

        try {
            producer.start();
            log.info("ProducerConfigure.getRocketMQProducer(): producer is start! groupName={}, nameServer={}", groupName, nameServer);
        } catch (MQClientException e) {
            log.error("ProducerConfigure.getRocketMQProducer(): {}", e.getErrorMessage(), e);
            throw e;
        }
        return producer;
    }

    @Bean(name = "transactionMQProducer")
    public TransactionMQProducer transactionMQProducer() throws MQClientException {
        if (StringUtils.isBlank(groupName)) {
            throw new IllegalArgumentException("groupName is blank");
        }

        if (StringUtils.isBlank(nameServer)) {
            throw new IllegalArgumentException("nameServerAddr is blank");
        }

        TransactionMQProducer producer = new TransactionMQProducer(groupName + "_transaction");
        producer.setNamesrvAddr(nameServer);

        //如果需要同一个jvm中不同的producer往不同的mq集群发送消息，需要设置不同的instanceName
        if (StringUtils.isNotBlank(instanceName)) {
            producer.setInstanceName(instanceName);
        }

        producer.setSendMsgTimeout(3000);//默认值 3s
        producer.setRetryTimesWhenSendFailed(1);//如果发送消息失败，设置重试次数，默认为2次

        try {
            producer.start();
            log.info("ProducerConfigure.getRocketMQProducer(): producer is start! groupName={}, nameServer={}", groupName, nameServer);
        } catch (MQClientException e) {
            log.error("ProducerConfigure.getRocketMQProducer(): {}", e.getErrorMessage(), e);
            throw e;
        }
        return producer;
    }
}