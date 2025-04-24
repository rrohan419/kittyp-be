/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.entity;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
@Setter
public class Address  implements Serializable {

	/**
	 * @author rrohan419@gmail.com
	 */
	private static final long serialVersionUID = 1L;
	private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
