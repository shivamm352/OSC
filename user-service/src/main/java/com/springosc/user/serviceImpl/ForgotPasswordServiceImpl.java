package com.springosc.user.serviceImpl;

import com.osc.user_proto.*;
import com.springosc.user.common.Helper;
import com.springosc.user.dao.UserRepository;
import com.springosc.user.dto.ResetPasswordDTO;
import com.springosc.user.dto.ResetPasswordRequestDTO;
import com.springosc.user.entity.User;
import com.springosc.user.gRPCRequest.GrpcClientRequest;
import com.springosc.user.kafkaProducer.KafkaPublisher;
import com.springosc.user.mapper.DTOMapper;
import com.springosc.user.service.ForgotPasswordService;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Slf4j
@Service
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private final UserRepository userRepository;
    private final KafkaPublisher kafkaPublisher;
    private final DTOMapper dtoMapper;
    private final GrpcClientRequest grpcClientRequest;

    public ForgotPasswordServiceImpl(
            UserRepository userRepository,
            KafkaPublisher kafkaPublisher, DTOMapper dtoMapper, GrpcClientRequest grpcClientRequest
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
    public ResetPasswordDTO sendOTPForForgotPassword(ResetPasswordRequestDTO resetPasswordRequestDTO) {
        ResetPasswordDTO resetPasswordDTO = dtoMapper.createResetPasswordDTO(resetPasswordRequestDTO);
        kafkaPublisher.publishOTPToResetPassword(resetPasswordDTO);
        return resetPasswordDTO;
    }

    @Override
    public UserOTPResponse validateOTPForForgotPassword(String email, long otp) {
        try {
            UserOTPResponse response = grpcClientRequest.getOTPToResetPassword(email);
            if (response != null && otp == response.getOtp()) {
                log.info("OTP Validated Successfully for email: {}", email);
                return response;
            } else {
                return grpcClientRequest.incrementInvalidOTPCount(email);
            }
        } catch (Exception e) {
            log.error("Error validating OTP for email: {}. Error: {}", email, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean updateUserPassword(String email, String newPassword) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setPassword(newPassword);
            userRepository.save(user);
            return true;
        } else {
            return false;
        }
    }
}
