package com.example.chatapp.dto;

import lombok.Data;

@Data
public class AuthResponseDto {
  private String accessToken;
  private String refreshToken;
  private String message;

  public AuthResponseDto(String accessToken, String refreshToken, String message) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.message = message;
  }
}
