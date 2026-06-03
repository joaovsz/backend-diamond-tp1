package com.diamond.leads.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.diamond.leads.domain.Lead;

public interface LeadRepository extends JpaRepository<Lead, UUID> {
}