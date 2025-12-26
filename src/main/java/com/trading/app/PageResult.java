package com.trading.app;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResult<T> {
    private List<T> data;
    private int currentPage;
    private int totalPages;
    private int totalItems;
}
