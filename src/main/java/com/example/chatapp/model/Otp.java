package com.example.chatapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import java.time.LocalDateTime;

@Data
@Document(collection = "otps")
public class Otp {
  @Id
  private String id;

  @Indexed(unique = true)
  private String phoneNumber;

  private String otp;

  @Indexed
  private LocalDateTime createdAt = LocalDateTime.now();

  private LocalDateTime expiry = LocalDateTime.now().plusMinutes(5); // Hết hạn sau 5 phút
}