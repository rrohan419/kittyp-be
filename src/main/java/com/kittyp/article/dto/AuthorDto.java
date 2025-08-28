/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
public class AuthorDto {

    @NotNull
	private String name;
    private String avatar;
    private String role;
}
