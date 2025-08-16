package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Clients subscribe to /topic/** (broadcast) and /queue/** (1:1 if needed)
        config.enableSimpleBroker("/topic", "/queue");
        // All client->server messages must be prefixed with /app (if you need any)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native WS: ws(s)://<host>/ws
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
        // SockJS fallback (optional)
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}