/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
@Setter
public class Address {

	private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
