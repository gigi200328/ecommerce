package com.ojt.ecommerce.backofficeinventory.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ojt.ecommerce.entity.Brand;
import com.ojt.ecommerce.enums.BrandStatus;

import jakarta.persistence.criteria.Predicate;

public class BrandSpecification {

    public static Specification<Brand> search(
            String keyword,
            BrandStatus status) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // ==============================
            // KEYWORD SEARCH
            // ==============================

            if (keyword != null && !keyword.trim().isEmpty()) {

                String searchKeyword =
                        "%" + keyword.trim().toLowerCase() + "%";

                Predicate brandName =
                        cb.like(
                                cb.lower(
                                        root.get("brandName")
                                ),
                                searchKeyword
                        );

                Predicate description =
                        cb.like(
                                cb.lower(
                                        root.get("description")
                                ),
                                searchKeyword
                        );

                predicates.add(
                        cb.or(
                                brandName,
                                description
                        )
                );
            }

            // ==============================
            // STATUS FILTER
            // ==============================

            if (status != null) {

                predicates.add(
                        cb.equal(
                                root.get("status"),
                                status
                        )
                );
            }

            // ==============================
            // FINAL
            // ==============================

            return cb.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }
}