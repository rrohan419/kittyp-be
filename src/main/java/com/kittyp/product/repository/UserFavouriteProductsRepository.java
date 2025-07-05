/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.product.entity.UserFavouriteProducts;

/**
 * @author rrohan419@gmail.com 
 */
public interface UserFavouriteProductsRepository extends JpaRepository<UserFavouriteProducts, Long> {
    
    Optional<UserFavouriteProducts> findByUserUuid(String userUuid);
    
    @Query("SELECT ufp FROM UserFavouriteProducts ufp WHERE ufp.userUuid = :userUuid AND (:category IS NULL OR ufp.favouriteCategory = :category)")
    Optional<UserFavouriteProducts> findByUserUuidAndCategory(
        @Param("userUuid") String userUuid,
        @Param("category") String category
    );
    
    @Query("SELECT ufp FROM UserFavouriteProducts ufp WHERE ufp.userUuid = :userUuid")
    Page<UserFavouriteProducts> findAllByUserUuid(
        @Param("userUuid") String userUuid,
        Pageable pageable
    );
}
