package com.springosc.user.service;

import com.springosc.user.dto.LoginRequestDTO;
import com.springosc.user.dto.LoginResponseDTO;
import com.springosc.user.dto.SessionEventDTO;
import com.springosc.user.entity.Session;

public interface LoginLogoutService {

    int checkCredentials(String userId, String password);

    LoginResponseDTO login(LoginRequestDTO loginRequestDTO);

    Session saveSession(String userId, String device);

    void logout(String userId, String sessionId);

}
