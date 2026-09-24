package com.ojt.ecommerce.storefront.catalog.repository;

import com.ojt.ecommerce.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface G5TagRepository extends JpaRepository<Tag, Long> {
}
