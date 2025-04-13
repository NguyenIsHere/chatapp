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

  /**
   * Đánh dấu user online:
   * Cách 1: Dùng setONLINE_USERS.
   */
  public void setUserOnline(String userId) {
    redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId);
  }

  /**
   * Đánh dấu user offline:
   */
  public void setUserOffline(String userId) {
    redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
  }

  /**
   * Kiểm tra user có trong set ONLINE_USERS không?
   */
  public boolean isUserOnline(String userId) {
    return redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId);
  }

  /**
   * Cách 2 (Tuỳ chọn): Dùng Key-Value kiểu "ONLINE:userId" -> "true"
   * => Triển khai setUserOnlineKV, setUserOfflineKV
   */
}
