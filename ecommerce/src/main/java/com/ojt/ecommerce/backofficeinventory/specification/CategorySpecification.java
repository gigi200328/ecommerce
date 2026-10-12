package com.ojt.ecommerce.backofficeinventory.specification;

import org.springframework.data.jpa.domain.Specification;
import com.ojt.ecommerce.entity.Category;

public class CategorySpecification {

	public static Specification<Category> search(String keyword) {
	    return (root, query, cb) -> {
	        if (keyword == null || keyword.isBlank()) {
	            return cb.conjunction();
	        }

	        String pattern = "%" + keyword.toLowerCase().trim() + "%";

	        // Parent Category နဲ့ Join လုပ်ခြင်း
	        var parentJoin = root.join("parent", jakarta.persistence.criteria.JoinType.LEFT);

	        return cb.or(
	            cb.like(cb.lower(root.get("categoryName")), pattern),
	            cb.like(cb.lower(root.get("description")), pattern),
	            cb.like(cb.lower(parentJoin.get("categoryName")), pattern)
	        );
	    };
	}
}