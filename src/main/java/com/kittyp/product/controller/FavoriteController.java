package com.kittyp.product.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.kittyp.auth.util.SecurityContextUtils;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.product.dto.FavoriteProductDTO;
import com.kittyp.product.service.FavoriteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final SecurityContextUtils securityContextUtils;

    @GetMapping
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<ApiResponse<PaginationModel<FavoriteProductDTO>>> getUserFavorites(
            @RequestParam String userUuid,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        securityContextUtils.requireSelfOrAdmin(userUuid);
        PaginationModel<FavoriteProductDTO> favorites = favoriteService.getUserFavorites(userUuid, category, pageNumber,
                pageSize);
        return ResponseEntity.ok(ApiResponse.success(favorites));
    }

    @PostMapping("/{productUuid}")
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<ApiResponse<Void>> addToFavorites(
            @RequestParam String userUuid,
            @PathVariable String productUuid,
            @RequestParam(required = false) String category) {
        securityContextUtils.requireSelfOrAdmin(userUuid);
        favoriteService.addToFavorites(userUuid, productUuid, category);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{productUuid}")
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<ApiResponse<Void>> removeFromFavorites(
            @RequestParam String userUuid,
            @PathVariable String productUuid) {
        securityContextUtils.requireSelfOrAdmin(userUuid);
        favoriteService.removeFromFavorites(userUuid, productUuid);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
