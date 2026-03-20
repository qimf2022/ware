package com.shiyu.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RabbitMQ 拓扑配置。
 */
@Data
@ConfigurationProperties(prefix = "app.rabbit")
public class AppRabbitMqProperties {

    /** 订单事件交换机（topic）。 */
    private String exchangeOrder = "ex.order";
    /** 支付回调交换机（direct）。 */
    private String exchangePay = "ex.pay";
    /** 通知交换机（topic）。 */
    private String exchangeNotify = "ex.notify";
    /** 死信交换机（topic）。 */
    private String exchangeDlx = "ex.dlx";

    /** 支付回调队列。 */
    private String queuePayCallback = "q.pay.callback";
    /** 订单超时延迟队列。 */
    private String queueOrderTimeoutDelay = "q.order.timeout.delay";
    /** 订单超时消费队列。 */
    private String queueOrderTimeoutConsume = "q.order.timeout.consume";
    /** 短信通知队列。 */
    private String queueNotifySms = "q.notify.sms";
    /** 微信通知队列。 */
    private String queueNotifyWx = "q.notify.wx";
    /** 售后退款补偿队列。 */
    private String queueAfterSaleRefundCompensate = "q.aftersale.refund.compensate";


    /** 支付死信队列。 */
    private String queueDlqPay = "q.dlq.pay";
    /** 订单死信队列。 */
    private String queueDlqOrder = "q.dlq.order";
    /** 通知死信队列。 */
    private String queueDlqNotify = "q.dlq.notify";

    /** 未支付订单超时毫秒数，默认 30 分钟。 */
    private Long orderTimeoutTtlMs = 1800000L;
}
