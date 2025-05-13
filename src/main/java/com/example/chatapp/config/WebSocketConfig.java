package com.example.chatapp.config;

import com.example.chatapp.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import java.security.Principal;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    @Autowired
    private PresenceService presenceService;

    @Value("${instance.id}")
    private String instanceId;

    @Autowired // Inject AuthChannelInterceptor
    private AuthChannelInterceptor authChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:5174", "http://localhost",
                        "http://localhost:3000") // Thêm port React Dev Server
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    // Override phương thức này để đăng ký interceptor cho channel đầu vào của
    // client
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        log.info("Registering AuthChannelInterceptor for client inbound channel...");
        registration.interceptors(authChannelInterceptor);
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        log.info(">>> handleWebSocketConnectListener INVOKED. Session ID: {}", event.getMessage().getHeaders().getId());
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser(); // Bây giờ principal nên có giá trị nếu AuthChannelInterceptor hoạt
                                                  // động

        if (principal != null && principal.getName() != null) {
            String phoneNumber = principal.getName();
            log.info("User {} connected via WebSocket on instance: {}. Principal: {}", phoneNumber, instanceId,
                    principal);
            presenceService.setUserOnline(phoneNumber, instanceId);
        } else {
            log.warn(
                    "WebSocket connected BUT Principal is NULL or name is NULL. Session ID: {}. User will NOT be marked online.",
                    accessor.getSessionId());
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        log.info(">>> handleWebSocketDisconnectListener INVOKED. Session ID: {}",
                event.getMessage().getHeaders().getId());
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();

        if (principal != null && principal.getName() != null) {
            String phoneNumber = principal.getName();
            log.info("User {} disconnected via WebSocket from instance: {}. Principal: {}", phoneNumber, instanceId,
                    principal);
            presenceService.setUserOffline(phoneNumber);
        } else {
            log.warn("WebSocket disconnected (possibly abruptly) BUT Principal is NULL or name is NULL. Session ID: {}",
                    accessor.getSessionId());
        }
    }
}