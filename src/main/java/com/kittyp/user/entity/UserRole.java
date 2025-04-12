package com.kittyp.user.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "user_roles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "role_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"user"})
@DynamicUpdate
public class UserRole extends BaseEntity {
    
	/**
	 * @author rrohan419@gmail.com
	 */
	private static final long serialVersionUID = 1L;

	public UserRole(User user, Role role) {
		this.user = user;
		this.role = role;
	}
	
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    // Additional fields can be added here
    private LocalDateTime assignedAt;
    
    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }
}
