package com.example.chatapp.config; // Hoặc một package phù hợp

import com.example.chatapp.repository.entities.PrivateChatMessage;
import jakarta.annotation.PostConstruct; // Hoặc javax.annotation.PostConstruct tùy phiên bản Java/Spring
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

@Configuration
public class MongoIndexConfiguration {

  private static final Logger log = LoggerFactory.getLogger(MongoIndexConfiguration.class);

  @Autowired
  private MongoTemplate mongoTemplate;

  @PostConstruct
  public void ensureIndexes() {
    log.info("Ensuring MongoDB indexes are created...");
    try {
      IndexOperations indexOps = mongoTemplate.indexOps(PrivateChatMessage.class); // Hoặc tên collection

      // Tạo unique index trên messageId
      Index messageIdIndex = new Index().on("messageId", Sort.Direction.ASC).unique();
      indexOps.ensureIndex(messageIdIndex);
      log.info("Ensured unique index on 'messageId' for PrivateChatMessage collection.");

      // index trên conversationId và timestamp để tối ưu query lịch sử chat
      Index conversationTimestampIndex = new Index()
          .on("conversationId", Sort.Direction.ASC)
          .on("timestamp", Sort.Direction.DESC);
      indexOps.ensureIndex(conversationTimestampIndex);
      log.info("Ensured index on 'conversationId' and 'timestamp' for PrivateChatMessage collection.");

    } catch (Exception e) {
      log.error("Error ensuring MongoDB indexes: {}", e.getMessage(), e);
    }
  }
}