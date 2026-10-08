
package com.ojt.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @Column(
        name = "tag_name",
        nullable = false,
        length = 100,
        unique = true
    )
    private String tagName;

    // Created information
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "created_by",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tag_created_by")
    )
    private User createdBy;

    // Modified information
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "modified_by",
        foreignKey = @ForeignKey(name = "fk_tag_modified_by")
    )
    private User modifiedBy;
}

