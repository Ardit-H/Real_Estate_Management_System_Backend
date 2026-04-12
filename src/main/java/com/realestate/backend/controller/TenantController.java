package com.realestate.backend.controller;

import com.realestate.backend.entity.tenant.TenantCompany;
import com.realestate.backend.repository.TenantCompanyRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantCompanyRepository repo;

    public TenantController(TenantCompanyRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public TenantCompany create(@RequestBody TenantCompany t) {
        return repo.save(t);
    }

    @GetMapping
    public List<TenantCompany> getAll() {
        return repo.findAll();
    }
}