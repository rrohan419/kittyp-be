package com.kittyp.user.entity;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.user.enums.ERole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roles")
@EqualsAndHashCode(callSuper = false)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity{

	/**
	 * @author rrohan419@gmail.com
	 */
	private static final long serialVersionUID = 1L;
	
	@Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private ERole name;
    
}
