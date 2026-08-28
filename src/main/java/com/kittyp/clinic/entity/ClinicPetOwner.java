package com.kittyp.clinic.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.common.entity.HasPublicId;
import com.kittyp.common.entity.PublicIdEntityListener;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Clinic-managed pet owner / client (not a platform User account until linked).
 * Pets live in the shared {@code pets} table via {@link Pet#getClinicOwner()}.
 */
@Entity
@Table(name = "clinic_pet_owners")
@EntityListeners(PublicIdEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true, exclude = { "pets", "linkedUser" })
public class ClinicPetOwner extends BaseEntity implements HasPublicId {

	@Column(nullable = false, unique = true, updatable = false)
	private String uuid;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "clinic_id", nullable = false)
	private Clinic clinic;

	@Column(nullable = false)
	private String firstName;

	private String lastName;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false, length = 20)
	private String phone;

	@Column(name = "alternate_phone", length = 20)
	private String alternatePhone;

	@Column(length = 500)
	private String address;

	@Column(columnDefinition = "TEXT")
	private String notes;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "linked_user_id")
	@ToString.Exclude
	private User linkedUser;

	@OneToMany(mappedBy = "clinicOwner", cascade = CascadeType.ALL)
	@Builder.Default
	@ToString.Exclude
	private List<Pet> pets = new ArrayList<>();
}
