/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class PaginationModel<T> {

    private Integer totalPages;

    private Long totalElements;

    private Long overAllTotalElements;

    private Boolean isFirst;

    private Boolean isLast;

    private List<T> models;

    private Integer pageNumber;

    private Integer pageSize;

    public PaginationModel(Integer totalPages, Long totalElements, Boolean isFirst, Boolean isLast) {
        super();
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.isFirst = isFirst;
        this.isLast = isLast;
    }

    public PaginationModel(Long totalElements, List<T> models) {
        super();
        this.totalElements = totalElements;
        this.models = models;
    }

}