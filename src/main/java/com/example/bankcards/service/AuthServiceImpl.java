package com.example.bankcards.service;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RegisterRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InvalidTokenException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.exception.UsernameAlreadyExistsException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final JwtService jwtService;
  private final UserRepository userRepository;
  private final AuthenticationManager authenticationManager;
  private final RedisTemplate<String, String> redisTemplate;
  private final PasswordEncoder passwordEncoder;

  @Value("${jwt.refresh-token-expiry}")
  private long refreshTokenExpiry;

  @Value("${jwt.access-token-expiry}")
  private long accessTokenExpiry;

  @Transactional
  public void register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new UsernameAlreadyExistsException("Email already registered: " + request.email());
    }

    if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
      throw new UsernameAlreadyExistsException("Phone number already registered: " + request.phoneNumber());
    }

    User user = User.builder()
        .email(request.email())
        .fullName(request.fullName())
        .phoneNumber(request.phoneNumber())
        .password(passwordEncoder.encode(request.password()))
        .role(Role.USER)
        .enabled(true)
        .emailVerified(false)
        .phoneVerified(false)
        .createdAt(LocalDateTime.now())
        .build();

    userRepository.save(user);
  }

  public AuthResponse login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> new UserNotFoundException("User not found " + request.email()));

    return buildAuthResponse(user);
  }

  @Override
  public AuthResponse refresh(String refreshToken) {
    String cleanRefreshToken = cleanTokenPrefix(refreshToken);
    if (!jwtService.isTokenValid(cleanRefreshToken)) {
      throw new InvalidTokenException("Invalid refresh token");
    }

    String email = jwtService.extractEmail(cleanRefreshToken);
    String stored = redisTemplate.opsForValue().get("refresh:" + email);

    if (!cleanRefreshToken.equals(stored)) {
      throw new InvalidTokenException("Refresh token mismatch");
    }
    redisTemplate.delete("refresh:" + email);
    User user = userRepository.findByEmail(email).orElseThrow(
        () -> new UserNotFoundException("User associated with token no longer exists"));
    return buildAuthResponse(user);
  }

  @Override
  public void logout(String accessToken, String email) {
    String cleanAccessToken = cleanTokenPrefix(accessToken);

    long remainingExpiryMs = jwtService.getRemainingExpiryTimeInMs(cleanAccessToken);

    if(remainingExpiryMs > 0) {
      redisTemplate.opsForValue().set(
          "blacklist:" + cleanAccessToken, "true",
          Duration.ofMillis(remainingExpiryMs));
    }
    redisTemplate.delete("refresh:" + email);
  }

  private AuthResponse buildAuthResponse(User user) {
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    redisTemplate.opsForValue().set(
        "refresh:" + user.getEmail(),
        refreshToken,
        Duration.ofMillis(refreshTokenExpiry));

    return AuthResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(accessTokenExpiry / 1000)
        .build();
  }

  private String cleanTokenPrefix(String token) {
    if (token != null && token.startsWith("Bearer ")) {
      return token.substring(7);
    }
    return token;
  }
}
