package com.example.chatapp.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {

  private final RedisTemplate<String, String> redisTemplate;
  private static final String ONLINE_USERS_KEY = "ONLINE_USERS"; // nếu muốn xài set

  public PresenceService(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void setUserOnline(String userId) {
    redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId);
  }

  public void setUserOffline(String userId) {
    redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
  }

  public boolean isUserOnline(String userId) {
    return redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId);
  }
}
