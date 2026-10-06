package com.ojt.ecommerce.backofficeinventory.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ojt.ecommerce.entity.Brand;

import jakarta.persistence.criteria.Predicate;

public class BrandSpecification {

    public static Specification<Brand> search(
            String keyword
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // ==============================
            // KEYWORD SEARCH
            // ==============================
            if (keyword != null && !keyword.isBlank()) {

                String searchKeyword =
                        "%" + keyword.trim().toLowerCase() + "%";

                Predicate brandName =
                        cb.like(
                            cb.lower(root.get("brandName")),
                            searchKeyword
                        );

                Predicate description =
                        cb.like(
                            cb.lower(root.get("description")),
                            searchKeyword
                        );

                predicates.add(
                    cb.or(brandName, description)
                );
            }

            return cb.and(
                predicates.toArray(new Predicate[0])
            );
        };
    }
}