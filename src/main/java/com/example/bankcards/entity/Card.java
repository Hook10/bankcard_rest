package com.example.bankcards.entity;

import com.example.bankcards.util.CardNumberConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter
@Setter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = CardNumberConverter.class)
    @Column(name = "card_number", nullable = false, unique = true, length = 512)
    @Setter(AccessLevel.NONE)
    private String cardNumber;

    @Column(name = "last_four_digits", nullable = false, length = 4)
    @Setter(AccessLevel.NONE)
    private String lastFourDigits;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public String getMaskedNumber() {
        return "**** **** **** " + lastFourDigits;
    }

    public static Card issueNew(String plainCardNumber, User owner, LocalDate expirationDate) {
        if (plainCardNumber == null || plainCardNumber.length() < 4) {
            throw new IllegalArgumentException("Card number must have at least 4 digits");
        }
        String last4 = plainCardNumber.substring(plainCardNumber.length() - 4);
        return Card.builder()
                .cardNumber(plainCardNumber)
                .lastFourDigits(last4)
                .owner(owner)
                .expirationDate(expirationDate)
                .status(CardStatus.ACTIVE)
                .build();
    }
}
