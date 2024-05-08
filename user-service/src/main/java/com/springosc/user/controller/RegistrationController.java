package com.springosc.user.controller;

import com.osc.user_proto.UserOTPValidationResponse;
import com.springosc.user.dto.*;
import com.springosc.user.response.CustomResponse;
import com.springosc.user.response.DataObject;
import com.springosc.user.response.ResponseCodes;
import com.springosc.user.service.RegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
public class RegistrationController {

    private final RegistrationService registrationService;

    @Autowired
    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<CustomResponse> createUser(@RequestBody UserRegisterDTO userRegisterDTO) {
        try {
            CustomResponse customResponse = new CustomResponse();
            if (registrationService.isEmailExists(userRegisterDTO.getEmail())) {
                customResponse.setCode(ResponseCodes.EMAIL_EXISTS);
                customResponse.setDataObject(null);
            } else {
                UserDTO userDTO = registrationService.sendUserDataInTopic(userRegisterDTO);
                if (userDTO != null) {
                    DataObject dataObject = DataObject.builder().userId(userDTO.getUserId()).build();
                    customResponse.setCode(ResponseCodes.SUCCESS);
                    customResponse.setDataObject(dataObject);
                } else {
                    customResponse.setCode(ResponseCodes.USER_DATA_NOT_SENT);
                    customResponse.setDataObject(null);
                }
            }
            return ResponseEntity.ok(customResponse);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.OK).body(new CustomResponse(ResponseCodes.UNKNOWN_ERROR, null));
        }
    }

    @PostMapping("/validateOtp")
    public ResponseEntity<CustomResponse> validateOtp(@RequestBody ValidateOTPDTO validateOtpDTO) {
        CustomResponse customResponse = new CustomResponse();
        try {
            UserOTPValidationResponse otpValidationResponse = registrationService.validateOTP(
                    validateOtpDTO.getUserId(), validateOtpDTO.getOtp());
            if (otpValidationResponse != null) {
                if (otpValidationResponse.getIsInValidUserId()) {
                    customResponse.setCode(ResponseCodes.INVALID_USER_ID);
                } else if (otpValidationResponse.getInvalidAttempts() == 1 || otpValidationResponse.getInvalidAttempts() == 2) {
                    customResponse.setCode(ResponseCodes.INVALID_ATTEMPTS_1_2);
                } else if (otpValidationResponse.getInvalidAttempts() >= 3) {
                    customResponse.setCode(ResponseCodes.INVALID_ATTEMPTS_3_OR_MORE);
                }
                else {
                    customResponse.setCode(ResponseCodes.VALID_OTP);
                }
            }
        } catch (Exception exception) {
            customResponse.setCode(ResponseCodes.UNKNOWN_ERROR);
        }
        return ResponseEntity.ok(customResponse);
    }

    @PostMapping("/addUserDetails")
    public ResponseEntity<CustomResponse> saveUser(@RequestBody SaveUserDTO saveUserDTO) {
        CustomResponse customResponse = new CustomResponse();
        try {
            boolean isUserSaved = registrationService.saveUser(saveUserDTO.getUserId(), saveUserDTO.getPassword());
            if (isUserSaved) {
                customResponse.setCode(ResponseCodes.SUCCESSFULLY_SAVED);
                log.info("User saved successfully with ID: " + saveUserDTO.getUserId());
            } else {
                log.error("Error saving user in Database: ");           
            }
            return ResponseEntity.ok(customResponse);
        } catch (Exception e) {
            customResponse.setCode(ResponseCodes.UNKNOWN_ERROR);
            log.error("Exception occurred during saving user in DB: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(customResponse);
        }
    }
}
