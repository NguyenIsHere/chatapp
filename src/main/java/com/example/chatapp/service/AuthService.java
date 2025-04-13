package com.example.chatapp.service;

import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.util.PhoneNumberUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
  private PresenceService presenceService;

  public String[] login(String phoneNumber) {
    String normalizedPhoneNumber = PhoneNumberUtil.normalizePhoneNumber(phoneNumber);

    // Kiểm tra user có tồn tại hay không
    Optional<User> userOptional = userRepository.findByPhoneNumber(normalizedPhoneNumber);
    if (userOptional.isEmpty()) {
      throw new RuntimeException("NEW_USER");
    }

    User user = userOptional.get();
    String refreshToken = user.getRefreshToken();
    String accessToken;

    // Kiểm tra refreshToken còn khả dụng không
    if (refreshToken != null && jwtService.validateToken(refreshToken)) {
      // Refresh token còn khả dụng, tạo access token mới
      accessToken = jwtService.generateAccessToken(normalizedPhoneNumber);
    } else {
      // Refresh token không khả dụng, tạo mới cả access và refresh token
      accessToken = jwtService.generateAccessToken(normalizedPhoneNumber);
      refreshToken = jwtService.generateRefreshToken(normalizedPhoneNumber);
      user.setRefreshToken(refreshToken);
      userRepository.save(user);
    }

    // Đánh dấu user online trong Redis
    log.info("Setting user online with ID = {}", user.getId());
    presenceService.setUserOnline(user.getId());

    return new String[] { accessToken, refreshToken };
  }

  public void registerUser(String phoneNumber, String username) {
    String normalizedPhoneNumber = PhoneNumberUtil.normalizePhoneNumber(phoneNumber);

    Optional<User> existingUser = userRepository.findByPhoneNumber(normalizedPhoneNumber);
    if (existingUser.isPresent()) {
      throw new RuntimeException("Phone number already registered");
    }

    User user = new User();
    user.setPhoneNumber(normalizedPhoneNumber);
    user.setUsername(username);
    user.setCreatedAt(new Date());
    user.setRefreshToken(jwtService.generateRefreshToken(normalizedPhoneNumber));
    userRepository.save(user);
  }

  // Hàm logout
  public void logout(String userId) {
    // Đánh dấu user offline
    presenceService.setUserOffline(userId);

    // (Tuỳ chọn) cập nhật lastSeenAt
    User user = userRepository.findById(userId).orElse(null);
    if (user != null) {
      // user.setLastSeenAt(new Date());
      userRepository.save(user);
    }
  }
}