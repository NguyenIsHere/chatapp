package com.example.chatapp.service;

import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.util.PhoneNumberUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // Cần cho instanceId nếu muốn log
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtService jwtService;

  @Autowired
  private PresenceService presenceService; // Vẫn inject để dùng cho logout

  // instance.id sẽ được inject từ application.properties (được set bởi
  // docker-compose)
  // Chúng ta không dùng nó để set online ở đây nữa, nhưng có thể giữ lại để log
  // nếu cần.
  // @Value("${instance.id}")
  // private String instanceId;

  public String[] login(String phoneNumber) {
    String normalizedPhoneNumber = PhoneNumberUtil.normalizePhoneNumber(phoneNumber);

    Optional<User> userOptional = userRepository.findByPhoneNumber(normalizedPhoneNumber);
    if (userOptional.isEmpty()) {
      // Client sẽ dựa vào HTTP status code (ví dụ 401 hoặc một code tùy chỉnh)
      // và message "NEW_USER" để biết cần chuyển sang màn hình đăng ký username.
      throw new RuntimeException("NEW_USER");
    }

    User user = userOptional.get();
    String currentRefreshToken = user.getRefreshToken();
    String newAccessToken;
    String newRefreshToken = currentRefreshToken; // Giữ lại refresh token cũ nếu còn hạn

    // Kiểm tra refreshToken còn khả dụng không
    if (currentRefreshToken != null && jwtService.validateToken(currentRefreshToken)) {
      // Refresh token còn khả dụng, tạo access token mới
      newAccessToken = jwtService.generateAccessToken(normalizedPhoneNumber);
      log.info("User {} logged in. Reused existing valid refresh token.", normalizedPhoneNumber);
    } else {
      // Refresh token không khả dụng (null, hết hạn, hoặc không hợp lệ),
      // tạo mới cả access và refresh token
      log.info("User {} logged in. Generating new access and refresh tokens.", normalizedPhoneNumber);
      newAccessToken = jwtService.generateAccessToken(normalizedPhoneNumber);
      newRefreshToken = jwtService.generateRefreshToken(normalizedPhoneNumber);
      user.setRefreshToken(newRefreshToken);
      userRepository.save(user);
    }

    // QUAN TRỌNG: KHÔNG gọi presenceService.setUserOnline() ở đây.
    // Việc này sẽ được xử lý bởi WebSocketConfig khi có SessionConnectEvent.
    // Lý do: User có thể login thành công qua HTTP nhưng chưa/không kết nối
    // WebSocket.
    // Trạng thái "online" thực sự chỉ nên được tính khi có kết nối WebSocket chủ
    // động.
    // log.info("User {} ({}) logged in successfully via HTTP. WebSocket connection
    // will handle online status.",
    // user.getUsername(), normalizedPhoneNumber);

    return new String[] { newAccessToken, newRefreshToken };
  }

  public User registerUser(String phoneNumber, String username) { // Trả về User để AuthController có thể login ngay
    String normalizedPhoneNumber = PhoneNumberUtil.normalizePhoneNumber(phoneNumber);

    Optional<User> existingUser = userRepository.findByPhoneNumber(normalizedPhoneNumber);
    if (existingUser.isPresent()) {
      throw new RuntimeException("Phone number already registered");
    }

    // Kiểm tra username đã tồn tại chưa (tùy chọn, nếu muốn username là unique)
    // Optional<User> existingUsername = userRepository.findByUsername(username);
    // if (existingUsername.isPresent()) {
    // throw new RuntimeException("Username already taken");
    // }

    User newUser = new User();
    newUser.setPhoneNumber(normalizedPhoneNumber);
    newUser.setUsername(username);
    newUser.setCreatedAt(new Date());
    // Không cần tạo refresh token ở đây nữa, vì sau khi register, client sẽ gọi
    // login.
    // Hoặc nếu muốn login ngay sau register thì bước login sẽ tạo refresh token.
    // newUser.setRefreshToken(jwtService.generateRefreshToken(normalizedPhoneNumber));

    User savedUser = userRepository.save(newUser);
    log.info("User registered: {} with phone {}", savedUser.getUsername(), savedUser.getPhoneNumber());
    return savedUser;
  }

  /**
   * Xử lý logout cho user.
   * Sẽ được gọi từ AuthController khi nhận request logout.
   * 
   * @param phoneNumber Số điện thoại của user (lấy từ Principal của
   *                    SecurityContext)
   */
  public void logout(String phoneNumber) {
    // Vì PresenceService dùng phoneNumber làm key, chúng ta truyền phoneNumber vào.
    // WebSocketConfig khi disconnect cũng sẽ dùng phoneNumber từ Principal để gọi
    // setUserOffline.
    // Việc gọi ở đây đảm bảo user offline ngay cả khi họ chỉ logout qua HTTP mà
    // không đóng tab (WebSocket chưa disconnect ngay).
    presenceService.setUserOffline(phoneNumber);
    log.info("User with phone {} logged out. Marked as offline in PresenceService.", phoneNumber);

    // Tùy chọn: Xóa refresh token khỏi DB để vô hiệu hóa hoàn toàn session từ phía
    // server
    // Điều này sẽ buộc user phải login lại hoàn toàn ở lần sau.
    User user = userRepository.findByPhoneNumber(phoneNumber).orElse(null);
    if (user != null) {
      user.setRefreshToken(null); // Xóa refresh token
      userRepository.save(user);
      log.info("Refresh token for user with phone {} has been invalidated.", phoneNumber);
    }
  }
}