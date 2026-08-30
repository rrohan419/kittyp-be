package com.kittyp.common.util;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.kittyp.common.model.PaginationModel;

public final class PaginationSupport {

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    private PaginationSupport() {}

    public static Pageable pageable(Integer pageNumber, Integer pageSize, String sortProperty) {
        int page = pageNumber == null || pageNumber < 1 ? 1 : pageNumber;
        int size = clampSize(pageSize);
        return PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, sortProperty));
    }

    public static <T> PaginationModel<T> fromPage(Page<T> page) {
        PaginationModel<T> model = new PaginationModel<>();
        model.setModels(page.getContent());
        model.setTotalElements(page.getTotalElements());
        model.setTotalPages(page.getTotalPages());
        model.setIsFirst(page.isFirst());
        model.setIsLast(page.isLast());
        model.setPageNumber(page.getNumber() + 1);
        model.setPageSize(page.getSize());
        return model;
    }

    public static <T> PaginationModel<T> slice(List<T> all, Integer pageNumber, Integer pageSize) {
        int page = pageNumber == null || pageNumber < 1 ? 1 : pageNumber;
        int size = clampSize(pageSize);
        int total = all == null ? 0 : all.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil(total / (double) size);
        int from = (page - 1) * size;
        List<T> models = all == null || from >= total ? List.of() : all.subList(from, Math.min(from + size, total));
        PaginationModel<T> model = new PaginationModel<>();
        model.setModels(models);
        model.setTotalElements((long) total);
        model.setTotalPages(totalPages);
        model.setIsFirst(page <= 1);
        model.setIsLast(total == 0 || page >= totalPages);
        model.setPageNumber(page);
        model.setPageSize(size);
        return model;
    }

    public static int clampSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(MAX_SIZE, pageSize);
    }
}
