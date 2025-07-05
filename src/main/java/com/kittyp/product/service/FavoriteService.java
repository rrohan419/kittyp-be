package com.kittyp.product.service;

import org.springframework.stereotype.Service;

import com.kittyp.common.model.PaginationModel;
import com.kittyp.product.dto.FavoriteProductDTO;

@Service
public interface FavoriteService {

    PaginationModel<FavoriteProductDTO> getUserFavorites(String userUuid, String category, Integer pageNumber, Integer pageSize);

    void addToFavorites(String userUuid, String productUuid, String category);

    public void removeFromFavorites(String userUuid, String productUuid);
} 