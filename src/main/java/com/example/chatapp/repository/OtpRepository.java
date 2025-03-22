package com.example.chatapp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.example.chatapp.model.Otp;
import java.util.Optional;

public interface OtpRepository extends MongoRepository<Otp, String> {
  Optional<Otp> findByPhoneNumber(String phoneNumber);
}
