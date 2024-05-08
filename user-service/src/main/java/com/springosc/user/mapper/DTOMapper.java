package com.springosc.user.mapper;

import com.osc.user_proto.UserSaveResponse;
import com.springosc.user.common.Helper;
import com.springosc.user.dto.*;
import com.springosc.user.entity.Session;
import com.springosc.user.entity.User;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DTOMapper {

    private final ModelMapper modelMapper;
    public DTOMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public UserDTO setUserDataInDTO(UserRegisterDTO userRegisterDTO) {
        UserDTO userDTO = modelMapper.map(userRegisterDTO, UserDTO.class);
        String userId = Helper.generateUserId(userDTO.getName());
        long otp = Helper.generateOTP();
        userDTO.setUserId(userId);
        userDTO.setOtp(otp);
        return userDTO;
    }

    public User setUserDataInEntity(UserSaveResponse userDetails, String password) {
        User user = new User();
        user.setUserId(userDetails.getUserId());
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        user.setContact(userDetails.getContact());
        user.setDob(userDetails.getDob());
        user.setPassword(password);
        return user;
    }

    public SessionEventDTO createSessionEventDTO(LoginRequestDTO loginRequestDTO) {
        return modelMapper.map(loginRequestDTO, SessionEventDTO.class);
    }

    public Session createSession(User user, String device) {
        String sessionId = UUID.randomUUID().toString().substring(0, 3);
        Session session = modelMapper.map(user, Session.class);
        session.setLoginTime(LocalDateTime.now());
        session.setDevice(device);
        session.setSessionId(sessionId);
        return session;
    }

    public ResetPasswordDTO createResetPasswordDTO(ResetPasswordRequestDTO resetPasswordRequestDTO) {
        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        long otp = Helper.generateOTP();
        resetPasswordDTO.setEmail(resetPasswordRequestDTO.getEmail());
        resetPasswordDTO.setOtp(otp);
        return resetPasswordDTO;
    }

}
