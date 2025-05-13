package com.example.chatapp.repository.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Message {

    private String messageId; // ID duy nhất cho mỗi tin nhắn
    private String sender; // Luôn là phoneNumber của người gửi
    private String content;
    private String roomId; // Sẽ là null cho tin nhắn 1-1, hoặc ID của phòng chat

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime timeStamp;

    private String recipientPhoneNumber; // phoneNumber của người nhận (chỉ dùng cho tin nhắn PRIVATE)
    private MessageType messageType; // Phân loại tin nhắn

    public enum MessageType {
        ROOM, PRIVATE
    }

    // Static factory method cho tin nhắn ROOM
    public static Message newRoomMessage(String messageId, String sender, String content, String roomId) {
        Message msg = new Message();
        msg.setMessageId(messageId); // Set messageId
        msg.setSender(sender);
        msg.setContent(content);
        msg.setRoomId(roomId);
        msg.setMessageType(MessageType.ROOM);
        msg.setTimeStamp(LocalDateTime.now());
        return msg;
    }

    // Static factory method cho tin nhắn PRIVATE
    public static Message newPrivateMessage(String messageId, String sender, String recipientPhoneNumber,
            String content) {
        Message msg = new Message();
        msg.setMessageId(messageId); // Set messageId
        msg.setSender(sender);
        msg.setRecipientPhoneNumber(recipientPhoneNumber);
        msg.setContent(content);
        msg.setMessageType(MessageType.PRIVATE);
        msg.setTimeStamp(LocalDateTime.now());
        return msg;
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageId='" + messageId + '\'' +
                ", sender='" + sender + '\'' +
                ", content='" + content + '\'' +
                ", roomId='" + roomId + '\'' +
                ", timeStamp=" + timeStamp +
                ", recipientPhoneNumber='" + recipientPhoneNumber + '\'' +
                ", messageType=" + messageType +
                '}';
    }
}