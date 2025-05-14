package com.example.chatapp.controller;

import com.example.chatapp.dto.PrivateMessageRequestDto;
import com.example.chatapp.repository.entities.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PrivateChatController {

  private static final Logger log = LoggerFactory.getLogger(PrivateChatController.class);
  private final KafkaTemplate<String, Message> kafkaTemplate;

  @Value("${kafka.topic.private-messages}")
  private String privateMessagesTopic;

  @MessageMapping("/private.sendMessage")
  public void sendPrivateMessage(@Payload PrivateMessageRequestDto clientRequest, Principal principal) {
    if (principal == null || principal.getName() == null) {
      log.error("Cannot send private message: sender principal is null.");
      return;
    }
    if (clientRequest.getRecipientPhoneNumber() == null || clientRequest.getRecipientPhoneNumber().isEmpty()) {
      log.error("Cannot send private message: recipientPhoneNumber is null or empty from DTO: {}", clientRequest);
      return;
    }

    String senderPhoneNumber = principal.getName();
    String messageId = UUID.randomUUID().toString(); // Tạo messageId duy nhất

    // Sử dụng static factory method đã cập nhật của Message
    Message messageToKafka = Message.newPrivateMessage(
        messageId, // Truyền messageId
        senderPhoneNumber,
        clientRequest.getRecipientPhoneNumber(),
        clientRequest.getContent());

    log.info("Sending PRIVATE message with ID [{}] to Kafka topic '{}', key='{}': Content='{}'",
        messageToKafka.getMessageId(),
        privateMessagesTopic,
        clientRequest.getRecipientPhoneNumber(),
        messageToKafka.getContent()); // Log content thay vì cả object để tránh quá dài
    try {
      kafkaTemplate.send(privateMessagesTopic, clientRequest.getRecipientPhoneNumber(), messageToKafka);
    } catch (Exception e) {
      log.error("Error sending PRIVATE message with ID [{}] to Kafka: {}", messageToKafka.getMessageId(),
          e.getMessage(), e);
    }
  }
}