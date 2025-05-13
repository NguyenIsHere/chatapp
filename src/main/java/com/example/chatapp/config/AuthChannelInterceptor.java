package com.example.chatapp.config; // Hoặc com.example.chatapp.interceptor

import com.example.chatapp.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.context.SecurityContextHolder; // Không cần thiết ở đây
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Objects;

@Component // Đánh dấu là một Spring Bean để có thể inject vào WebSocketConfig
public class AuthChannelInterceptor implements ChannelInterceptor {

  private static final Logger log = LoggerFactory.getLogger(AuthChannelInterceptor.class);

  @Autowired // Inject JwtService của bạn
  private JwtService jwtService;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    // Lấy StompHeaderAccessor từ message để dễ dàng truy cập các header của STOMP
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    // Chỉ xử lý khi command là CONNECT (lúc client cố gắng thiết lập kết nối STOMP)
    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
      log.info("AuthChannelInterceptor: Intercepting STOMP CONNECT command. Session ID: {}", accessor.getSessionId());

      // Lấy header "Authorization" từ native headers của STOMP message
      // Client React của bạn đã được cấu hình để gửi header này
      List<String> authorizationHeaders = accessor.getNativeHeader("Authorization");
      log.debug("AuthChannelInterceptor: Native 'Authorization' header: {}", authorizationHeaders);

      if (authorizationHeaders != null && !authorizationHeaders.isEmpty()) {
        String authHeaderValue = authorizationHeaders.get(0); // Lấy giá trị đầu tiên nếu có nhiều

        if (authHeaderValue != null && authHeaderValue.toLowerCase().startsWith("bearer ")) {
          String accessToken = authHeaderValue.substring(7); // Bỏ "Bearer "
          log.debug("AuthChannelInterceptor: Extracted Access Token: {}", accessToken);

          if (jwtService.validateToken(accessToken)) {
            String phoneNumber = jwtService.getPhoneNumberFromToken(accessToken);
            if (phoneNumber != null) {
              // Nếu token hợp lệ và lấy được phoneNumber,
              // tạo một đối tượng Authentication (ví dụ: UsernamePasswordAuthenticationToken)
              // và set nó vào user của StompHeaderAccessor.
              // Điều này sẽ làm cho principal.getName() trong các event listener (như
              // SessionConnectEvent)
              // trả về phoneNumber này.
              UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                  phoneNumber, // principal (sẽ là principal.getName())
                  null, // credentials (không cần cho JWT đã xác thực)
                  null // authorities (để null nếu không dùng role-based)
              );
              accessor.setUser(authentication); // QUAN TRỌNG: Set user cho session STOMP này
              log.info("AuthChannelInterceptor: STOMP User Principal set for phone number: {}", phoneNumber);
            } else {
              log.warn("AuthChannelInterceptor: JWT valid but no phone number found in token.");
              // Có thể từ chối kết nối ở đây nếu yêu cầu phải có phoneNumber
            }
          } else {
            log.warn("AuthChannelInterceptor: Invalid JWT token received in STOMP CONNECT header.");
            // Từ chối kết nối STOMP nếu token không hợp lệ
            // throw new SecurityException("Invalid STOMP token"); // Hoặc một exception phù
            // hợp hơn
          }
        } else {
          log.warn("AuthChannelInterceptor: 'Authorization' header found, but not a Bearer token: {}", authHeaderValue);
        }
      } else {
        log.warn("AuthChannelInterceptor: No 'Authorization' header found in STOMP CONNECT.");
        // Nếu không có token, user sẽ là anonymous (accessor.getUser() sẽ null)
        // Bạn có thể quyết định từ chối kết nối ở đây nếu ứng dụng yêu cầu tất cả kết
        // nối WebSocket phải được xác thực.
      }
    }
    // Cho phép message đi tiếp trong channel
    return message;
  }

  // Bạn có thể override các phương thức khác của ChannelInterceptor nếu cần
  // (postSend, afterSendCompletion, etc.)
  // nhưng preSend là quan trọng nhất cho việc xác thực khi kết nối.
}