package com.example.bankcards.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CardNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOTAL_LENGTH = 16;
    private static final int GENERATE_COUNT = TOTAL_LENGTH - 1;

    public String generate() {
        StringBuilder digits = new StringBuilder(TOTAL_LENGTH);

        // Start with '4' for Visa identification
        digits.append('4');

        for (int i = 1; i < GENERATE_COUNT; i++) {
            digits.append(RANDOM.nextInt(10));
        }

        int checkDigit = calculateLuhnCheckDigit(digits.toString());
        digits.append(checkDigit);

        return digits.toString();
    }

    private int calculateLuhnCheckDigit(String numberWithoutCheckDigit) {
        int sum = 0;

        boolean shouldDouble = true;

        for (int i = numberWithoutCheckDigit.length() - 1; i >= 0; i--) {
            int digit = numberWithoutCheckDigit.charAt(i) - '0';

            if (shouldDouble) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            shouldDouble = !shouldDouble;
        }

        return (10 - (sum % 10)) % 10;
    }
}
