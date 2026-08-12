package com.example.bankcards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Result of a successful transfer between two of the caller's own cards")
public record TransferResponse(
        @Schema(description = "ID of the debited card", example = "1")
        Long fromCardId,

        @Schema(description = "ID of the credited card", example = "2")
        Long toCardId,

        @Schema(description = "Amount transferred", example = "50.00")
        BigDecimal amount,

        @Schema(description = "New balance of the debited card after the transfer", example = "950.00")
        BigDecimal fromCardNewBalance,

        @Schema(description = "New balance of the credited card after the transfer", example = "1300.75")
        BigDecimal toCardNewBalance
) {
}
