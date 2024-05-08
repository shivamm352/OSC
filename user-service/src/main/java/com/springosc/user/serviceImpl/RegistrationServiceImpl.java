package com.springosc.user.serviceImpl;

import com.springosc.user.gRPCRequest.GrpcClientRequest;
import lombok.extern.slf4j.Slf4j;
import com.osc.user_proto.*;
import com.springosc.user.dao.UserRepository;
import com.springosc.user.dto.UserDTO;
import com.springosc.user.dto.UserRegisterDTO;
import com.springosc.user.entity.User;
import com.springosc.user.kafkaProducer.KafkaPublisher;
import com.springosc.user.mapper.DTOMapper;
import com.springosc.user.service.RegistrationService;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import java.util.Optional;


@Slf4j
@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final KafkaPublisher kafkaPublisher;
    private final DTOMapper dtoMapper;
    private final GrpcClientRequest grpcClientRequest;

    public RegistrationServiceImpl(
            UserRepository userRepository, KafkaPublisher kafkaPublisher,
            DTOMapper dtoMapper, GrpcClientRequest grpcClientRequest
    ) {
        this.userRepository = userRepository;
        this.kafkaPublisher = kafkaPublisher;
        this.dtoMapper = dtoMapper;
        this.grpcClientRequest = grpcClientRequest;
    }

    @Override
    public boolean isEmailExists(String email) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        return existingUser.isPresent();
    }


    @Override
    public UserDTO sendUserDataInTopic(UserRegisterDTO userRegisterDTO) {
        UserDTO userDTO = dtoMapper.setUserDataInDTO(userRegisterDTO);
        kafkaPublisher.publishUserDataInTopic(userDTO);
        return userDTO;
    }

    @Override
    public UserOTPValidationResponse validateOTP(String userId, long otp) {
        UserOTPValidationResponse userOtpResponse = grpcClientRequest.getUserOTP(userId);
        if (userOtpResponse == null) {
            return null;
        }

        if (userOtpResponse.getIsInValidUserId()) {
            log.info("Invalid UserId: {}", userId);
            return UserOTPValidationResponse.newBuilder().setIsInValidUserId(true).build();
        }

        long retrievedOtp = userOtpResponse.getOtp();
        if (otp == retrievedOtp) {
            log.info("OTP Validated Successfully for UserId: {}", userId);
            return userOtpResponse;
        }

        UserOTPValidationResponse incrementAttemptsResponse = grpcClientRequest.incrementInvalidAttempts(userId, otp);
        if (incrementAttemptsResponse != null) {
            log.info("Invalid OTP for UserId: {}. Count: {}", userId, incrementAttemptsResponse.getInvalidAttempts());
            return incrementAttemptsResponse;
        }
        return null;
    }


    @Override
    public boolean saveUser(String userId, String password) {
        UserSaveResponse userDetails = grpcClientRequest.getSaveRequest(userId);
        if (userDetails != null) {
            User user = dtoMapper.setUserDataInEntity(userDetails, password);
            userRepository.save(user);
            return true;
        } else {
            return false;
        }
    }

}
