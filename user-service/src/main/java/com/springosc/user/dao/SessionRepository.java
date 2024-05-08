package com.springosc.user.dao;

import com.springosc.user.entity.Session;
import com.springosc.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Integer> {

    Optional<Session> findActiveSessionByUserIdAndSessionId(String userId, String sessionId);

    Optional<Session> findBySessionId(String SessionId);

}
