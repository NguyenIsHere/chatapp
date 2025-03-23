package com.example.chatapp.controller;

import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.AuthService;
import com.example.chatapp.service.JwtService;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

  @Autowired
  private AuthService authService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtService jwtService;

  @Data
  public static class LoginRequest {
    private String phoneNumber;
  }

  @Data
  public static class RegisterRequest {
    private String phoneNumber;
    private String username;
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    try {
      String[] tokens = authService.login(request.getPhoneNumber());
      Map<String, String> response = new HashMap<>();
      response.put("accessToken", tokens[0]);
      response.put("refreshToken", tokens[1]);
      return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
      if ("NEW_USER".equals(e.getMessage())) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body("New user, please register username");
      }
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Login failed: " + e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Internal server error: " + e.getMessage());
    }
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    try {
      if (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("Phone number is required");
      }
      if (request.getUsername() == null || request.getUsername().isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("Username is required");
      }

      // Đăng ký user
      authService.registerUser(request.getPhoneNumber(), request.getUsername());

      // Tạo token ngay sau khi đăng ký
      String[] tokens = authService.login(request.getPhoneNumber());
      Map<String, String> response = new HashMap<>();
      response.put("accessToken", tokens[0]);
      response.put("refreshToken", tokens[1]);
      response.put("message", "User registered and logged in successfully");
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Registration failed: " + e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Internal server error: " + e.getMessage());
    }
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
    String refreshToken = request.get("refreshToken");
    try {
      if (refreshToken == null || !jwtService.validateToken(refreshToken)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body("Invalid or expired refresh token");
      }

      String phoneNumber = jwtService.getPhoneNumberFromToken(refreshToken);
      Optional<User> userOptional = userRepository.findByPhoneNumber(phoneNumber);
      if (userOptional.isEmpty() || !refreshToken.equals(userOptional.get().getRefreshToken())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body("Invalid refresh token");
      }

      String newAccessToken = jwtService.generateAccessToken(phoneNumber);
      Map<String, String> response = new HashMap<>();
      response.put("accessToken", newAccessToken);
      response.put("refreshToken", refreshToken);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Internal server error: " + e.getMessage());
    }
  }

  @GetMapping("/some-protected-endpoint")
  public ResponseEntity<?> someProtectedEndpoint() {
    try {
      // Lấy phoneNumber từ SecurityContext
      String phoneNumber = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

      // Tìm user trong database
      Optional<User> userOptional = userRepository.findByPhoneNumber(phoneNumber);
      if (userOptional.isEmpty()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body("User not found");
      }

      // Trả về thông tin user
      User user = userOptional.get();
      Map<String, String> response = new HashMap<>();
      response.put("phoneNumber", user.getPhoneNumber());
      response.put("username", user.getUsername());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Internal server error: " + e.getMessage());
    }
  }
}