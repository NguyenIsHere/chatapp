package com.example.chatapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessageRequestDto {
  private String recipientPhoneNumber; // SĐT của người nhận
  private String content; // Nội dung tin nhắn
}