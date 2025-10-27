package com.kittyp.product.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.product.dto.FavoriteProductDTO;
import com.kittyp.product.entity.Product;
import com.kittyp.product.entity.UserFavouriteProducts;
import com.kittyp.product.repository.ProductRepository;
import com.kittyp.product.repository.UserFavouriteProductsRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final UserFavouriteProductsRepository userFavouriteProductsRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public PaginationModel<FavoriteProductDTO> getUserFavorites(String userUuid, String category, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        
        Optional<UserFavouriteProducts> userFavorites = category != null && !category.isEmpty() 
            ? userFavouriteProductsRepository.findByUserUuidAndCategory(userUuid, category)
            : userFavouriteProductsRepository.findByUserUuid(userUuid);

        if (userFavorites.isEmpty() || userFavorites.get().getProductUuids() == null || userFavorites.get().getProductUuids().isEmpty()) {
            return createEmptyPaginationModel(pageable);
        }

        List<String> productUuids = userFavorites.get().getProductUuids();
        List<Product> products = productRepository.findAllByUuidIn(productUuids);
        
        List<FavoriteProductDTO> favoriteProducts = products.stream()
            .map(product -> {
                FavoriteProductDTO dto = new FavoriteProductDTO();
                dto.setUuid(product.getUuid());
                dto.setName(product.getName());
                dto.setDescription(product.getDescription());
                dto.setPrice(product.getPrice().doubleValue());
                dto.setCategory(product.getCategory());
                dto.setImageUrls(new ArrayList<>(product.getProductImageUrls()));
                dto.setStatus(product.getStatus());
                return dto;
            }).toList();

        Page<FavoriteProductDTO> page = new PageImpl<>(favoriteProducts, pageable, favoriteProducts.size());
        return convertToPaginationModel(page);
    }

    @Override
    @Transactional
    public void addToFavorites(String userUuid, String productUuid, String category) {
        Product product = productRepository.findByUuid(productUuid)
            .orElseThrow(() -> new CustomException("Product not found", HttpStatus.NOT_FOUND));

        UserFavouriteProducts userFavorites = userFavouriteProductsRepository.findByUserUuid(userUuid)
            .orElseGet(() -> UserFavouriteProducts.builder()
                .userUuid(userUuid)
                .favouriteCategory(category)
                .productUuids(new ArrayList<>())
                .build());

        if (userFavorites.getProductUuids() == null) {
            userFavorites.setProductUuids(new ArrayList<>());
        }

        if (!userFavorites.getProductUuids().contains(productUuid)) {
            userFavorites.getProductUuids().add(productUuid);
            userFavouriteProductsRepository.save(userFavorites);
        }
    }

    @Override
    @Transactional
    public void removeFromFavorites(String userUuid, String productUuid) {
        UserFavouriteProducts userFavorites = userFavouriteProductsRepository.findByUserUuid(userUuid)
            .orElseThrow(() -> new CustomException("No favorites found for user", HttpStatus.NOT_FOUND));

        if (userFavorites.getProductUuids() != null) {
            userFavorites.getProductUuids().remove(productUuid);
            userFavouriteProductsRepository.save(userFavorites);
        }
    }

    private PaginationModel<FavoriteProductDTO> createEmptyPaginationModel(Pageable pageable) {
        Page<FavoriteProductDTO> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
        return convertToPaginationModel(emptyPage);
    }

    private PaginationModel<FavoriteProductDTO> convertToPaginationModel(Page<FavoriteProductDTO> page) {
        PaginationModel<FavoriteProductDTO> paginationModel = new PaginationModel<>();
        paginationModel.setModels(page.getContent());
        paginationModel.setIsFirst(page.isFirst());
        paginationModel.setIsLast(page.isLast());
        paginationModel.setTotalElements(page.getTotalElements());
        paginationModel.setTotalPages(page.getTotalPages());
        return paginationModel;
    }
} 