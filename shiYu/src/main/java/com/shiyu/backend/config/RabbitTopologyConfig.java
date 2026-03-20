package com.shiyu.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 交换机、队列与绑定拓扑定义。
 */
@Configuration
public class RabbitTopologyConfig {

    /**
     * 订单事件交换机。
     *
     * @param p RabbitMQ 配置属性
     * @return TopicExchange
     */
    @Bean
    public TopicExchange orderExchange(AppRabbitMqProperties p) {
        return new TopicExchange(p.getExchangeOrder(), true, false);
    }

    /**
     * 支付回调交换机。
     *
     * @param p RabbitMQ 配置属性
     * @return DirectExchange
     */
    @Bean
    public DirectExchange payExchange(AppRabbitMqProperties p) {
        return new DirectExchange(p.getExchangePay(), true, false);
    }

    /**
     * 通知交换机。
     *
     * @param p RabbitMQ 配置属性
     * @return TopicExchange
     */
    @Bean
    public TopicExchange notifyExchange(AppRabbitMqProperties p) {
        return new TopicExchange(p.getExchangeNotify(), true, false);
    }

    /**
     * 死信交换机。
     *
     * @param p RabbitMQ 配置属性
     * @return TopicExchange
     */
    @Bean
    public TopicExchange deadLetterExchange(AppRabbitMqProperties p) {
        return new TopicExchange(p.getExchangeDlx(), true, false);
    }

    /**
     * 支付回调队列。
     *
     * @param p RabbitMQ 配置属性
     * @return Queue
     */
    @Bean
    public Queue payCallbackQueue(AppRabbitMqProperties p) {
        return new Queue(p.getQueuePayCallback(), true, false, false, deadLetterArgs(p.getExchangeDlx(), "dlq.pay"));
    }

    /**
     * 订单超时延迟队列（TTL 后转发到订单交换机）。
     *
     * @param p RabbitMQ 配置属性
     * @return Queue
     */
    @Bean
    public Queue orderTimeoutDelayQueue(AppRabbitMqProperties p) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", p.getOrderTimeoutTtlMs());
        args.put("x-dead-letter-exchange", p.getExchangeOrder());
        args.put("x-dead-letter-routing-key", "order.timeout");
        return new Queue(p.getQueueOrderTimeoutDelay(), true, false, false, args);
    }

    /**
     * 订单超时消费队列。
     *
     * @param p RabbitMQ 配置属性
     * @return Queue
     */
    @Bean
    public Queue orderTimeoutConsumeQueue(AppRabbitMqProperties p) {
        return new Queue(p.getQueueOrderTimeoutConsume(), true, false, false, deadLetterArgs(p.getExchangeDlx(), "dlq.order"));
    }

    /**
     * 短信通知队列。
     *
     * @param p RabbitMQ 配置属性
     * @return Queue
     */
    @Bean
    public Queue notifySmsQueue(AppRabbitMqProperties p) {
        return new Queue(p.getQueueNotifySms(), true, false, false, deadLetterArgs(p.getExchangeDlx(), "dlq.notify"));
    }

    /**
     * 微信通知队列。
     *
     * @param p RabbitMQ 配置属性
     * @return Queue
     */
    @Bean
    public Queue notifyWxQueue(AppRabbitMqProperties p) {
        return new Queue(p.getQueueNotifyWx(), true, false, false, deadLetterArgs(p.getExchangeDlx(), "dlq.notify"));
    }

    /**
     * 售后退款补偿队列。
     *
     * @param p RabbitMQ 配置属性
     * @return Queue
     */
    @Bean
    public Queue afterSaleRefundCompensateQueue(AppRabbitMqProperties p) {
        return new Queue(p.getQueueAfterSaleRefundCompensate(), true, false, false, deadLetterArgs(p.getExchangeDlx(), "dlq.order"));
    }

    /**
     * 支付死信队列。
     *
     * @param p RabbitMQ 配置属性
     * @return Queue
     */
    @Bean
    public Queue payDlq(AppRabbitMqProperties p) {
        return new Queue(p.getQueueDlqPay(), true);
    }

    /**
     * 订单死信队列。
     *
     * @param p RabbitMQ 配置属性
     * @return Queue
     */
    @Bean
    public Queue orderDlq(AppRabbitMqProperties p) {
        return new Queue(p.getQueueDlqOrder(), true);
    }

    /**
     * 通知死信队列。
     *
     * @param p RabbitMQ 配置属性
     * @return Queue
     */
    @Bean
    public Queue notifyDlq(AppRabbitMqProperties p) {
        return new Queue(p.getQueueDlqNotify(), true);
    }

    /**
     * 绑定支付回调路由。
     *
     * @param p                RabbitMQ 配置属性
     * @param payCallbackQueue 支付回调队列
     * @param payExchange      支付交换机
     * @return Binding
     */
    @Bean
    public Binding payCallbackBinding(AppRabbitMqProperties p, Queue payCallbackQueue, DirectExchange payExchange) {
        return BindingBuilder.bind(payCallbackQueue).to(payExchange).with("pay.callback");
    }

    /**
     * 绑定订单超时路由。
     *
     * @param p                       RabbitMQ 配置属性
     * @param orderTimeoutConsumeQueue 订单超时消费队列
     * @param orderExchange           订单交换机
     * @return Binding
     */
    @Bean
    public Binding orderTimeoutConsumeBinding(AppRabbitMqProperties p, Queue orderTimeoutConsumeQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderTimeoutConsumeQueue).to(orderExchange).with("order.timeout");
    }

    /**
     * 绑定短信通知路由。
     *
     * @param p              RabbitMQ 配置属性
     * @param notifySmsQueue 短信通知队列
     * @param notifyExchange 通知交换机
     * @return Binding
     */
    @Bean
    public Binding notifySmsBinding(AppRabbitMqProperties p, Queue notifySmsQueue, TopicExchange notifyExchange) {
        return BindingBuilder.bind(notifySmsQueue).to(notifyExchange).with("notify.sms");
    }

    /**
     * 绑定微信通知路由。
     *
     * @param p             RabbitMQ 配置属性
     * @param notifyWxQueue 微信通知队列
     * @param notifyExchange 通知交换机
     * @return Binding
     */
    @Bean
    public Binding notifyWxBinding(AppRabbitMqProperties p, Queue notifyWxQueue, TopicExchange notifyExchange) {
        return BindingBuilder.bind(notifyWxQueue).to(notifyExchange).with("notify.wx");
    }

    /**
     * 绑定售后退款补偿路由。
     *
     * @param p                          RabbitMQ 配置属性
     * @param afterSaleRefundCompensateQueue 售后退款补偿队列
     * @param orderExchange              订单交换机
     * @return Binding
     */
    @Bean
    public Binding afterSaleRefundCompensateBinding(AppRabbitMqProperties p,
                                                    Queue afterSaleRefundCompensateQueue,
                                                    TopicExchange orderExchange) {
        return BindingBuilder.bind(afterSaleRefundCompensateQueue).to(orderExchange).with("aftersale.refund.compensate");
    }

    /**
     * 绑定支付死信路由。
     *
     * @param p                 RabbitMQ 配置属性
     * @param payDlq            支付死信队列
     * @param deadLetterExchange 死信交换机
     * @return Binding
     */
    @Bean
    public Binding payDlqBinding(AppRabbitMqProperties p, Queue payDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(payDlq).to(deadLetterExchange).with("dlq.pay");
    }

    /**
     * 绑定订单死信路由。
     *
     * @param p                  RabbitMQ 配置属性
     * @param orderDlq           订单死信队列
     * @param deadLetterExchange 死信交换机
     * @return Binding
     */
    @Bean
    public Binding orderDlqBinding(AppRabbitMqProperties p, Queue orderDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(orderDlq).to(deadLetterExchange).with("dlq.order");
    }

    /**
     * 绑定通知死信路由。
     *
     * @param p                  RabbitMQ 配置属性
     * @param notifyDlq          通知死信队列
     * @param deadLetterExchange 死信交换机
     * @return Binding
     */
    @Bean
    public Binding notifyDlqBinding(AppRabbitMqProperties p, Queue notifyDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(notifyDlq).to(deadLetterExchange).with("dlq.notify");
    }

    /**
     * 生成队列死信参数。
     *
     * @param dlxExchange    死信交换机名
     * @param dlxRoutingKey 死信路由键
     * @return 死信参数 Map
     */
    private Map<String, Object> deadLetterArgs(String dlxExchange, String dlxRoutingKey) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", dlxExchange);
        args.put("x-dead-letter-routing-key", dlxRoutingKey);
        return args;
    }
}
