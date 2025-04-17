/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.model;

import com.kittyp.user.models.UserDetailsModel;

import lombok.Data;

/**
 * @author rrohan419@gmail.com
 */
@Data
public class CommentModel {

	private Long id;
	private String content;
	private Integer likes;
	private UserDetailsModel userDetails;
}
