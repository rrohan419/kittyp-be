/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.entity;


import java.util.List;

import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
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

	/** Links this author profile to a platform user (e.g. independent doctor). */
	@Column(name = "user_uuid", unique = true)
	private String userUuid;

	@OneToMany(mappedBy = "author")
	private List<Article> articles;

}
