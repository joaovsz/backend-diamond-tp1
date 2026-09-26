package com.diamond.clients.infrastructure.messaging;

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
 * Configuração do RabbitMQ para o Client Service.
 *
 * Este serviço consome comandos da fila client.resolve.queue e publica
 * eventos no exchange client.events.
 */
@Configuration
public class RabbitMQConfig {

    public static final String LEAD_COMMANDS_EXCHANGE = "lead.commands";
    public static final String CLIENT_EVENTS_EXCHANGE = "client.events";

    public static final String CLIENT_RESOLVE_QUEUE = "client.resolve.queue";

    public static final String RK_CLIENT_RESOLVE = "lead.client.resolve";
    public static final String RK_CLIENT_RESOLVED = "client.resolved";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange leadCommandsExchange() {
        return new DirectExchange(LEAD_COMMANDS_EXCHANGE);
    }

    @Bean
    public TopicExchange clientEventsExchange() {
        return new TopicExchange(CLIENT_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue clientResolveQueue() {
        return QueueBuilder.durable(CLIENT_RESOLVE_QUEUE).build();
    }

    @Bean
    public Binding clientResolveBinding(Queue clientResolveQueue, DirectExchange leadCommandsExchange) {
        return BindingBuilder.bind(clientResolveQueue).to(leadCommandsExchange).with(RK_CLIENT_RESOLVE);
    }
}
