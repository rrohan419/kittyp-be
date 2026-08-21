package com.kittyp.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.Mapper;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.models.UserDetailsModel;

@ExtendWith(MockitoExtension.class)
class UserServiceImplGetAllUsersTest {

	@Mock
	private UserDao userDao;

	@Mock
	private Mapper mapper;

	@InjectMocks
	private UserServiceImpl userService;

	@Test
	void getAllUsers_usesPetOwnerQuery() {
		Role userRole = new Role();
		userRole.setName(ERole.ROLE_USER);
		User owner = User.builder().email("owner@example.com").password("x").uuid("u1").firstName("Pat")
				.lastName("Owner").build();
		owner.getUserRoles().add(new UserRole(owner, userRole));

		when(userDao.findPetOwnerUsers(eq(""), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(owner)));
		when(mapper.convert(eq(owner), eq(UserDetailsModel.class))).thenReturn(UserDetailsModel.builder()
				.email("owner@example.com").firstName("Pat").lastName("Owner").uuid("u1").build());

		PaginationModel<UserDetailsModel> page = userService.getAllUsers(1, 10, null);

		verify(userDao).findPetOwnerUsers(eq(""), any(Pageable.class));
		assertEquals(1, page.getModels().size());
		assertEquals(Set.of("ROLE_USER"), page.getModels().get(0).getRoles());
		assertEquals("owner@example.com", page.getModels().get(0).getEmail());
	}

	@Test
	void getAllUsers_passesTrimmedQuery() {
		when(userDao.findPetOwnerUsers(eq("pat"), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));

		PaginationModel<UserDetailsModel> page = userService.getAllUsers(1, 10, "  pat  ");

		verify(userDao).findPetOwnerUsers(eq("pat"), any(Pageable.class));
		assertEquals(0, page.getModels().size());
	}
}
