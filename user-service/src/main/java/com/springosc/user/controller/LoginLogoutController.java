package com.springosc.user.controller;

import com.springosc.user.dto.LoginRequestDTO;
import com.springosc.user.dto.LoginResponseDTO;
import com.springosc.user.dto.LogoutDTO;
import com.springosc.user.gRPCRequest.GrpcClientRequest;
import com.springosc.user.response.CustomResponse;
import com.springosc.user.response.DataObject;
import com.springosc.user.response.ResponseCodes;
import com.springosc.user.service.LoginLogoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users")
public class LoginLogoutController {

    private final LoginLogoutService userLoginLogoutService;
    private final GrpcClientRequest grpcClientRequest;

    @Autowired
    public LoginLogoutController(LoginLogoutService userLoginLogoutService, GrpcClientRequest grpcClientRequest) {
        this.userLoginLogoutService = userLoginLogoutService;
        this.grpcClientRequest = grpcClientRequest;
    }

    @PostMapping("/login")
    public ResponseEntity<CustomResponse> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        CustomResponse customResponse = new CustomResponse();
        try {
            int credentialsCheckCode = userLoginLogoutService.checkCredentials(loginRequestDTO.getUserId(), loginRequestDTO.getPassword());
            switch (credentialsCheckCode) {
                case 200:
                    boolean isSessionActive = grpcClientRequest.getSessionStatus(loginRequestDTO.getUserId(), loginRequestDTO.getDevice());
                    if (isSessionActive) {
                        customResponse.setCode(204);
                    } else {
                        LoginResponseDTO loginResponseDTO = userLoginLogoutService.login(loginRequestDTO);
                        DataObject dataObject = new DataObject();
                        dataObject.setSessionId(loginResponseDTO.getSessionId());
                        dataObject.setName(loginResponseDTO.getName());
                        customResponse.setCode(ResponseCodes.LOGGED_IN);
                        customResponse.setDataObject(dataObject);
                    }
                    break;
                case 201:
                    log.info("Invalid UserId");
                    customResponse.setCode(ResponseCodes.INVALID_USERID);
                    break;
                case 202:
                    log.info("Invalid Password");
                    customResponse.setCode(ResponseCodes.INVALID_PASSWORD);
                    break;
            }
        } catch (Exception exception) {
            customResponse.setCode(ResponseCodes.UNKNOWN_ERROR);
        }
        return ResponseEntity.ok(customResponse);
    }


    @PostMapping("/logout")
    public ResponseEntity<CustomResponse> logout(@RequestBody LogoutDTO logoutDTO) {
        CustomResponse customResponse = new CustomResponse();
        try {
            userLoginLogoutService.logout(logoutDTO.getUserId(), logoutDTO.getSessionId());
            customResponse.setCode(ResponseCodes.LOGGED_OUT);
            log.info("Logout successful for user: " + logoutDTO.getUserId());
        } catch (Exception exception) {
            customResponse.setCode(ResponseCodes.UNKNOWN_ERROR);
            log.error("Error during logout for user " + logoutDTO.getUserId());
        }
        return ResponseEntity.ok(customResponse);
    }

}
