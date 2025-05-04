/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.model;

import java.util.List;

import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
public class UserFavouriteModel {

	private String favouriteCategory;
	
	private List<ProductModel> products;
}
