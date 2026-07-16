package com.diamond.leads.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.diamond.leads.domain.LeadHistoryEntry;

public interface LeadHistoryRepository extends JpaRepository<LeadHistoryEntry, UUID> {

    List<LeadHistoryEntry> findByLeadIdOrderByTimestampDesc(UUID leadId);

    List<LeadHistoryEntry> findByStatusIgnoreCase(String status);

    @Query("select h from LeadHistoryEntry h where h.timestamp between :start and :end order by h.timestamp desc")
    List<LeadHistoryEntry> findByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
