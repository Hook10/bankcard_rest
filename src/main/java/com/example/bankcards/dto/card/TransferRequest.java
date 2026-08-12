package com.example.bankcards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Payload for transferring funds between two of the caller's own cards")
public record TransferRequest(
        @Schema(description = "ID of the card to debit", example = "1")
        @NotNull(message = "Source card id is required")
        Long fromCardId,

        @Schema(description = "ID of the card to credit", example = "2")
        @NotNull(message = "Destination card id is required")
        Long toCardId,

        @Schema(description = "Amount to transfer, must be greater than zero", example = "50.00")
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount
) {
}
