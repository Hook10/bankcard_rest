package com.example.util;

import com.example.bankcards.util.CardNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardNumberGeneratorTest {

  private CardNumberGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new CardNumberGenerator();
  }

  @Test
  @DisplayName("Should generate a card number with exactly 16 characters")
  void shouldGenerateCorrectLength() {
    String cardNumber = generator.generate();

    assertThat(cardNumber).hasSize(16);
  }

  @Test
  @DisplayName("Should start with '4' for Visa identification")
  void shouldStartWithVisaPrefix() {
    String cardNumber = generator.generate();

    assertThat(cardNumber).startsWith("4");
  }

  @Test
  @DisplayName("Should contain only numeric digits")
  void shouldContainOnlyDigits() {
    String cardNumber = generator.generate();

    assertThat(cardNumber).containsOnlyDigits();
  }

  @RepeatedTest(100)
  @DisplayName("Should always generate a valid Luhn-compliant card number")
  void shouldPassLuhnCheckRepeatedly() {
    String cardNumber = generator.generate();

    assertThat(isValidLuhn(cardNumber))
        .withFailMessage("Generated card number %s failed Luhn validation", cardNumber)
        .isTrue();
  }

  private boolean isValidLuhn(String cardNumber) {
    if (cardNumber == null || cardNumber.length() != 16) {
      return false;
    }

    int sum = 0;
    boolean shouldDouble = false; // False because we start from the check digit (rightmost)

    for (int i = cardNumber.length() - 1; i >= 0; i--) {
      int digit = cardNumber.charAt(i) - '0';

      if (shouldDouble) {
        digit *= 2;
        if (digit > 9) {
          digit -= 9;
        }
      }

      sum += digit;
      shouldDouble = !shouldDouble;
    }

    return (sum % 10 == 0);
  }
}
