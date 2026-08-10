package com.pinnacle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Clients connect to /ws (SockJS fallback enabled for browsers/proxies that
 * block raw WebSocket), then subscribe to /topic/prices/{SYMBOL} for live
 * tick broadcasts. Server-originated broadcasts are published by
 * MarketDataIngestionService via SimpMessagingTemplate.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Same env var as SecurityConfig's CORS setup, kept in sync deliberately —
    // one place to update when the deployed frontend's domain changes.
    @Value("${pinnacle.cors.allowed-origins:http://localhost:*}")
    private String allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
