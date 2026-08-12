package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.PageResponse;
import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.dto.card.TransferResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.security.UserDetailsImpl;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Self-service endpoints for a user's own cards (requires role USER or ADMIN)")
public class CardController {

    private final CardService cardService;

    @GetMapping
    @Operation(
            summary = "List my cards",
            description = "Returns a paginated, optionally filtered list of cards owned by the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of cards returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content)
    })
    public ResponseEntity<PageResponse<CardResponse>> getMyCards(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Filter by card status") @RequestParam(required = false) CardStatus status,
            @Parameter(description = "Filter by the last 4 digits of the card number") @RequestParam(required = false) String last4,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(cardService.getMyCards(principal.getId(), status, last4, pageable));
    }

    @GetMapping("/{cardId}")
    @Operation(
            summary = "Get one of my cards",
            description = "Returns details of a single card. Fails if the card does not belong to the caller."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content),
            @ApiResponse(responseCode = "404", description = "Card not found or not owned by the caller", content = @Content)
    })
    public ResponseEntity<CardResponse> getMyCard(@AuthenticationPrincipal UserDetailsImpl principal,
                                                   @Parameter(description = "ID of the card", required = true) @PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.getMyCard(cardId, principal.getId()));
    }

    @GetMapping("/{cardId}/balance")
    @Operation(
            summary = "Get the balance of one of my cards",
            description = "Returns the current balance for a card owned by the caller."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content),
            @ApiResponse(responseCode = "404", description = "Card not found or not owned by the caller", content = @Content)
    })
    public ResponseEntity<Map<String, BigDecimal>> getBalance(@AuthenticationPrincipal UserDetailsImpl principal,
                                                                @Parameter(description = "ID of the card", required = true) @PathVariable Long cardId) {
        return ResponseEntity.ok(Map.of("balance", cardService.getBalance(cardId, principal.getId())));
    }

    @PostMapping("/{cardId}/block-request")
    @Operation(
            summary = "Request that one of my cards be blocked",
            description = "Marks the card as BLOCKED. Intended for a user reporting a lost/stolen/compromised card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card blocked"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content),
            @ApiResponse(responseCode = "404", description = "Card not found or not owned by the caller", content = @Content),
            @ApiResponse(responseCode = "400", description = "Card is already blocked or expired", content = @Content)
    })
    public ResponseEntity<CardResponse> requestBlock(@AuthenticationPrincipal UserDetailsImpl principal,
                                                       @Parameter(description = "ID of the card", required = true) @PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.requestBlock(cardId, principal.getId()));
    }

    @PostMapping("/transfer")
    @Operation(
            summary = "Transfer funds between two of my own cards",
            description = "Moves the given amount from `fromCardId` to `toCardId`. Both cards must belong to the " +
                    "authenticated caller and be ACTIVE; the source card must have sufficient balance."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer completed, new balances returned"),
            @ApiResponse(responseCode = "400", description = "Validation failed (missing fields, non-positive amount)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content),
            @ApiResponse(responseCode = "404", description = "One or both cards not found or not owned by the caller", content = @Content),
            @ApiResponse(responseCode = "422", description = "Insufficient funds on the source card", content = @Content)
    })
    public ResponseEntity<TransferResponse> transfer(@AuthenticationPrincipal UserDetailsImpl principal,
                                                       @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(cardService.transfer(principal.getId(), request));
    }
}

