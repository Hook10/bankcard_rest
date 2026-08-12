package com.example.bankcards.service;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RegisterRequest;

public interface AuthService {
  void register(RegisterRequest request);

  AuthResponse login(LoginRequest request);

  AuthResponse refresh(String refreshToken);

  void logout(String accessToken, String email);
}
