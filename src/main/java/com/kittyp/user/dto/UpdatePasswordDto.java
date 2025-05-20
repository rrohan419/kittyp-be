/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.dto;

import lombok.Getter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
public class UpdatePasswordDto {

	private String email;
	
	private String code;
	
	private String password;
}
