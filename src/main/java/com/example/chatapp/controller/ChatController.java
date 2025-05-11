package com.example.chatapp.controller;

import com.example.chatapp.dto.MessageRequests;
import com.example.chatapp.repository.RoomRepository;
import com.example.chatapp.repository.entities.Message;
import com.example.chatapp.repository.entities.Room;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final RoomRepository roomRepository;
    private final KafkaTemplate<String, Message> kafkaTemplate;

    // for sending and receiving messages
    @MessageMapping("/sendMessage/{roomId}") // /app/sendMessage/roomId
    public void sendMessage(
            @DestinationVariable String roomId,
            @RequestBody MessageRequests request) {
        Message message = new Message();
        message.setContent(request.getContent());
        message.setSender(request.getSender());
        message.setTimeStamp(LocalDateTime.now());
        message.setRoomId(roomId);
        log.info("Message sent: " + message);
        kafkaTemplate.send("chat-messages", message);

    }
}
