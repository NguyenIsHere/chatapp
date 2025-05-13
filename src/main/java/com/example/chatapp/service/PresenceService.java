package com.example.chatapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.Collections; // Cho Set rỗng an toàn

@Service
public class PresenceService {

  private static final Logger log = LoggerFactory.getLogger(PresenceService.class);
  private final RedisTemplate<String, String> redisTemplate;

  // Key prefix trong Redis
  // Lưu trữ: user_presence:{phoneNumber} -> {instanceId}
  private static final String USER_INSTANCE_KEY_PREFIX = "user_instance:";
  // Set chứa tất cả các phoneNumber đang online
  private static final String ONLINE_USERS_SET_KEY = "online_users_set";

  public PresenceService(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * Đánh dấu user online và lưu instanceId họ đang kết nối.
   * 
   * @param phoneNumber Số điện thoại của user (dùng làm userId)
   * @param instanceId  ID của instance backend
   */
  public void setUserOnline(String phoneNumber, String instanceId) {
    String key = USER_INSTANCE_KEY_PREFIX + phoneNumber;
    try {
      redisTemplate.opsForValue().set(key, instanceId);
      redisTemplate.opsForSet().add(ONLINE_USERS_SET_KEY, phoneNumber);
      log.info("User {} marked online on instance {}", phoneNumber, instanceId);
    } catch (Exception e) {
      log.error("Error setting user {} online on instance {}: {}", phoneNumber, instanceId, e.getMessage(), e);
    }
  }

  /**
   * Đánh dấu user offline.
   * 
   * @param phoneNumber Số điện thoại của user
   */
  public void setUserOffline(String phoneNumber) {
    String key = USER_INSTANCE_KEY_PREFIX + phoneNumber;
    try {
      redisTemplate.delete(key);
      redisTemplate.opsForSet().remove(ONLINE_USERS_SET_KEY, phoneNumber);
      log.info("User {} marked offline", phoneNumber);
    } catch (Exception e) {
      log.error("Error setting user {} offline: {}", phoneNumber, e.getMessage(), e);
    }
  }

  /**
   * Kiểm tra xem user có đang online không.
   * 
   * @param phoneNumber Số điện thoại của user
   * @return true nếu online, false nếu không.
   */
  public boolean isUserOnline(String phoneNumber) {
    try {
      Boolean isMember = redisTemplate.opsForSet().isMember(ONLINE_USERS_SET_KEY, phoneNumber);
      return Boolean.TRUE.equals(isMember);
    } catch (Exception e) {
      log.error("Error checking online status for user {}: {}", phoneNumber, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Lấy instanceId mà user đang kết nối (nếu online).
   * 
   * @param phoneNumber Số điện thoại của user
   * @return instanceId, hoặc null nếu user offline hoặc có lỗi.
   */
  public String getUserInstance(String phoneNumber) {
    String key = USER_INSTANCE_KEY_PREFIX + phoneNumber;
    try {
      return redisTemplate.opsForValue().get(key);
    } catch (Exception e) {
      log.error("Error getting instance for user {}: {}", phoneNumber, e.getMessage(), e);
      return null;
    }
  }

  /**
   * Lấy danh sách tất cả các phoneNumber đang online.
   * 
   * @return Set các phoneNumber, hoặc Set rỗng nếu có lỗi.
   */
  public Set<String> getOnlineUsers() {
    try {
      Set<String> members = redisTemplate.opsForSet().members(ONLINE_USERS_SET_KEY);
      return members != null ? members : Collections.emptySet();
    } catch (Exception e) {
      log.error("Error getting online users: {}", e.getMessage(), e);
      return Collections.emptySet();
    }
  }
}