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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IncidentMapper incidentMapper;

    @InjectMocks
    private IncidentService incidentService;

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_shouldReturnResponse_whenReporterExists() {
        UUID reporterId = UUID.randomUUID();
        User reporter = new User();
        reporter.setId(reporterId);

        IncidentCreateRequest request = new IncidentCreateRequest();
        request.setTitle("Server down");
        request.setPriority(Incident.Priority.HIGH);

        Incident incident = new Incident();
        IncidentResponse expectedResponse = new IncidentResponse();

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(incidentMapper.toEntity(request)).thenReturn(incident);
        when(incidentRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toResponse(incident)).thenReturn(expectedResponse);

        IncidentResponse result = incidentService.create(request, reporterId);

        assertThat(result).isEqualTo(expectedResponse);
        assertThat(incident.getReporter()).isEqualTo(reporter);
        assertThat(incident.getStatus()).isEqualTo(Incident.Status.OPEN);
        verify(incidentRepository).save(incident);

    }

    @Test
    void create_shouldThrowException_whenReporterNotFound() {
        UUID reporterId = UUID.randomUUID();
        IncidentCreateRequest request = new IncidentCreateRequest();

        when(userRepository.findById(reporterId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.create(request, reporterId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_shouldReturnResponse_whenIncidentExists() {
        UUID id = UUID.randomUUID();
        Incident incident = new Incident();
        IncidentResponse expectedResponse = new IncidentResponse();

        when(incidentRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(incident));
        when(incidentMapper.toResponse(incident)).thenReturn(expectedResponse);

        IncidentResponse result = incidentService.getById(id);

        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void getById_shouldThrowException_whenIncidentNotFound() {
        UUID id = UUID.randomUUID();

        when(incidentRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    void getAll_shouldReturnMappedList() {
        Pageable pageable = Pageable.unpaged();
        Incident incident = new Incident();
        IncidentResponse response = new IncidentResponse();
        Page<Incident> page = new PageImpl<>(List.of(incident));

        when(incidentRepository.findAllByDeletedAtIsNull(pageable)).thenReturn(page);
        when(incidentMapper.toResponse(incident)).thenReturn(response);

        List<IncidentResponse> result = incidentService.getAll(pageable);

        assertThat(result).hasSize(1).containsExactly(response);
    }

    // ── getByReporter ─────────────────────────────────────────────────────────

    @Test
    void getByReporter_shouldReturnList_whenReporterExists() {
        UUID reporterId = UUID.randomUUID();
        User reporter = new User();
        reporter.setId(reporterId);
        Incident incident = new Incident();
        IncidentResponse response = new IncidentResponse();

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(incidentRepository.findAllByReporterAndDeletedAtIsNull(reporter)).thenReturn(List.of(incident));
        when(incidentMapper.toResponse(incident)).thenReturn(response);

        List<IncidentResponse> result = incidentService.getByReporter(reporterId);

        assertThat(result).hasSize(1).containsExactly(response);
    }

    @Test
    void getByReporter_shouldThrowException_whenReporterNotFound() {
        UUID reporterId = UUID.randomUUID();

        when(userRepository.findById(reporterId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.getByReporter(reporterId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getByAnalyst ──────────────────────────────────────────────────────────

    @Test
    void getByAnalyst_shouldReturnList_whenAnalystExists() {
        UUID analystId = UUID.randomUUID();
        User analyst = new User();
        analyst.setId(analystId);
        Incident incident = new Incident();
        IncidentResponse response = new IncidentResponse();

        when(userRepository.findById(analystId)).thenReturn(Optional.of(analyst));
        when(incidentRepository.findAllByAssignedAnalystAndDeletedAtIsNull(analyst)).thenReturn(List.of(incident));
        when(incidentMapper.toResponse(incident)).thenReturn(response);

        List<IncidentResponse> result = incidentService.getByAnalyst(analystId);

        assertThat(result).hasSize(1).containsExactly(response);
    }

    @Test
    void getByAnalyst_shouldThrowException_whenAnalystNotFound() {
        UUID analystId = UUID.randomUUID();

        when(userRepository.findById(analystId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.getByAnalyst(analystId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_shouldApplyOnlyNonNullFields() {
        UUID id = UUID.randomUUID();
        Incident incident = new Incident();
        incident.setTitle("Original title");
        incident.setDescription("Original description");
        incident.setPriority(Incident.Priority.LOW);
        incident.setStatus(Incident.Status.OPEN);

        IncidentUpdateRequest request = new IncidentUpdateRequest();
        request.setTitle("Updated title");
        request.setStatus(Incident.Status.IN_PROGRESS);
        // description, priority, assignedAnalystId intentionally left null

        when(incidentRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);
        when(incidentMapper.toResponse(any(Incident.class))).thenReturn(new IncidentResponse());

        incidentService.update(id, request);

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        Incident saved = captor.getValue();

        assertThat(saved.getTitle()).isEqualTo("Updated title");
        assertThat(saved.getDescription()).isEqualTo("Original description");
        assertThat(saved.getPriority()).isEqualTo(Incident.Priority.LOW);
        assertThat(saved.getStatus()).isEqualTo(Incident.Status.IN_PROGRESS);
    }

    @Test
    void update_shouldThrowException_whenIncidentNotFound() {
        UUID id = UUID.randomUUID();

        when(incidentRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.update(id, new IncidentUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_shouldAssignAnalyst_whenAssignedAnalystIdProvided() {
        UUID id = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        User analyst = new User();
        analyst.setId(analystId);
        Incident incident = new Incident();

        IncidentUpdateRequest request = new IncidentUpdateRequest();
        request.setAssignedAnalystId(analystId);

        when(incidentRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(incident));
        when(userRepository.findById(analystId)).thenReturn(Optional.of(analyst));
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);
        when(incidentMapper.toResponse(any(Incident.class))).thenReturn(new IncidentResponse());

        incidentService.update(id, request);

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        assertThat(captor.getValue().getAssignedAnalyst()).isEqualTo(analyst);
    }

    // ── softDelete ────────────────────────────────────────────────────────────

    @Test
    void softDelete_shouldSetDeletedAt_whenIncidentExists() {
        UUID id = UUID.randomUUID();
        Incident incident = new Incident();

        when(incidentRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(incident));

        incidentService.softDelete(id);

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void softDelete_shouldThrowException_whenIncidentNotFound() {
        UUID id = UUID.randomUUID();

        when(incidentRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.softDelete(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}