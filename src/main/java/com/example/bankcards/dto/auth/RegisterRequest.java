package com.example.bankcards.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Registration payload for creating a new USER account")
public record RegisterRequest(
        @Schema(description = "Unique email address, used as the login identifier", example = "jane.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "Full legal name as it appears on bank documents", example = "Jane Doe")
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String fullName,

        @Schema(description = "Phone number in international E.164-like format", example = "+15551234567")
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be in international format (e.g., +1234567890)")
        String phoneNumber,

        @Schema(description = "Plain-text password (min 8 characters); stored as a BCrypt hash", example = "Str0ngPassw0rd!")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
