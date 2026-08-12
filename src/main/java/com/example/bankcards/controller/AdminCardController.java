package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.PageResponse;
import com.example.bankcards.entity.CardStatus;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@Tag(name = "Admin - Cards", description = "Card management for administrators (requires role ADMIN)")
public class AdminCardController {

    private final CardService cardService;

    @PostMapping
    @Operation(
            summary = "Issue a new card",
            description = "Creates a new ACTIVE card with a system-generated card number and zero balance, " +
                    "assigned to the given owner. The card number itself cannot be chosen by the caller."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Card created"),
            @ApiResponse(responseCode = "400", description = "Validation failed (missing owner, expiration not in the future)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Caller does not have role ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Owner (user) not found", content = @Content)
    })
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CreateCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request));
    }

    @GetMapping
    @Operation(
            summary = "List all cards in the system",
            description = "Returns a paginated, optionally filtered list of every card, across all owners."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of cards returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Caller does not have role ADMIN", content = @Content)
    })
    public ResponseEntity<PageResponse<CardResponse>> getAllCards(
            @Parameter(description = "Filter by owner (user) ID") @RequestParam(required = false) Long ownerId,
            @Parameter(description = "Filter by card status") @RequestParam(required = false) CardStatus status,
            @Parameter(description = "Filter by the last 4 digits of the card number") @RequestParam(required = false) String last4,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(cardService.getAllCards(ownerId, status, last4, pageable));
    }

    @GetMapping("/{cardId}")
    @Operation(
            summary = "Get any card by ID",
            description = "Returns details of any card in the system, regardless of owner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Caller does not have role ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Card not found", content = @Content)
    })
    public ResponseEntity<CardResponse> getCard(@Parameter(description = "ID of the card", required = true) @PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.getCardForAdmin(cardId));
    }

    @PostMapping("/{cardId}/block")
    @Operation(
            summary = "Block a card",
            description = "Sets the card's status to BLOCKED, preventing further transfers to/from it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card blocked"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Caller does not have role ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Card not found", content = @Content),
            @ApiResponse(responseCode = "400", description = "Card is already blocked or expired", content = @Content)
    })
    public ResponseEntity<CardResponse> blockCard(@Parameter(description = "ID of the card", required = true) @PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.blockCard(cardId));
    }

    @PostMapping("/{cardId}/activate")
    @Operation(
            summary = "Activate a card",
            description = "Sets the card's status to ACTIVE, re-enabling transfers to/from it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card activated"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Caller does not have role ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Card not found", content = @Content),
            @ApiResponse(responseCode = "400", description = "Card is expired and cannot be activated", content = @Content)
    })
    public ResponseEntity<CardResponse> activateCard(@Parameter(description = "ID of the card", required = true) @PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.activateCard(cardId));
    }

    @DeleteMapping("/{cardId}")
    @Operation(
            summary = "Delete a card",
            description = "Permanently removes the card from the system. This action cannot be undone."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Card deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Caller does not have role ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Card not found", content = @Content)
    })
    public ResponseEntity<Void> deleteCard(@Parameter(description = "ID of the card", required = true) @PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }
}

