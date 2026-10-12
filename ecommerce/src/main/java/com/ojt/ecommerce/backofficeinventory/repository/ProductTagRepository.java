package com.ojt.ecommerce.backofficeinventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.ProductTag;
import com.ojt.ecommerce.entity.id.ProductTagId;

@Repository
public interface ProductTagRepository extends JpaRepository<ProductTag, ProductTagId> {

    List<ProductTag> findByProductProductId(Long productId);

    void deleteByProductProductId(Long productId);

    void deleteByProductProductIdAndTagTagId(Long productId, Long tagId);

    boolean existsByProductProductIdAndTagTagId(Long productId, Long tagId);
}
