package com.example.chatapp.model;

import lombok.Data;

@Data
public class VerifyOtpRequest {
  private String phoneNumber;
  private String otp;
}
