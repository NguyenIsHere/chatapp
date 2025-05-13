// src/main/java/com/example/chatapp/repository/PrivateChatMessageRepository.java
package com.example.chatapp.repository;

import com.example.chatapp.repository.entities.PrivateChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface PrivateChatMessageRepository extends MongoRepository<PrivateChatMessage, String> {
  List<PrivateChatMessage> findByConversationIdOrderByTimestampAsc(String conversationId);

  Page<PrivateChatMessage> findByConversationIdOrderByTimestampDesc(String conversationId, Pageable pageable);

  Optional<PrivateChatMessage> findByMessageId(String messageId);
}