package com.kittyp.clinic.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctor;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.common.entity.BaseEntity;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.enums.DoctorStatus;
import com.kittyp.user.entity.Address;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserFcmToken;
import com.kittyp.user.entity.UserRole;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Runs {@link ClinicRepository#findAllOrganizationFetchOwner()} against a real schema.
 * Personal practices (roster role {@code owner}) stay hidden. An organization clinic whose
 * owner is also a doctor on the roster (role {@code doctor}) stays visible.
 */
@SpringJUnitConfig(ClinicOrganizationListQueryTest.Config.class)
@Transactional
class ClinicOrganizationListQueryTest {

	@PersistenceContext
	private EntityManager entityManager;

	@org.springframework.beans.factory.annotation.Autowired
	private ClinicRepository clinicRepository;

	@org.springframework.beans.factory.annotation.Autowired
	private ClinicDoctorRepository clinicDoctorRepository;

	@Test
	void pendingClinicWithOwnerOnRosterAppearsOnceAndKeepsDoctorCount() {
		User owner = persistUser("owner@happy.test");
		DoctorProfile ownerDoctor = persistDoctor(owner, "doc-owner");
		User colleague = persistUser("colleague@happy.test");
		DoctorProfile colleagueDoctor = persistDoctor(colleague, "doc-colleague");
		Clinic clinic = persistClinic("happy-pet", "Happy Pet Clinic", ClinicStatus.PENDING, owner);
		link(clinic, ownerDoctor, "doctor");
		link(clinic, colleagueDoctor, "doctor");
		flush();

		List<Clinic> matches = listed("happy-pet");

		assertEquals(1, matches.size());
		assertEquals(ClinicStatus.PENDING, matches.get(0).getStatus());
		assertEquals(2, clinicDoctorRepository.findByClinic_IdAndIsActiveTrue(clinic.getId()).size());
		assertEquals(1, clinicRepository.countOrganizationClinics());
		assertEquals(1, clinicRepository.countOrganizationClinicsByStatus(ClinicStatus.PENDING));
	}

	@Test
	void pendingClinicWithoutRosterRemainsVisible() {
		User owner = persistUser("plain@clinic.test");
		persistClinic("plain-clinic", "Plain Clinic", ClinicStatus.PENDING, owner);
		flush();

		List<Clinic> matches = listed("plain-clinic");

		assertEquals(1, matches.size());
		assertEquals(ClinicStatus.PENDING, matches.get(0).getStatus());
		assertEquals(0, clinicDoctorRepository.findByClinic_IdAndIsActiveTrue(matches.get(0).getId()).size());
	}

	@Test
	void verifiedAndRejectedOrganizationClinicsRemainListed() {
		User verifiedOwner = persistUser("verified@clinic.test");
		User rejectedOwner = persistUser("rejected@clinic.test");
		persistClinic("verified-clinic", "Verified Clinic", ClinicStatus.VERIFIED, verifiedOwner);
		persistClinic("rejected-clinic", "Rejected Clinic", ClinicStatus.REJECTED, rejectedOwner);
		flush();

		assertEquals(ClinicStatus.VERIFIED, listed("verified-clinic").get(0).getStatus());
		assertEquals(ClinicStatus.REJECTED, listed("rejected-clinic").get(0).getStatus());
		assertEquals(1, clinicRepository.countOrganizationClinicsByStatus(ClinicStatus.VERIFIED));
		assertEquals(1, clinicRepository.countOrganizationClinicsByStatus(ClinicStatus.REJECTED));
		assertEquals(2, clinicRepository.countOrganizationClinics());
	}

	@Test
	void personalPracticeStaysOutOfAdminList() {
		User doctor = persistUser("solo@doctor.test");
		DoctorProfile profile = persistDoctor(doctor, "doc-solo");
		Clinic practice = persistClinic("solo-practice", "Dr. Solo", ClinicStatus.VERIFIED, doctor);
		link(practice, profile, "owner");
		flush();

		assertEquals(0, listed("solo-practice").size());
		assertEquals(0, clinicRepository.countOrganizationClinics());
		assertEquals(0, clinicRepository.countOrganizationClinicsByStatus(ClinicStatus.VERIFIED));
		assertEquals(1, clinicDoctorRepository.findByClinic_IdAndIsActiveTrue(practice.getId()).size());
	}

	@Test
	void approvingPendingOwnerDoctorClinicKeepsItListed() {
		User owner = persistUser("approve@happy.test");
		DoctorProfile ownerDoctor = persistDoctor(owner, "doc-approve");
		Clinic clinic = persistClinic("approve-clinic", "Happy Pet Clinic", ClinicStatus.PENDING, owner);
		link(clinic, ownerDoctor, "doctor");
		flush();

		assertEquals(1, listed("approve-clinic").size());
		Clinic toApprove = clinicRepository.findByUuid("approve-clinic");
		assertNotNull(toApprove);
		toApprove.setStatus(ClinicStatus.VERIFIED);
		clinicRepository.saveAndFlush(toApprove);
		entityManager.clear();

		List<Clinic> matches = listed("approve-clinic");
		assertEquals(1, matches.size());
		assertEquals(ClinicStatus.VERIFIED, matches.get(0).getStatus());
		assertEquals(1, clinicDoctorRepository.findByClinic_IdAndIsActiveTrue(clinic.getId()).size());
		assertEquals(1, clinicRepository.countOrganizationClinicsByStatus(ClinicStatus.VERIFIED));
		assertEquals(0, clinicRepository.countOrganizationClinicsByStatus(ClinicStatus.PENDING));
	}

	private List<Clinic> listed(String uuid) {
		return clinicRepository.findAllOrganizationFetchOwner().stream()
				.filter(clinic -> uuid.equals(clinic.getUuid()))
				.toList();
	}

	private void flush() {
		entityManager.flush();
		entityManager.clear();
	}

	private User persistUser(String email) {
		User user = User.builder().email(email).password("secret").uuid(email).firstName("Owner").build();
		stamp(user);
		entityManager.persist(user);
		return user;
	}

	private DoctorProfile persistDoctor(User user, String uuid) {
		DoctorProfile profile = DoctorProfile.builder()
				.uuid(uuid)
				.user(user)
				.status(DoctorStatus.DOCUMENTS_SUBMITTED)
				.build();
		stamp(profile);
		entityManager.persist(profile);
		return profile;
	}

	private Clinic persistClinic(String uuid, String name, ClinicStatus status, User owner) {
		Clinic clinic = Clinic.builder().uuid(uuid).name(name).status(status).owner(owner).build();
		stamp(clinic);
		entityManager.persist(clinic);
		return clinic;
	}

	private void link(Clinic clinic, DoctorProfile doctor, String role) {
		ClinicDoctor row = ClinicDoctor.builder()
				.clinic(clinic)
				.doctor(doctor)
				.role(role)
				.isActive(true)
				.joinedAt(LocalDate.now())
				.build();
		stamp(row);
		entityManager.persist(row);
	}

	private static void stamp(BaseEntity entity) {
		LocalDateTime now = LocalDateTime.now();
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);
		entity.setIsActive(true);
	}

	@Configuration
	@EnableTransactionManagement
	@EnableJpaRepositories(basePackageClasses = ClinicRepository.class, excludeFilters = @Filter(type = FilterType.CUSTOM, classes = ComponentScanFilter.class))
	static class Config {

		@Bean
		javax.sql.DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
		}

		@Bean
		LocalContainerEntityManagerFactoryBean entityManagerFactory(javax.sql.DataSource dataSource) {
			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setDataSource(dataSource);
			factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
			factory.setManagedTypes(PersistenceManagedTypes.of(
					User.class.getName(),
					Role.class.getName(),
					UserRole.class.getName(),
					Address.class.getName(),
					Pet.class.getName(),
					UserFcmToken.class.getName(),
					Clinic.class.getName(),
					ClinicDoctor.class.getName(),
					ClinicPetOwner.class.getName(),
					DoctorProfile.class.getName()));
			Properties jpa = new Properties();
			jpa.setProperty("hibernate.hbm2ddl.auto", "create-drop");
			jpa.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
			factory.setJpaProperties(jpa);
			return factory;
		}

		@Bean
		PlatformTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory entityManagerFactory) {
			return new JpaTransactionManager(entityManagerFactory);
		}
	}

	/** Keeps this slice on the two repositories whose queries are under test. */
	static class ComponentScanFilter implements TypeFilter {
		@Override
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
			String name = metadataReader.getClassMetadata().getClassName();
			return !ClinicRepository.class.getName().equals(name)
					&& !ClinicDoctorRepository.class.getName().equals(name);
		}
	}
}
