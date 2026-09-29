package com.ojt.ecommerce.storefront.catalog.dto;
import lombok.Builder;
import lombok.Data;
import java.util.List;
@Data @Builder public class PageResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int size;
    private int number;
    private boolean first;
    private boolean last;
    private boolean empty;
}