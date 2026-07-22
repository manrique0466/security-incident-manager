package com.securityincidentmanager.domain.repository;

import com.securityincidentmanager.domain.entity.Incident;
import com.securityincidentmanager.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Page<Incident> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Incident> findAllByReporterAndDeletedAtIsNull(User reporter, Pageable pageable);

    Page<Incident> findAllByAssignedAnalystAndDeletedAtIsNull(User analyst, Pageable pageable);

    Optional<Incident> findByIdAndDeletedAtIsNull(UUID id);
}