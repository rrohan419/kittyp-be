/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.entity;

import java.util.List;

import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Entity
@Table(name = "user_favourite_products")
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFavouriteProducts extends BaseEntity {
	
	private static final long serialVersionUID = 1L;
	
	@Column(nullable = false)
	private String userUuid;
	
	@Column
	private String favouriteCategory;
	
	@Column
	private List<String> productUuids;

}
