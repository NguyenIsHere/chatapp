package com.example.chatapp.controller;

import com.example.chatapp.model.Otp;
import com.example.chatapp.repository.OtpRepository;
import com.example.chatapp.service.AuthService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

  @Autowired
  private AuthService authService;

  @Autowired
  private OtpRepository otpRepository;

  // DTO cho request body của /request-otp
  @Data
  public static class RequestOtpRequest {
    private String phoneNumber;
  }

  // DTO cho request body của /verify-otp
  @Data
  public static class VerifyOtpRequest {
    private String phoneNumber;
    private String otp;
  }

  // DTO cho request body của /register
  @Data
  public static class RegisterRequest {
    private String phoneNumber;
    private String username;
  }

  @PostMapping("/request-otp")
  public ResponseEntity<String> requestOtp(@RequestBody RequestOtpRequest request) {
    try {
      // Firebase đã gửi OTP (123456 cho số test +84846920014)
      // Lưu OTP vào database để backend kiểm tra
      String normalizedPhoneNumber = authService.normalizePhoneNumber(request.getPhoneNumber());
      Otp otp = new Otp();
      otp.setPhoneNumber(normalizedPhoneNumber);
      otp.setOtp("123456"); // OTP cố định cho số test
      otpRepository.save(otp);
      return ResponseEntity.ok("OTP sent successfully");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to send OTP: " + e.getMessage());
    }
  }

  @PostMapping("/verify-otp")
  public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
    try {
      String result = authService.verifyOtp(request.getPhoneNumber(), request.getOtp());
      if ("NEW_USER".equals(result)) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body("New user, please register username");
      }
      return ResponseEntity.ok(result);
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("OTP verification failed: " + e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Internal server error: " + e.getMessage());
    }
  }

  @PostMapping("/register")
  public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
    try {
      // Kiểm tra dữ liệu đầu vào
      if (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("Phone number is required");
      }
      if (request.getUsername() == null || request.getUsername().isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("Username is required");
      }

      authService.registerUser(request.getPhoneNumber(), request.getUsername());
      return ResponseEntity.status(HttpStatus.CREATED)
          .body("User registered successfully");
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Registration failed: " + e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Internal server error: " + e.getMessage());
    }
  }
}