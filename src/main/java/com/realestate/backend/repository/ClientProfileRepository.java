package com.realestate.backend.repository;

import com.realestate.backend.entity.profile.ClientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientProfileRepository extends JpaRepository<ClientProfile, Long> {

    Optional<ClientProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
