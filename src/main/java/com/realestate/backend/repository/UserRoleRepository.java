package com.realestate.backend.repository;

import com.realestate.backend.entity.auth.UserRole;
import com.realestate.backend.entity.auth.UserRolePK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRolePK> {}