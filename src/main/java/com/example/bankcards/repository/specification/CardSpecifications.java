package com.example.bankcards.repository.specification;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class CardSpecifications {

    private CardSpecifications() {
    }

    public static Specification<Card> withFilters(Long ownerId, CardStatus status, String last4) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (ownerId != null) {
                predicates.add(cb.equal(root.get("owner").get("id"), ownerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(last4)) {
                predicates.add(cb.like(root.get("lastFourDigits"), "%" + last4.trim() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
