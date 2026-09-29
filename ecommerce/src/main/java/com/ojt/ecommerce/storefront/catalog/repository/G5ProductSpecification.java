package com.ojt.ecommerce.storefront.catalog.repository;

import com.ojt.ecommerce.entity.Inventory;
import com.ojt.ecommerce.entity.Product;
import com.ojt.ecommerce.entity.ProductTag;
import com.ojt.ecommerce.entity.ProductVariant;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

public class G5ProductSpecification {

    public static Specification<Product> searchProducts(List<Long> categoryIds, Long brandId, String search,
                                                        BigDecimal minPrice, BigDecimal maxPrice,
                                                        String availability, List<String> tags) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get("status"), "ACTIVE");

            if (categoryIds != null && !categoryIds.isEmpty()) {
                p = cb.and(p, root.get("category").get("categoryId").in(categoryIds));
            }
            if (brandId != null) {
                p = cb.and(p, cb.equal(root.get("brand").get("brandId"), brandId));
            }
            
            // Deduplicate results caused by joins
            query.distinct(true);

            if (search != null && !search.trim().isEmpty()) {
                String likeSearch = "%" + search.toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("productName")), likeSearch);
                
                Subquery<Long> variantSub = query.subquery(Long.class);
                Root<ProductVariant> vRoot = variantSub.from(ProductVariant.class);
                variantSub.select(vRoot.get("product").get("productId"));
                variantSub.where(cb.like(cb.lower(vRoot.get("sku")), likeSearch));
                
                Predicate skuMatch = cb.in(root.get("productId")).value(variantSub);
                p = cb.and(p, cb.or(nameMatch, skuMatch));
            }

            if (minPrice != null || maxPrice != null) {
                Subquery<Long> priceSub = query.subquery(Long.class);
                Root<ProductVariant> pv = priceSub.from(ProductVariant.class);
                priceSub.select(pv.get("product").get("productId"));
                
                Expression<BigDecimal> effectivePrice = cb.coalesce(pv.get("discountPrice"), pv.get("sellingPrice"));
                Predicate pricePred = cb.equal(pv.get("status"), "ACTIVE");
                
                if (minPrice != null) {
                    pricePred = cb.and(pricePred, cb.greaterThanOrEqualTo(effectivePrice, minPrice));
                }
                if (maxPrice != null) {
                    pricePred = cb.and(pricePred, cb.lessThanOrEqualTo(effectivePrice, maxPrice));
                }
                priceSub.where(pricePred);
                p = cb.and(p, cb.in(root.get("productId")).value(priceSub));
            }

            if (availability != null && !availability.equalsIgnoreCase("ALL")) {
                Subquery<Long> stockSub = query.subquery(Long.class);
                Root<Inventory> inv = stockSub.from(Inventory.class);
                stockSub.select(inv.get("variant").get("product").get("productId"));
                stockSub.where(
                    cb.and(
                        cb.equal(inv.get("variant").get("status"), "ACTIVE"),
                        cb.greaterThan(inv.get("quantity"), 0)
                    )
                );
                
                if (availability.equalsIgnoreCase("IN_STOCK")) {
                    p = cb.and(p, cb.in(root.get("productId")).value(stockSub));
                } else if (availability.equalsIgnoreCase("SOLD_OUT")) {
                    p = cb.and(p, cb.not(cb.in(root.get("productId")).value(stockSub)));
                }
            }

            if (tags != null && !tags.isEmpty()) {
                for (String tag : tags) {
                    Subquery<Long> tagSub = query.subquery(Long.class);
                    Root<ProductTag> pt = tagSub.from(ProductTag.class);
                    tagSub.select(pt.get("product").get("productId"));
                    tagSub.where(cb.equal(pt.get("tag").get("tagName"), tag));
                    p = cb.and(p, cb.in(root.get("productId")).value(tagSub));
                }
            }

            return p;
        };
    }
}
