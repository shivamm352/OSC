package com.springosc.user.dao;

import com.springosc.user.dto.SaveUserDTO;
import com.springosc.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUserId(String userId);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email")
    boolean validateEmail(@Param("email") String email);

    Optional<User> findByEmail(String email);
}
