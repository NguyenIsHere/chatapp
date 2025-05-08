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

    @KafkaListener(topics = "chat-messages", groupId = "chat-group-2", containerFactory = "kafkaListenerContainerFactory")
    public void listen(Message message) {
        log.info("Received message: {}", message);
        messagingTemplate.convertAndSend("/topic/room/" + message.getRoomId(), message);
    }
}

