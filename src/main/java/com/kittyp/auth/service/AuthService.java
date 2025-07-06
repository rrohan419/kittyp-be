package com.kittyp.auth.service;

import com.kittyp.auth.dto.SocialSso;
import com.kittyp.common.dto.LoginRequestDto;
import com.kittyp.common.dto.SignupRequestDto;
import com.kittyp.common.model.JwtResponseModel;
import com.kittyp.common.model.MessageResponse;

public interface AuthService {

	MessageResponse registerUser(SignupRequestDto signupRequestDto);
	
	JwtResponseModel loginUser(LoginRequestDto loginRequestDto);

	JwtResponseModel googleUserSignin(SocialSso socialSso);
}
