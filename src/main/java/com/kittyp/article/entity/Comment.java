/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.entity;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.user.entity.User;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "comments")
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity{

	/**
	 * @author rrohan419@gmail.com
	 */
	private static final long serialVersionUID = 1L;
	private String content;
    private Integer likes;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "article_id")
    private Article article;
}
