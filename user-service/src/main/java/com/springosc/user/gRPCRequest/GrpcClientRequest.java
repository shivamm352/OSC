package com.springosc.user.gRPCRequest;

import com.osc.session_proto.SessionServiceGrpc;
import com.osc.session_proto.SessionStatusResponse;
import com.osc.session_proto.UserSessionRequest;
import com.osc.user_proto.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GrpcClientRequest {

    private final CacheServiceGrpc.CacheServiceBlockingStub cacheServiceBlockingStub;
    private final SessionServiceGrpc.SessionServiceBlockingStub sessionServiceBlockingStub;
    private final ForgetPasswordServiceGrpc.ForgetPasswordServiceBlockingStub forgetPasswordServiceBlockingStub;

    public GrpcClientRequest(@GrpcClient("cache") CacheServiceGrpc.CacheServiceBlockingStub cacheServiceBlockingStub,
                             @GrpcClient("session")SessionServiceGrpc.SessionServiceBlockingStub sessionServiceBlockingStub,
                             @GrpcClient("cache")ForgetPasswordServiceGrpc.ForgetPasswordServiceBlockingStub forgetPasswordServiceBlockingStub) {
        this.cacheServiceBlockingStub = cacheServiceBlockingStub;
        this.sessionServiceBlockingStub = sessionServiceBlockingStub;
        this.forgetPasswordServiceBlockingStub = forgetPasswordServiceBlockingStub;
    }

    public UserOTPValidationResponse getUserOTP(String userId) {
        UserOTPValidationRequest userOtpRequest = UserOTPValidationRequest.newBuilder()
                .setUserId(userId)
                .build();
        return cacheServiceBlockingStub.getUserDetails(userOtpRequest);
    }

    public UserOTPValidationResponse incrementInvalidAttempts(String userId, long otp) {
        try {
            UserOTPValidationRequest incrementAttemptsRequest = UserOTPValidationRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            UserOTPValidationResponse incrementAttemptsResponse = cacheServiceBlockingStub.incrementInvalidAttempts(incrementAttemptsRequest);
            if (incrementAttemptsResponse != null) {
                int count = incrementAttemptsResponse.getInvalidAttempts();
                return UserOTPValidationResponse.newBuilder()
                        .setIsInValidUserId(false)
                        .setInvalidAttempts(count)
                        .build();
            } else {
                log.info("Error incrementing invalid attempts for UserId: " + userId);
                return null;
            }
        } catch (Exception e) {
            log.error("Error sending gRPC request to CacheService: {}", e.getMessage());
            return null;
        }
    }


    public UserSaveResponse getSaveRequest(String userId) {
        try {
            UserSaveRequest request = UserSaveRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            return cacheServiceBlockingStub.getSaveRequest(request);
        } catch (Exception e) {
            log.error("Error sending gRPC request to CacheService: {}", e.getMessage());
            return null;
        }
    }

    public boolean getSessionStatus(String userId, String device) {
        try {
            UserSessionRequest request = UserSessionRequest.newBuilder()
                    .setUserId(userId)
                    .setDevice(device)
                    .build();
            SessionStatusResponse response = sessionServiceBlockingStub.getSessionStatus(request);
            return response.getIsSessionActive();
        } catch (Exception e) {
            log.error("error occurred during gRPC call: {}", e.getMessage());
            return false;
        }
    }

    public UserOTPResponse getOTPToResetPassword(String email) {
        try {
            UserOTPRequest request = UserOTPRequest.newBuilder()
                    .setEmail(email)
                    .build();
            return forgetPasswordServiceBlockingStub.getUserOTPRequest(request);
        } catch (Exception e) {
            log.error("Error sending gRPC request to ForgetPasswordService: {}", e.getMessage());
            return null;
        }
    }

    public UserOTPResponse incrementInvalidOTPCount(String email) {
        try {
            UserOTPRequest request = UserOTPRequest.newBuilder()
                    .setEmail(email)
                    .build();
            return forgetPasswordServiceBlockingStub.incrementInvalidOTPAttempts(request);
        } catch (Exception e) {
            log.error("Error sending gRPC request to ForgetPasswordService: {}", e.getMessage());
            return null;
        }
    }

}
