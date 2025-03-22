package com.example.chatapp.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
  @Value("${jwt.secret}")
  private String secretKey;

  @Value("${jwt.access.expiration}")
  private long accessTokenExpiration;

  @Value("${jwt.refresh.expiration}")
  private long refreshTokenExpiration;

  private SecretKey getSigningKey() {
    byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String generateAccessToken(String phoneNumber) {
    return Jwts.builder()
        .subject(phoneNumber)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
        .signWith(getSigningKey(), Jwts.SIG.HS256)
        .compact();
  }

  public String generateRefreshToken(String phoneNumber) {
    return Jwts.builder()
        .subject(phoneNumber)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
        .signWith(getSigningKey(), Jwts.SIG.HS256)
        .compact();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser()
          .verifyWith(getSigningKey())
          .build()
          .parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }
}