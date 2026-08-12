package com.example.bankcards.controller;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RegisterRequest;
import com.example.bankcards.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, token refresh and logout")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  @SecurityRequirements
  @Operation(
          summary = "Register a new user",
          description = "Creates a new account with role USER. Email and phone number must be unique. " +
                  "No authentication required."
  )
  @ApiResponses({
          @ApiResponse(responseCode = "201", description = "Account created successfully"),
          @ApiResponse(responseCode = "400", description = "Validation failed (invalid email/phone format, weak password)", content = @Content),
          @ApiResponse(responseCode = "409", description = "Email or phone number already registered", content = @Content)
  })
  public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
    authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PostMapping("/login")
  @SecurityRequirements
  @Operation(
          summary = "Login with email and password",
          description = "Validates credentials and returns a JWT access token (short-lived) and " +
                  "refresh token (long-lived). No authentication required."
  )
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Login successful, tokens issued"),
          @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
          @ApiResponse(responseCode = "401", description = "Invalid email or password", content = @Content),
          @ApiResponse(responseCode = "403", description = "Account is disabled", content = @Content)
  })
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/refresh")
  @SecurityRequirements
  @Operation(
          summary = "Exchange a refresh token for a new token pair",
          description = "Validates the supplied refresh token against the stored value, then issues a new " +
                  "access token and refresh token (rotation). The old refresh token is invalidated. " +
                  "No Bearer authentication required — pass the refresh token via the `X-Refresh-Token` header."
  )
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "New token pair issued"),
          @ApiResponse(responseCode = "401", description = "Refresh token is invalid, expired, or does not match the stored token", content = @Content)
  })
  public ResponseEntity<AuthResponse> refresh(
          @Parameter(description = "Refresh token previously issued by /login or /refresh", required = true)
          @RequestHeader("X-Refresh-Token") String token) {
    return ResponseEntity.ok(authService.refresh(token));
  }

  @PostMapping("/logout")
  @Operation(
          summary = "Logout the current user",
          description = "Blacklists the current access token (rejected on all instances via shared storage) " +
                  "and revokes the associated refresh token so it can no longer be used to obtain new tokens."
  )
  @ApiResponses({
          @ApiResponse(responseCode = "204", description = "Logged out successfully"),
          @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content)
  })
  public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader,
                                     @AuthenticationPrincipal UserDetails userDetails) {
    authService.logout(authHeader.substring(7), userDetails.getUsername());
    return ResponseEntity.noContent().build();
  }
}
