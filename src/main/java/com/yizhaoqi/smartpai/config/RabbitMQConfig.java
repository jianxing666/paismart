package com.yizhaoqi.smartpai.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange.file-processing}")
    private String fileProcessingExchangeName;

    @Value("${app.rabbitmq.queue.file-processing}")
    private String fileProcessingQueueName;

    @Value("${app.rabbitmq.exchange.file-processing-dlx}")
    private String fileProcessingDlxExchangeName;

    @Value("${app.rabbitmq.queue.file-processing-dlq}")
    private String fileProcessingDlqName;

    @Value("${app.rabbitmq.routing-key.file-processing}")
    private String fileProcessingRoutingKey;

    // ===== Getters for SpEL in @RabbitListener and producer usage =====
    public String getFileProcessingQueue() {
        return fileProcessingQueueName;
    }

    public String getFileProcessingExchange() {
        return fileProcessingExchangeName;
    }

    public String getFileProcessingRoutingKey() {
        return fileProcessingRoutingKey;
    }

    // ===== Exchanges =====
    @Bean
    public DirectExchange fileProcessingExchange() {
        return new DirectExchange(fileProcessingExchangeName);
    }

    @Bean
    public DirectExchange fileProcessingDlxExchange() {
        return new DirectExchange(fileProcessingDlxExchangeName);
    }

    // ===== Queues =====
    @Bean
    public Queue fileProcessingQueue() {
        return QueueBuilder.durable(fileProcessingQueueName)
                .withArgument("x-dead-letter-exchange", fileProcessingDlxExchangeName)
                .withArgument("x-dead-letter-routing-key", fileProcessingRoutingKey)
                .build();
    }

    @Bean
    public Queue fileProcessingDlq() {
        return QueueBuilder.durable(fileProcessingDlqName).build();
    }

    // ===== Bindings =====
    @Bean
    public Binding fileProcessingBinding() {
        return BindingBuilder.bind(fileProcessingQueue())
                .to(fileProcessingExchange())
                .with(fileProcessingRoutingKey);
    }

    @Bean
    public Binding fileProcessingDlqBinding() {
        return BindingBuilder.bind(fileProcessingDlq())
                .to(fileProcessingDlxExchange())
                .with(fileProcessingRoutingKey);
    }

    // ===== Message Converter =====
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ===== RabbitTemplate =====
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    // ===== Retry Interceptor（匹配原 Kafka 行为：固定 3s 退避，最多 5 次尝试） =====
    @Bean
    public RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(5)                               // 1 次初始 + 4 次重试
                .backOffOptions(3000L, 1.0, 3000L)            // 固定 3 秒退避
                .recoverer(new RejectAndDontRequeueRecoverer()) // 重试耗尽后进入 DLX → DLQ
                .build();
    }

    // ===== Listener Container Factory =====
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter,
            RetryOperationsInterceptor retryInterceptor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAdviceChain(retryInterceptor);
        return factory;
    }
}
