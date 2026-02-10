package com.enterprise.openfinance.personalfinancialdata.domain.model;

import java.util.List;
import java.util.Optional;

public record PagedResult<T>(
        List<T> items,
        int page,
        int pageSize,
        long totalRecords
) {

    public PagedResult {
        if (items == null) {
            throw new IllegalArgumentException("items is required");
        }
        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1");
        }
        if (totalRecords < 0) {
            throw new IllegalArgumentException("totalRecords must be >= 0");
        }
        items = List.copyOf(items);
    }

    public int totalPages() {
        if (totalRecords == 0) {
            return 1;
        }
        return (int) ((totalRecords + pageSize - 1) / pageSize);
    }

    public boolean hasNext() {
        return page < totalPages();
    }

    public Optional<Integer> nextPage() {
        return hasNext() ? Optional.of(page + 1) : Optional.empty();
    }
}
