package com.example.bankcards.dto.card;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Public representation of a bank card")
public record CardResponse(
        @Schema(description = "Card ID", example = "1")
        Long id,

        @Schema(description = "Masked card number — only the last 4 digits are visible", example = "**** **** **** 1234")
        String maskedNumber,

        @Schema(description = "ID of the user who owns this card", example = "2")
        Long ownerId,

        @Schema(description = "Full name of the card owner", example = "Jane Doe")
        String ownerFullName,

        @Schema(description = "Card expiration date", example = "2028-12-31")
        LocalDate expirationDate,

        @Schema(description = "Current lifecycle status of the card")
        CardStatus status,

        @Schema(description = "Current balance available on the card", example = "1250.75")
        BigDecimal balance,

        @Schema(description = "Timestamp the card was created")
        LocalDateTime createdAt
) {
    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getMaskedNumber(),
                card.getOwner().getId(),
                card.getOwner().getFullName(),
                card.getExpirationDate(),
                card.getStatus(),
                card.getBalance(),
                card.getCreatedAt()
        );
    }
}
