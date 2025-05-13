// src/main/java/com/example/chatapp/service/PresenceService.java
package com.example.chatapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PresenceService {

  private static final Logger log = LoggerFactory.getLogger(PresenceService.class);
  private final RedisTemplate<String, String> redisTemplate;

  // Key prefix cho Set chứa các "instanceId:sessionId" của một user
  private static final String USER_SESSIONS_KEY_PREFIX = "user_sessions:";
  // Key cho Set chứa các phoneNumber đang online
  private static final String ONLINE_USERS_PHONE_SET_KEY = "online_users_phone_set";

  @Autowired
  public PresenceService(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  private String formatSessionKey(String instanceId, String sessionId) {
    return instanceId + ":" + sessionId;
  }

  /**
   * Thêm một session WebSocket mới cho user.
   *
   * @param phoneNumber Số điện thoại của user
   * @param instanceId  ID của instance backend mà session kết nối tới
   * @param sessionId   ID của session WebSocket
   */
  public void addSession(String phoneNumber, String instanceId, String sessionId) {
    if (phoneNumber == null || instanceId == null || sessionId == null) {
      log.warn("Cannot add session with null phoneNumber, instanceId, or sessionId.");
      return;
    }
    String userSessionsKey = USER_SESSIONS_KEY_PREFIX + phoneNumber;
    String sessionValue = formatSessionKey(instanceId, sessionId);
    try {
      redisTemplate.opsForSet().add(userSessionsKey, sessionValue);
      redisTemplate.opsForSet().add(ONLINE_USERS_PHONE_SET_KEY, phoneNumber);
      log.info("User {} added session {} on instance {} to Redis.", phoneNumber, sessionId, instanceId);
    } catch (Exception e) {
      log.error("Error adding session for user {}: {}", phoneNumber, e.getMessage(), e);
    }
  }

  /**
   * Xóa một session WebSocket của user.
   * Nếu user không còn session nào active, xóa họ khỏi danh sách online.
   *
   * @param phoneNumber Số điện thoại của user
   * @param instanceId  ID của instance backend
   * @param sessionId   ID của session WebSocket
   */
  public void removeSession(String phoneNumber, String instanceId, String sessionId) {
    if (phoneNumber == null || instanceId == null || sessionId == null) {
      log.warn("Cannot remove session with null phoneNumber, instanceId, or sessionId.");
      return;
    }
    String userSessionsKey = USER_SESSIONS_KEY_PREFIX + phoneNumber;
    String sessionValue = formatSessionKey(instanceId, sessionId);
    try {
      Long removedCount = redisTemplate.opsForSet().remove(userSessionsKey, sessionValue);
      if (removedCount != null && removedCount > 0) {
        log.info("User {} removed session {} on instance {} from Redis.", phoneNumber, sessionId, instanceId);
      }

      // Kiểm tra xem user còn session nào khác không
      Long remainingSessions = redisTemplate.opsForSet().size(userSessionsKey);
      if (remainingSessions == null || remainingSessions == 0) {
        redisTemplate.opsForSet().remove(ONLINE_USERS_PHONE_SET_KEY, phoneNumber);
        // Cũng nên xóa key user_sessions:{phoneNumber} nếu nó rỗng để dọn dẹp
        if (remainingSessions == 0)
          redisTemplate.delete(userSessionsKey);
        log.info("User {} has no more active sessions and is marked offline.", phoneNumber);
      }
    } catch (Exception e) {
      log.error("Error removing session for user {}: {}", phoneNumber, e.getMessage(), e);
    }
  }

  /**
   * Xóa tất cả các session của một user (ví dụ khi user chủ động logout qua
   * HTTP).
   * 
   * @param phoneNumber Số điện thoại của user
   * @return
   */
  public void removeAllSessionsForUser(String phoneNumber) {
    if (phoneNumber == null)
      return;
    String userSessionsKey = USER_SESSIONS_KEY_PREFIX + phoneNumber;
    try {
      redisTemplate.delete(userSessionsKey);
      redisTemplate.opsForSet().remove(ONLINE_USERS_PHONE_SET_KEY, phoneNumber);
      log.info("All sessions for user {} removed. Marked as offline.", phoneNumber);
    } catch (Exception e) {
      log.error("Error removing all sessions for user {}: {}", phoneNumber, e.getMessage(), e);
    }
  }

  /**
   * Kiểm tra xem user có đang online không (có ít nhất 1 session active).
   *
   * @param phoneNumber Số điện thoại của user
   * @return true nếu online, false nếu không.
   */
  public boolean isUserOnline(String phoneNumber) {
    if (phoneNumber == null)
      return false;
    try {
      Boolean isMember = redisTemplate.opsForSet().isMember(ONLINE_USERS_PHONE_SET_KEY, phoneNumber);
      return Boolean.TRUE.equals(isMember);
    } catch (Exception e) {
      log.error("Error checking online status for user {}: {}", phoneNumber, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Lấy tất cả các session active (dưới dạng "instanceId:sessionId") của một
   * user.
   *
   * @param phoneNumber Số điện thoại của user
   * @return Set các chuỗi "instanceId:sessionId", hoặc Set rỗng.
   */
  public Set<String> getUserActiveSessions(String phoneNumber) {
    if (phoneNumber == null)
      return Collections.emptySet();
    String userSessionsKey = USER_SESSIONS_KEY_PREFIX + phoneNumber;
    try {
      Set<String> sessions = redisTemplate.opsForSet().members(userSessionsKey);
      return sessions != null ? sessions : Collections.emptySet();
    } catch (Exception e) {
      log.error("Error getting active sessions for user {}: {}", phoneNumber, e.getMessage(), e);
      return Collections.emptySet();
    }
  }

  /**
   * Kiểm tra xem user có bất kỳ session nào đang active trên instanceId cụ thể
   * này không.
   *
   * @param phoneNumber      Số điện thoại của user
   * @param targetInstanceId ID của instance cần kiểm tra
   * @return true nếu có, false nếu không.
   */
  public boolean hasUserActiveSessionOnInstance(String phoneNumber, String targetInstanceId) {
    if (phoneNumber == null || targetInstanceId == null)
      return false;
    Set<String> activeSessions = getUserActiveSessions(phoneNumber);
    for (String sessionInfo : activeSessions) {
      if (sessionInfo.startsWith(targetInstanceId + ":")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Lấy danh sách tất cả các phoneNumber đang online.
   *
   * @return Set các phoneNumber, hoặc Set rỗng nếu có lỗi.
   */
  public Set<String> getOnlineUsers() {
    try {
      Set<String> members = redisTemplate.opsForSet().members(ONLINE_USERS_PHONE_SET_KEY);
      return members != null ? members : Collections.emptySet();
    } catch (Exception e) {
      log.error("Error getting online users: {}", e.getMessage(), e);
      return Collections.emptySet();
    }
  }
}