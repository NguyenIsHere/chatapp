package com.example.chatapp.controller;

import com.example.chatapp.dto.UserDto;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.PresenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PresenceService presenceService;

  // Tìm user theo username (tìm kiếm gần đúng, không phân biệt hoa thường)
  @GetMapping("/search")
  public ResponseEntity<List<UserDto>> findUsersByUsername(@RequestParam String usernameQuery) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String currentPhoneNumber = authentication.getName(); // Lấy SĐT của user đang tìm kiếm

    List<User> users = userRepository.findByUsernameContainingIgnoreCase(usernameQuery);

    List<UserDto> userDtos = users.stream()
        // Không hiển thị chính mình trong kết quả tìm kiếm
        .filter(user -> !user.getPhoneNumber().equals(currentPhoneNumber))
        .map(user -> new UserDto(
            user.getId(),
            user.getPhoneNumber(),
            user.getUsername(),
            presenceService.isUserOnline(user.getPhoneNumber()) // Dùng phoneNumber làm key trong PresenceService
        ))
        .collect(Collectors.toList());
    return ResponseEntity.ok(userDtos);
  }

  // Lấy thông tin user cụ thể bằng số điện thoại
  @GetMapping("/{phoneNumber}")
  public ResponseEntity<UserDto> getUserByPhoneNumber(@PathVariable String phoneNumber) {
    Optional<User> userOptional = userRepository.findByPhoneNumber(phoneNumber);
    if (userOptional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    User user = userOptional.get();
    UserDto userDto = new UserDto(
        user.getId(),
        user.getPhoneNumber(),
        user.getUsername(),
        presenceService.isUserOnline(user.getPhoneNumber()));
    return ResponseEntity.ok(userDto);
  }

  // Lấy danh sách tất cả user đang online (trừ user hiện tại)
  @GetMapping("/online")
  public ResponseEntity<List<UserDto>> getOnlineUsersList() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String currentPhoneNumber = authentication.getName();

    Set<String> onlineUserPhoneNumbers = presenceService.getOnlineUsers();

    List<UserDto> onlineUserDtos = onlineUserPhoneNumbers.stream()
        .filter(phoneNumber -> !phoneNumber.equals(currentPhoneNumber)) // Loại trừ user hiện tại
        .map(phoneNumber -> {
          Optional<User> userOptional = userRepository.findByPhoneNumber(phoneNumber);
          // Chỉ trả về thông tin nếu user tồn tại trong DB
          return userOptional.map(user -> new UserDto(
              user.getId(),
              user.getPhoneNumber(),
              user.getUsername(),
              true // Đã lấy từ danh sách online
          )).orElse(null);
        })
        .filter(userDto -> userDto != null) // Loại bỏ các trường hợp user không tìm thấy trong DB
        .collect(Collectors.toList());
    return ResponseEntity.ok(onlineUserDtos);
  }
}