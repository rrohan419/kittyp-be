/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
@AllArgsConstructor
public class EmailAddress {
    private String address;
    private String name;
}
