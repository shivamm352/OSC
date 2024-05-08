package com.springosc.user.service;

import com.osc.user_proto.UserOTPValidationResponse;
import com.springosc.user.dto.UserDTO;
import com.springosc.user.dto.UserRegisterDTO;

public interface RegistrationService {

    UserDTO sendUserDataInTopic(UserRegisterDTO userRegisterDTO);

    boolean isEmailExists(String email);

    UserOTPValidationResponse validateOTP(String userId, long otp);

    boolean saveUser(String userId, String password);

}
