// src/main/java/com/example/chatapp/config/WebSocketConfig.java
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
    private String instanceId; // ID của instance hiện tại

    @Autowired
    private AuthChannelInterceptor authChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:5174", "http://localhost",
                        "http://localhost:3000", "http://localhost:8088") // Thêm các origin cần thiết
                .setAllowedOriginPatterns("*") // Cân nhắc bỏ nếu đã có setAllowedOrigins cụ thể
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        log.info("Registering AuthChannelInterceptor for client inbound channel...");
        registration.interceptors(authChannelInterceptor);
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal principal = accessor.getUser();

        log.info(">>> handleWebSocketConnectListener INVOKED. Session ID: {}, Instance ID: {}", sessionId, instanceId);

        if (principal != null && principal.getName() != null && sessionId != null) {
            String phoneNumber = principal.getName();
            log.info("User {} (Principal: {}) connected via WebSocket session {} on instance: {}",
                    phoneNumber, principal, sessionId, instanceId);
            presenceService.addSession(phoneNumber, instanceId, sessionId);
        } else {
            log.warn(
                    "WebSocket connected (Session ID: {}) BUT Principal is NULL or name/sessionId is NULL. User will NOT be marked online by this event.",
                    sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId(); // Luôn lấy sessionId
        Principal principal = accessor.getUser(); // Principal có thể null nếu disconnect đột ngột trước khi auth

        log.info(">>> handleWebSocketDisconnectListener INVOKED. Session ID: {}, Instance ID: {}", sessionId,
                instanceId);

        if (principal != null && principal.getName() != null && sessionId != null) {
            String phoneNumber = principal.getName();
            log.info("User {} (Principal: {}) disconnected via WebSocket session {} from instance: {}",
                    phoneNumber, principal, sessionId, instanceId);
            presenceService.removeSession(phoneNumber, instanceId, sessionId);
        } else if (sessionId != null) {
            // Trường hợp không có principal (ví dụ client đóng tab trước khi STOMP CONNECT
            // hoàn tất với auth)
            // Chúng ta có thể không biết phoneNumber để xóa khỏi Set
            // user_sessions:{phoneNumber} một cách dễ dàng
            // Nếu bạn có một map sessionID -> phoneNumber (lưu khi CONNECT), bạn có thể
            // dùng nó ở đây
            // Hiện tại, chỉ log. Việc dọn dẹp session mồ côi có thể cần cơ chế khác.
            log.warn(
                    "WebSocket session {} disconnected (possibly abruptly) BUT Principal is NULL or name is NULL. Instance ID: {}",
                    sessionId, instanceId);
        } else {
            log.warn("WebSocket disconnected event received with NULL sessionId. Instance ID: {}", instanceId);
        }
    }
}