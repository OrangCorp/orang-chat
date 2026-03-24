package com.orang.messageservice.dto;

import com.orang.shared.constants.PaginationConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequestDto {

    @Min(value = 0, message = "Page number cannot be negative")
    private int page = PaginationConstants.DEFAULT_PAGE_NUMBER;

    @Min(value = PaginationConstants.MIN_PAGE_SIZE,
            message = "Page size must be at least " + PaginationConstants.MIN_PAGE_SIZE)
    @Max(value = PaginationConstants.MAX_PAGE_SIZE,
            message = "Page size cannot exceed " + PaginationConstants.MAX_PAGE_SIZE)
    private int size = PaginationConstants.DEFAULT_PAGE_SIZE;

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }

    public Pageable toPageable(Sort sort) {
        return PageRequest.of(page, size, sort);
    }
}

