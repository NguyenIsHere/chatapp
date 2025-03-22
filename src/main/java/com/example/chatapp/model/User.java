package com.example.chatapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import java.util.Date;

@Data
@Document(collection = "users")
public class User {
  @Id
  private String id;

  @Indexed(unique = true)
  private String phoneNumber;

  private String username;
  private String refreshToken;
  private Date createdAt;
}
