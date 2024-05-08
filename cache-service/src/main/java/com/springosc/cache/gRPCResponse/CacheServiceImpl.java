package com.springosc.cache.gRPCResponse;

import com.osc.user_proto.*;
import com.springosc.cache.dto.UserDTO;
import com.springosc.cache.service.InvalidOTPCounterCacheService;
import com.springosc.cache.service.UserDataCacheService;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@GrpcService
public class CacheServiceImpl extends CacheServiceGrpc.CacheServiceImplBase {

    private final UserDataCacheService userDataCacheService;

    private final InvalidOTPCounterCacheService invalidOTPCounterCacheService;

    @Autowired
    public CacheServiceImpl(UserDataCacheService userDataCacheService, InvalidOTPCounterCacheService invalidOTPCounterCacheService) {
        this.userDataCacheService = userDataCacheService;
        this.invalidOTPCounterCacheService = invalidOTPCounterCacheService;
    }

    @Override
    public void getUserDetails(UserOTPValidationRequest request, StreamObserver<UserOTPValidationResponse> responseObserver) {
        String userId = request.getUserId();
        UserDTO userDTO = userDataCacheService.getDataFromMap(userId);
        UserOTPValidationResponse.Builder responseBuilder = UserOTPValidationResponse.newBuilder();
        if (userDTO == null) {
            responseBuilder.setIsInValidUserId(true);
        } else {
            responseBuilder
                    .setIsInValidUserId(false)
                    .setUserId(userDTO.getUserId())
                    .setOtp(userDTO.getOtp());
        }
        UserOTPValidationResponse response = responseBuilder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void incrementInvalidAttempts(UserOTPValidationRequest request, StreamObserver<UserOTPValidationResponse> responseObserver) {
        String userId = request.getUserId();
        int count = otpCount(userId);
        UserOTPValidationResponse response = UserOTPValidationResponse.newBuilder()
                .setIsInValidUserId(false)
                .setInvalidAttempts(count)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private int otpCount(String userId) {
        int count = invalidOTPCounterCacheService.getInvalidOTPCount(userId);
            if (count < 3) {
                count++;
                invalidOTPCounterCacheService.storeInvalidOTPCount(userId, count);
                log.info("Invalid count are: {}", count);
            } else {
                log.error("OTP attempt limit exceeded for UserId: " + userId + ". Please regenerate OTP.");
            }
            return count;
    }


    @Override
    public void getSaveRequest(UserSaveRequest request, StreamObserver<UserSaveResponse> responseObserver) {
        String userId = request.getUserId();
        UserDTO userDTO = userDataCacheService.getDataFromMap(userId);
        UserSaveResponse.Builder responseBuilder = UserSaveResponse.newBuilder();
        if (userDTO != null) {
            responseBuilder
                    .setUserId(userDTO.getUserId())
                    .setName(userDTO.getName())
                    .setEmail(userDTO.getEmail())
                    .setContact(userDTO.getContact())
                    .setDob(userDTO.getDob());
        }
        UserSaveResponse response = responseBuilder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}