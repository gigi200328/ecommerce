
package com.ojt.ecommerce.backofficeinventory.repository;

import com.ojt.ecommerce.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    boolean existsByTagNameIgnoreCase(String tagName);

    Optional<Tag> findByTagNameIgnoreCase(String tagName);
}

