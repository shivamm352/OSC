package com.springosc.user.controller;

import com.osc.user_proto.UserOTPResponse;
import com.springosc.user.dto.ResetPasswordDTO;
import com.springosc.user.dto.ResetPasswordRequestDTO;
import com.springosc.user.dto.UpdatePasswordDTO;
import com.springosc.user.response.CustomResponse;
import com.springosc.user.response.ResponseCodes;
import com.springosc.user.service.ForgotPasswordService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users")
public class ForgotPasswordController {

    @Autowired
    private ForgotPasswordService forgotPasswordService;

    @PostMapping("/forgetPassword")
    public ResponseEntity<CustomResponse> resetPassword(@RequestBody ResetPasswordRequestDTO resetPasswordRequestDTO) {
        CustomResponse customResponse = new CustomResponse();

        try {
            ResetPasswordDTO resetPasswordDTO = forgotPasswordService.sendOTPForForgotPassword(resetPasswordRequestDTO);

            if (resetPasswordDTO != null && resetPasswordDTO.getOtp() > 0) {
                customResponse.setCode(ResponseCodes.SUCCESS);
            } else {
                customResponse.setCode(ResponseCodes.EMAIL_NOT_SENT);
            }

        } catch (Exception exception) {
            customResponse.setCode(ResponseCodes.UNKNOWN_ERROR);
        }

        return new ResponseEntity<>(customResponse, HttpStatus.OK);
    }


    @PostMapping("/validateOtpForForgotPassword")
    public ResponseEntity<CustomResponse> validateOtp(@RequestBody ResetPasswordDTO resetPasswordDTO) {
        CustomResponse customResponse = new CustomResponse();
        try {
            if (!forgotPasswordService.isEmailExists(resetPasswordDTO.getEmail())) {
                customResponse.setCode(ResponseCodes.EMAIL_ALREADY_EXISTS);
            } else {
                UserOTPResponse otpValidationResponse = forgotPasswordService.validateOTPForForgotPassword(resetPasswordDTO.getEmail(), resetPasswordDTO.getOtp());
                if (otpValidationResponse != null) {
                    if (otpValidationResponse.getInvalidAttemptCount() == 1 || otpValidationResponse.getInvalidAttemptCount() == 2) {
                        customResponse.setCode(ResponseCodes.INVALID_OTP_ATTEMPTS_1_2);
                    } else if (otpValidationResponse.getInvalidAttemptCount() >= 3) {
                        customResponse.setCode(ResponseCodes.INVALID_ATTEMPTS_3_OR_MORE);
                    } else {
                        customResponse.setCode(ResponseCodes.SUCCESS);
                    }
                }
            }
        } catch (Exception exception) {
            customResponse.setCode(ResponseCodes.UNKNOWN_ERROR);
        }
        return ResponseEntity.ok(customResponse);
    }



    @PostMapping("/changePassword")
    public ResponseEntity<CustomResponse> updatePassword(@RequestBody UpdatePasswordDTO updatePasswordDTO) {
        CustomResponse customResponse = new CustomResponse();
        boolean updated = forgotPasswordService.updateUserPassword(updatePasswordDTO.getEmail(), updatePasswordDTO.getPassword());
        if (updated) {
            customResponse.setCode(ResponseCodes.SUCCESS);
            log.info("Password updated successfully for user: " + updatePasswordDTO.getEmail());
            return ResponseEntity.ok(customResponse);
        } else {
            customResponse.setCode(199);
            log.error("User not found for password update: " + updatePasswordDTO.getEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(customResponse);
        }
    }

}
