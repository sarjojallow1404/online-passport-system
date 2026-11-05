package com.passport.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.passport.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    java.util.Optional<User> findByEmailIgnoreCase(String email);
}
