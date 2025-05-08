package com.example.chatapp.consumer;

import com.example.chatapp.repository.entities.Message;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "chat-messages", groupId = "chat-group", containerFactory = "kafkaListenerContainerFactory")
    public void listen(Message message) {
        messagingTemplate.convertAndSend("/topic/room/" + message.getRoomId(), message);
    }
}

