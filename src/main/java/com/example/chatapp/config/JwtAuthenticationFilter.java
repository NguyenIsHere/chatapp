package com.example.chatapp.config;

import com.example.chatapp.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  @Autowired
  private JwtService jwtService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // Lấy header Authorization
    String authHeader = request.getHeader("Authorization");

    // Kiểm tra header có tồn tại và bắt đầu bằng "Bearer "
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    // Lấy accessToken từ header
    String accessToken = authHeader.substring(7); // Bỏ "Bearer " (7 ký tự)

    // Xác minh accessToken
    if (jwtService.validateToken(accessToken)) {
      // Lấy phoneNumber từ token
      String phoneNumber = jwtService.getPhoneNumberFromToken(accessToken);

      // Tạo đối tượng Authentication
      UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
          phoneNumber, null, null); // Có thể thêm authorities nếu cần

      // Thiết lập chi tiết cho authentication
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      // Lưu Authentication vào SecurityContext
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // Tiếp tục chuỗi filter
    filterChain.doFilter(request, response);
  }
}