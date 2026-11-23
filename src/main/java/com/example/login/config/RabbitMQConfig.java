package com.example.login.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "sns.events";

    public static final String ROUTING_KEY_USER_DELETED = "user.deleted";
    // 추가할 상수
    public static final String USER_DELETED_QUEUE = "user.deleted.queue";
    // 반송하는 곳
    public static final String USER_DELETED_DLQ = "user.deleted.queue.dlq";
    // Excharge 빈. C# 부분과 완벽히 같아야함
    @Bean
    public TopicExchange snsEventExcharge(){
        // C#쪽 초안보고 짜는중인데 이것과 동일해야함
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    // JSON 메시지 변환. 이게 없으면 Rabbit 템플릿이 자바 직렬화로 객체를 보내기에 필수
    @Bean
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper){
        return new JacksonJsonMessageConverter(jsonMapper);
    }
    // 지워진 큐
    @Bean
    public Queue userDeletedQueue() {
        // durable = true (브로커 재시작 시에도 큐 유지)
        return QueueBuilder.durable(USER_DELETED_QUEUE)
                .withArgument("x-dead-letter-exchange", "") // 기본 익스체인지 사용
                .withArgument("x-dead-letter-routing-key", USER_DELETED_DLQ)
                .build();
    }
    @Bean
    public Queue userDeletedDlq() {
        return QueueBuilder.durable(USER_DELETED_DLQ).build();
    }
    @Bean
    public Binding userDeletedBinding(
            Queue userDeletedQueue,
            TopicExchange exchange // 기존에 선언된 Exchange 빈 이름/타입에 맞게 주입
    ) {
        return BindingBuilder
                .bind(userDeletedQueue)
                .to(exchange)
                .with(ROUTING_KEY_USER_DELETED);
    }
}
