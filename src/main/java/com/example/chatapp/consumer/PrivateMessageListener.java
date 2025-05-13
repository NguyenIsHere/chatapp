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
        kafkaReceivedMessage.getMessageId() == null) { // Luôn kiểm tra messageId
      log.trace("[Instance: {}] Ignored invalid PRIVATE message (missing type, recipient, or messageId): {}",
          currentInstanceId, kafkaReceivedMessage);
      return;
    }

    String recipientPhoneNumber = kafkaReceivedMessage.getRecipientPhoneNumber();
    String senderPhoneNumber = kafkaReceivedMessage.getSender();
    String messageId = kafkaReceivedMessage.getMessageId();

    log.info("[Instance: {}] Received PRIVATE message ID [{}] from Kafka for recipient [{}]: '{}' from sender [{}]",
        currentInstanceId, messageId, recipientPhoneNumber, kafkaReceivedMessage.getContent(), senderPhoneNumber);

    // --- LƯU TIN NHẮN VÀO DATABASE (VỚI XỬ LÝ UNIQUE INDEX) ---
    PrivateChatMessage chatToSave = new PrivateChatMessage(kafkaReceivedMessage);
    boolean messageSavedByThisInstance = false;
    try {
      privateMessageRepository.save(chatToSave);
      messageSavedByThisInstance = true; // Đánh dấu là instance này đã lưu thành công
      log.info("[Instance: {}] Successfully saved PRIVATE message ID [{}] to DB. DB_ID: {}",
          currentInstanceId, messageId, chatToSave.getId());
    } catch (DuplicateKeyException e) {
      log.warn(
          "[Instance: {}] PRIVATE message ID [{}] already exists in DB (likely saved by another instance). Skipping save. Exception: {}",
          currentInstanceId, messageId, e.getMessage());
    } catch (Exception e) {
      log.error(
          "[Instance: {}] Error saving PRIVATE message ID [{}] to DB (Sender: {}, Recipient: {}): {}. Message: {}",
          currentInstanceId, messageId, senderPhoneNumber, recipientPhoneNumber, e.getMessage(), kafkaReceivedMessage,
          e);
    }
    // --- KẾT THÚC LƯU DATABASE ---

    // --- GỬI WEBSOCKET ---
    // Chỉ instance nào đang giữ kết nối của người nhận mới gửi tin nhắn WebSocket
    String recipientActualInstanceId = presenceService.getUserInstance(recipientPhoneNumber);

    if (currentInstanceId.equals(recipientActualInstanceId)) {
      String userSpecificQueueName = "/queue/private-messages";
      log.info("[Instance: {}] Recipient [{}] for message ID [{}] is ON THIS INSTANCE. Sending via WebSocket...",
          currentInstanceId, recipientPhoneNumber, messageId);
      try {
        messagingTemplate.convertAndSendToUser(
            recipientPhoneNumber,
            userSpecificQueueName,
            kafkaReceivedMessage);
        log.info("[Instance: {}] Successfully sent PRIVATE message ID [{}] to user [{}] via WebSocket.",
            currentInstanceId, messageId, recipientPhoneNumber);
      } catch (Exception e) {
        log.error("[Instance: {}] Error sending PRIVATE message ID [{}] via WebSocket to user [{}]: {}",
            currentInstanceId, messageId, recipientPhoneNumber, e.getMessage(), e);
      }
    } else {
      if (recipientActualInstanceId != null) {
        log.info(
            "[Instance: {}] Recipient [{}] for message ID [{}] is online on instance [{}]. Message not sent from this instance [{}].",
            currentInstanceId, recipientPhoneNumber, messageId, recipientActualInstanceId, currentInstanceId);
      } else {
        log.info(
            "[Instance: {}] Recipient [{}] for message ID [{}] is OFFLINE. Message not sent from this instance [{}]. (Saved status: {})",
            currentInstanceId, recipientPhoneNumber, messageId, currentInstanceId,
            messageSavedByThisInstance ? "Saved by this instance" : "Potentially saved by another or save failed");
      }
    }
  }
}