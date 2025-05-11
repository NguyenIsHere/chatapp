package com.example.chatapp.consumer;

import com.example.chatapp.repository.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageListener {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageListener.class);
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Loại bỏ groupId khỏi đây, nó sẽ được lấy từ cấu hình ConsumerFactory
    @KafkaListener(topics = "chat-messages", containerFactory = "kafkaListenerContainerFactory")
    public void listen(Message message) {
        log.info("Received message in instance: {}, message: {}", System.getenv("SPRING_KAFKA_CONSUMER_GROUP_ID"),
                message); // Thêm log để biết instance nào nhận
        messagingTemplate.convertAndSend("/topic/room/" + message.getRoomId(), message);
    }
}
