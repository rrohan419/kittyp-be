package com.kittyp.booking.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.booking.dto.VideoJoinModel;
import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingMode;
import com.kittyp.booking.enums.BookingStatus;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingVideoService {

	private final BookingRepository bookingRepository;
	private final UserDao userDao;
	private final JitsiMeetService jitsiMeetService;

	@Transactional
	public VideoJoinModel join(String email, String bookingUuid) {
		User user = userDao.userByEmail(email);
		Booking booking = bookingRepository.findByUuid(bookingUuid)
				.orElseThrow(() -> new ResourceNotFoundException("booking", "uuid", bookingUuid));
		if (!canJoin(user, booking)) {
			throw new AccessDeniedException("You cannot join this video consult");
		}
		if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.NO_SHOW) {
			throw new CustomException("This appointment is no longer active", HttpStatus.BAD_REQUEST);
		}
		if (booking.getMode() != BookingMode.VIDEO) {
			throw new CustomException("This appointment is not a video consult", HttpStatus.BAD_REQUEST);
		}
		jitsiMeetService.ensureVideoRoom(booking);
		bookingRepository.save(booking);
		return new VideoJoinModel(
				booking.getUuid(),
				booking.getJitsiRoomId(),
				jitsiMeetService.domain(),
				booking.getVideoJoinUrl(),
				displayName(user));
	}

	private static boolean canJoin(User user, Booking booking) {
		if (user == null || user.getId() == null) {
			return false;
		}
		if (booking.getOwner() != null && user.getId().equals(booking.getOwner().getId())) {
			return true;
		}
		return booking.getDoctor() != null
				&& booking.getDoctor().getUser() != null
				&& user.getId().equals(booking.getDoctor().getUser().getId());
	}

	private static String displayName(User user) {
		if (user == null) {
			return "Guest";
		}
		String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
				+ (user.getLastName() == null ? "" : user.getLastName())).trim();
		return name.isBlank() ? (user.getEmail() == null ? "Guest" : user.getEmail()) : name;
	}
}
