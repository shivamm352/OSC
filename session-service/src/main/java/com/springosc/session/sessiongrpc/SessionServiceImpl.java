package com.springosc.session.sessiongrpc;

import com.osc.session_proto.SessionServiceGrpc;
import com.osc.session_proto.SessionStatusResponse;
import com.osc.session_proto.UserSessionRequest;
import com.springosc.session.kakastreamconfig.KafkaKTableImpl;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@GrpcService
public class SessionServiceImpl extends SessionServiceGrpc.SessionServiceImplBase {

    private final KafkaKTableImpl kafkaKTable;

    public SessionServiceImpl(KafkaKTableImpl kafkaKTable) {
        this.kafkaKTable = kafkaKTable;
    }

    @Override
    public void getSessionStatus(UserSessionRequest request, StreamObserver<SessionStatusResponse> responseObserver) {
        try {
            boolean isSessionActive = kafkaKTable.getSessionStatus(request.getUserId(), request.getDevice());

            if (isSessionActive) {
                SessionStatusResponse response = SessionStatusResponse.newBuilder()
                        .setIsSessionActive(true)
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                SessionStatusResponse response = SessionStatusResponse.newBuilder()
                        .setIsSessionActive(false)
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }

        } catch (Exception exception) {
            log.error("Error occurred during KTable interaction: ");
            SessionStatusResponse response = SessionStatusResponse.newBuilder()
                    .setIsSessionActive(false)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
