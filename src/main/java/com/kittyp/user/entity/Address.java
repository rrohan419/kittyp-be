/**
* @author rrohan419@gmail.com
*/
package com.kittyp.user.entity;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.user.enums.AddressType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@Entity
@Table(name = "address")
@EqualsAndHashCode(callSuper = false)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(nullable = false, unique = true)
    private String uuid;

    private String name;

    private String street;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    private String formattedAddress;

    @Column(nullable = false)
    private AddressType addressType;

    private String phoneNumber;

    @ManyToOne
    @JoinColumn(name = "user_uuid", nullable = false, referencedColumnName = "uuid")
    private User user;

}
