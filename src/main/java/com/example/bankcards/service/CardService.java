package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.PageResponse;
import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.dto.card.TransferResponse;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface CardService {

    // Admin operations
    CardResponse createCard(CreateCardRequest request);

    CardResponse blockCard(Long cardId);

    CardResponse activateCard(Long cardId);

    void deleteCard(Long cardId);

    PageResponse<CardResponse> getAllCards(Long ownerId, CardStatus status, String last4, Pageable pageable);

    CardResponse getCardForAdmin(Long cardId);

    // Self-service (owner-scoped) operations
    PageResponse<CardResponse> getMyCards(Long ownerId, CardStatus status, String last4, Pageable pageable);

    CardResponse getMyCard(Long cardId, Long ownerId);

    BigDecimal getBalance(Long cardId, Long ownerId);

    CardResponse requestBlock(Long cardId, Long ownerId);

    TransferResponse transfer(Long ownerId, TransferRequest request);
}
