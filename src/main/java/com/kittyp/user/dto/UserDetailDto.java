/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.dto;

import lombok.Getter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
public class UserDetailDto {

	private String firstName;
	private String lastName;
    private String email;
    private String phoneNumber;
    private String phoneCountryCode;
    private String profilePictureUrl;
}
