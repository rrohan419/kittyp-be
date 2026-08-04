package com.kittyp.auth.service;

import java.util.Map;

import com.kittyp.auth.dto.SignupOtpSendRequest;
import com.kittyp.auth.dto.SignupOtpVerifyRequest;
import com.kittyp.auth.dto.SocialSso;
import com.kittyp.common.dto.LoginRequestDto;
import com.kittyp.common.dto.SignupClinicRequestDto;
import com.kittyp.common.dto.SignupDoctorRequestDto;
import com.kittyp.common.dto.SignupRequestDto;
import com.kittyp.common.model.JwtResponseModel;
import com.kittyp.common.model.MessageResponse;

public interface AuthService {

	MessageResponse registerUser(SignupRequestDto signupRequestDto);

	MessageResponse registerDoctor(SignupDoctorRequestDto signupDoctorRequestDto);

	MessageResponse registerClinic(SignupClinicRequestDto signupClinicRequestDto);

	MessageResponse sendSignupOtp(SignupOtpSendRequest request);

	Map<String, Boolean> verifySignupOtp(SignupOtpVerifyRequest request);
	
	JwtResponseModel loginUser(LoginRequestDto loginRequestDto);

	JwtResponseModel googleUserSignin(SocialSso socialSso);
}
