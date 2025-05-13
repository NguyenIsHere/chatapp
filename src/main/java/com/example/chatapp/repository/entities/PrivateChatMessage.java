package com.example.chatapp.repository.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed; // Thêm cho unique index
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "private_chat_messages")
public class PrivateChatMessage {
  @Id
  private String id; // MongoDB document ID

  @Indexed(unique = true) // Đảm bảo messageId là duy nhất ở tầng DB để tránh trùng lặp tuyệt đối
  private String messageId; // ID gốc của tin nhắn, được tạo bởi producer

  private String senderPhoneNumber;
  private String recipientPhoneNumber;
  private String content;
  private LocalDateTime timestamp;
  private Message.MessageType messageType = Message.MessageType.PRIVATE;
  private String conversationId;

  public PrivateChatMessage() {
  }

  // Constructor nhận Message object từ Kafka
  public PrivateChatMessage(Message kafkaMessage) {
    this.messageId = kafkaMessage.getMessageId(); // QUAN TRỌNG: Lấy messageId từ tin nhắn Kafka
    this.senderPhoneNumber = kafkaMessage.getSender();
    this.recipientPhoneNumber = kafkaMessage.getRecipientPhoneNumber();
    this.content = kafkaMessage.getContent();
    this.timestamp = kafkaMessage.getTimeStamp(); // Nên là thời gian từ server Kafka/producer
    this.messageType = kafkaMessage.getMessageType(); // Lấy cả messageType
    this.conversationId = generateConversationId(kafkaMessage.getSender(), kafkaMessage.getRecipientPhoneNumber());
  }

  public static String generateConversationId(String user1, String user2) {
    if (user1 == null || user2 == null) { // Xử lý trường hợp null
      // Hoặc ném một exception, hoặc trả về một giá trị đặc biệt
      // Tạm thời, nếu một trong hai là null, không tạo conversationId hợp lệ
      // Điều này không nên xảy ra với tin nhắn private hợp lệ
      return "invalid_conversation_" + (user1 == null ? "null" : user1) + "_" + (user2 == null ? "null" : user2);
    }
    if (user1.compareTo(user2) > 0) {
      return user2 + "_" + user1;
    }
    return user1 + "_" + user2;
  }
}