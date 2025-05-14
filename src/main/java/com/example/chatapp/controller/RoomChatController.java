package com.example.chatapp.controller;

import com.example.chatapp.dto.MessageRequests;
import com.example.chatapp.repository.entities.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class RoomChatController {

    private static final Logger log = LoggerFactory.getLogger(RoomChatController.class);
    private final KafkaTemplate<String, Message> kafkaTemplate;

    @Value("${kafka.topic.room-messages}")
    private String roomMessagesTopic;

    // Client gửi tin nhắn tới /app/room.sendMessage/{roomId}
    @MessageMapping("/room.sendMessage/{roomId}")
    public void sendRoomMessage(@DestinationVariable String roomId,
            @Payload MessageRequests clientRequest, // Client chỉ cần gửi content
            Principal principal) {
        if (principal == null || principal.getName() == null) {
            log.error("Cannot send room message: sender principal is null. Room: {}", roomId);
            return;
        }
        if (clientRequest.getContent() == null || clientRequest.getContent().trim().isEmpty()) {
            log.warn("Cannot send empty room message from user {} to room {}", principal.getName(), roomId);
            return;
        }

        String senderPhoneNumber = principal.getName(); // Lấy phoneNumber từ Principal
        String messageId = UUID.randomUUID().toString(); // Tạo messageId duy nhất cho tin nhắn room

        // Sử dụng static factory method đã cập nhật của Message
        Message messageToKafka = Message.newRoomMessage(
                messageId, // Truyền messageId
                senderPhoneNumber,
                clientRequest.getContent(),
                roomId);

        log.info("Sending ROOM message with ID [{}] to Kafka topic '{}', key='{}': Content='{}'",
                messageToKafka.getMessageId(),
                roomMessagesTopic,
                roomId, // Dùng roomId làm Kafka key
                messageToKafka.getContent());
        try {
            kafkaTemplate.send(roomMessagesTopic, roomId, messageToKafka);
        } catch (Exception e) {
            log.error("Error sending ROOM message with ID [{}] to Kafka: {}", messageToKafka.getMessageId(),
                    e.getMessage(), e);
        }
    }
}