package com.example.bankcards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Payload for issuing a new card to a user (admin-only)")
public record CreateCardRequest(
        @Schema(description = "ID of the user who will own the card", example = "2")
        @NotNull(message = "Owner id is required")
        Long ownerId,

        @Schema(description = "Card expiration date, must be in the future", example = "2028-12-31")
        @NotNull(message = "Expiration date is required")
        @Future(message = "Expiration date must be in the future")
        LocalDate expirationDate
) {
}
