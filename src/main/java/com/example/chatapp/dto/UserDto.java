package com.example.chatapp.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
  private String id; // MongoDB ID
  private String phoneNumber;
  private String username;
  private boolean online;
  // private String instanceId; // Tùy chọn: có thể thêm nếu FE cần biết user đang
  // ở instance nào
}