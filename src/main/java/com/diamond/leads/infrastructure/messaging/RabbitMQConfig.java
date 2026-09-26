package com.diamond.leads.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração central do RabbitMQ para o Lead Service.
 *
 * Topologia:
 * - lead.commands (Direct Exchange)  → client.resolve.queue
 * - lead.events   (Topic Exchange)   → lead.notifications.queue (bound a lead.#)
 * - client.events (Topic Exchange)   → lead.client.resolved.queue
 */
@Configuration
public class RabbitMQConfig {

    // === Exchanges ===
    public static final String LEAD_COMMANDS_EXCHANGE = "lead.commands";
    public static final String LEAD_EVENTS_EXCHANGE = "lead.events";
    public static final String CLIENT_EVENTS_EXCHANGE = "client.events";

    // === Queues ===
    public static final String CLIENT_RESOLVE_QUEUE = "client.resolve.queue";
    public static final String LEAD_NOTIFICATIONS_QUEUE = "lead.notifications.queue";
    public static final String LEAD_CLIENT_RESOLVED_QUEUE = "lead.client.resolved.queue";

    // === Routing Keys ===
    public static final String RK_CLIENT_RESOLVE = "lead.client.resolve";
    public static final String RK_STATUS_CHANGED = "lead.status-changed";
    public static final String RK_ASSIGNED = "lead.assigned";
    public static final String RK_CLIENT_RESOLVED = "client.resolved";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // --- Exchanges ---

    @Bean
    public DirectExchange leadCommandsExchange() {
        return new DirectExchange(LEAD_COMMANDS_EXCHANGE);
    }

    @Bean
    public TopicExchange leadEventsExchange() {
        return new TopicExchange(LEAD_EVENTS_EXCHANGE);
    }

    @Bean
    public TopicExchange clientEventsExchange() {
        return new TopicExchange(CLIENT_EVENTS_EXCHANGE);
    }

    // --- Queues ---

    @Bean
    public Queue clientResolveQueue() {
        return QueueBuilder.durable(CLIENT_RESOLVE_QUEUE).build();
    }

    @Bean
    public Queue leadNotificationsQueue() {
        return QueueBuilder.durable(LEAD_NOTIFICATIONS_QUEUE).build();
    }

    @Bean
    public Queue leadClientResolvedQueue() {
        return QueueBuilder.durable(LEAD_CLIENT_RESOLVED_QUEUE).build();
    }

    // --- Bindings ---

    @Bean
    public Binding clientResolveBinding(Queue clientResolveQueue, DirectExchange leadCommandsExchange) {
        return BindingBuilder.bind(clientResolveQueue).to(leadCommandsExchange).with(RK_CLIENT_RESOLVE);
    }

    @Bean
    public Binding leadNotificationsBinding(Queue leadNotificationsQueue, TopicExchange leadEventsExchange) {
        return BindingBuilder.bind(leadNotificationsQueue).to(leadEventsExchange).with("lead.#");
    }

    @Bean
    public Binding leadClientResolvedBinding(Queue leadClientResolvedQueue, TopicExchange clientEventsExchange) {
        return BindingBuilder.bind(leadClientResolvedQueue).to(clientEventsExchange).with(RK_CLIENT_RESOLVED);
    }
}
