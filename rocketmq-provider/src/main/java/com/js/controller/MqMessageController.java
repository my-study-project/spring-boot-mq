package com.js.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mqMessageController")
@Slf4j
public class MqMessageController {
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @GetMapping("/pushMessage")
    public String get(@RequestParam("id") int id) {
        rocketMQTemplate.convertAndSend("first-topic", "你好,Java旅途" + id);
        log.info("推送消息完成");
        return "SUCCESS";
    }

}