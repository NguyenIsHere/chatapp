package com.example.chatapp.consumer;

import com.example.chatapp.repository.entities.Message;
import com.example.chatapp.repository.entities.Message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoomMessageListener {

  private static final Logger log = LoggerFactory.getLogger(RoomMessageListener.class);
  private final SimpMessagingTemplate messagingTemplate;

  @Value("${instance.id}")
  private String instanceId;

  @Autowired
  public RoomMessageListener(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  // GroupId sẽ được lấy từ spring.kafka.consumer.group-id (đã set riêng cho mỗi
  // instance trong docker-compose)
  @KafkaListener(topics = "${kafka.topic.room-messages}", containerFactory = "kafkaListenerContainerFactory")
  public void listenRoomMessages(Message message) {
    // Vì mỗi instance là một consumer group riêng, nó sẽ nhận TẤT CẢ tin nhắn room.
    // Không cần kiểm tra instanceId ở đây. SimpleBroker sẽ chỉ gửi đến client trên
    // instance này.
    if (message.getMessageType() != MessageType.ROOM || message.getRoomId() == null) {
      log.trace("[Instance: {}] Ignored non-ROOM message or message with null roomId: {}", instanceId, message);
      return;
    }

    log.info("[Instance: {}] Received ROOM message from Kafka for room [{}]: '{}' from sender [{}]",
        instanceId, message.getRoomId(), message.getContent(), message.getSender());

    // Gửi tin nhắn tới tất cả client đang subscribe vào room đó TRÊN INSTANCE NÀY
    // STOMP Destination: /topic/room/{roomId}
    String destination = "/topic/room/" + message.getRoomId();
    try {
      messagingTemplate.convertAndSend(destination, message);
      log.info("[Instance: {}] Sent ROOM message via WebSocket to STOMP destination: {}, for room: {}",
          instanceId, destination, message.getRoomId());
    } catch (Exception e) {
      log.error("[Instance: {}] Error sending ROOM message via WebSocket to {}: {}",
          instanceId, destination, e.getMessage(), e);
    }
  }
}