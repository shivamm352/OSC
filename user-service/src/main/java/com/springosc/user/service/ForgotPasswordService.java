package com.springosc.user.service;

import com.osc.user_proto.UserOTPResponse;
import com.springosc.user.dto.ResetPasswordDTO;
import com.springosc.user.dto.ResetPasswordRequestDTO;

public interface ForgotPasswordService {

    boolean isEmailExists(String email);

    ResetPasswordDTO sendOTPForForgotPassword(ResetPasswordRequestDTO resetPasswordRequestDTO);

    UserOTPResponse validateOTPForForgotPassword(String email, long otp);

    boolean updateUserPassword(String email, String newPassword);

}
