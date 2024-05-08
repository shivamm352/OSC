package com.springosc.cache.configuration;

import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
//public class GrpcServerConfig {
//
//    @Value("${grpc.server.max-inbound-message-size}")
//    private int maxInboundMessageSize;
//
//    @Bean
//    public NettyServerBuilder nettyServerBuilder() {
//        return NettyServerBuilder.forPort(9090)
//                .maxInboundMessageSize(maxInboundMessageSize);
//    }
//
//}
