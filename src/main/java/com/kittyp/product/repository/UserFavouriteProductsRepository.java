/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.product.entity.UserFavouriteProducts;

/**
 * @author rrohan419@gmail.com 
 */
public interface UserFavouriteProductsRepository extends JpaRepository<UserFavouriteProducts, Long> {

}
