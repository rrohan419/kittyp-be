/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.models;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsModel {
    
	private String firstName;
	private String lastName;
    private String email;
    private Set<String> roles;
    private String uuid;
    private LocalDateTime createdAt;
    private String phoneNumber;
    private String phoneCountryCode;
    private Integer age;
    private boolean enabled;
    private String profilePictureUrl;
    private Set<PetModel> ownerPets;
    /** Present when email was changed so the client can refresh JWT without logging out. */
    private String accessToken;
}
