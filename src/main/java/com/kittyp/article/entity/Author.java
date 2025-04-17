/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.entity;


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
@Table(name = "authors")
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author extends BaseEntity {
	
	private static final long serialVersionUID = 1L;
	
	private String name;
	
	@Column(columnDefinition = "TEXT")
    private String avatar;
    
    private String role;

}
