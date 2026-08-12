package com.example.bankcards.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "JWT token pair issued after successful login or refresh")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

  @Schema(description = "Short-lived JWT used to authenticate API requests via the Authorization header",
          example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqYW5lLmRvZUBleGFtcGxlLmNvbSJ9.signature")
  private String accessToken;

  @Schema(description = "Long-lived token used to obtain a new token pair via POST /api/auth/refresh " +
          "(sent via the X-Refresh-Token header, not stored client-side in JS-readable storage if possible)",
          example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
  private String refreshToken;

  @Schema(description = "Type of the access token, always \"Bearer\"", example = "Bearer")
  private String tokenType = "Bearer";

  @Schema(description = "Access token lifetime in seconds from the moment of issuance", example = "3600")
  private long expiresIn;
}
