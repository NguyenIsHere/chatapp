package com.example.chatapp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.chatapp.model.User;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
  Optional<User> findByPhoneNumber(String phoneNumber);
}
