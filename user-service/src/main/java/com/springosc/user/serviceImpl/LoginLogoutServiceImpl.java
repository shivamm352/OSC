package com.springosc.user.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.saveRecentlyViewed.*;
import com.osc.save_cart_product.*;
import com.osc.session_proto.SessionServiceGrpc;
import com.osc.session_proto.SessionStatusResponse;
import com.osc.session_proto.UserSessionRequest;
import com.springosc.user.dao.SessionRepository;
import com.springosc.user.dao.UserRepository;
import com.springosc.user.dto.LoginRequestDTO;
import com.springosc.user.dto.LoginResponseDTO;
import com.springosc.user.dto.LogoutDTO;
import com.springosc.user.dto.SessionEventDTO;
import com.springosc.user.entity.Session;
import com.springosc.user.entity.User;
import com.springosc.user.gRPCRequest.GrpcClientRequest;
import com.springosc.user.kafkaProducer.KafkaPublisher;
import com.springosc.user.mapper.DTOMapper;
import com.springosc.user.service.LoginLogoutService;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class LoginLogoutServiceImpl implements LoginLogoutService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final KafkaPublisher kafkaPublisher;
    private final DTOMapper dtoMapper;
    private final GrpcClientRequest grpcClientRequest;
    private final SaveViewedProductsGrpc.SaveViewedProductsBlockingStub saveViewedProductsBlockingStub;

    private final SaveCartAddedProductsGrpc.SaveCartAddedProductsBlockingStub saveCartAddedProductsBlockingStub;

    public LoginLogoutServiceImpl(
            UserRepository userRepository,
            SessionRepository sessionRepository, KafkaPublisher kafkaPublisher, DTOMapper dtoMapper, GrpcClientRequest grpcClientRequest,
            @GrpcClient("product") SaveViewedProductsGrpc.SaveViewedProductsBlockingStub saveViewedProductsBlockingStub,
            @GrpcClient("product") SaveCartAddedProductsGrpc.SaveCartAddedProductsBlockingStub saveCartAddedProductsBlockingStub
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.kafkaPublisher = kafkaPublisher;
        this.dtoMapper = dtoMapper;
        this.grpcClientRequest = grpcClientRequest;
        this.saveViewedProductsBlockingStub = saveViewedProductsBlockingStub;
        this.saveCartAddedProductsBlockingStub = saveCartAddedProductsBlockingStub;
    }

    @Override
    public int checkCredentials(String userId, String password) {
        try {
            Optional<User> userOptional = userRepository.findByUserId(userId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if (user.getPassword().equals(password)) {
                    return 200;
                } else {
                    return 202;
                }
            } else {
                return 201;
            }
        } catch (Exception exception) {
            return 0;
        }
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        LoginResponseDTO loginResponseDTO = new LoginResponseDTO();
        try {
            SessionEventDTO sessionEventDTO = dtoMapper.createSessionEventDTO(loginRequestDTO);
            kafkaPublisher.publishUserSession(sessionEventDTO, true);
            Session session = saveSession(loginRequestDTO.getUserId(), loginRequestDTO.getDevice());
            loginResponseDTO.setSessionId(session.getSessionId());
            loginResponseDTO.setName(session.getName());
            log.info("User is Logged in: {}", session);
        } catch (Exception e) {
            log.error("Error occurred during login: {}", e.getMessage());
        }
        return loginResponseDTO;
    }

    @Override
    public Session saveSession(String userId, String device) {
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow();
            Session session = dtoMapper.createSession(user, device);
            sessionRepository.save(session);
            log.info("Session saved successfully for user: {}", userId);
            return session;
        } catch (Exception e) {
            log.error("User not found for userId: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void logout(String userId, String sessionId) {
        try {
            Optional<Session> optionalSession = sessionRepository.findActiveSessionByUserIdAndSessionId(userId, sessionId);
            optionalSession.ifPresent(session -> {
                boolean isSessionActive = grpcClientRequest.getSessionStatus(userId, session.getDevice());
                if (isSessionActive) {
                    SessionEventDTO sessionEventDTO = new SessionEventDTO();
                    sessionEventDTO.setUserId(userId);
                    sessionEventDTO.setDevice(session.getDevice());
                    kafkaPublisher.publishUserSession(sessionEventDTO, false);
                    session.setLogoutTime(LocalDateTime.now());
                    sessionRepository.save(session);
                    log.info("User logged out: " + userId);
                    saveRecentlyViewedProducts(userId);
                    saveCartProducts(userId);
                }
            });
        } catch (Exception e) {
            log.error("Error occurred during logout: {}", e.getMessage());
        }
    }


    /*
=====================================================================================================================================================================
*/

    //PHASE 2
    @KafkaListener(topics = "logout-user", groupId = "logoutUser")
    public void receiveLogoutMessage(String message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            LogoutDTO logoutDTO = objectMapper.readValue(message, LogoutDTO.class);

            String userId = logoutDTO.getUserId();
            String sessionId = logoutDTO.getSessionId();

            logout(userId, sessionId);
        } catch (Exception e) {
            log.info("Error occurred during force full logout {}", e.getMessage());
        }
    }


/*
=====================================================================================================================================================================
*/

    //PHASE 3

    private void saveRecentlyViewedProducts(String userId){
        try {
            RecentlyViewedDataRequest request = RecentlyViewedDataRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            SaveResponse response = saveViewedProductsBlockingStub.saveRecentlyViewedData(request);
            log.info("Received gRPC response for saving recently viewed data: {}", response.getMessage());
        }catch (Exception e){
            log.error("Error occurred during gRPC call to saveRecentlyViewedData: {}", e.getMessage());
        }
    }



    private void saveCartProducts(String userId) {
        try {
            CartProductRequest request = CartProductRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            SaveCartProduct response = saveCartAddedProductsBlockingStub.saveCartProducts(request);
            log.info("Received gRPC response for saving Cart Added Products: {}", response.getMessage());
        } catch (Exception e) {
            log.error("Some Error Occurred: {}", e.getMessage());
        }
    }
}