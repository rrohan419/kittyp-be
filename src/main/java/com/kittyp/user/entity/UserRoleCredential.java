package com.kittyp.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Bcrypt hash for one role on an account. Never stores a raw password.
 * A row is inserted when that role is first created, and is not replaced
 * when the same role is submitted again.
 */
@Entity
@Table(name = "user_role_credentials", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "user_id", "role_id" })
})
@Getter
@Setter
@NoArgsConstructor
public class UserRoleCredential extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	@JsonIgnore
	@ToString.Exclude
	private User user;

	@ManyToOne
	@JoinColumn(name = "role_id", nullable = false)
	private Role role;

	@Column(name = "password_hash", nullable = false)
	@JsonIgnore
	private String passwordHash;
}
