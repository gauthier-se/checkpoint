package com.checkpoint.api.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.checkpoint.api.entities.Company;

/**
 * Repository for Company entity.
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    /**
     * Finds a company by its name (case-insensitive).
     *
     * @param name the company name
     * @return Optional containing the company if found
     */
    Optional<Company> findByNameIgnoreCase(String name);
}
