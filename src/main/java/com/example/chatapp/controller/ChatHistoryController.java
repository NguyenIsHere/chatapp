package com.example.chatapp.controller;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.chatapp.repository.PrivateChatMessageRepository;
import com.example.chatapp.repository.entities.PrivateChatMessage;

// Ví dụ trong một ChatHistoryController.java
@RestController
@RequestMapping("/api/v1/chat-history")
public class ChatHistoryController {
  @Autowired
  private PrivateChatMessageRepository privateMessageRepository;

  @GetMapping("/private/{user1PhoneNumber}/{user2PhoneNumber}")
  public ResponseEntity<List<PrivateChatMessage>> getPrivateChatHistory(
      @PathVariable String user1PhoneNumber,
      @PathVariable String user2PhoneNumber,
      @RequestParam(defaultValue = "0") int page, // Cho phân trang
      @RequestParam(defaultValue = "20") int size) {

    // Xác thực user hiện tại có quyền xem cuộc hội thoại này không (quan trọng)
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String currentPrincipalName = authentication.getName();
    if (!currentPrincipalName.equals(user1PhoneNumber) && !currentPrincipalName.equals(user2PhoneNumber)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    String conversationId = PrivateChatMessage.generateConversationId(user1PhoneNumber, user2PhoneNumber);
    // Ví dụ lấy 20 tin nhắn mới nhất
    Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
    Page<PrivateChatMessage> messagePage = privateMessageRepository
        .findByConversationIdOrderByTimestampDesc(conversationId, pageable);

    // Đảo ngược lại để hiển thị theo thứ tự cũ -> mới trên UI nếu cần
    List<PrivateChatMessage> messages = messagePage.getContent().stream()
        .sorted(Comparator.comparing(PrivateChatMessage::getTimestamp))
        .collect(Collectors.toList());
    return ResponseEntity.ok(messages);
  }
}
