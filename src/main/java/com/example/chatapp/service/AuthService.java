package com.example.chatapp.service;

import com.example.chatapp.model.Otp;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.OtpRepository;
import com.example.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

@Service
public class AuthService {

  @Autowired
  private OtpRepository otpRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtService jwtService;

  public String verifyOtp(String phoneNumber, String otp) {
    String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);

    // Kiểm tra OTP (Firebase đã xác minh, nhưng ta vẫn lưu OTP để kiểm tra logic)
    Optional<Otp> otpEntry = otpRepository.findByPhoneNumber(normalizedPhoneNumber);
    if (otpEntry.isEmpty() || !otpEntry.get().getOtp().equals(otp)
        || otpEntry.get().getExpiry().isBefore(LocalDateTime.now())) {
      throw new RuntimeException("Invalid or expired OTP");
    }

    otpRepository.delete(otpEntry.get()); // Xóa OTP sau khi dùng

    Optional<User> user = userRepository.findByPhoneNumber(normalizedPhoneNumber);
    if (user.isEmpty()) {
      return "NEW_USER";
    }

    String accessToken = jwtService.generateAccessToken(normalizedPhoneNumber);
    String refreshToken = jwtService.generateRefreshToken(normalizedPhoneNumber);

    user.get().setRefreshToken(refreshToken);
    userRepository.save(user.get());

    return accessToken + "," + refreshToken;
  }

  public void registerUser(String phoneNumber, String username) {
    String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);

    // Kiểm tra xem số điện thoại đã tồn tại chưa
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

  public String normalizePhoneNumber(String phoneNumber) {
    String cleanedNumber = phoneNumber.replaceAll("[^0-9+]", "");
    if (!cleanedNumber.startsWith("+")) {
      if (cleanedNumber.startsWith("0")) {
        cleanedNumber = "+84" + cleanedNumber.substring(1);
      } else {
        cleanedNumber = "+84" + cleanedNumber;
      }
    }
    return cleanedNumber;
  }
}