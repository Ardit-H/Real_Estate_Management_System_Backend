package com.realestate.backend.repository;

import com.realestate.backend.entity.tenant.TenantCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantCompanyRepository extends JpaRepository<TenantCompany, Long> {
}