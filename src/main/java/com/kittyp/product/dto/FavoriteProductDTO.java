package com.kittyp.product.dto;

import java.util.List;

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
public class FavoriteProductDTO {
    private String uuid;
    private String name;
    private String description;
    private Double price;
    private String category;
    private List<String> imageUrls;
} 