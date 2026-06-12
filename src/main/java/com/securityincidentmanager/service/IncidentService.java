package com.securityincidentmanager.service;

import com.securityincidentmanager.domain.entity.Incident;
import com.securityincidentmanager.domain.entity.User;
import com.securityincidentmanager.domain.repository.IncidentRepository;
import com.securityincidentmanager.domain.repository.UserRepository;
import com.securityincidentmanager.dto.request.IncidentCreateRequest;
import com.securityincidentmanager.dto.request.IncidentUpdateRequest;
import com.securityincidentmanager.dto.response.IncidentResponse;
import com.securityincidentmanager.exception.ResourceNotFoundException;
import com.securityincidentmanager.mapper.IncidentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private static final String USER_NOT_FOUND = "User not found: ";
    private static final String INCIDENT_NOT_FOUND = "Incident not found: ";

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final IncidentMapper incidentMapper;

    public IncidentResponse create(IncidentCreateRequest request, UUID reporterId) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + reporterId));
        Incident incident = incidentMapper.toEntity(request);
        incident.setReporter(reporter);
        incident.setStatus(Incident.Status.OPEN);
        return incidentMapper.toResponse(incidentRepository.save(incident));
    }

    public IncidentResponse getById(UUID id) {
        Incident incident = incidentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(INCIDENT_NOT_FOUND + id));
        return incidentMapper.toResponse(incident);
    }

    public List<IncidentResponse> getAll(Pageable pageable) {
        return incidentRepository.findAllByDeletedAtIsNull(pageable)
                .getContent()
                .stream()
                .map(incidentMapper::toResponse)
                .toList();
    }

    public List<IncidentResponse> getByReporter(UUID reporterId) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + reporterId));
        return incidentRepository.findAllByReporterAndDeletedAtIsNull(reporter)
                .stream()
                .map(incidentMapper::toResponse)
                .toList();
    }

    public List<IncidentResponse> getByAnalyst(UUID analystId) {
        User analyst = userRepository.findById(analystId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + analystId));
        return incidentRepository.findAllByAssignedAnalystAndDeletedAtIsNull(analyst)
                .stream()
                .map(incidentMapper::toResponse)
                .toList();
    }

    public IncidentResponse update(UUID id, IncidentUpdateRequest request) {
        Incident incident = incidentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(INCIDENT_NOT_FOUND + id));
        if (request.getTitle() != null) {
            incident.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            incident.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            incident.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            incident.setStatus(request.getStatus());
        }
        if (request.getAssignedAnalystId() != null) {
            User analyst = userRepository.findById(request.getAssignedAnalystId())
                    .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + request.getAssignedAnalystId()));
            incident.setAssignedAnalyst(analyst);
        }
        return incidentMapper.toResponse(incidentRepository.save(incident));
    }

    public void softDelete(UUID id) {
        Incident incident = incidentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(INCIDENT_NOT_FOUND + id));
        incident.setDeletedAt(LocalDateTime.now());
        incidentRepository.save(incident);
    }
}
