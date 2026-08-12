package com.example.bankcards.service;

import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardNumberGenerator cardNumberGenerator;

    private CardServiceImpl cardService;

    @BeforeEach
    void setUp() {
        cardService = new CardServiceImpl(cardRepository, userRepository, cardNumberGenerator);
    }

    @Test
    void createCard_savesIssuedCardForExistingOwner() {
        User owner = user(10L);
        CreateCardRequest request = new CreateCardRequest(10L, LocalDate.now().plusYears(3));
        when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(cardNumberGenerator.generate()).thenReturn("4111111111111111");
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = cardService.createCard(request);

        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(cardCaptor.capture());
        Card saved = cardCaptor.getValue();

        assertThat(saved.getOwner().getId()).isEqualTo(10L);
        assertThat(saved.getExpirationDate()).isEqualTo(request.expirationDate());
        assertThat(saved.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(saved.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(response.ownerId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(CardStatus.ACTIVE);
        assertThat(response.maskedNumber()).endsWith("1111");
    }

    @Test
    void createCard_throwsWhenOwnerMissing() {
        CreateCardRequest request = new CreateCardRequest(404L, LocalDate.now().plusYears(3));
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.createCard(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("404");

        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void deleteCard_deletesWhenCardExists() {
        when(cardRepository.existsById(7L)).thenReturn(true);

        cardService.deleteCard(7L);

        verify(cardRepository).deleteById(7L);
    }

    @Test
    void deleteCard_throwsWhenCardMissing() {
        when(cardRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> cardService.deleteCard(7L))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("7");

        verify(cardRepository, never()).deleteById(7L);
    }

    @Test
    void requestBlock_throwsWhenCardAlreadyBlocked() {
        Card card = activeCard(100L, 20L, "4222222222222222", BigDecimal.TEN);
        card.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(100L)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.requestBlock(100L, 20L))
                .isInstanceOf(InvalidCardOperationException.class)
                .hasMessageContaining("already blocked");
    }

    @Test
    void requestBlock_throwsWhenOwnerDoesNotMatch() {
        Card card = activeCard(100L, 20L, "4222222222222222", BigDecimal.TEN);
        when(cardRepository.findById(100L)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.requestBlock(100L, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("do not have access");
    }

    @Test
    void transfer_throwsWhenSameCardIdsUsed() {
        TransferRequest request = new TransferRequest(1L, 1L, new BigDecimal("10.00"));

        assertThatThrownBy(() -> cardService.transfer(5L, request))
                .isInstanceOf(InvalidCardOperationException.class)
                .hasMessageContaining("same card");
    }

    @Test
    void transfer_throwsWhenAnyCardNotOwnedByCaller() {
        Card from = activeCard(1L, 10L, "4333333333333333", new BigDecimal("100.00"));
        Card to = activeCard(2L, 11L, "4444444444444444", new BigDecimal("20.00"));
        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(to));

        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("5.00"));

        assertThatThrownBy(() -> cardService.transfer(10L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("own cards");
    }

    @Test
    void transfer_throwsWhenSourceBalanceInsufficient() {
        Card from = activeCard(1L, 10L, "4333333333333333", new BigDecimal("10.00"));
        Card to = activeCard(2L, 10L, "4444444444444444", new BigDecimal("20.00"));
        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(to));

        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("50.00"));

        assertThatThrownBy(() -> cardService.transfer(10L, request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    void transfer_movesMoneyAndReturnsNewBalances() {
        Card from = activeCard(1L, 10L, "4333333333333333", new BigDecimal("100.00"));
        Card to = activeCard(2L, 10L, "4444444444444444", new BigDecimal("20.00"));
        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(to));

        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("15.50"));
        var result = cardService.transfer(10L, request);

        assertThat(result.fromCardId()).isEqualTo(1L);
        assertThat(result.toCardId()).isEqualTo(2L);
        assertThat(result.amount()).isEqualByComparingTo("15.50");
        assertThat(result.fromCardNewBalance()).isEqualByComparingTo("84.50");
        assertThat(result.toCardNewBalance()).isEqualByComparingTo("35.50");

        assertThat(from.getBalance()).isEqualByComparingTo("84.50");
        assertThat(to.getBalance()).isEqualByComparingTo("35.50");
        assertThat(from.getUpdatedAt()).isNotNull();
        assertThat(to.getUpdatedAt()).isNotNull();
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        user.setFullName("User " + id);
        user.setPassword("encoded");
        return user;
    }

    private static Card activeCard(Long cardId, Long ownerId, String cardNumber, BigDecimal balance) {
        Card card = Card.issueNew(cardNumber, user(ownerId), LocalDate.now().plusYears(2));
        card.setId(cardId);
        card.setBalance(balance);
        card.setStatus(CardStatus.ACTIVE);
        return card;
    }
}
