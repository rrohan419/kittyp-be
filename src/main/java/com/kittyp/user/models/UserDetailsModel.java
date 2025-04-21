/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.models;

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
    
    private Long id;
    private String email;
    private Set<String> roles;
    private boolean enabled;
    private String uuid;
}
