package com.springosc.cache.gRPCResponse;

import com.osc.user_proto.*;
import com.springosc.cache.service.ForgotPasswordInvalidOTPCounter;
import com.springosc.cache.service.ResetPasswordCacheService;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@GrpcService
public class ForgetPasswordImpl extends ForgetPasswordServiceGrpc.ForgetPasswordServiceImplBase {

    private final ResetPasswordCacheService resetPasswordCacheService;

    private final ForgotPasswordInvalidOTPCounter forgotPasswordInvalidOTPCounter;

    @Autowired
    public ForgetPasswordImpl(ResetPasswordCacheService resetPasswordCacheService, ForgotPasswordInvalidOTPCounter forgotPasswordInvalidOTPCounter) {
        this.resetPasswordCacheService = resetPasswordCacheService;
        this.forgotPasswordInvalidOTPCounter = forgotPasswordInvalidOTPCounter;
    }

    @Override
    public void getUserOTPRequest(UserOTPRequest request, StreamObserver<UserOTPResponse> responseObserver) {
        String email = request.getEmail();
        Long otp = resetPasswordCacheService.getStoredOTP(email);

        UserOTPResponse.Builder responseBuilder = UserOTPResponse.newBuilder();
        if (otp != null) {
            responseBuilder
                    .setEmail(email)
                    .setOtp(otp);
        }
        UserOTPResponse response = responseBuilder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void incrementInvalidOTPAttempts(UserOTPRequest request, StreamObserver<UserOTPResponse> responseObserver) {
        String email = request.getEmail();
        int count = otpCount(email);
        UserOTPResponse response = UserOTPResponse.newBuilder()
                .setEmail(email)
                .setInvalidAttemptCount(count)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private int otpCount(String email) {
        int count = forgotPasswordInvalidOTPCounter.getInvalidOTPCount(email);
        if (count < 3) {
            count++;
            forgotPasswordInvalidOTPCounter.storeInvalidOTPCount(email, count);
        } else {
            log.error("OTP attempt limit exceeded for Email: " + email + ". Please regenerate OTP.");
        }
        return count;
    }
}
