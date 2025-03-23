package com.example.chatapp.controller;

import com.example.chatapp.dto.*;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.AuthService;
import com.example.chatapp.service.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequestDto request) {
    try {
      String[] tokens = authService.login(request.getPhoneNumber());
      return ResponseEntity.ok(new AuthResponseDto(tokens[0], tokens[1], "Login successful"));
    } catch (RuntimeException e) {
      if ("NEW_USER".equals(e.getMessage())) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(new AuthResponseDto(null, null, "New user, please register username"));
      }
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new AuthResponseDto(null, null, "Login failed: " + e.getMessage()));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new AuthResponseDto(null, null, "Internal server error: " + e.getMessage()));
    }
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegisterRequestDto request) {
    try {
      if (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new AuthResponseDto(null, null, "Phone number is required"));
      }
      if (request.getUsername() == null || request.getUsername().isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new AuthResponseDto(null, null, "Username is required"));
      }

      authService.registerUser(request.getPhoneNumber(), request.getUsername());

      String[] tokens = authService.login(request.getPhoneNumber());
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(new AuthResponseDto(tokens[0], tokens[1], "User registered and logged in successfully"));
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new AuthResponseDto(null, null, "Registration failed: " + e.getMessage()));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new AuthResponseDto(null, null, "Internal server error: " + e.getMessage()));
    }
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequestDto request) {
    String refreshToken = request.getRefreshToken();
    try {
      if (refreshToken == null || !jwtService.validateToken(refreshToken)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new AuthResponseDto(null, null, "Invalid or expired refresh token"));
      }

      String phoneNumber = jwtService.getPhoneNumberFromToken(refreshToken);
      Optional<User> userOptional = userRepository.findByPhoneNumber(phoneNumber);
      if (userOptional.isEmpty() || !refreshToken.equals(userOptional.get().getRefreshToken())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new AuthResponseDto(null, null, "Invalid refresh token"));
      }

      String newAccessToken = jwtService.generateAccessToken(phoneNumber);
      return ResponseEntity.ok(new AuthResponseDto(newAccessToken, refreshToken, "Token refreshed successfully"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new AuthResponseDto(null, null, "Internal server error: " + e.getMessage()));
    }
  }

  @GetMapping("/some-protected-endpoint")
  public ResponseEntity<?> someProtectedEndpoint() {
    try {
      String phoneNumber = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

      Optional<User> userOptional = userRepository.findByPhoneNumber(phoneNumber);
      if (userOptional.isEmpty()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new AuthResponseDto(null, null, "User not found"));
      }

      User user = userOptional.get();
      return ResponseEntity.ok(new AuthResponseDto(null, null, "User found: " + user.getUsername()));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new AuthResponseDto(null, null, "Internal server error: " + e.getMessage()));
    }
  }
}
