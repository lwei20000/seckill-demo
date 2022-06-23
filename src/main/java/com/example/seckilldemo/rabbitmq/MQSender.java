package com.example.seckilldemo.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息发送者
 *
 * @author: LC
 * @date 2022/3/7 7:42 下午
 * @ClassName: MQSender
 */
@Service
@Slf4j
public class MQSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送秒杀信息
     * @author LiChao
     * @operation add
     * @date 6:44 下午 2022/3/8
     * @param message
     * @return void
     **/
    public void sendSeckillMessage(String message) {
        log.info("发送消息" + message);
        rabbitTemplate.convertAndSend("seckillExchange", "seckill.message", message);
    }
}
