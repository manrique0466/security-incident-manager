package com.securityincidentmanager.domain.repository;

import com.securityincidentmanager.domain.entity.Incident;
import com.securityincidentmanager.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Page<Incident> findAllByDeletedAtIsNull(Pageable pageable);

    List<Incident> findAllByReporterAndDeletedAtIsNull(User reporter);

    List<Incident> findAllByAssignedAnalystAndDeletedAtIsNull(User analyst);

    Optional<Incident> findByIdAndDeletedAtIsNull(UUID id);
}