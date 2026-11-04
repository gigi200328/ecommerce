
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
			// Product Name, Description, SKU,
			// Brand Name, Category Name
			// ==========================================
			if (keyword != null && !keyword.isBlank()) {

				String searchKeyword = "%" + keyword.trim().toLowerCase() + "%";

				List<Predicate> keywordPredicates = new ArrayList<>();

				// Search Product Name
				keywordPredicates.add(cb.like(cb.lower(root.get("productName")), searchKeyword));

				// Search Description
				keywordPredicates.add(cb.like(cb.lower(root.get("description")), searchKeyword));

				// Search Brand Name
				keywordPredicates.add(cb.like(cb.lower(root.get("brand").get("brandName")), searchKeyword));

				// Search Category Name
				keywordPredicates.add(cb.like(cb.lower(root.get("category").get("categoryName")), searchKeyword));

				// Search SKU through Product Variants
				Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.LEFT);

				keywordPredicates.add(cb.like(cb.lower(variantJoin.get("sku")), searchKeyword));

				predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));

				query.distinct(true);
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
			// Product -> Variants -> Selling Price
			// ==========================================
			if (minPrice != null || maxPrice != null) {

				Join<Product, ProductVariant> priceVariantJoin = root.join("variants", JoinType.INNER);

				// Minimum Price
				if (minPrice != null) {

					predicates.add(cb.greaterThanOrEqualTo(priceVariantJoin.get("sellingPrice"), minPrice));
				}

				// Maximum Price
				if (maxPrice != null) {

					predicates.add(cb.lessThanOrEqualTo(priceVariantJoin.get("sellingPrice"), maxPrice));
				}

				query.distinct(true);
			}

			// ==========================================
			// 6. COMBINE ALL CONDITIONS
			// ==========================================
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
