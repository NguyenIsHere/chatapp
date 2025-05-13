// src/main/java/com/example/chatapp/consumer/PrivateMessageListener.java
package com.example.chatapp.consumer;

import com.example.chatapp.repository.PrivateChatMessageRepository;
import com.example.chatapp.repository.entities.Message;
import com.example.chatapp.repository.entities.Message.MessageType;
import com.example.chatapp.repository.entities.PrivateChatMessage;
import com.example.chatapp.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class PrivateMessageListener {

  private static final Logger log = LoggerFactory.getLogger(PrivateMessageListener.class);
  private final SimpMessagingTemplate messagingTemplate;
  private final PresenceService presenceService;
  private final PrivateChatMessageRepository privateMessageRepository;

  @Value("${instance.id}")
  private String currentInstanceId;

  @Autowired
  public PrivateMessageListener(SimpMessagingTemplate messagingTemplate,
      PresenceService presenceService,
      PrivateChatMessageRepository privateMessageRepository) {
    this.messagingTemplate = messagingTemplate;
    this.presenceService = presenceService;
    this.privateMessageRepository = privateMessageRepository;
  }

  @KafkaListener(topics = "${kafka.topic.private-messages}", containerFactory = "kafkaListenerContainerFactory")
  public void listenPrivateMessages(Message kafkaReceivedMessage) {
    if (kafkaReceivedMessage.getMessageType() != MessageType.PRIVATE ||
        kafkaReceivedMessage.getRecipientPhoneNumber() == null ||
        kafkaReceivedMessage.getSender() == null ||
        kafkaReceivedMessage.getMessageId() == null) {
      log.trace("[Instance: {}] Ignored invalid PRIVATE message: {}", currentInstanceId, kafkaReceivedMessage);
      return;
    }

    String recipientPhoneNumber = kafkaReceivedMessage.getRecipientPhoneNumber();
    String senderPhoneNumber = kafkaReceivedMessage.getSender();
    String messageId = kafkaReceivedMessage.getMessageId();

    log.info("[Instance: {}] Received PRIVATE message ID [{}] from Kafka. Sender: [{}], Recipient: [{}], Content: '{}'",
        currentInstanceId, messageId, senderPhoneNumber, recipientPhoneNumber, kafkaReceivedMessage.getContent());

    // --- LƯU TIN NHẮN VÀO DATABASE (CHỈ 1 LẦN) ---
    PrivateChatMessage chatToSave = new PrivateChatMessage(kafkaReceivedMessage);
    boolean messageNewlySaved = false;
    try {
      if (!privateMessageRepository.findByMessageId(messageId).isPresent()) {
        privateMessageRepository.save(chatToSave);
        messageNewlySaved = true;
        log.info("[Instance: {}] Saved PRIVATE message ID [{}] to DB. DB_ID: {}",
            currentInstanceId, messageId, chatToSave.getId());
      } else {
        log.info("[Instance: {}] Message ID [{}] already in DB. Skipping save.", currentInstanceId, messageId);
      }
    } catch (DuplicateKeyException e) {
      log.warn(
          "[Instance: {}] Message ID [{}] caused DuplicateKeyException (likely saved by another instance). Skipping. Msg: {}",
          currentInstanceId, messageId, e.getMessage());
    } catch (Exception e) {
      log.error("[Instance: {}] Error saving PRIVATE message ID [{}] to DB: {}. Message: {}",
          currentInstanceId, messageId, e.getMessage(), kafkaReceivedMessage, e);
    }
    // --- KẾT THÚC LƯU DATABASE ---

    // --- GỬI WEBSOCKET CHO NGƯỜI NHẬN ---
    // Kiểm tra xem người nhận có session nào active trên instance này không
    if (presenceService.hasUserActiveSessionOnInstance(recipientPhoneNumber, currentInstanceId)) {
      log.info(
          "[Instance: {}] RECIPIENT [{}] for message ID [{}] has active session(s) ON THIS INSTANCE. Sending via WebSocket.",
          currentInstanceId, recipientPhoneNumber, messageId);
      sendToUserViaWebSocket(recipientPhoneNumber, kafkaReceivedMessage, "RECIPIENT");
    } else {
      if (presenceService.isUserOnline(recipientPhoneNumber)) { // User online nhưng ở instance khác
        log.info(
            "[Instance: {}] RECIPIENT [{}] for message ID [{}] is online on ANOTHER instance. Not sending WebSocket from this instance [{}].",
            currentInstanceId, recipientPhoneNumber, messageId, currentInstanceId);
      } else { // User offline hoàn toàn
        log.info(
            "[Instance: {}] RECIPIENT [{}] for message ID [{}] is OFFLINE. Not sending WebSocket from this instance [{}]. Saved status: {}",
            currentInstanceId, recipientPhoneNumber, messageId, currentInstanceId, messageNewlySaved);
      }
    }

    // --- GỬI WEBSOCKET CHO NGƯỜI GỬI (ĐỂ ĐỒNG BỘ CÁC THIẾT BỊ KHÁC CỦA NGƯỜI GỬI)
    // ---
    // Chỉ gửi nếu người gửi không phải là người nhận (tránh gửi lại cho chính mình
    // nếu tự chat)
    if (!senderPhoneNumber.equals(recipientPhoneNumber)) {
      if (presenceService.hasUserActiveSessionOnInstance(senderPhoneNumber, currentInstanceId)) {
        log.info(
            "[Instance: {}] SENDER [{}] for message ID [{}] has active session(s) ON THIS INSTANCE. Sending SYNC message for sender's other devices.",
            currentInstanceId, senderPhoneNumber, messageId);
        sendToUserViaWebSocket(senderPhoneNumber, kafkaReceivedMessage, "SENDER_SYNC");
      } else {
        // Không cần log nhiều ở đây vì instance khác (nơi sender active) sẽ xử lý
        log.trace(
            "[Instance: {}] SENDER [{}] for message ID [{}] has no active session on this instance. Sync message not sent from here.",
            currentInstanceId, senderPhoneNumber, messageId);
      }
    }
  }

  private void sendToUserViaWebSocket(String targetUserPhoneNumber, Message messagePayload, String purpose) {
    String userSpecificQueueName = "/queue/private-messages"; // Client subscribe vào /user/queue/private-messages
    try {
      messagingTemplate.convertAndSendToUser(
          targetUserPhoneNumber,
          userSpecificQueueName,
          messagePayload);
      log.info("[Instance: {}] Sent {} message (Origin ID: [{}]) to user [{}] via WebSocket (queue: {}).",
          currentInstanceId, purpose, messagePayload.getMessageId(), targetUserPhoneNumber, userSpecificQueueName);
    } catch (Exception e) {
      log.error("[Instance: {}] Error sending {} message (Origin ID: [{}]) via WebSocket to user [{}]: {}",
          currentInstanceId, purpose, messagePayload.getMessageId(), targetUserPhoneNumber, e.getMessage(), e);
    }
  }
}