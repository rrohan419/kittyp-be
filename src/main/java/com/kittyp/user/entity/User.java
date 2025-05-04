package com.kittyp.user.entity;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true, exclude = {"userRoles"})
public class User extends BaseEntity {
    
    /**
	 * @author rrohan419@gmail.com
	 */
	private static final long serialVersionUID = 1L;
	
	@Column
	private String firstName;
	
	@Column
	private String lastName;

	@Column(nullable = false, unique = true)
    private String uuid;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column
    private String phoneCountryCode;
    
    @Column
    private String phoneNumber;
    
    @Column(nullable = false)
    private String password;
    
    @Builder.Default
    private boolean enabled = true;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    @ToString.Exclude
    private Set<UserRole> userRoles = new HashSet<>();
    
    // Helper method to add a role
    public void addRole(Role role) {
        UserRole userRole = new UserRole(this, role);
        if (!userRoles.contains(userRole)) {
            userRoles.add(userRole);
        }
    }
    
    // Helper method to remove a role
    public void removeRole(Role role) {
        for (Iterator<UserRole> iterator = userRoles.iterator(); iterator.hasNext();) {
            UserRole userRole = iterator.next();
            
            if (userRole.getUser().equals(this) && userRole.getRole().equals(role)) {
                iterator.remove();
                userRole.setUser(null);
                userRole.setRole(null);
            }
        }
    }
}
