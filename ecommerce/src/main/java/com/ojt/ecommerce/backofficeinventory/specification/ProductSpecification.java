package com.ojt.ecommerce.backofficeinventory.specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ojt.ecommerce.entity.Product;
import com.ojt.ecommerce.entity.ProductVariant;
import com.ojt.ecommerce.enums.ProductStatus;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class ProductSpecification {

	public static Specification<Product> search(String keyword, Long categoryId, Long brandId, ProductStatus status,
			BigDecimal minPrice, BigDecimal maxPrice) {

		return (root, query, cb) -> {

			List<Predicate> predicates = new ArrayList<>();

			// ==========================================
			// 1. KEYWORD SEARCH
			// ==========================================
			if (keyword != null && !keyword.isBlank()) {

				String searchKeyword = "%" + keyword.trim().toLowerCase() + "%";

				Predicate productName = cb.like(cb.lower(root.get("productName")), searchKeyword);

				Predicate description = cb.like(cb.lower(root.get("description")), searchKeyword);

				predicates.add(cb.or(productName, description));
			}

			// ==========================================
			// 2. CATEGORY FILTER
			// ==========================================
			if (categoryId != null) {

				predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
			}

			// ==========================================
			// 3. BRAND FILTER
			// ==========================================
			if (brandId != null) {

				predicates.add(cb.equal(root.get("brand").get("brandId"), brandId));
			}

			// ==========================================
			// 4. PRODUCT STATUS FILTER
			// ==========================================
			if (status != null) {

				predicates.add(cb.equal(root.get("status"), status));
			}

			// ==========================================
			// 5. PRICE FILTER
			// Product -> variants -> sellingPrice
			// ==========================================
			if (minPrice != null || maxPrice != null) {

				Join<Product, ProductVariant> variant = root.join("variants", JoinType.INNER);

				// Minimum price
				if (minPrice != null) {

					predicates.add(cb.greaterThanOrEqualTo(variant.get("sellingPrice"), minPrice));
				}

				// Maximum price
				if (maxPrice != null) {

					predicates.add(cb.lessThanOrEqualTo(variant.get("sellingPrice"), maxPrice));
				}

				// Prevent duplicate products
				query.distinct(true);
			}

			// ==========================================
			// 6. COMBINE ALL CONDITIONS
			// ==========================================
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}