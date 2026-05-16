package com.realestate.backend.repository;

import com.realestate.backend.entity.InviteToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InviteRepository extends JpaRepository<InviteToken, Long> {

    Optional<InviteToken> findByToken(String token);
}