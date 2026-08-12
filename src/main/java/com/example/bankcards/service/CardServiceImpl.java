package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.PageResponse;
import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.dto.card.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.repository.specification.CardSpecifications;
import com.example.bankcards.util.CardNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardNumberGenerator cardNumberGenerator;

    @Override
    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new UserNotFoundException(request.ownerId()));

        String cardNumber = cardNumberGenerator.generate();
        Card card = Card.issueNew(cardNumber, owner, request.expirationDate());
        return CardResponse.from(cardRepository.save(card));
    }

    @Override
    @Transactional
    public CardResponse blockCard(Long cardId) {
        Card card = getCardOrThrow(cardId);
        card.setStatus(CardStatus.BLOCKED);
        card.setUpdatedAt(LocalDateTime.now());
        return CardResponse.from(card);
    }

    @Override
    @Transactional
    public CardResponse activateCard(Long cardId) {
        Card card = getCardOrThrow(cardId);
        if (card.getExpirationDate().isBefore(LocalDate.now())) {
            throw new InvalidCardOperationException("Cannot activate an expired card");
        }
        card.setStatus(CardStatus.ACTIVE);
        card.setUpdatedAt(LocalDateTime.now());
        return CardResponse.from(card);
    }

    @Override
    @Transactional
    public void deleteCard(Long cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException(cardId);
        }
        cardRepository.deleteById(cardId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getAllCards(Long ownerId, CardStatus status, String last4, Pageable pageable) {
        Page<CardResponse> page = cardRepository
                .findAll(CardSpecifications.withFilters(ownerId, status, last4), pageable)
                .map(CardResponse::from);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getCardForAdmin(Long cardId) {
        return CardResponse.from(getCardOrThrow(cardId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getMyCards(Long ownerId, CardStatus status, String last4, Pageable pageable) {
        Page<CardResponse> page = cardRepository
                .findAll(CardSpecifications.withFilters(ownerId, status, last4), pageable)
                .map(CardResponse::from);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getMyCard(Long cardId, Long ownerId) {
        return CardResponse.from(getOwnedCardOrThrow(cardId, ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long cardId, Long ownerId) {
        return getOwnedCardOrThrow(cardId, ownerId).getBalance();
    }

    @Override
    @Transactional
    public CardResponse requestBlock(Long cardId, Long ownerId) {
        Card card = getOwnedCardOrThrow(cardId, ownerId);
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new InvalidCardOperationException("Card is already blocked");
        }
        card.setStatus(CardStatus.BLOCKED);
        card.setUpdatedAt(LocalDateTime.now());
        return CardResponse.from(card);
    }

    @Override
    @Transactional
    public TransferResponse transfer(Long ownerId, TransferRequest request) {
        if (request.fromCardId().equals(request.toCardId())) {
            throw new InvalidCardOperationException("Cannot transfer to the same card");
        }

        Long firstId = Math.min(request.fromCardId(), request.toCardId());
        Long secondId = Math.max(request.fromCardId(), request.toCardId());
        Card first = cardRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new CardNotFoundException(firstId));
        Card second = cardRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new CardNotFoundException(secondId));

        Card from = request.fromCardId().equals(firstId) ? first : second;
        Card to = request.fromCardId().equals(firstId) ? second : first;

        if (!from.getOwner().getId().equals(ownerId) || !to.getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("You can only transfer between your own cards");
        }
        if (from.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardOperationException("Source card is not active");
        }
        if (to.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardOperationException("Destination card is not active");
        }
        if (from.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds on source card");
        }

        from.setBalance(from.getBalance().subtract(request.amount()));
        to.setBalance(to.getBalance().add(request.amount()));
        LocalDateTime now = LocalDateTime.now();
        from.setUpdatedAt(now);
        to.setUpdatedAt(now);

        return new TransferResponse(from.getId(), to.getId(), request.amount(), from.getBalance(), to.getBalance());
    }

    private Card getCardOrThrow(Long cardId) {
        return cardRepository.findById(cardId).orElseThrow(() -> new CardNotFoundException(cardId));
    }

    private Card getOwnedCardOrThrow(Long cardId, Long ownerId) {
        Card card = getCardOrThrow(cardId);
        if (!card.getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("You do not have access to this card");
        }
        return card;
    }
}
