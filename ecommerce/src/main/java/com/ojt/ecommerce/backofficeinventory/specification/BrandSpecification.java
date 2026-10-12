
package com.ojt.ecommerce.backofficeinventory.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ojt.ecommerce.entity.Brand;
import com.ojt.ecommerce.entity.BrandCategory;
import com.ojt.ecommerce.entity.Category;
import com.ojt.ecommerce.enums.BrandStatus;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class BrandSpecification {

    private BrandSpecification() {
        // Prevent object creation
    }

    public static Specification<Brand> search(
            String keyword,
            BrandStatus status) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // ==============================
            // KEYWORD SEARCH
            // Brand Name, Description, Category Name
            // ==============================

            if (keyword != null && !keyword.trim().isEmpty()) {

                String searchKeyword =
                        "%" + keyword.trim().toLowerCase() + "%";

                // Join Brand -> BrandCategory
                Join<Brand, BrandCategory> brandCategoryJoin =
                        root.join(
                                "brandCategories",
                                JoinType.LEFT
                        );

                // Join BrandCategory -> Category
                Join<BrandCategory, Category> categoryJoin =
                        brandCategoryJoin.join(
                                "category",
                                JoinType.LEFT
                        );

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

                Predicate categoryName =
                        cb.like(
                                cb.lower(categoryJoin.get("categoryName")),
                                searchKeyword
                        );

                predicates.add(
                        cb.or(
                                brandName,
                                description,
                                categoryName
                        )
                );

                // Avoid duplicate brands caused by joins
                query.distinct(true);
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

